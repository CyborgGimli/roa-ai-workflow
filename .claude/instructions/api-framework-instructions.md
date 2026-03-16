# **ROA API Framework Instructions**

## Purpose of This Document

This file provides the **API architecture and basic patterns** of the ROA framework for **agentic code platforms**.

It explains how to build **clean, consistent ROA API automation** using:

* typed endpoints (`AppEndpoints`)
* the ROA API fluent ring (`RING_OF_API`)
* storage-driven chaining (`StorageKeysApi.API`)
* centralized JSONPaths (`ApiResponsesJsonPaths`)
* declarative auth (`@AuthenticateViaApi`)
* hooks (`@ApiHook`) and retry (`retryUntil`)

### Source of Truth

* **Skill metadata:** skill-name:pandora `.claude/skills/pandora/SKILL.md`
* **Subfolder MD files:** Advanced patterns and detailed examples
* **Project examples:** `api-test-examples.md` under `.claude/examples/`

### Prerequisites

Read **core-framework-instructions.md** first for Quest, Rings, lifecycle, and storage fundamentals.

---

## API Module Structure

Your API project layer is organized as:

| Package               | Purpose                                                               |
|-----------------------|-----------------------------------------------------------------------|
| `api/`                | API layer root                                                        |
| `api/authentication/` | Credentials + auth client used by `@AuthenticateViaApi`               |
| `api/dto/`            | Request/response models (Jackson/Lombok DTOs)                         |
| `api/extractors/`     | Central JSONPath registry (`ApiResponsesJsonPaths`)                   |
| `api/hooks/`          | API hook flows + implementations (`ApiHookFlows`, `ApiHookFunctions`) |
| `api/AppEndpoints`    | **Typed endpoint enum** implementing `Endpoint<T>`                    |

---

## The API Ring (RING_OF_API)

### What the API Ring Is

The API ring is the fluent service that performs:

* requests
* validation (status/header/body assertions)
* response storage (automatic)
* retry / eventual consistency patterns

Tests must use:

```java
quest.use(RING_OF_API)
```

### Core Operations in Tests

Typical flow:

* `requestAndValidate(endpoint, assertions...)`
* `requestAndValidate(endpoint, body, assertions...)`
* `request(endpoint)` / `request(endpoint, body)` (store response, validate later)
* `validate(Runnable)` hard assertions
* `validate(Consumer<SoftAssertions>)` soft assertions
* `.complete()` always at the end

**Rule:** Tests do **not** call RestAssured directly.

---

## AppEndpoints (Typed Endpoint Registry)

### Why Endpoints Exist

`AppEndpoints` is the **single source of truth** for:

* HTTP method + relative URL
* default configuration for all calls
* shared headers (like mandatory `x-api-key`)
* consistent discoverability across tests

### Endpoint Definition Pattern

```java
public enum AppEndpoints implements Endpoint<AppEndpoints> {

   GET_ALL_USERS(Method.GET, "/users"),
   GET_USER(Method.GET, "/users/{id}"),
   POST_CREATE_USER(Method.POST, "/users"),
   POST_LOGIN_USER(Method.POST, "/login"),
   DELETE_USER(Method.DELETE, "/users/{id}");

   // method(), url(), enumImpl()

   @Override
   public RequestSpecification defaultConfiguration() {
      RequestSpecification spec = Endpoint.super.defaultConfiguration();
      spec.contentType(ContentType.JSON);
      spec.header(API_KEY_HEADER, API_KEY_VALUE); // mandatory x-api-key
      return spec;
   }
}
```

### Parameterization (No Manual Strings)

Use endpoint parameterization instead of building URLs manually:

* `withQueryParam(key, value)`
* `withPathParam(key, value)`
* `withHeader(key, value)`

Example:

```java
GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO)
GET_USER.withPathParam(ID_PARAM, ID_THREE)
GET_USER.withHeader(EXAMPLE_HEADER, token)
```

---

## DTOs (Request / Response Models)

DTOs are plain models used to keep:

* request bodies structured (e.g., `CreateUserDto`, `LoginDto`)
* response mapping clean (e.g., `CreatedUserDto`, `GetUsersDto`)

**Guidelines:**

* Use Lombok (`@Data`, `@Builder`) for brevity.
* Response DTOs should tolerate extra fields:

    * `@JsonIgnoreProperties(ignoreUnknown = true)`
* Use `@JsonProperty` when API keys don’t match Java naming (e.g. `_meta`).

---

## Extractors (ApiResponsesJsonPaths)

### Why JSONPaths Are Centralized

Tests must not scatter raw JSONPath strings like `"data[0].id"` everywhere.

All JSONPaths must live in a registry enum:

```java
public enum ApiResponsesJsonPaths {
   TOTAL("total"),
   USER_ID("data[%d].id"),
   TOKEN("token"),
   ERROR("error");

   public String getJsonPath(Object... args) { ... }
}
```

Usage:

```java
Assertion.builder()
   .target(BODY)
   .key(TOKEN.getJsonPath())
   .type(NOT_NULL)
   .expected(true)
   .build();
```

---

## Response Storage + Chaining

### Automatic Storage

Every API call made through the ring stores the last response under:

* `StorageKeysApi.API`
* keyed by the **endpoint enum** (e.g. `GET_ALL_USERS`, `POST_LOGIN_USER`)

### Retrieval Pattern

```java
Response response = retrieve(StorageKeysApi.API, GET_ALL_USERS, Response.class);
GetUsersDto users = response.getBody().as(GetUsersDto.class);
```

### Use Cases

* Chain requests (read id from list → use as path param)
* Token reuse (login → extract token → header in next request)
* Complex assertions (validate later with JUnit/AssertJ)

---

## Authentication (AuthenticateViaApi)

### Components

Authentication is split into:

1. **Credentials provider** (e.g., `AdminAuth implements Credentials`)

* resolves username/password from config-backed test data (`Data.testData()`)

2. **Auth client** (`AppAuth extends BaseAuthenticationClient`)

* performs the login request
* extracts token
* returns a `Header` (e.g., `Authorization: Bearer <token>`)

### Test Usage

```java
@AuthenticateViaApi(credentials = AdminAuth.class, type = AppAuth.class, cacheCredentials = true)
@Test
void authenticatedCall(Quest quest) { ... }
```

**Rules:**

* No hardcoded credentials in tests.
* Prefer annotation-based auth to repeating login flows in every test.
* Use `cacheCredentials=true` only when reuse in the same run is correct.

---

## API Hooks (@ApiHook)

### What Hooks Are

Hooks are **class-level** flows that run **once**:

* BEFORE the test class
* AFTER the test class

They’re used for:

* reachability checks (ping / health-style verification)
* cleanup flows (delete demo users, reset state)

### Pattern

* `ApiHookFlows` implements `ApiHookFlow`
* `ApiHookFunctions` contains static implementations using `RestService`

Example annotation usage:

```java
@API
@ApiHook(when = HookExecution.BEFORE, type = ApiHookFlows.Data.PING_REQRES)
@ApiHook(when = HookExecution.AFTER, type = ApiHookFlows.Data.DELETE_LEADER_USER)
class ApiHooksExamplesTest extends BaseQuest { ... }
```

---

## Retry / Eventual Consistency

Use retry when:

* async processing must complete
* external state is eventually consistent
* CI hits transient instability

Pattern from tests:

```java
quest
   .use(RING_OF_API)
   .retryUntil(condition, Duration.ofSeconds(10), Duration.ofSeconds(1))
   .requestAndValidate(...)
   .complete();
```

---

## Validation Rules (API)

API validations should prefer:

1. `requestAndValidate(... Assertion.builder() ...)`
2. `request(...)` then `validate(...)` only when you need custom logic

Targets:

* `STATUS`
* `HEADER`
* `BODY`

Types:

* `IS`, `CONTAINS`, `NOT_NULL`, `MATCHES_REGEX`, etc.

---

## Non-Negotiable Rules

| Rule                     | Description                                                              |
|--------------------------|--------------------------------------------------------------------------|
| **ROA Ring Only**        | Tests must use `quest.use(RING_OF_API)` — no direct RestAssured in tests |
| **Typed Endpoints Only** | All calls must use `AppEndpoints` (no raw method/url in tests)           |
| **No Hardcoded Secrets** | Credentials & API key come from config/env/test data                     |
| **x-api-key Mandatory**  | Must be sent on every request (ideally via `defaultConfiguration()`)     |
| **Centralize JSONPaths** | Use `ApiResponsesJsonPaths`, never raw strings in tests                  |
| **Use constants**        | Params/ids/headers/expected values come from constants classes           |
| **No wildcard imports**  | Import only what is used                                                 |
| **Always complete()**    | Every test chain ends with `.complete()`                                 |

---

## Where to Find More

| Feature                | Look In                                                 |
|------------------------|---------------------------------------------------------|
| Full runnable patterns | `.claude/examples/api-test-examples.md`               |
| Core mental model      | `.claude/instructions/core-framework-instructions.md` |
| Strict rules           | `.claude/rules/rules.md`                              |
| Metadata + AI surfaces | `.claude/skills/pandora/SKILL.md`                     |