**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md` for metadata and usage of ROA framework classes.

# API package (root) — endpoints registry

## Goal

This folder is the **root of the project API layer**. It contains:

- `AppEndpoints` — the **single typed registry** of all HTTP endpoints used by ROA API tests.
- Subpackages that support API testing:
  - `authentication/` — declarative auth via `@AuthenticateViaApi`
  - `dto/` — request/response models
  - `extractors/` — centralized JSON path registry
  - `hooks/` — class-level API hook flows

This root package must stay small and opinionated: **typed endpoints live here**.

---

## AppEndpoints (single source of truth)

`AppEndpoints` is the only place where method + relative URL definitions exist.

**Rule:** Tests and services must call APIs using endpoint enums only:
- ✅ `GET_ALL_USERS`
- ✅ `POST_LOGIN_USER`
- ❌ raw `Method.GET` + `"/users"` in tests
- ❌ building full URLs or query strings manually

### Contract (required)

`AppEndpoints` must:

- be an `enum`
- implement `io.cyborgcode.roa.api.core.Endpoint<AppEndpoints>`
- define for each constant:
  - `Method method`
  - `String url` (relative path, may contain `{pathParams}`)
- implement:
  - `method()`
  - `url()`
  - `enumImpl()`

---

## Default configuration (global request setup)

All endpoints share a **default configuration** applied through:

```java
@Override
public RequestSpecification defaultConfiguration() {
  RequestSpecification spec = Endpoint.super.defaultConfiguration();
  spec.contentType(ContentType.JSON);
  spec.header(API_KEY_HEADER, API_KEY_VALUE);
  return spec;
}
````

**Rules**

* Common headers required for every call (e.g., `x-api-key`) must be added here.
* Do not hardcode secrets: header values must come from config/constants (never inline strings).
* Keep it global and stable. Endpoint-specific headers should usually be passed using `withHeader(...)` at call-site.

---

## Endpoint parameterization (no manual URL building)

To customize requests, use endpoint parameterization:

* `withQueryParam(key, value)`
* `withPathParam(key, value)`
* `withHeader(key, value)`

**Examples**

* `GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO)`
* `GET_USER.withPathParam(ID_PARAM, ID_THREE)`
* `GET_USER.withHeader(AUTHORIZATION_HEADER_KEY, bearerToken)`

**Don’t**

* Don’t concatenate strings (`"/users?page=" + page`)
* Don’t build query strings manually
* Don’t embed IDs directly in URLs (use `{id}` + `withPathParam`)

---

## Adding a new endpoint (checklist)

When a new API operation is needed:

1. Add a new enum constant in `AppEndpoints`:

    * Correct `Method`
    * Correct relative `url` (use `{param}` placeholders when needed)
2. Reuse the same default configuration (do not duplicate headers in tests).
3. Use the new constant everywhere (tests/services) instead of raw paths/methods.
4. If the test needs new JSON fields, add them to `api/extractors/ApiResponsesJsonPaths` (no raw JSONPath strings).

---

## Subpackage map (where to put things)

| Area       | Put it here           | What it contains                                                                     |
|------------|-----------------------|--------------------------------------------------------------------------------------|
| Auth       | `api/authentication/` | `Credentials` + `BaseAuthenticationClient` implementations for `@AuthenticateViaApi` |
| DTOs       | `api/dto/`            | Request/response models (Lombok/Jackson)                                             |
| JSON paths | `api/extractors/`     | `ApiResponsesJsonPaths` registry (no raw strings in tests)                           |
| Hooks      | `api/hooks/`          | `ApiHookFlows` + `ApiHookFunctions` used by `@ApiHook`                               |

---

## DO / DON'T

### DO

* Keep `AppEndpoints` as the only endpoint registry.
* Put global request defaults into `defaultConfiguration()` (content type, mandatory headers).
* Use endpoint parameterization methods (`withQueryParam`, `withPathParam`, `withHeader`).
* Keep endpoint URLs relative (base URL comes from ROA config).

### DON'T

* Don’t define endpoints in tests or services.
* Don’t hardcode URLs, query strings, or credentials.
* Don’t create a second endpoints enum in another package.
* Don’t scatter JSON paths in tests; add them to `ApiResponsesJsonPaths`.

