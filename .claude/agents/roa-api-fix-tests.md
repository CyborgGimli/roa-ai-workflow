---
description: Fix failing ROA API tests by analyzing errors and applying fixes
---

# ROA API Fix Tests Workflow

## Step 1: Identify Failure Type
Analyze the test failure output to determine the root cause:
- **Endpoint mismatch:** Wrong method/path, missing `{pathParam}`, incorrect base URL
- **Request setup error:** Missing required header (API key), wrong content type, missing query/path params
- **Assertion failed:** Expected vs actual mismatch (status/header/body)
- **Authentication failed:** Auth client/credentials issue, wrong auth scheme/header
- **Serialization / DTO issue:** Request/response DTO mismatch, Jackson mapping problems
- **JSONPath issue:** Missing/wrong entry in `ApiResponsesJsonPaths`
- **Hook failure:** `@ApiHook` BEFORE/AFTER flow failing
- **Retry / flakiness:** Transient failure or eventual consistency case
- **Compilation error:** Code issue, missing imports, wrong signatures

## Step 2: Fix Based on Failure Type

### Endpoint / Contract Issues
1. Use Swagger MCP to confirm correct method + path + params + expected statuses
2. Update `api/AppEndpoints`:
   - correct URL (including `{id}` placeholders)
   - correct HTTP method
3. Prefer parameterization:
   - `withPathParam(...)`, `withQueryParam(...)`, `withHeader(...)`
4. Re-run the failing test method only

### Request Setup (Headers / Content Type / Params)
1. Ensure shared mandatory headers are applied in `AppEndpoints.defaultConfiguration()` (e.g., `x-api-key`)
2. Ensure request uses constants (no magic strings) for:
   - header keys/values
   - query param keys/values
   - path param keys/ids
3. Validate body is sent only when required and uses a DTO

### Assertion Issues (Status / Header / Body)
1. Confirm actual behavior using Swagger spec examples (or known API behavior)
2. Update assertions:
   - `Assertion.builder().target(STATUS|HEADER|BODY)...`
3. If expected values are environment-dependent, move them into test data / constants

### Authentication Issues
1. Verify credentials resolve via `Data.testData()` (no hardcoded values)
2. Check `BaseAuthenticationClient` implementation:
   - uses `RestService` (not direct RestAssured in tests)
   - returns the correct `Header` for the AUT (Bearer is not assumed)
3. Confirm whether `cacheCredentials=true` is correct for the suite

### DTO / Serialization Issues
1. If request fails due to schema mismatch, update request DTO fields/types
2. If response mapping fails, update response DTO:
   - add `@JsonIgnoreProperties(ignoreUnknown = true)`
   - add `@JsonProperty` where needed
3. If DTO mapping is not required for this case, validate via JSONPath assertions instead

### JSONPath Issues
1. Do NOT use raw JSONPath strings in tests
2. Add or fix enum entry in `ApiResponsesJsonPaths`:
   - use indexed formatting where needed (`data[%d].id`)
3. Update tests to use `ApiResponsesJsonPaths.X.getJsonPath(...)`

### Hook Failures
1. Identify whether failure is in BEFORE or AFTER hook
2. Fix hook logic in `ApiHookFunctions` (keep registry in `ApiHookFlows`)
3. Ensure hook uses `RestService` + proper assertions
4. If hook is unnecessary for the class, remove it rather than hiding failures

### Retry / Flakiness
1. Only add retry when justified (eventual consistency / transient external dependency)
2. Use `retryUntil(...)` with sensible timeout + polling
3. Don’t blanket-retry every request

### Compilation Errors
1. Check imports (no wildcard imports)
2. Use Pandora metadata (`.claude/skills/pandora/SKILL.md`) to verify correct ROA types/signatures
3. Run `mvn -q test-compile` after each fix

## Step 3: Verify Fix
// turbo
```bash
mvn test -Pe2e -Dtest=<TestClassName>#<fixedTestMethod>
```

## Step 4: Confirm Resolution

* **PASSED:** Fix confirmed. Return to run workflow.
* **FAILED:** Repeat Steps 1–3 with new failure info.

## Completion Criteria

* The specific failing test now passes
* No new compilation errors introduced