# API Test Examples

Read these files before generating code:
- `.claude/instructions/core-framework-instructions.md`
- `.claude/instructions/api-framework-instructions.md`
- `.claude/rules/rules.md`
---

## Test Class Structure

A correct ROA API test follows this shape:

- Annotate the class with `@API`
- Extend `BaseQuest` (parallel per-test by default)
- Inject `Quest quest` as a method parameter
- Start with `quest.use(RING_OF_API)` (directly)
- Use **typed Endpoints** + **Assertion.builder()**
- End with `.complete()` (always)

```java
import io.cyborgcode.roa.api.annotations.API;
import io.cyborgcode.roa.framework.base.BaseQuest;
import io.cyborgcode.roa.framework.quest.Quest;
import org.junit.jupiter.api.Test;
// NOTE: RING_OF_API is a project constant. Import it from your project Rings class (see "Required Imports by Feature").

@API
class ApiExampleTests extends BaseQuest {

   @Test
   void example(Quest quest) {
      quest
            .use(RING_OF_API)
            // request / validate here...
            .complete();
   }
}
```

---

## Required Imports by Feature

> No wildcard imports. Import only what you use (especially for `static`).

| Feature                                              | Import                                                                                            |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Base class                                           | `io.cyborgcode.roa.framework.base.BaseQuest`                                                      |
| Quest parameter                                      | `io.cyborgcode.roa.framework.quest.Quest`                                                         |
| `@API` annotation                                    | `io.cyborgcode.roa.api.annotations.API`                                                           |
| JUnit `@Test`                                        | `org.junit.jupiter.api.Test`                                                                      |
| Ring constant                                        | `static {project}.base.Rings.RING_OF_API`                                                         |
| Endpoints (per endpoint)                             | `static {project}.api.AppEndpoints.GET_ALL_USERS` *(and/or `GET_USER`, `POST_CREATE_USER`, etc.)* |
| Endpoint contract (when implementing endpoints enum) | `io.cyborgcode.roa.api.core.Endpoint`                                                             |
| Assertions (builder)                                 | `io.cyborgcode.roa.validator.core.Assertion`                                                      |
| Assertion targets (per target)                       | `static io.cyborgcode.roa.api.validator.RestAssertionTarget.STATUS` *(and/or `HEADER`, `BODY`)*   |
| Assertion types (per type)                           | `static io.cyborgcode.roa.validator.core.AssertionTypes.IS` *(and/or `CONTAINS`, etc.)*           |
| HTTP status codes (per code)                         | `static org.apache.http.HttpStatus.SC_OK` *(and/or `SC_CREATED`, etc.)*                           |
| Storage keys (API)                                   | `io.cyborgcode.roa.api.storage.StorageKeysApi`                                                    |
| Response type (when reading storage)                 | `io.restassured.response.Response`                                                                |
| `@Craft`                                             | `io.cyborgcode.roa.framework.annotation.Craft`                                                    |
| `@Journey`                                           | `io.cyborgcode.roa.framework.annotation.Journey`                                                  |
| `@JourneyData`                                       | `io.cyborgcode.roa.framework.annotation.JourneyData`                                              |
| `@Ripper`                                            | `io.cyborgcode.roa.framework.annotation.Ripper`                                                   |
| Late initialization                                  | `io.cyborgcode.roa.framework.parameters.Late`                                                     |
| API Authentication annotation                        | `io.cyborgcode.roa.api.annotations.AuthenticateViaApi`                                            |
| Credentials contract                                 | `io.cyborgcode.roa.api.authentication.Credentials`                                                |
| Base auth client (when implementing auth client)     | `io.cyborgcode.roa.api.authentication.BaseAuthenticationClient`                                   |
| API Hooks annotation                                 | `io.cyborgcode.roa.api.annotations.ApiHook`                                                       |
| Hook execution enum                                  | `io.cyborgcode.roa.framework.hooks.HookExecution`                                                 |
| Retry / eventual consistency                         | `io.cyborgcode.roa.api.retry.RetryConditionApi`                                                   |
| Reusable constants (examples)                        | `static {project}.constants.QueryParams.PAGE_PARAM` *(etc.)*                                      |
| API config access                                    | `static io.cyborgcode.roa.api.config.ApiConfigHolder.getApiConfig`                                |

---

## Mistakes to Avoid

| Mistake                                    | Why it’s wrong                                                           | Do this instead                                                                         |
|--------------------------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| Calling RestAssured directly in tests      | Breaks ROA abstraction & reporting                                       | Use `quest.use(RING_OF_API)` only                                                       |
| Hardcoding full URLs                       | Makes tests non-portable across envs                                     | Use API config (e.g. `getApiConfig().baseUrl()`)                                        |
| Building query strings manually            | Error-prone & unreadable                                                 | Use `withQueryParam`, `withPathParam`, `withHeader`                                     |
| Forgetting `.complete()`                   | Soft assertions won’t flush; lifecycle not finalized                     | Always end with `.complete()`                                                           |
| Scattering JSONPaths as strings everywhere | Hard to maintain                                                         | Centralize in `ApiResponsesJsonPaths` enum                                              |
| Mixing “setup flows” inside every test     | Duplicates logic                                                         | Use `@AuthenticateViaApi`, `@Journey`, `CustomService`                                  |
| Hardcoded credentials                      | Secrets leak + tests break across environments (`"admin"`, `"password"`) | Load from test data/config (`Data.testData().username()`, `Data.testData().password()`) |

---

## Example 1: Minimal GET (Status + Header)

> All constants come from {project}.constants.* classes (QueryParams, PathVariables, Headers, TestConstants).

```java
@Test
void getAllUsersMinimal(Quest quest) {
   quest
         .use(RING_OF_API)
         .requestAndValidate(
               GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO),
               Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
               Assertion.builder().target(HEADER).key(CONTENT_TYPE).type(CONTAINS).expected(APPLICATION_JSON).build()
         )
         .complete();
}
```

---

## Example 2: Path Param + Body Assertion + Storage Read

> All constants come from {project}.constants.* classes (QueryParams, PathVariables, Headers, TestConstants).

```java
@Test
void getUserPathParamAndStorageRead(Quest quest) {
   quest
         .use(RING_OF_API)
         .requestAndValidate(
               GET_USER.withPathParam(ID_PARAM, USER_ID_THREE),
               Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
               Assertion.builder().target(BODY).key(ApiResponsesJsonPaths.USER_ID).type(IS).expected(USER_ID_THREE_INT).build()
         )
         .validate(() -> {
            Response response = retrieve(StorageKeysApi.API, GET_USER, Response.class);

            // Extra checks (plain JUnit assertions), without duplicating requests:
            // Assertions.assertNotNull(response.jsonPath().get("data"));
         })
         .complete();
}
```

**Pattern to remember:**

* `requestAndValidate(...)` stores the last response in `StorageKeysApi.API` keyed by the endpoint enum.
* You can then `retrieve(...)` and do custom assertions.

---

## Example 3: POST with DTO Body via @Craft and Late

> All constants come from {project}.constants.* classes (QueryParams, PathVariables, Headers, TestConstants).

```java
@Test
void createUserWithCraftedBodyAndLate(
      Quest quest,
      @Craft(model = DataCreator.Data.USER_LEADER) CreateUserDto leaderUser,
      @Craft(model = DataCreator.Data.USER_MANAGER) Late<CreateUserDto> managerUserLate
) {
   quest
         .use(RING_OF_API)
         .requestAndValidate(
               POST_CREATE_USER,
               leaderUser,
               Assertion.builder().target(STATUS).type(IS).expected(SC_CREATED).build()
         )
         .requestAndValidate(
               POST_CREATE_USER,
               managerUserLate.create(),
               Assertion.builder().target(STATUS).type(IS).expected(SC_CREATED).build()
         )
         .complete();
}
```

---

## Example 4: Authentication via @AuthenticateViaApi

> All constants come from {project}.constants.* classes (QueryParams, PathVariables, Headers, TestConstants).

```java
@Test
@AuthenticateViaApi(credentials = AdminAuth.class, type = AppAuth.class, cacheCredentials = true)
void authenticatedCallReusesAuth(Quest quest) {
   quest
         .use(RING_OF_API)
         .requestAndValidate(
               GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO),
               Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build()
         )
         .complete();
}
```

**Notes:**

* `cacheCredentials = true` can reuse auth header across tests (same run) when appropriate.
* Your `AppAuth` implementation defines how to produce the auth header.

---

## Example 5: Journey + Ripper Cleanup

> All constants come from {project}.constants.* classes (QueryParams, PathVariables, Headers, TestConstants).

```java
@Test
@Journey(
      value = Preconditions.Data.CREATE_NEW_USER,
      journeyData = {@JourneyData(DataCreator.Data.USER_INTERMEDIATE)},
      order = 1
)
@Ripper(targets = {DataCleaner.Data.DELETE_USER_INTERMEDIATE})
void journeyCreatesUserThenCleanup(Quest quest) {
   quest
         .use(RING_OF_API)
         .requestAndValidate(
               GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO),
               Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build()
         )
         .complete();
}
```

---

## Example 6: Retry

> All constants come from {project}.constants.* classes (QueryParams, PathVariables, Headers, TestConstants).

```java
@Test
void retryUntilGetUserIsOk(Quest quest) {
   quest
         .use(RING_OF_API)
         .retryUntil(
               GET_USER.withPathParam(ID_PARAM, USER_ID_THREE),
               RetryConditionApi.statusIsOk(),
               Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build()
         )
         .complete();
}
```

---

## Generation Rules (for API tests)

* Always start with `quest.use(RING_OF_API)`.
* Never use wildcard imports (`*`). Import only what you use.
* Prefer `requestAndValidate(...)` over manual request + scattered assertions.
* Use endpoint parameterization:

    * `withQueryParam(key, value)`
    * `withPathParam(key, value)`
    * `withHeader(key, value)`
* Do not hardcode test data (params/ids/headers/values). Use reusable constants (e.g. `QueryParams`, `PathVariables`, `Headers`, `TestConstants`).
* Centralize JSONPaths in an enum (e.g., `ApiResponsesJsonPaths`).
* Never hardcode environment base URLs in tests.
* Use storage reads only when needed:

    * `retrieve(StorageKeysApi.API, endpointEnum, Response.class)`
* For login/auth, prefer `@AuthenticateViaApi` over repeating login requests.
* For setup data, prefer `@Journey` (+ `@JourneyData`) over “setup code” inside test bodies.
* For cleanup, always use `@Ripper` (idempotent cleaners).
* End every test with `.complete()`.
