**Goal:** Centralize ROA test data: **create**, **clean**, and **load** configuration-backed/static values.

**Folders**
- `data/test_data` — config-backed values (`OWNER`) + static providers (`StaticDataProvider`).
- `data/creator` — data creation registries used by `@Craft` (`DataForge` / `Late<T>`).
- `data/cleaner` — teardown registries used by `@Ripper` (`DataRipper`).

**Core rules**
- Config values live in `DataProperties`, accessed only via `Data.testData()`.
- Static providers are enabled via `@StaticTestData(...)` and retrieved by key.
- DataCreator/DataCleaner enums are registries; implementation lives in `*Functions`.

**Source of truth:** skill-name:roa-pandora-metadata `.claude/skills/roa-pandora-metadata/SKILL.md/` for metadata and usage of ROA framework classes.


# Examples

---

## `cleaner/` — teardown via `@Ripper`

**Files**
- `DataCleaner` — enum registry implementing `DataRipper<DataCleaner>`
- `DataCleanerFunctions` — cleanup implementations (`Consumer<SuperQuest>`)

**Rules**
- `DataCleaner` is **registry only** (enums + mapping). No cleanup logic.
- Each enum maps to a function in `DataCleanerFunctions`.
- Cleanup functions receive `SuperQuest` and can use storage/artifacts/context.
- Don't use RING_OF_UI meaning any ui tech for clean up data. Try to clean via database or api if applicable

### Minimal example: `DataCleaner`
```java
public enum DataCleaner implements DataRipper<DataCleaner> {

   DELETE_CREATED_ORDERS(DataCleanerFunctions::cleanAllOrders);

   public static final class Data {

      public static final String DELETE_CREATED_ORDERS = "DELETE_CREATED_ORDERS";

      private Data() {
      }

   }

   private final Consumer<SuperQuest> cleanUpFunction;

   DataCleaner(final Consumer<SuperQuest> cleanUpFunction) {
      this.cleanUpFunction = cleanUpFunction;
   }


   @Override
   public Consumer<SuperQuest> eliminate() {
      return cleanUpFunction;
   }

   @Override
   public DataCleaner enumImpl() {
      return this;
   }

}
```

### Minimal example: `DataCleanerFunctions`
```java
public final class DataCleanerFunctions {

  private DataCleanerFunctions() {}

  public static void cleanAllOrders(SuperQuest quest) {
     // implement cleanup logic here using SuperQuest context/storage/artifacts
  }
}
```

### Usage in test
```java
@Test
@Ripper(targets = { DataCleaner.Data.DELETE_CREATED_ORDERS })
void test(Quest quest) { }
```

**Important**
- `@Ripper.targets` are **strings** that must match enum names.
- Use `DataCleaner.Data.*` to avoid duplicating strings in tests.

---

## `creator/` — parameter injection via `@Craft`

**Files**
- `DataCreator` — enum registry implementing `DataForge<DataCreator>`
- `DataCreatorFunctions` — Data creation implementations (`Late<Object>`)

**Late** - Same as Supplier<Object>
```java
@FunctionalInterface
public interface Late<T> { T create(); }
```

**Rules**
- `DataCreator` is **registry only** (enums + mapping). No DataCreation logic.
- Each enum maps to a function in `DataCreatorFunctions`.
- DataCreation functions can access current test context via:
  - `SuperQuest superQuest = QuestHolder.get();`

### Minimal example: `DataCreator` (eager or lazy)
```java
public enum DataCreator implements DataForge<DataCreator> {

   USER_LEADER(DataCreatorFunctions::leaderUser),

   public static final class Data {

      private Data() {
      }

      public static final String USER_LEADER = "USER_LEADER";
   }

   private final Late<Object> createDataFunction;

   DataCreator(final Late<Object> createDataFunction) {
      this.createDataFunction = createDataFunction;
   }

   @Override
   public Late<Object> dataCreator() {
      return createDataFunction;
   }

   @Override
   public DataCreator enumImpl() {
      return this;
   }

}
```

### Minimal example: `DataCreatorFunctions`
```java
public final class DataCreatorFunctions {

  private DataCreatorFunctions() {}

   public static CreateUserDto leaderUser() {
     //implement logic for creation of CreateUserDto
     //use SuperQuest quest = QuestHolder.get(); to access current test context if needed (storage, articats...)
   }

}
```

### Usage in test (eager)
```java
@Test
void test(Quest quest,
          @Craft(model = DataCreator.Data.USER_LEADER) CreateUserDto leaderUser) { }
```

### Usage in test (lazy)
```java
@Test
void test(Quest quest,
          @Craft(model = DataCreator.Data.USER_LEADER) Late<CreateUserDto> leaderUser) {
  CreateUserDto u = leaderUser.create();
}
```

**Important**
- `@Craft.model` is a **string** that must match enum names.
- Use `DataCreator.Data.*` to avoid duplicating strings in tests.

---

## `test_data/` — config-backed + static test data

### Config-backed values (OWNER)
**Files**
- `Data` — singleton accessor
- `DataProperties` — keys/accessors (extends `PropertyConfig` using aeonbits.owner library)

**Rules**
- Add environment-dependent values as methods in `DataProperties` (`@Key(...)`).
- Access config only via `Data.testData()`.

#### Minimal example: `Data`
```java
public final class Data {

  private Data() {}

  public static DataProperties testData() {
    return ConfigCache.getOrCreate(DataProperties.class);
  }
}
```

#### Minimal example: `DataProperties`
```java
@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({"system:properties", "classpath:${test.data.file}.properties"})
public interface DataProperties extends PropertyConfig {

  @Key("seller.email")
  String sellerEmail();
}
```

**Usage (tests/services)**
```java
String sellerEmail = Data.testData().sellerEmail();
```

---

### Static test data provider
**File**
- `StaticData` — implements `StaticDataProvider` and provides a `Map<String,Object>`

**Rules**
- Keep stable keys as `public static final String ...` in the provider.
- Use in tests with `@StaticTestData(...)` and retrieve by key.

#### Minimal example: `StaticData`
```java
public class StaticData implements StaticDataProvider {

  public static final String SELLER = "seller";

  @Override
  public Map<String, Object> staticTestData() {
    Map<String, Object> data = new HashMap<>();
    data.put(SELLER, DataCreatorFunctions.createSeller());
    return data;
  }
}
```

**Usage in test**
```java
@Test
@StaticTestData(StaticData.class)
void test(Quest quest) {
  Seller seller = retrieve(staticTestData(StaticData.SELLER), Seller.class);
  // use seller in the flow...
}
```


---

## DO / DON'T

**DO**
- Keep registries thin (`DataCleaner`, `DataCreator`): enums + ROA interfaces only.
- Put implementations only in `*Functions` classes.
- Use `*.Data.*` constants for annotation strings.
- Read config through `Data.testData()` only.
- Use `QuestHolder.get()` inside DataCreation functions when context is required.

**DON'T**
- Don't use UI in clean up data. Use api or database if applicable
- Don’t hardcode environment-specific values in code.
- Don’t implement cleanup/DataCreation logic inside enums or tests.
- Don’t change enum names without updating corresponding `Data.*` string constants and usages.
