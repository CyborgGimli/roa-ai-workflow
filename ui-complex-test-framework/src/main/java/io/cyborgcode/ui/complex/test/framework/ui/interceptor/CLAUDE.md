# UI request interception package

## Description

**Goal:** Intercept backend responses triggered by UI actions, store matched response bodies, and enable extraction for validations/test data.

**How it works**
- Add `@InterceptRequests(...)` on the test method.
- While the test runs, ROA intercepts network traffic.
- For each request URL that **contains** any configured substring, ROA **keeps the response body**.
- Stored bodies can be extracted later (e.g., via `DataExtractorsUi` + `retrieve(...)`) for assertions or context.

**Provide in this area**
- `RequestsInterceptor` enum (matchers): named endpoint substrings used for interception.
- Optional extraction helpers live elsewhere (not in this enum).

**Core rules**
- Interceptor enums are **declarative**: name + endpoint substring only.
- Use `RequestsInterceptor.Data.*` string constants in annotations (no duplicated strings).
- Choose substrings that are **stable** and **specific** (avoid overmatching).
- Do not parse/extract inside interceptor enums.

**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.


---

## Minimal example (2 matchers)

### `RequestsInterceptor` enum (registry only)
```java
import io.cyborgcode.roa.ui.parameters.DataIntercept;

public enum RequestsInterceptor implements DataIntercept<RequestsInterceptor> {

  INTERCEPT_AUTH_SESSION("/api/auth/session"),
  INTERCEPT_ORDER_UPDATE("/api/orders/update");

  public static final class Data {
    public static final String INTERCEPT_AUTH_SESSION = "INTERCEPT_AUTH_SESSION";
    public static final String INTERCEPT_ORDER_UPDATE = "INTERCEPT_ORDER_UPDATE";
    private Data() {}
  }

  private final String endpointSubString;

  RequestsInterceptor(String endpointSubString) {
    this.endpointSubString = endpointSubString;
  }

  @Override
  public String getEndpointSubString() {
    return endpointSubString;
  }

  @Override
  public RequestsInterceptor enumImpl() {
    return this;
  }
}
```

---

## Usage in tests (single example)

```java
@Test
@InterceptRequests(requestUrlSubStrings = {
  RequestsInterceptor.Data.INTERCEPT_AUTH_SESSION,
  RequestsInterceptor.Data.INTERCEPT_ORDER_UPDATE
})
void test(Quest quest) {
  // execute UI logic first (actions that trigger backend calls)

  String token = retrieve(
    DataExtractorsUi.responseBodyExtraction(
      RequestsInterceptor.INTERCEPT_AUTH_SESSION.getEndpointSubString(),
      "$.token"
    ),
    String.class
  );

  // validate / use extracted data...
}
```

---

## DO / DON'T

**DO**
- Prefer specific substrings like `/api/auth/session` over generic ones like `/api`.
- Keep names intent-revealing: `INTERCEPT_AUTH_SESSION`, `INTERCEPT_ORDER_UPDATE`, etc.
- Keep all matchers in one place (`RequestsInterceptor`) unless domain separation improves clarity.
- Use interception only when the test needs backend response data (validation/test data).

**DON'T**
- Don’t implement parsing/extraction logic in `RequestsInterceptor`.
- Don’t use unstable substrings (query params, timestamps, random IDs).
- Don’t rename existing enum constants without updating `Data.*` strings and test annotations.
