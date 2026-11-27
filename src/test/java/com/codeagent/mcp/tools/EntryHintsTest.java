package com.codeagent.mcp.tools;

import com.codeagent.core.model.CodeNodeKind;
import com.codeagent.mcp.codegraph.CodeGraph.Node;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EntryHintsTest {

    @Test
    void Flask_Django_진입점은_인증_단서를_보여준다() {
        assertThat(CodeAgentTools.dependencies(entry(Map.of("guards", List.of("login_required", "before_request:check")))))
                .isEqualTo(" · 인증 단서: login_required, before_request:check");
        assertThat(CodeAgentTools.dependencies(entry(Map.of("guards", List.of()))))
                .isEqualTo(" · 인증 단서 없음(전역 설정은 따로 확인)");
    }

    @Test
    void FastAPI_는_Depends_를_Java_는_아무것도_보여주지_않는다() {
        assertThat(CodeAgentTools.dependencies(entry(Map.of("dependencies", List.of(), "language", "python"))))
                .isEqualTo(" · Depends 없음");
        assertThat(CodeAgentTools.dependencies(entry(Map.of()))).isEmpty();
    }

    @Test
    void Node_처리기는_파일_이름부터_보여준다() {
        Node node = entry(Map.of("language", "node"));
        assertThat(CodeAgentTools.shortName(node, "src/routes/products.js#GET /search")).isEqualTo("products.js#GET /search");
        assertThat(CodeAgentTools.shortName(node, "src/users/users.controller.ts#UsersController.find"))
                .isEqualTo("users.controller.ts#UsersController.find");
    }

    @Test
    void Node_저장소는_grep_기본_대상이_JS_TS_다() {
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("node", "javascript"))).isEqualTo("**.{js,ts,mjs,cjs}");
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("java", "node"))).isEqualTo("**.{java,js,ts,mjs,cjs}");
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("javascript"))).isEqualTo("**.java");
    }

    private static Node entry(Map<String, Object> props) {
        return new Node(1, CodeNodeKind.ENTRY_POINT, "GET /x -> h", "a.py", 1, props);
    }
}
