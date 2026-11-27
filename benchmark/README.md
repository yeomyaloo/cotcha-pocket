# Benchmark

An evaluation harness that measures how well an audit with this plugin finds vulnerabilities, and whether repeated audits of the same code reach the same conclusions. The same benchmark lives in cotcha-net, where `run.py` drives the backend; here you run the audit in Claude Code and score the exported SARIF.

| File | What it is |
|---|---|
| `shop-app/` | A small Spring + React shop with **11 real vulnerabilities** and **10 safe look-alikes** (traps): each vulnerable spot has a safe twin of the same category. There are no hint comments in the code. |
| `node-shop/` | A Node.js (Express) shop API with **12 real vulnerabilities** and **9 safe look-alikes**, including Node-specific ones (NoSQL operator injection, prototype pollution, `node-serialize`). No hint comments either. |
| `node-shop.expected.json` | Its answer key, in the same format and also outside the app folder. |
| `go-shop/` | A Go (gin + database/sql) shop API with **11 real vulnerabilities** and **8 safe look-alikes**, including Go-specific ones (`template.HTML`, template injection, `InsecureSkipVerify`, interface calls into the store). |
| `go-shop.expected.json` | Its answer key. |
| `php-shop/` | A PHP (Laravel) shop API with **11 real vulnerabilities** and **8 safe look-alikes**, including PHP-specific ones (`unserialize`, XXE with `LIBXML_NOENT`, `eval`, calls through a repository interface). |
| `php-shop.expected.json` | Its answer key. |
| `ruby-shop/` | A Ruby (Rails) shop API with **11 real vulnerabilities** and **7 safe look-alikes**, including Ruby-specific ones (`Kernel#open` pipe, `Marshal.load`, ERB template injection, `html_safe`, `allow_other_host: true`). |
| `ruby-shop.expected.json` | Its answer key. |
| `cs-shop/` | A C# (ASP.NET Core) shop API with **11 real vulnerabilities** and **8 safe look-alikes**, including .NET-specific ones (`FromSqlRaw` vs `FromSqlInterpolated`, JSON.NET `TypeNameHandling`, `XmlUrlResolver`, `CSharpScript`, `Redirect` vs `LocalRedirect`). |
| `cs-shop.expected.json` | Its answer key. |
| `shop-app.expected.json` | The answer key: file, line range, sink line, accepted CWEs and entry point of each item. It lives **outside** `shop-app/` so Claude cannot read it while auditing. |
| `score.py` | Scores one or more SARIF reports against the answer key: recall, precision, trap false positives, wrong CWE, duplicates and, for several runs, consistency. |
| `test_score.py` | Tests for the scoring rules. |

`BenchmarkTest` (part of `./gradlew test`, runs in CI) checks, for every app, that every vulnerability and trap shows up in `list_sinks`, nothing else does, and each vulnerability with an entry point shows up in `list_sink_paths`.

## Scoring an audit (manual)

1. In Claude Code, start a fresh session for each run and ask: "Audit `<absolute path>/benchmark/shop-app` for security vulnerabilities". Approve the plan.
2. At the end, have Claude call `export_report` with `format: sarif` and save it as `report-1.sarif`. Run `clear_findings` before the next run so findings don't carry over.
3. Score:

```bash
python benchmark/score.py benchmark/shop-app.expected.json report-1.sarif report-2.sarif report-3.sarif
python benchmark/score.py benchmark/shop-app.expected.json report-*.sarif --confirmed-only
```

Rules: a finding points at an item when one of its locations is inside the item's file and line range. With an accepted CWE it is a hit, with another CWE a wrong category. Pointing at a trap is a trap false positive; pointing at nothing is another false positive. Rejected findings are ignored. Consistency is the share of items that got the same outcome in every run, plus the mean Jaccard similarity between runs.

## Results: node-shop (blind)

Three blind runs (plugin 0.21.0, cotcha-trail 0.18.0). Each run was a separate agent that audited a copy of `node-shop/` with the plugin tools and could not read the answer key. `check_dependencies` was not used.

- Recall 100%, 100%, 100%. Precision 100%. Trap false positives 0/9 in every run. The same outcome for 21/21 items, mean Jaccard 1.00.
- The first scoring found two bugs in the answer key, not in the audits, and they are fixed. All three runs reported the reset token as CWE-330, but the key accepted only CWE-338 (the shop-app key accepts 330/338/340). A trap's line range also covered the whole handler, so that wrong-CWE hit was counted as a trap.
- All three runs also noticed that lodash `^4.17.21` already blocks `__proto__` in `merge`, so the prototype-pollution item was only marked uncertain/low. `package.json` now pins lodash 4.17.4, where `merge` is exploitable (fixed in 4.17.5).

**Rerun on the fixed app (lodash 4.17.4):** three new blind runs under the same instructions. Recall 100%, 100%, 100%. Precision 100%. Trap false positives 0/9 in every run. All 12 findings in each run were confirmed, and prototype pollution is now confirmed as high instead of left uncertain. The same outcome for 21/21 items, mean Jaccard 1.00 (also with `--confirmed-only`).


## Results: go-shop (blind)

Three blind runs (plugin 0.22.0, cotcha-trail 0.19.0), same instructions as node-shop. Recall 100%, 100%, 100%. Precision 100%. Trap false positives 0/8 in every run. All 11 findings confirmed in each run, the same outcome for 19/19 items, mean Jaccard 1.00. Every run followed the search query through the `Store` interface into the SQL string and judged the payment-status request safe because its host comes from config and the id is escaped. Two runs lowered the `template.HTML` XSS to low because the route needs a Bearer header, which a browser following a link does not send.

## Results: php-shop (blind)

Three blind runs (plugin 0.23.0, cotcha-trail 0.20.0), same instructions as the other apps. Recall 100%, 100%, 100%. Precision 100%. Trap false positives 0/8 in every run. All 11 findings confirmed in each run, the same outcome for 19/19 items, mean Jaccard 1.00. Every run followed the search query through the repository interface into the SQL string, noted that `unserialize` had no `allowed_classes` limit, and judged the `escapeshellarg` + allow-list report command safe. No run tripped over `reason`/`reasoning` (accepted as an alias since 0.22.1).

## Results: ruby-shop (blind)

Three blind runs (plugin 0.24.0, cotcha-trail 0.21.0), same instructions as the other apps. Recall 100%, 100%, 100%. Precision 100%. Trap false positives 0/7 in every run. All 11 findings confirmed in each run, the same outcome for 18/18 items, mean Jaccard 1.00. Every run recognized the `Kernel#open` pipe (`|cmd`) as command injection, noted that `allow_other_host: true` turns off Rails 7's redirect protection, and judged the allow-listed `system('pg_dump', ...)` with separate arguments safe.

---

# 벤치마크

이 플러그인으로 한 보안 분석이 취약점을 얼마나 잘 찾는지, 같은 코드를 다시 분석해도 같은 결론을 내는지 재는 평가 하네스다. cotcha-net 에도 같은 벤치마크가 있고 거기서는 `run.py` 가 백엔드를 돌린다. 여기서는 Claude Code 에서 분석하고 내보낸 SARIF 를 채점한다.

| 파일 | 내용 |
|---|---|
| `shop-app/` | 작은 Spring + React 쇼핑몰. **진짜 취약점 11개**와 비슷하게 생겼지만 **안전한 코드 10개**(함정)를 섞었다. 취약한 곳마다 같은 분류의 안전한 짝이 있다. 코드에 힌트 주석은 없다. |
| `node-shop/` | Node.js(Express) 쇼핑몰 API. **진짜 취약점 12개**와 **안전한 함정 9개**, Node 에만 있는 것(NoSQL 연산자 주입, 프로토타입 오염, `node-serialize`)도 들어 있다. 힌트 주석은 없다. |
| `node-shop.expected.json` | 그 정답 파일. 형식이 같고 역시 앱 폴더 밖에 둔다. |
| `go-shop/` | Go(gin + database/sql) 쇼핑몰 API. **진짜 취약점 11개**와 **안전한 함정 8개**, Go 에만 있는 것(`template.HTML`, 템플릿 주입, `InsecureSkipVerify`, 저장소 인터페이스 호출)도 들어 있다. |
| `go-shop.expected.json` | 그 정답 파일. |
| `php-shop/` | PHP(Laravel) 쇼핑몰 API. **진짜 취약점 11개**와 **안전한 함정 8개**, PHP 에만 있는 것(`unserialize`, `LIBXML_NOENT` XXE, `eval`, 저장소 인터페이스 호출)도 들어 있다. |
| `php-shop.expected.json` | 그 정답 파일. |
| `ruby-shop/` | Ruby(Rails) 쇼핑몰 API. **진짜 취약점 11개**와 **안전한 함정 7개**, Ruby 에만 있는 것(`Kernel#open` 파이프, `Marshal.load`, ERB 템플릿 주입, `html_safe`, `allow_other_host: true`)도 들어 있다. |
| `ruby-shop.expected.json` | 그 정답 파일. |
| `cs-shop/` | C#(ASP.NET Core) 쇼핑몰 API. **진짜 취약점 11개**와 **안전한 함정 8개**, .NET 에만 있는 것(`FromSqlRaw` 와 `FromSqlInterpolated`, JSON.NET `TypeNameHandling`, `XmlUrlResolver`, `CSharpScript`, `Redirect` 와 `LocalRedirect`)도 들어 있다. |
| `cs-shop.expected.json` | 그 정답 파일. |
| `shop-app.expected.json` | 정답 파일. 항목마다 파일, 줄 범위, 위험 지점 줄, 인정하는 CWE, 진입점을 적었다. Claude 가 분석 중에 읽지 못하도록 `shop-app/` **밖**에 둔다. |
| `score.py` | SARIF 보고서를 정답과 비교해 채점한다. 재현율, 정밀도, 함정 오탐, 분류 틀림, 중복, 그리고 여러 번 돌렸으면 일관성. |
| `test_score.py` | 채점 규칙 테스트. |

`BenchmarkTest`(`./gradlew test` 에 포함, CI 에서 돈다)는 앱마다 정답의 취약점과 함정이 모두 `list_sinks` 에 나오고 그 밖의 것은 없으며, 진입점이 있는 취약점은 `list_sink_paths` 에도 나오는지 확인한다.

## 분석 채점하기 (수동)

1. 실행마다 Claude Code 새 세션에서 "`<절대 경로>/benchmark/shop-app` 보안 취약점 분석해줘"라고 하고 계획을 승인한다.
2. 끝나면 `export_report` 를 `format: sarif` 로 불러 `report-1.sarif` 로 저장한다. 다음 실행 전에 `clear_findings` 로 발견을 비운다.
3. 채점한다.

```bash
python benchmark/score.py benchmark/shop-app.expected.json report-1.sarif report-2.sarif report-3.sarif
python benchmark/score.py benchmark/shop-app.expected.json report-*.sarif --confirmed-only
```

규칙: 발견의 위치 중 하나가 항목의 파일·줄 범위 안에 있으면 그 항목을 가리킨 것이다. CWE 가 인정 목록에 있으면 맞힌 것, 다르면 분류 틀림이다. 함정을 가리키면 함정 오탐, 아무것도 가리키지 않으면 기타 오탐이다. 기각된 발견은 빼고 센다. 일관성은 모든 실행에서 같은 결론이 난 항목의 비율과 실행 간 평균 자카드 유사도다.

## 결과: node-shop (블라인드)

블라인드로 3번 돌렸다(플러그인 0.21.0, cotcha-trail 0.18.0). 실행마다 다른 에이전트가 `node-shop/` 복사본을 플러그인 도구로 분석했고, 정답 파일은 읽을 수 없었다. `check_dependencies` 는 쓰지 않았다.

- 재현율 100%, 100%, 100%. 정밀도 100%. 함정 오탐은 매번 0/9. 21개 항목 모두 세 번 같은 결론, 평균 자카드 1.00.
- 처음 채점할 때 분석이 아니라 정답 파일에서 버그 두 개가 나와서 고쳤다. 세 번 모두 재설정 토큰을 CWE-330 으로 보고했는데, 정답은 CWE-338 만 인정했다(shop-app 정답은 330·338·340 을 인정한다). 또 함정 하나의 줄 범위가 처리기 전체를 덮어서, CWE 가 다른 그 발견이 함정 오탐으로 잡혔다.
- 세 번 모두 lodash `^4.17.21` 은 `merge` 에서 `__proto__` 를 이미 막는다고 짚어서, 프로토타입 오염 항목을 불확실·낮음으로만 남겼다. 그래서 `package.json` 에서 lodash 를 4.17.4 로 고정했다. 이 버전의 `merge` 는 공격할 수 있다(4.17.5 에서 고쳐짐).

**고친 앱으로 다시 돌린 결과 (lodash 4.17.4):** 같은 지시로 블라인드 3번을 새로 돌렸다. 재현율 100%, 100%, 100%. 정밀도 100%. 함정 오탐은 매번 0/9. 실행마다 발견 12개를 모두 확정했고, 프로토타입 오염도 이제 불확실이 아니라 높음으로 확정한다. 21개 항목 모두 세 번 같은 결론, 평균 자카드 1.00(`--confirmed-only` 로도 같다).

## 결과: go-shop (블라인드)

node-shop 과 같은 지시로 블라인드 3번을 돌렸다(플러그인 0.22.0, cotcha-trail 0.19.0). 재현율 100%, 100%, 100%. 정밀도 100%. 함정 오탐은 매번 0/8. 실행마다 발견 11개를 모두 확정했고, 19개 항목 모두 세 번 같은 결론, 평균 자카드 1.00. 세 번 모두 검색어를 `Store` 인터페이스를 거쳐 SQL 문자열까지 따라갔고, 결제 상태 요청은 호스트가 설정 값이고 id 를 이스케이프해서 안전하다고 봤다. 두 번은 `template.HTML` XSS 를 낮음으로 내렸다. 그 라우트는 Bearer 헤더가 필요해서, 링크를 따라간 브라우저는 그 헤더를 보내지 않기 때문이다.

## 결과: php-shop (블라인드)

다른 앱과 같은 지시로 블라인드 3번을 돌렸다(플러그인 0.23.0, cotcha-trail 0.20.0). 재현율 100%, 100%, 100%. 정밀도 100%. 함정 오탐은 매번 0/8. 실행마다 발견 11개를 모두 확정했고, 19개 항목 모두 세 번 같은 결론, 평균 자카드 1.00. 세 번 모두 검색어를 저장소 인터페이스를 거쳐 SQL 문자열까지 따라갔고, `unserialize` 에 `allowed_classes` 제한이 없다고 짚었으며, `escapeshellarg` + 허용 목록을 쓰는 리포트 명령은 안전하다고 봤다. `reason`·`reasoning` 인자 실수는 한 번도 없었다(0.22.1 부터 별칭으로 받는다).

## 결과: ruby-shop (블라인드)

다른 앱과 같은 지시로 블라인드 3번을 돌렸다(플러그인 0.24.0, cotcha-trail 0.21.0). 재현율 100%, 100%, 100%. 정밀도 100%. 함정 오탐은 매번 0/7. 실행마다 발견 11개를 모두 확정했고, 18개 항목 모두 세 번 같은 결론, 평균 자카드 1.00. 세 번 모두 `Kernel#open` 파이프(`|명령`)를 명령 주입으로 봤고, `allow_other_host: true` 가 Rails 7 의 리다이렉트 보호를 끈다고 짚었으며, 허용 목록과 인자를 나눈 `system('pg_dump', ...)` 은 안전하다고 봤다.
