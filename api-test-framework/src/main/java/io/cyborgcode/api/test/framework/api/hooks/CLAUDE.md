**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md` for metadata and usage of ROA framework classes.

# API hooks package

## Goal

Provide **class-level API hooks** for ROA tests via `@ApiHook`, to run lightweight flows **once per test class**:

* **BEFORE**: reachability / “ping” checks, pre-test readiness
* **AFTER**: cleanup / teardown flows (idempotent)

Hooks are **not** per-test setup. For per-test setup, use `@Journey`. For per-test cleanup, use `@Ripper`.

---

## Architecture

This package has exactly two parts:

1. **Hook registry enum**: `ApiHookFlows`

    * Implements `io.cyborgcode.roa.api.hooks.ApiHookFlow<ApiHookFlows>`
    * Maps each hook “type” to a function implementation
    * Contains a nested `Data` class with **string constants** used by annotations

2. **Hook implementations**: `ApiHookFunctions`

    * `final` utility class (`private` constructor)
    * Holds `public static` methods that implement hook flows
    * Uses `RestService` to execute requests
    * Uses `Assertion.builder()` for validation (no raw RestAssured usage in tests)

---

## How hooks are used

### Annotation usage (pattern)

Hooks are applied on the **test class**:

```java
@API
@ApiHook(when = HookExecution.BEFORE, type = ApiHookFlows.Data.PING_REQRES)
@ApiHook(when = HookExecution.AFTER, type = ApiHookFlows.Data.DELETE_LEADER_USER)
class ApiHooksExamplesTest extends BaseQuest {
  // @Test methods...
}
```

**Meaning:**

* `BEFORE` runs once before any tests in the class.
* `AFTER` runs once after all tests in the class.

---

## Files

### `ApiHookFlows` (registry only)

**Rules**

* Registry only: **no logic** inside the enum body beyond wiring.
* Every enum value must map to a method reference in `ApiHookFunctions`.
* `Data` nested class must expose **string constants** matching enum entries.
* Signature is a `TriConsumer<RestService, Map<Object,Object>, String[]>`.

**Template (pattern)**

```java
public enum ApiHookFlows implements ApiHookFlow<ApiHookFlows> {

  PING_REQRES(ApiHookFunctions::pingReqres),
  DELETE_LEADER_USER(ApiHookFunctions::deleteLeaderUser);

  public static final class Data {
    private Data() {}
    public static final String PING_REQRES = "PING_REQRES";
    public static final String DELETE_LEADER_USER = "DELETE_LEADER_USER";
  }

  private final TriConsumer<RestService, Map<Object, Object>, String[]> flow;

  ApiHookFlows(TriConsumer<RestService, Map<Object, Object>, String[]> flow) {
    this.flow = flow;
  }

  @Override
  public TriConsumer<RestService, Map<Object, Object>, String[]> flow() {
    return flow;
  }

  @Override
  public ApiHookFlows enumImpl() {
    return this;
  }
}
```

### `ApiHookFunctions` (logic only)

**Rules**

* `final` class with a private constructor.
* Hook methods must be `public static void <name>(RestService, Map<Object,Object>, String[])`.
* Must use **typed endpoints** (`AppEndpoints`) and parameterization (`withQueryParam/withPathParam/withHeader`).
* Must use constants for params/ids/expected values (no hardcoding).
* Must validate using `Assertion.builder()`.

**Template (pattern)**

```java
public final class ApiHookFunctions {

  private ApiHookFunctions() {}

  public static void pingReqres(RestService service, Map<Object, Object> storage, String[] args) {
    service.requestAndValidate(
      GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO),
      Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build()
    );
  }
}
```

---

## When to use hooks vs journeys vs rippers

* **@ApiHook (class-level, once):**

    * ping / readiness checks
    * global cleanup for the whole class

* **@Journey (per-test, before each test):**

    * create required preconditions for a specific test
    * prepare per-test context

* **@Ripper (per-test, after each test):**

    * teardown for data created by a specific test

---

## DO / DON'T

### DO

* Keep `ApiHookFlows` as wiring only.
* Keep hook logic in `ApiHookFunctions`.
* Use `ApiHookFlows.Data.*` in annotations (no duplicated strings).
* Prefer idempotent “safe” cleanup in AFTER hooks.
* Use typed endpoints + constants + `Assertion.builder()`.

### DON'T

* Don’t put heavy setup logic in class hooks (use `@Journey`).
* Don’t put per-test teardown in class AFTER hooks (use `@Ripper`).
* Don’t hardcode params/ids/status codes/paths in hook functions.
* Don’t call RestAssured directly in tests; use ROA `RestService` / `RING_OF_API` patterns only.
