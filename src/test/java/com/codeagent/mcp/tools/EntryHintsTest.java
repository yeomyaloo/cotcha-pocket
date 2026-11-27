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

    @Test
    void Go_처리기는_마지막_폴더부터_보여주고_grep_기본_대상은_go_다() {
        Node node = entry(Map.of("language", "go"));
        assertThat(CodeAgentTools.shortName(node, "internal/handlers#Handler.Search")).isEqualTo("handlers#Handler.Search");
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("go"))).isEqualTo("**.go");
    }

    @Test
    void PHP_처리기는_클래스_이름부터_보여주고_grep_기본_대상은_php_다() {
        Node node = entry(Map.of("language", "php"));
        assertThat(CodeAgentTools.shortName(node, "App\\Http\\Controllers\\UserController#show")).isEqualTo("UserController#show");
        assertThat(CodeAgentTools.shortName(node, "public/search.php#<main>")).isEqualTo("search.php#<main>");
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("php"))).isEqualTo("**.php");
    }

    @Test
    void Ruby_처리기는_클래스_이름부터_보여주고_grep_기본_대상은_rb_다() {
        Node node = entry(Map.of("language", "ruby"));
        assertThat(CodeAgentTools.shortName(node, "Api::Admin::ToolsController#export")).isEqualTo("ToolsController#export");
        assertThat(CodeAgentTools.shortName(node, "sinatra/app.rb#GET /fetch")).isEqualTo("app.rb#GET /fetch");
        assertThat(CodeAgentTools.shortName(node, "ProductRepository#search")).isEqualTo("ProductRepository#search");
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("ruby"))).isEqualTo("**.rb");
    }

    @Test
    void CSharp_처리기는_클래스나_파일_이름부터_보여주고_grep_기본_대상은_cs_다() {
        Node node = entry(Map.of("language", "csharp"));
        assertThat(CodeAgentTools.shortName(node, "CsShop.Controllers.AuthController#Login")).isEqualTo("AuthController#Login");
        assertThat(CodeAgentTools.shortName(node, "src/Api/Program.cs#GET /health")).isEqualTo("Program.cs#GET /health");
        assertThat(CodeAgentTools.defaultGlob(java.util.Set.of("csharp"))).isEqualTo("**.cs");
    }

    private static Node entry(Map<String, Object> props) {
        return new Node(1, CodeNodeKind.ENTRY_POINT, "GET /x -> h", "a.py", 1, props);
    }
}
