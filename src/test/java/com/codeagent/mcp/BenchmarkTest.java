package com.codeagent.mcp;

import com.codeagent.mcp.findings.FindingStore;
import com.codeagent.mcp.tools.Args;
import com.codeagent.mcp.tools.CodeAgentTools;
import com.codeagent.mcp.tools.ToolDef;
import com.codeagent.mcp.workspace.RepositoryRegistry;
import com.codeagent.mcp.workspace.Workspace;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 평가 벤치마크(benchmark/)의 정해진 부분. 정답 파일의 취약점과 함정이 모두 list_sinks 에 나오고(조사 대상에 오르고),
 * 진입점이 있는 취약점은 list_sink_paths 에도 나오는지 확인한다. LLM 의 판단은 benchmark/score.py 로 따로 잰다.
 * 앱마다(Java/Spring shop-app, Node.js/Express node-shop) 같은 검사를 한다.
 */
class BenchmarkTest {

    private static final Pattern SINK_LINE = Pattern.compile("\\[([A-Z_]+) CWE-\\d+\\].* · (\\S+:\\d+)$");

    @TempDir
    Path home;

    private CodeAgentTools tools;
    private JsonNode expected;

    private void load(String app) throws Exception {
        Workspace workspace = new Workspace(home, List.of("github.com"), 50L * 1024 * 1024, 30);
        tools = new CodeAgentTools(workspace, new RepositoryRegistry(workspace), new FindingStore(workspace.findings()));
        call("load_repository", Map.of("path", Path.of("benchmark", app).toAbsolutePath().toString()));
        expected = JsonMapper.builder().build().readTree(Files.readString(Path.of("benchmark", app + ".expected.json")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"shop-app", "node-shop", "go-shop", "php-shop", "ruby-shop"})
    void 정답의_취약점과_함정이_모두_위험_지점으로_나오고_다른_것은_없다(String app) throws Exception {
        load(app);
        List<String> answer = new ArrayList<>();
        expected.path("vulnerabilities").forEach(i -> answer.add(key(i)));
        expected.path("safe").forEach(i -> answer.add(key(i)));

        List<String> sinks = new ArrayList<>();
        for (String line : call("list_sinks", Map.of("repo", app)).split("\n")) {
            Matcher m = SINK_LINE.matcher(line.strip());
            if (m.find()) {
                sinks.add(m.group(2) + " " + m.group(1));
            }
        }

        assertThat(sinks).containsExactlyInAnyOrderElementsOf(answer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"shop-app", "node-shop", "go-shop", "php-shop", "ruby-shop"})
    void 진입점이_있는_취약점은_경로로도_나온다(String app) throws Exception {
        load(app);
        String paths = call("list_sink_paths", Map.of("repo", app));
        for (JsonNode vuln : expected.path("vulnerabilities")) {
            if (!vuln.path("entryPoint").isNull()) {
                assertThat(paths).as(vuln.path("id").asString())
                        .contains(vuln.path("entryPoint").asString() + "  →  [" + vuln.path("category").asString());
            }
        }
        // 분석 대상 폴더 안에 정답이 없어야 한다
        assertThat(Path.of("benchmark", app, "expected.json")).doesNotExist();
        assertThat(Path.of("benchmark", app, app + ".expected.json")).doesNotExist();
    }

    private static String key(JsonNode item) {
        return item.path("file").asString() + ":" + item.path("sinkLine").asInt() + " " + item.path("category").asString();
    }

    private String call(String name, Map<String, Object> args) {
        ToolDef tool = tools.all().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
        return tool.handler().apply(new Args(args));
    }
}
