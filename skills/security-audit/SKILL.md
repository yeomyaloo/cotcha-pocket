---
name: security-audit
description: Audit a Java/Spring, Kotlin, Python (FastAPI, Flask, Django) , Node.js (Express, Koa, Fastify, NestJS, Next.js) or Go (net/http, gin, echo, chi, gorilla/mux, fiber) codebase for security vulnerabilities with the Cotcha (cotcha-pocket) MCP tools — plan the audit from the code graph (entry point → sink paths and endpoints), investigate each item by reading real code, record findings, re-verify every finding adversarially, and export a Markdown/SARIF report. Use when the user asks to security-review, audit, pentest-review, or find vulnerabilities (SQL injection, command injection, path traversal, deserialization, SSRF, XXE, XSS, missing authorization/IDOR, weak crypto, hardcoded secrets, vulnerable Maven/npm dependencies with known CVEs, and risky frontend JS/TS code) in a Java/Spring, Python (FastAPI, Flask, Django) , Node.js/TypeScript or Go backend repository, given a local folder path or a public Git URL.
---

# Java/Spring · Kotlin · Python (FastAPI, Flask, Django) · Node.js · Go security audit (Cotcha)

You lead a source-code security audit using the Cotcha (`cotcha`) MCP tools. The MCP server is the hands (code graph, file access, records); you are the brain (planning and judgment). Reply in the user's language.

The code graph is static analysis and can be wrong. **Every judgment must come from reading the actual code with `read_file`.** Only read the target code — never build or run it.

## 1. Load and plan

1. `load_repository` with `path` (absolute folder) or `git_url` (public https repo). Note the returned repo id.
   If it reports earlier findings and the user wants a fresh audit, call `clear_findings`.
2. Look at the attack surface:
   - `list_sink_paths` — entry point → dangerous API paths, riskiest categories first (COMMAND, CODE/EXPRESSION_INJECTION, DESERIALIZATION, JNDI, SQL, XXE, PATH, SSRF, XSS, OPEN_REDIRECT, WEAK_CRYPTO, WEAK_RANDOM, and IDOR: Spring Data `findById`/`deleteById`-style access by id, a missing-owner-check candidate). For IDOR, check whether the code compares the requesting user with the data owner (session/token user vs owner, owner-scoped queries like `findByIdAndUser`, `@PreAuthorize`); public data is not a vulnerability. Other IDOR candidates: repository methods whose only condition is an id (`findByBuyerId`, single-id `@Query`), MyBatis `@Mapper` methods taking one id, and `save` of a `@RequestBody` object (the body's id can overwrite someone else's row) — check whether the id comes from the request rather than the session/token.
   - `list_entry_points` — every HTTP endpoint, including ones that reach no sink. For FastAPI it lists the `Depends(...)` dependencies of each route (router-level ones included); a route with `Depends 없음` that changes or reveals data is a missing-authentication candidate (CWE-306/862). For Flask and Django it lists auth hints (`인증 단서`: `login_required`, `permission_classes`, `LoginRequiredMixin`, `before_request` hooks); `인증 단서 없음` is a candidate too, but first check global settings (middleware, `DEFAULT_PERMISSION_CLASSES`, app-wide `before_request`).
   - Python repositories (`load_repository` says `언어: python`) use the same categories: `subprocess`/`os.system`/`asyncio.create_subprocess_*`/python-nmap → COMMAND (python-nmap and exec-style calls without a shell → argument injection CWE-88 when the value can start with `-`), `requests`/`httpx`/`aiohttp`/`urllib` → SSRF, `open`/`pathlib.Path`/`FileResponse`/`shutil` → PATH, a built query string passed to `execute`-like methods or `sqlalchemy.text` → SQL, `pickle`/`yaml.load`/`marshal` → DESERIALIZATION.
   - Node.js repositories (`언어: node`) use them too, plus NOSQL_INJECTION (a request value passed straight to `find`/`findOne`/`updateOne`…, where an object like `{"$ne": null}` becomes a query operator) and PROTOTYPE_POLLUTION (request data merged with lodash `merge`/`defaultsDeep` or deepmerge). `child_process` → COMMAND, a built string passed to `query`/`execute`/`raw` → SQL, `axios`/`fetch`/`got` → SSRF, `fs`/`res.sendFile` → PATH, `node-serialize` → DESERIALIZATION, `eval`/`vm`/`Function` → CODE_INJECTION. `list_entry_points` shows route and mount middleware (`requireAuth`) and NestJS `@UseGuards` as `인증 단서`; also check app-wide `app.use(auth)` and global guards.
   - Go repositories (`언어: go`) use the same categories: `os/exec` → COMMAND (only `sh -c` strings or user-chosen programs are injectable; separate arguments are not, unless a value can start with `-`), a query built with `fmt.Sprintf`/`+` passed to `Query`/`Exec`/gorm `Raw`/`Where` → SQL, `os.Open`/`ReadFile`/`c.File`/`http.ServeFile` → PATH (`http.ServeFile` and `c.File` already reject `..` in the URL path, but not in a query value), `http.Get`/`NewRequest` → SSRF, `template.HTML(...)` or request data written as HTML → XSS, a request value passed to `template.Parse` → CODE_INJECTION (CWE-1336), md5/sha1/des/rc4 and `InsecureSkipVerify` → WEAK_CRYPTO (CWE-295 for TLS), `math/rand` → WEAK_RANDOM. `list_entry_points` shows group and route middleware (`middleware.RequireAuth()`) as `인증 단서`.
   - `list_sinks` — dangerous calls, including ones no entry point reaches (check them with `get_callers`).
   - Frontend sinks in `list_sinks` are marked `[프론트엔드]` (JS/TS/Vue/HTML: XSS sinks, `eval`, open redirects, tokens in `localStorage`, `postMessage`, hardcoded keys). They have no entry point path, so plan one item per file and trace where the value comes from (URL, `location`, `postMessage`, API data written by other users) and whether it is sanitized (DOMPurify; React `{value}` is escaped). Publishable keys (Firebase web `apiKey`, Stripe `pk_`) are not secrets.
   - `check_dependencies` — libraries from the build files (Maven, Gradle, npm) with known vulnerabilities (CVE, severity, fixed version) from the public OSV database (OWASP A03). Only library coordinates and versions are sent. Each scan is saved with the commit, and the result says what changed since the previous scan (version changes, new and resolved vulnerabilities); mention new vulnerabilities to the user.
3. Build a short plan (aim for 5–12 items, highest risk first):
   - Each high-risk sink path.
   - Problems paths cannot show: endpoints that read/update/delete by an id path variable without checking the caller owns the data (CWE-639), privileged endpoints without role checks (CWE-862), security config (`SecurityFilterChain`, `permitAll`, CSRF, CORS), hardcoded secrets in `src/main/resources` (CWE-798; never print secret values — mask them), authentication (CWE-287: login, token issuing, JWT signature/`none` algorithm/expiry checks, CWE-347), password storage (plaintext or MD5/SHA-1 instead of BCrypt/Argon2, CWE-256/916), template XSS (`th:utext`, unescaped JSP output, CWE-79).
   - Serious library vulnerabilities from `check_dependencies`: one item per library to check whether the code actually uses the vulnerable feature (CWE-1395), e.g. SnakeYAML `new Yaml().load` on user input, Jackson default typing, Log4j 2 logging user input. An outdated version alone is not a finding — report it as a recommendation with the fixed version.
   Group endpoints that share a pattern into one item.
4. **Get the user's approval before investigating.** Show the plan as a numbered list (for each item: the endpoint or file, the category/CWE, and one line on what you will check), then stop and ask the user to approve it. They may drop items, add items, or reject the plan. Do not call `read_file` or start investigating until they answer; this is the point where they decide what the audit spends time and tokens on.
   - Apply their changes and investigate only the approved items. If they reject the plan, stop and don't record anything.
   - Skip the wait only when the user already said to go ahead without confirmation (e.g. "just run it", "no need to ask"), or when you are running unattended with no one to answer. Then say which items you chose and continue.
   - If investigation turns up follow-up work outside the approved plan (e.g. another caller of the same sink), list it and ask again before investigating it.

## 2. Investigate (worker role)

For each plan item:
- Read the methods along the path with `read_file`; follow how user input enters, changes, and reaches the sink. Use `get_callers` / `get_callees` / `find_code_nodes` when the graph looks doubtful.
- Category notes:
  - **XXE (CWE-611)**: look at how the parser factory is configured. `disallow-doctype-decl=true`, external entities off, `XMLConstants.ACCESS_EXTERNAL_DTD=""`, `XMLInputFactory.SUPPORT_DTD=false` block it.
  - **XSS (CWE-79)**: is the response HTML (Content-Type), and is the value escaped (`HtmlUtils.htmlEscape`)? JSON-only responses are usually not XSS.
  - **WEAK_CRYPTO (CWE-327/328)**: check the algorithm (MD5, SHA-1, DES, RC4, ECB) and the purpose. Passwords, signatures and encryption are vulnerable; cache keys, ETags and checksums are not.
  - **WEAK_RANDOM (CWE-330)**: only a problem when the value must be unguessable (tokens, reset codes, session values); those need `SecureRandom`.
- Actively search for defenses with `grep_code`: Bean Validation (`@Valid`, `@Pattern`), escaping, parameter binding (`?` in PreparedStatement, JPA parameters), allow-lists, path normalization + base-dir checks, authorization (`SecurityFilterChain`, `@PreAuthorize`, owner comparisons), filters and interceptors.
  In Python: Pydantic models/validators and `Field(pattern=...)`, `ipaddress`/regex allow-lists, `shell=False` with an argument list, `shlex.quote`, `execute(sql, params)` / SQLAlchemy bound parameters, `os.path.realpath` + base-dir check, auth dependencies (`Depends(get_current_user)`), Flask/Django `login_required`, DRF `permission_classes`, `LoginRequiredMixin`, middleware.
  In Node.js: bound parameters (`query(sql, [value])`, ORM binds), schema validation (joi, zod, class-validator), `String()`/`Number()` casts (they block NoSQL operator objects), `execFile`/`spawn` with an argument array, `path.basename` or the `root` option, allow-lists, auth middleware and `@UseGuards`.
  In Go: `$1`/`?` placeholders, `exec.Command` with separate arguments (no shell), `filepath.Base` or a `filepath.Rel` check against the base folder, html/template auto-escaping (not bypassed with `template.HTML`), allow-lists, group middleware.
- Input that is only constants or server-side values is not exploitable.
- Call `record_finding` **only** when you confirmed in code that external input reaches the sink unblocked (or that an owner/role check is missing on a reachable endpoint). Include file:line evidence and a concrete attack request.
- **One finding per weakness** (a weakness = one location + one CWE). Do not fold one weakness into another, even when they are related; mention the related finding id in the description instead. In particular:
  - A missing authentication or ownership check on an endpoint (CWE-306/862/639, at the handler or the id lookup) is separate from an injection in the same handler (SQL, command, path).
  - A hardcoded secret or key (CWE-798/321, where the value is) is separate from the weak algorithm that uses it (CWE-327/328).
  - A frontend sink (XSS, token in `localStorage`, `postMessage` without an origin check) is separate from the backend bug that feeds it.
  - The same weakness reported twice (same location and CWE) is a duplicate: record it once.
- **Code with no caller:** weaknesses in the code itself (a hardcoded secret, password hashing with MD5/SHA-1, a trust-all TrustManager or `verify=False` client) are still findings when the code ships, because they can be read from the source or build or called later. Record them with `severity` low unless the secret alone is damaging, and say "no caller found" in the description. Injection-type sinks (SQL, command, path, SSRF, deserialization) need external input: if no reachable endpoint or job feeds them, they are not findings.

If you can spawn subagents (Claude Code `Agent` tool), give each plan item to its own subagent in parallel and pass it the repo id, the item, and the rules above; have it report its verdict and what it recorded.

## 3. Verify (verifier role)

After investigation, switch sides. For each finding (`list_findings` with `detail=true`), assume the report may be wrong and re-read the code independently:
- **Source** — is the value really attacker-controlled?
- **Reachability** — is the endpoint exposed, and does every hop of the call chain really connect?
- **Defenses** — does any control above block the attack?
- **Impact** — what can an attacker actually do? Is the severity inflated?

Then `submit_verdict`: `confirmed`, `rejected` (blocked or not attacker-controlled — a false positive), or `uncertain` (only when the deciding code is outside the repository). Adjust `severity` if needed. Do not reject a finding because another finding covers the same endpoint: a different CWE at a different location is a separate weakness. For a weakness in the code itself (secret, weak hashing, trust-all TLS), having no caller lowers the severity; it is not a reason to reject.
If subagents are available, use a different subagent from the one that investigated, so the verdict is independent.

## 4. Report

`export_report` (`format=markdown`; add `format=sarif` if the user wants GitHub code scanning). Then summarize for the user:
- Confirmed vulnerabilities by severity, each with location (file:line), the attack, and how to fix it.
- One line per rejected finding explaining why it was a false positive.
- The saved report path.

Do not publish vulnerability details of someone else's live, unpatched system; keep the report with the user.
