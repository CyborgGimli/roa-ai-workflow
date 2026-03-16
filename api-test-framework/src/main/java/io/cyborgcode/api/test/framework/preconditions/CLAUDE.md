# Preconditions package

## Description

**Goal:** Provide reusable pre-test setup (“journeys”) executed before the test body.

**Provide in this area**
- Preconditions registry (`Preconditions`) — named journeys used by `@Journey(...)`.
- Preconditions logic (`PreconditionFunctions`) — implementation of journeys.

**Core rules**
- `Preconditions` is a **registry only**: enum entries + mapping to functions.
- Journey signature is `BiConsumer<SuperQuest, Object[]>`:
  - `SuperQuest` = active quest context
  - `Object[]` = optional inputs (ordered; casted by convention)
- Use `Preconditions.Data.*` string constants in annotations (no duplicated strings).
- Journey data is passed via `@JourneyData(...)` and uses `DataCreator` models.
- `@JourneyData(late = true)` behaves like `Late<T>`: created on demand.

**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md/` for metadata and usage of ROA framework classes.

---

## Contract

- Signature: `BiConsumer<SuperQuest, Object[]>`
- Preconditions are **setup-only**:
  - prepare state
  - optionally validate prerequisites
  - optionally store context (storage/artifacts) for the test

---

### `@Journey` annotation (how it works)
`@Journey` is **repeatable** on a test method. Each usage schedules one precondition execution.

```java
@Repeatable(PreQuest.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Journey {
  String value();                 // precondition name (matches enum name via Preconditions.Data.*)
  JourneyData[] journeyData() default {}; // optional inputs (DataCreator models)
  int order() default 0;          // execution order (ascending)
}
```

- Use multiple `@Journey(...)` annotations to run multiple preconditions.
- `order` controls execution sequence.
- `journeyData` provides optional inputs (resolved from `DataCreator`, optionally `late=true`).

## Minimal example (2 preconditions)

### `Preconditions` enum (registry only)
```java
import io.cyborgcode.roa.framework.parameters.PreQuestJourney;
import io.cyborgcode.roa.framework.quest.SuperQuest;

import java.util.function.BiConsumer;

public enum Preconditions implements PreQuestJourney<Preconditions> {

  CREATE_DEFAULT_USER((quest, args) -> PreconditionFunctions.createDefaultUser(quest)),
  CREATE_NEW_USER((quest, args) -> PreconditionFunctions.createNewUser(quest, (CreateUserDto) args[0]));
  CREATE_NEW_USER_LATE((quest, args) -> PreconditionFunctions.createNewUser(quest, (Late<CreateUserDto>) args[0]));

  public static final class Data {
    public static final String CREATE_DEFAULT_USER = "CREATE_DEFAULT_USER";
    public static final String CREATE_NEW_USER = "CREATE_NEW_USER"; 
    public static final String CREATE_NEW_USER_LATE = "CREATE_NEW_USER_LATE";
	
    private Data() {}
  }

  private final BiConsumer<SuperQuest, Object[]> function;

  Preconditions(BiConsumer<SuperQuest, Object[]> function) {
    this.function = function;
  }

  @Override
  public BiConsumer<SuperQuest, Object[]> journey() {
    return function;
  }

  @Override
  public Preconditions enumImpl() {
    return this;
  }
}
```

### `PreconditionFunctions` (logic only)
```java
public final class PreconditionFunctions {

  private PreconditionFunctions() {}

  public static void createDefaultUser(SuperQuest quest) {
    // implement setup logic (may use rings, store context, etc.)
  }

  public static void createNewUser(SuperQuest quest, CreateUserDto user) {
    // implement setup logic using provided argument(s)
  }

  public static void createNewUser(SuperQuest quest, Late<CreateUserDto> user) {
    // implement setup logic using provided argument(s)
  }
}
```

---

## Usage in tests

### Simple journey
```java
@Test
@Journey(value = Preconditions.Data.CREATE_DEFAULT_USER, order = 1)
void test(Quest quest) { }
```

### Journey with data (uses `DataCreator`)
```java
@Test
@Journey(
  value = Preconditions.Data.CREATE_NEW_USER,
  journeyData = { @JourneyData(DataCreator.Data.USER_LEADER) },
  order = 1
)
@Journey(
  value = Preconditions.Data.CREATE_NEW_USER,
  journeyData = { @JourneyData(value = DataCreator.Data.USER_INTERMEDIATE, late = true) },
  order = 2
)
void test(Quest quest) { }
```

`JourneyData`:
```java
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JourneyData {
  String value();
  boolean late() default false;
}
```

- `late=false`: model resolved eagerly before the precondition runs.
- `late=true`: model provided as `Late<T>` (created on demand inside precondition logic).

---

## DO / DON'T

**DO**
- Keep `Preconditions` as wiring only (enum entries + mapping).
- Put all logic into `PreconditionFunctions`.
- Use `Preconditions.Data.*` constants in annotations.
- For `Object[] args`: keep order stable, cast explicitly, validate when needed.
- Use `JourneyData(..., late=true)` only when creation must be deferred.

**DON'T**
- Don’t put setup logic inside the enum.
- Don’t hardcode environment-specific values; prefer `Data.testData()` / `DataCreator`.
- Don’t create multiple competing precondition registries.
