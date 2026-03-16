**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md` for metadata and usage of ROA framework classes.

# API extractors package

## Goal

Centralize **all JSONPath expressions** used by ROA API tests so tests never contain raw path strings like:
- `"data[0].id"`
- `"token"`
- `"error"`

This keeps assertions consistent, discoverable, and easy to update if the API contract changes.

---

## What belongs here

### `ApiResponsesJsonPaths` (JSONPath registry)

A single enum that:
- stores JSONPath patterns as strings
- supports indexed/list paths via placeholders (e.g. `%d`)
- exposes one method: `getJsonPath(Object... args)` to format when needed

This enum is used by:
- `Assertion.builder().key(...)` for BODY assertions
- auth clients extracting tokens (e.g. `TOKEN.getJsonPath()`)
- any response parsing that requires a JSONPath

---

## Rules (non-negotiable)

- **No raw JSONPath strings in tests.** Always use `ApiResponsesJsonPaths`.
- Keep this package **paths only**:
  - no RestService calls
  - no Quest/storage logic
  - no assertions
- Use consistent naming:
  - `TOKEN`, `ERROR`, `TOTAL_PAGES`, `USER_ID`, etc.
- For indexed paths, prefer placeholders:
  - `USER_ID("data[%d].id")`
  - call `USER_ID.getJsonPath(0)` rather than building strings manually
- Add/update paths based on contract (Swagger/OpenAPI), not guesswork.
- If a required response field JSONPath is missing, **add a new enum constant** to `ApiResponsesJsonPaths` (do not use a raw JSONPath string as a workaround).
  - Prefer placeholders for indexed paths (e.g. `data[%d].id`) and call `getJsonPath(index)` in tests.
  - Name constants clearly by meaning (`USER_AVATAR_BY_INDEX`, `SINGLE_USER_EMAIL_EXPLICIT`, `SUPPORT_URL`, etc.).

---

## Template: `ApiResponsesJsonPaths`

```java
public enum ApiResponsesJsonPaths {

  TOTAL("total"),
  TOTAL_PAGES("total_pages"),
  DATA("data"),

  USER_ID("data[%d].id"),
  USER_FIRST_NAME("data[%d].first_name"),

  TOKEN("token"),
  ERROR("error");

  private final String jsonPath;

  ApiResponsesJsonPaths(String jsonPath) {
    this.jsonPath = jsonPath;
  }

  public String getJsonPath(Object... args) {
    if (args != null && args.length > 0) {
      return String.format(jsonPath, args);
    }
    return jsonPath;
  }
}
````

---

## Usage patterns

### Assertion builder (BODY)

```java
Assertion.builder()
  .target(BODY)
  .key(ApiResponsesJsonPaths.TOTAL_PAGES.getJsonPath())
  .type(IS)
  .expected(EXPECTED_TOTAL_PAGES)
  .build();
```

### Indexed field

```java
Assertion.builder()
  .target(BODY)
  .key(ApiResponsesJsonPaths.USER_ID.getJsonPath(ZERO_INDEX))
  .type(IS)
  .expected(USER_ID_THREE_INT)
  .build();
```

### Auth token extraction (example)

```java
String token = response.jsonPath().getString(ApiResponsesJsonPaths.TOKEN.getJsonPath());
```

---

## DO / DON'T

### DO

* Keep all JSONPaths in `ApiResponsesJsonPaths`.
* Use placeholders for list/index access.
* Use these paths in assertions, auth flows, and any parsing logic.
* Rename/update paths in one place when the contract changes.

### DON'T

* Don’t hardcode JSONPath strings in tests or services.
* Don’t add request/validation logic into this package.
* Don’t create multiple competing JSONPath registries.
* Don’t mix “constants” like ids/params/status codes here (those belong in `{project}.constants.*`).

