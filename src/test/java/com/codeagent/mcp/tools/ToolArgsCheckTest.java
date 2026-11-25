package com.codeagent.mcp.tools;

import com.codeagent.mcp.findings.FindingStore;
import com.codeagent.mcp.workspace.RepositoryRegistry;
import com.codeagent.mcp.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 에이전트가 자주 틀리는 인자를 한 번에 알려 주는지 */
class ToolArgsCheckTest {

    @TempDir
    Path home;

    @Test
    void 비슷한_이름의_인자를_알려_준다() {
        assertThatThrownBy(() -> call("submit_verdict", Map.of("repo", "r", "finding_id", "F1", "verdict", "confirmed", "reason", "근거")))
                .isInstanceOf(ToolException.class)
                .hasMessageContaining("reason: 이 도구의 인자가 아님. reasoning 을(를) 말한 건가요?")
                .hasMessageContaining("reasoning: 꼭 넣어야 함 (판정 근거")
                .hasMessageContaining("reasoning (필수)");
    }

    @Test
    void 모르는_인자를_조용히_무시하지_않는다() {
        assertThatThrownBy(() -> call("grep_code", Map.of("repo", "r", "pattern", "token", "glob", "**.py")))
                .hasMessageContaining("glob: 이 도구의 인자가 아님. file_glob 을(를) 말한 건가요?");
    }

    @Test
    void 배열_안_객체의_필드와_허용_값을_한_번에_알려_준다() {
        assertThatThrownBy(() -> call("record_finding", Map.of(
                "repo", "r", "title", "t", "cwe", "CWE-89", "severity", "urgent", "description", "d",
                "evidence", List.of(Map.of("path", "A.java", "line", "열둘")))))
                .hasMessageContaining("confidence: 꼭 넣어야 함")
                .hasMessageContaining("(받은 값: urgent)")
                .hasMessageContaining("evidence[0].path: 이 도구의 인자가 아님")
                .hasMessageContaining("evidence[0].file: 꼭 넣어야 함")
                .hasMessageContaining("evidence[0].line: 정수여야 함")
                .hasMessageContaining("\"file\": \"…\"");
    }

    @Test
    void 맞는_인자는_그대로_통과한다() {
        assertThat(ToolArgsCheck.closest("filePattern", List.of("repo", "pattern", "file_glob"))).isEqualTo("pattern");
        assertThatThrownBy(() -> call("list_findings", Map.of("repo", "nothing")))
                .hasMessageNotContaining("인자를 고쳐서");
    }

    private String call(String name, Map<String, Object> args) {
        Workspace workspace = new Workspace(home, List.of("github.com"), 50L * 1024 * 1024, 30);
        CodeAgentTools tools = new CodeAgentTools(workspace, new RepositoryRegistry(workspace), new FindingStore(workspace.findings()));
        ToolDef tool = tools.all().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
        return tool.handler().apply(new Args(args));
    }
}
