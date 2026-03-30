# API Test Failure Fix Summary

## Root Cause Analysis

The API tests failed due to **contract changes in the reqres.in test API**. The API's support metadata (text and URL) changed between test runs, causing three test assertions to fail:

1. **Support text changed**: "Tired of writing" → "Become a better CTO. A playbook of painful stories and practical advice from a two-time startup CTO."
2. **Support URL changed**: `https://contentcaddy.io?utm_source=reqres&utm_medium=json&utm_campaign=referral` → `https://benhowdle.im/first-cto-playbook?utm_source=reqres&utm_medium=json&utm_campaign=referral`
3. **Status code expectation mismatch**: Test expected SC_CONFLICT (409) but API returns SC_CREATED (201) for POST /users

## Failing Tests
- `AdvancedExamplesTest.showsComprehensiveAssertionsOnUsersList` (line 140)
- `AdvancedExamplesTest.showsSlimmerTestsViaCustomServiceRing` (CustomService.java:103)
- `BasicToAdvancedEvolutionTest.showsManualLifecycleWithExplicitLoginAndTokenHandling` (lines 88, 93)

## MCP Usage
**Not used.** Fixed the tests using the OpenAPI spec context and actual API response analysis from Allure artifacts.

## Files Changed

### 1. TestConstants.java
- **Updated constant:** `SUPPORT_TEXT_PREFIX`
  - From: `"Tired of writing"`
  - To: `"Become a better CTO"`
- **Updated constant:** `SUPPORT_URL_REGEX`
  - From: `"https:\\/\\/contentcaddy\\.io\\?utm_source=reqres&utm_medium=json&utm_campaign=referral"`
  - To: `"https:\\/\\/benhowdle\\.im\\/first-cto-playbook\\?utm_source=reqres&utm_medium=json&utm_campaign=referral"`

### 2. BasicToAdvancedEvolutionTest.java
- **Changed expected status code** (lines 88, 93)
  - From: `SC_CONFLICT` (409)
  - To: `SC_CREATED` (201)
  - Reason: The reqres.in API does not enforce conflict on duplicate user creation; it creates successfully (201)

## Why This Fix Is Correct

1. **Contract alignment**: The fixes align the tests with the actual current API contract. The support metadata is part of the API response, and tests should validate against actual behavior.

2. **Minimal changes**: Only test constants and expectations were modified—no business logic or test infrastructure was altered.

3. **Status code correction**: The SC_CONFLICT expectation was incorrect. The next test method in the same file (`showsApiLevelAuthenticationWithAuthenticateViaApi`) validates the same scenario but correctly expects SC_CREATED (201), confirming the API's actual behavior.

4. **Test validation**: All 22 Regression-tagged tests now pass:
   - AdvancedExamplesTest: 11/11 ✓
   - BasicToAdvancedEvolutionTest: 11/11 ✓

## Test Command Output Summary

```
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time: 33.626 s
[INFO] Finished at: 2026-03-30T18:38:53Z
```

### Iteration 1
- Initial run after TestConstants fix: 2 failures (MATCHES_REGEX on URL)
- Added SUPPORT_URL_REGEX fix
- Final run: All 22 tests PASSING ✓
