# Fix Summary

## Root Cause

Two independent contract mismatches between test expectations and the live reqres.in API:

### 1. Stale `support` field constants (`AdvancedExamplesTest` × 2 tests)
The reqres.in API rotates its `support.text` and `support.url` values. The constants in `TestConstants.Support` were pinned to outdated values:

| Field | Old (stale) | New (actual) |
|---|---|---|
| `SUPPORT_TEXT_PREFIX` | `"Tired of writing!"` | `"Become a better CTO."` |
| `SUPPORT_URL_REGEX` | matches `contentcaddy.io` | matches `benhowdle.im/first-cto-playbook` |

Two tests failed because of this: `showsComprehensiveAssertionsOnUsersList` and `showsSlimmerTestsViaCustomServiceRing`. Both use `STARTS_WITH` on `SUPPORT_TEXT_PREFIX` and `MATCHES_REGEX` on `SUPPORT_URL_REGEX`.

### 2. Wrong expected HTTP status (`BasicToAdvancedEvolutionTest`)
`showsManualLifecycleWithExplicitLoginAndTokenHandling` expected `SC_CONFLICT` (409) for `POST /api/users`, but the OpenAPI contract (and actual API) specifies `201 Created` as the only success response for user creation. `reqres.in` is a stateless mock that always returns 201 — it never returns 409.

## MCP Contract Lookup

Yes — used the local OpenAPI spec (`specs/openapi.json`) via direct file read. Confirmed:
- `POST /api/users` contract only defines `201` and `400` responses (no 409).
- `GET /api/users` `support` field is `additionalProperties: true` (not pinned in schema), so actual values from logs are the ground truth.

The `reqres_openapi` MCP server was not needed beyond what the spec file provided.

## Files Changed

| File | Change |
|---|---|
| `api-test-framework/src/main/java/io/cyborgcode/api/test/framework/data/constants/TestConstants.java` | Updated `SUPPORT_TEXT_PREFIX` and `SUPPORT_URL_REGEX` to match current API values |
| `api-test-framework/src/test/java/io/cyborgcode/api/test/framework/BasicToAdvancedEvolutionTest.java` | Changed expected status from `SC_CONFLICT` (409) to `SC_CREATED` (201) for both POST /api/users calls in `showsManualLifecycleWithExplicitLoginAndTokenHandling` |

## Why This Fix Is Correct

- **Support text/URL**: The test intent is to verify the `support` block exists and matches known patterns (STARTS_WITH, CONTAINS, MATCHES_REGEX). Updating to current API values preserves test coverage without reducing it — the assertions still verify content shape, not arbitrary values.
- **HTTP status**: The OpenAPI contract unambiguously defines `201` as the success status for `POST /api/users`. The sibling test `showsApiLevelAuthenticationWithAuthenticateViaApi` already correctly expects `SC_CREATED`, confirming 409 was a bug in the manual test.

## Test Command Output

```
mvn -U -pl api-test-framework -am test -Pe2e \
  -Dtest="AdvancedExamplesTest#showsComprehensiveAssertionsOnUsersList+showsSlimmerTestsViaCustomServiceRing,BasicToAdvancedEvolutionTest#showsManualLifecycleWithExplicitLoginAndTokenHandling" \
  -Dmaven.repo.local=$GITHUB_WORKSPACE/.m2/repository

✔ showsComprehensiveAssertionsOnUsersList  — SUCCESS
✔ showsSlimmerTestsViaCustomServiceRing    — SUCCESS
✔ showsManualLifecycleWithExplicitLoginAndTokenHandling — SUCCESS

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
