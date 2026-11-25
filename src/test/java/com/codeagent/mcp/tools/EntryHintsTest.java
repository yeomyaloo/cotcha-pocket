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

    private static Node entry(Map<String, Object> props) {
        return new Node(1, CodeNodeKind.ENTRY_POINT, "GET /x -> h", "a.py", 1, props);
    }
}
