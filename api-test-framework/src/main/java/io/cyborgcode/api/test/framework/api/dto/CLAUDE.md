**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md` for metadata and usage of ROA framework classes.

# API DTO package (request + response)

## Goal

Provide **plain request/response models** for ROA API tests so:
- request bodies are strongly-typed (no raw maps / ad-hoc JSON),
- responses can be mapped cleanly when needed,
- tests stay readable and framework-consistent.

DTOs are **data-only**. No ROA logic inside DTOs.

---

## What belongs here

### Request DTOs (`api/dto/request`)
Used as bodies in:
- `request(endpoint, body)`
- `requestAndValidate(endpoint, body, assertions...)`

Example: `CreateUserDto`, `LoginDto`, `RegisterDto`, etc.

### Response DTOs (`api/dto/response`)
Used when mapping responses from storage:
- `response.getBody().as(SomeResponseDto.class)`

Example: `CreatedUserDto`, `GetUsersDto`, etc.

---

## Rules (non-negotiable)

- DTOs contain **fields only** (state), no test logic.
- **No RestAssured / RestService / Quest / rings** in DTOs.
- Keep DTOs **serializable by Jackson** (default constructors help).
- Use Lombok for brevity:
  - `@Data`
  - `@Builder`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
- Response DTOs must tolerate contract drift:
  - `@JsonIgnoreProperties(ignoreUnknown = true)`
- Use `@JsonProperty` only when the JSON key cannot be represented cleanly in Java
  - Example: `_meta` → `@JsonProperty("_meta") private Meta meta;`

---

## Templates

### Request DTO (pattern)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDto {

  private String name;
  private String job;

}
````

### Response DTO (pattern)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatedUserDto {

  private String name;
  private String job;
  private String id;
  private String createdAt;

  @JsonProperty("_meta")
  private Meta meta;

}
```

---

## Usage patterns

### Request body (typed)

```java
quest
  .use(RING_OF_API)
  .requestAndValidate(
    POST_CREATE_USER,
    createUserDto,
    Assertion.builder().target(STATUS).type(IS).expected(SC_CREATED).build()
  )
  .complete();
```

### Response mapping (optional)

```java
quest
  .use(RING_OF_API)
  .request(GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO))
  .validate(() -> {
     Response response = retrieve(StorageKeysApi.API, GET_ALL_USERS, Response.class);
     GetUsersDto users = response.getBody().as(GetUsersDto.class);
     // assert on mapped DTO if needed
  })
  .complete();
```

---

## DO / DON'T

### DO
* Keep DTOs minimal and stable.
* Add new DTOs when the request/response structure is reused or non-trivial.
* Prefer DTOs over raw maps for bodies.
* Use response DTO mapping only when it improves clarity (otherwise JSONPath assertions are fine).

### DON'T
* Don’t add ROA annotations (`@Journey`, `@Ripper`, `@Craft`) to DTOs.
* Don’t add validation/assertion logic inside DTOs.
* Don’t hardcode environment values, secrets, or base URLs in DTOs.
* Don’t create “god DTOs” with unrelated fields—split by endpoint/contract.

