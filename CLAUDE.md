# cotcha-pocket

채팅 앱(Claude Code, Claude Desktop)에 연결하는 stdio MCP 서버. Claude가 판단하고, 이 서버는 코드 그래프(Java/Spring, Python/FastAPI, 프론트엔드 JS/TS)·파일 읽기·발견 기록·보고서를 맡는다. 코드 그래프 엔진은 공용 라이브러리 cotcha-trail(`../cotcha-trail`, 패키지 `com.codeagent.core`)를 cotcha-net 와 같이 쓴다.

## 명령

```bash
./gradlew test          # 테스트 (DB·네트워크·API 키 불필요)
./gradlew installDist   # build/install/cotcha-pocket/bin/cotcha-pocket(.bat)
./gradlew pluginServer  # Claude Code 플러그인용 server/lib (커밋 대상)
claude plugin validate . # 플러그인 구조 검사 (CLAUDE.md 경고 하나는 정상)
python -m unittest discover -s benchmark   # 벤치마크 채점 스크립트 테스트
```

- 빌드는 JDK 21 toolchain, 바이트코드는 `release 17`. 채팅 앱이 PATH의 java(17일 수 있음)로 띄우므로 Java 21 전용 API(`getFirst`, `addFirst` 등)를 쓰지 않는다.
- Windows PowerShell에서는 `.\gradlew.bat`.

## 구조

- `McpCodeAgent`: 서버 진입점. 도구는 `tools/CodeAgentTools.all()`, 프롬프트는 `Prompts`.
- 새 도구는 `CodeAgentTools`에 `ToolDef`를 추가하고 `all()`에 넣는다. 입력 오류는 `ToolException`으로 던진다(isError 결과로 Claude에게 전달됨).
- 파일 접근은 반드시 `RepoFiles.resolve()`로 경로를 검사한다(저장소 밖 차단).
- 평가 하네스 `benchmark/` 는 cotcha-net 의 것과 같다(run.py 제외). 정답 파일은 분석 대상 폴더 밖에 두고, 앱에 힌트 주석을 달지 않는다. 고치면 두 저장소를 같이 맞춘다.
- 코드 그래프 엔진(파싱, 진입점 탐지, 위험 지점 규칙)은 cotcha-trail 에 있다. 이 저장소의 `codegraph/CodeGraph`는 `CodeGraphEngine.analyze()` 결과를 메모리 색인으로 옮기는 어댑터다. 규칙을 고치려면 core 를 고치고 버전을 올린 뒤 `build.gradle` 의존 버전을 맞춘다.
- core 를 소스로 바로 쓰려면 `-PcoreCodeAgentDir=../cotcha-trail`(composite build). 없으면 GitHub Packages 에서 받으므로 `gpr.user`/`gpr.key`(또는 `GITHUB_ACTOR`/`GITHUB_TOKEN`, `read:packages`)가 필요하다.
- `source/`는 cotcha-net 에서 복사했다. 고치면 두 저장소를 같이 맞춘다.

## 규칙

- **표준 출력은 MCP 메시지 전용이다.** `System.out`에 쓰지 않는다. 로그는 SLF4J(slf4j-simple, 표준 오류)로만.
- 도구 설명, 결과 문구, 주석, 테스트 메서드 이름은 한국어.
- 분석 대상 코드는 읽기만 한다. 빌드하거나 실행하지 않는다.
- 도구나 프롬프트를 바꾸면 README의 도구 표, 프롬프트 표도 갱신한다.
- 이 저장소는 Claude Code 플러그인이자 마켓플레이스다(`.claude-plugin/plugin.json`, `marketplace.json`, `skills/security-audit`). 서버 코드를 바꾸면 `./gradlew pluginServer`로 `server/lib`를 다시 만들어 커밋하고 `plugin.json`의 `version`을 올린다. 의존성을 바꾸면 `server/THIRD-PARTY.md`도 갱신한다.
- README는 영어 먼저, `---` 아래에 한국어. 둘 다 같이 고친다.
- 작업이 끝나고 `./gradlew test`가 통과하면 커밋하고 푸시해도 된다.
- 사용자에게 설명과 요약은 한국어로 한다.
