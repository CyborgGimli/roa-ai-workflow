**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md` for metadata and usage of ROA framework classes.

# API authentication package

## Goal

Provide **declarative API authentication** for ROA tests via `@AuthenticateViaApi`, without repeating authentication flows in every test.

This package contains two responsibilities only:

1. **Credentials provider**
   - Implements `io.cyborgcode.roa.api.authentication.Credentials`
   - Reads authentication inputs from config-backed test data (`Data.testData()`)

2. **Authentication client**
   - Extends `io.cyborgcode.roa.api.authentication.BaseAuthenticationClient`
   - Performs the authentication flow using `RestService` (login, token exchange, session creation, etc.)
   - Extracts/builds the required authentication artifact (use `ApiResponsesJsonPaths` when reading response fields)
   - Returns a `io.restassured.http.Header` (format is application-specific)

   Examples:
   - `Authorization: Bearer <token>` (common)
   - `X-Auth-Token: <token>`
   - `Cookie: SESSION=<id>`
   - Any other required auth header

---

## How ROA uses these classes

When a test is annotated:

```java
@AuthenticateViaApi(credentials = AdminAuth.class, type = AppAuth.class, cacheCredentials = true)
````

ROA will:

1. Instantiate `AdminAuth` to obtain `username()` / `password()` (or other credential inputs)
2. Invoke `AppAuth.authenticateImpl(restService, username, password)`
3. Store the returned header and apply it to subsequent API calls
4. If `cacheCredentials = true`, reuse the header across tests in the same run (when appropriate)

---

## Files

### `AdminAuth` (Credentials provider)

**Rules**

* Must be a tiny class. No logic beyond reading config.
* No hardcoded values.
* Values must come from OWNER-backed config, via `Data.testData()`.

**Template**

```java
public class AdminAuth implements Credentials {

  @Override
  public String username() {
    return Data.testData().username();
  }

  @Override
  public String password() {
    return Data.testData().password();
  }
}
```

### `AppAuth` (Authentication client)

**Rules**

* Must extend `BaseAuthenticationClient`.
* Must use the ROA API `RestService` to execute authentication.
* Must not call RestAssured directly inside tests.
* If reading response fields (token, session id, etc.), use `ApiResponsesJsonPaths` (no raw strings).
* Must return a `Header` only (ROA will apply it).

**Template (token-based example)**

```java
public class AppAuth extends BaseAuthenticationClient {

  @Override
  protected Header authenticateImpl(RestService restService, String username, String password) {
    String token = restService
      .request(POST_LOGIN_USER, new LoginDto(username, password))
      .getBody()
      .jsonPath()
      .getString(TOKEN.getJsonPath());

    return new Header(AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE + token);
  }
}
```

---

## Usage in tests (pattern)

```java
@Test
@AuthenticateViaApi(credentials = AdminAuth.class, type = AppAuth.class, cacheCredentials = true)
void authenticatedCall(Quest quest) {
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

## DO / DON'T

### DO

* Keep credentials providers minimal and config-driven (`Data.testData()` only).
* Keep all auth flow logic in `AppAuth` (centralize authentication + header construction).
* Use `ApiResponsesJsonPaths` for any fields extracted from responses.
* Use `cacheCredentials=true` only when reuse across tests in the same run is correct.

### DON'T

* Don’t hardcode credentials/secrets in tests, services, or DTOs.
* Don’t duplicate authentication requests inside tests.
* Don’t parse values using raw JSON path strings (e.g., `"token"`).
* Don’t return anything other than a `Header` from auth logic.
* Don’t mix general API testing concerns (endpoints, assertions) into auth classes.

