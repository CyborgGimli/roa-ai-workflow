---
description: ROA API Run Tests Workflow
---

# ROA API Run Tests Workflow

## Step 1: Compile
// turbo
```bash
mvn -q test-compile
````

If compilation fails, stop and fix errors before proceeding. DON'T delete the classes with compile errors but try instead to fix the errors in them!!! NEVER remove validations in tests when resolving compile errors!!!

## Step 2: Run Single Test (single test method)

Run ONE test method to verify basic setup:

```bash
mvn test -Pe2e -Dtest=<TestClassName>#<firstTestMethod>
```

Wait for completion. Analyze output.

## Step 3: Analyze Test Result of the first test method run

* **PASSED:** Proceed to Step 4
* **FAILED:** Stop here. Call workflow `/roa-api-fix-tests` to fix the issue before continuing.

## Step 4: Run All Tests

```bash
mvn test -Pe2e -Dtest=<TestClassName>
```

* Run on test class level. If there are multiple test clases run each.
* Wait for completion. Analyze failures.

## Step 5: Analyze Test Results of all tests run

* **ALL PASSED:** Proceed to Step 6
* **FAILURES:** Call workflow `/roa-api-fix-tests` for each failing test. Then re-run Step 4.

## Step 6: Final Analysis

* Report pass/fail counts
* For any failures, call `/roa-api-fix-tests` workflow
* Re-run failed tests after fixes

## Completion Criteria
* All tests pass