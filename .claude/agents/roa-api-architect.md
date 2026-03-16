---
description: Workflow used for setting up ROA API test framework and writing API automation test cases.
---

# ROA API Architect Agent - Task List

**CRITICAL USAGE:** Execute this workflow by strictly following `/ai-architect-rules.md` (phases are mandatory, in order, no skipping).

---

## Prerequisites (MANDATORY)

1) **Read the relevant `app-knowledge-{app}.yaml`** (e.g., `app-knowledge-reqres.yaml`, `app-knowledge-zerobank.yaml`)
- base URL, auth rules, required headers (e.g., `x-api-key`), flows, secrets policy
- if missing → request it (do not guess)

2) **Read project rules + examples**
- `.claude/examples/api-test-examples.md`
- `.claude/instructions/core-framework-instructions.md`
- `.claude/instructions/api-framework-instructions.md`
- `.claude/rules/rules.md`

3) **Read ALL relevant `CLAUDE.md` (mandatory)**
- `CLAUDE.md` (project root → explains overall structure + conventions; read first)
- `common/base/CLAUDE.md` (Rings)
- `common/data/CLAUDE.md` (Data/DataCreator/DataCleaner)
- `common/preconditions/CLAUDE.md` (Preconditions/Journey)
- `common/service/CLAUDE.md` (Custom services – optional but useful)
- `api/CLAUDE.md` (root api package + AppEndpoints rules)
- `api/authentication/CLAUDE.md`
- `api/dto/CLAUDE.md`
- `api/extractors/CLAUDE.md`
- `api/hooks/CLAUDE.md`

4) **Use Swagger MCP for contract discovery (no guessing)**
- fetch spec → list endpoints → inspect details for endpoints you will test
- use examples/status codes from spec when available

5) **Baseline compile**
- run `mvn -q test-compile`
- if fails → fix BEFORE Phase 1 (no deleting classes; use Pandora metadata)

---

## Phase 1: Discovery (Contract + Scenarios)

1) Use Swagger MCP to confirm:
- paths, methods, params (query/path/header), request bodies, response shapes, status codes

2) Identify auth + mandatory request requirements:
- which endpoints require auth, which are public
- required headers (API key, correlation ids, etc.)
- auth scheme (Bearer/cookie/session/custom header) — do not assume Bearer

3) Define **15–25 scenarios**, grouped by feature:
- happy + negative paths
- contract checks (status/header/body)
- storage chaining (list → extract id → get single)
- optional: retry/eventual consistency only if needed

4) Identify dependencies:
- which scenarios create data
- what needs cleanup
- which can run independently

---

## Phase 2: Project Configuration

**MANDATORY BEFORE:** Read Pandora metadata for ROA API classes/annotations you will use.

1) Update configuration files (do not guess file locations):
- `src/main/resources/config.properties`
  * set `api.base.url`
  * ensure `project.packages` (plural) is set correctly
  * set any project flags (logging/body-shortening/etc.)
- `src/main/resources/system.properties` (or the project’s env selector file)
  * ensure the active env/profile is selected correctly
- `src/main/resources/config-<env>.properties` / `test_data-<env>.properties` (if used)
  * env-specific values and credential sources (never commit secrets)

2) Enforce secrets policy:
- no secrets in code
- env vars / local untracked files only
- credentials resolve via `Data.testData()` (OWNER-backed)

**MANDATORY AFTER:** `mvn -q test-compile` must pass before Phase 3.

---

## Phase 3: Endpoint Registry (AppEndpoints) + Constants

**MANDATORY BEFORE:** Confirm patterns for `Endpoint<T>` + parameterization.

1) Ensure `AppEndpoints` includes every endpoint required by scenarios:
- correct HTTP method + relative URL
- `{id}` placeholders for path params

2) Ensure `defaultConfiguration()` applies shared defaults for ALL endpoints:
- JSON content type when applicable
- mandatory headers (e.g., `x-api-key`)
- any shared defaults required by the project

3) Ensure reusable constants exist (no magic strings):
- query param keys/values
- path variable keys/ids
- header keys/values
- common expected values

4) Do not build URLs manually:
- always use endpoint parameterization (`withQueryParam`, `withPathParam`, `withHeader`)

**MANDATORY AFTER:** `mvn -q test-compile` must pass before Phase 4.

---

## Phase 4: DTOs + JSONPath Registry (Extractors)

1) DTO strategy:
- request DTOs for bodies (when needed)
- response DTOs when mapping helps
- response DTOs must tolerate extras: `@JsonIgnoreProperties(ignoreUnknown = true)`
- `@JsonProperty` for mismatched names (e.g., `_meta`)

2) JSONPath strategy:
- maintain `ApiResponsesJsonPaths` as the **only** source for JSONPath strings
- tests must NOT use raw JSONPath strings
- tests must NOT use raw body keys in assertions (e.g., `"token"`) — always use `ApiResponsesJsonPaths`

**MANDATORY RULE:** If a needed JSONPath doesn’t exist → add a new enum entry in `ApiResponsesJsonPaths` (with formatting support if indexed).

**MANDATORY AFTER:** mvn -q test-compile` must pass before Phase 5.

---

## Phase 5: Authentication (Declarative)

**MANDATORY BEFORE:** Confirm `@AuthenticateViaApi`, `Credentials`, `BaseAuthenticationClient` behavior in metadata.

1) Credentials provider(s):
- implements `Credentials`
- reads from `Data.testData()` only
- no hardcoded values

2) Auth client(s):
- extends `BaseAuthenticationClient`
- uses ROA `RestService` to perform login/handshake (if required)
- extracts fields via `ApiResponsesJsonPaths`
- returns `Header` required by AUT (Bearer is just one example)

**MANDATORY AFTER:** `mvn -q test-compile` must pass before Phase 6.

---

## Phase 6: Hooks + Retry (Optional but Common)

1) Hooks (if useful):
- BEFORE: reachability check (ping/health-style)
- AFTER: cleanup flow
- keep registry in `ApiHookFlows`, logic in `ApiHookFunctions`
- logic must use `RestService` and assertions

2) Retry (only when justified):
- eventual consistency / transient failures
- do not blanket-add retry everywhere

**MANDATORY AFTER:** `mvn -q test-compile` must pass before Phase 7.

---

## Phase 7: Lifecycle Data + Preconditions + Cleanup

1) `DataCreator` + `DataCreatorFunctions`:
- factory methods create typed DTOs for tests
- use `@Craft(model = DataCreator.Data.X)` in test params
- use `Late<T>` only when creation must be deferred

2) Preconditions (when reuse improves clarity):
- `Preconditions` enum + `PreconditionFunctions`
- executed via `@Journey` + `@JourneyData`

3) Cleanup (when needed):
- prefer API/DB cleanup, not UI
- use `DataCleaner` + `@Ripper` targets

4) Storage chaining:
- retrieve responses via `StorageKeysApi.API` + endpoint enum key
- reuse extracted data through endpoint parameterization (`withPathParam`, etc.)

**MANDATORY AFTER:** `mvn -q test-compile` must pass before Phase 8.

---

## Phase 8: Test Implementation

**MANDATORY RULES**
- Each test class: `@API` + extends `BaseQuest` (**preferred**)
- Use `BaseQuestSequential` only when test ordering is truly required (tests depend on each other)
- Tests use **ROA ring only**: `quest.use(RING_OF_API)` (no direct RestAssured in tests)
- Typed endpoints only (`AppEndpoints`)
- Use constants (no hardcoded params/ids/headers/expected values)
  - If needed constants classes do not exist, create them (examples: `Headers`, `QueryParams`, `PathVariables`, `TestConstants`, `AssertionMessages`)
- Use `ApiResponsesJsonPaths` (no raw JSONPath strings)
- Every test has meaningful validation (no empty validate blocks)
- Always ends with `.complete()`
- No wildcard imports

**Quality gate**
- `mvn -q test-compile`
- `mvn clean install`

---

## Phase 9: Run + Fix Loop

1) Run: execute `/roa-api-run-tests.md`
2) Fix failures: execute `/roa-api-fix-tests.md`
3) Repeat run → fix → run until all pass

**Completion criteria**
- All tests executed
- All tests passing