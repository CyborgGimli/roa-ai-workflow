**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.


# Base package: Rings registry

This folder contains **only one class**: `Rings.java`.

`Rings` is a **registry of ring tokens** (class references) used with:

- `Quest#use(Class)`

No logic. No methods. **Constants only.**

---

## What goes into `Rings.java`

Each constant must reference a class that **extends**:

- `io.cyborgcode.roa.framework.chain.FluentService`

Rings are used like:

```java
quest
  .use(Rings.RING_OF_API)
  .requestAndValidate(
      GET_ALL_USERS.withQueryParam(PAGE_PARAM, PAGE_TWO),
      Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build()
  );
```

---

## Defaults

Projects usually start with:

- `RING_OF_API`
- `RING_OF_UI`
- `RING_OF_DB`

Add new rings only when introducing a new fluent service.

---

## Template: `Rings.java`

```java
@UtilityClass
public class Rings {

  public static final Class<RestServiceFluent> RING_OF_API = RestServiceFluent.class;
  public static final Class<CustomService> RING_OF_CUSTOM = CustomService.class;
  public static final Class<EvolutionService> RING_OF_EVOLUTION = EvolutionService.class;

}
```

---

## Add a new ring

1. Create a new fluent service class extending `FluentService`.
2. Add one constant in `Rings.java`:
   - `public static final Class<YourService> RING_OF_X = YourService.class;`
3. Use it only via `quest.use(Rings.RING_OF_X)`.

---

## DO / DON'T

**DO**
- Keep `Rings` as a pure registry (constants only).
- Use stable naming: `RING_OF_API`, `RING_OF_DB`, `RING_OF_UI`, `RING_OF_<CUSTOM_NAME>`.
- Add rings only for new fluent services.
- Select capabilities via `quest.use(...)` (do not instantiate services).

**DON'T**
- Don’t add logic/methods in `Rings`.
- Don’t create other ring registries in other packages.
- Don’t rename ring constants casually.
- Don’t instantiate fluent services in tests.
