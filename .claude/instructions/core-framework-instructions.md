# **ROA Core Framework Instructions**

## Purpose of This Document

This file defines the **core mental model, syntax patterns, and invariants** of the ROA test automation framework.

It is designed for **agentic code platforms** to understand the framework's architecture, enabling accurate code
generation and modification without technology-specific assumptions (UI, API, DB).

### Source of Truth

- **Skill metadata:** skill-name:pandora `.claude/skills/pandora/SKILL.md`
- **Subfolder MD files:** Advanced patterns and detailed examples

---

## Framework Identity

**ROA** is a test automation framework built on top of **JUnit 5**.

Core characteristics:

- **Fluent chained syntax** — all test code flows as method chains
- **Quest-centric orchestration** — each test receives a `Quest` object
- **Ring-based capabilities** — services (called "rings") provide domain-specific actions
- **Annotation-driven lifecycle** — setup, cleanup, and data injection via annotations
- **Per-test isolation** — each test has independent state via storage

---

## Reading Order (MANDATORY)

When working with ROA:

1. **Read this file first** — understand Quest, Rings, lifecycle, storage, and cross-module rules
2. **Read module-level instruction files** (if they exist):
    - UI → `ui-framework-instructions.md`
    - API → `api-framework-instructions.md`
    - DB → `db-framework-instructions.md`
3. **Read module-level CLAUDE.md or CLAUDE.md** — source of truth for implementation details

---

## Test Class Structure

### Base Classes

Every test class **must extend** one of:

- `BaseQuest` — for parallel-safe, independent tests
- `BaseQuestSequential` — only when test ordering is required

```java
class MyTests extends BaseQuest {
   // tests here
}
```

### Test Method Signature

Every test method:

- Must be annotated with `@Test`
- Must accept `Quest` as the **first parameter** (injected by framework)
- Must end with `.complete()`

```java

@Test
void exampleTest(Quest quest) {
   quest
         .use(RING_OF_SOMETHING)
         .doAction()
         .complete();
}
```

---

## Quest (Central Orchestrator)

### What Quest Is

- A **strict facade** that orchestrates test execution
- **Injected automatically** by the framework via JUnit 5 parameter resolver
- Provides access to rings via `use()` method
- Owns the test's storage instance
- Must be finalized with `complete()` at the end of each test

### Quest Methods Available in Tests

| Method           | Purpose                                           |
|------------------|---------------------------------------------------|
| `use(RingClass)` | Switch to a fluent service (ring)                 |
| `complete()`     | Finalize the test — **MUST be called at the end** |

```java
quest
      .use(RING_OF_CUSTOM)
      .performAction()
      .complete();
```

### What Quest Is NOT

- ❌ A service locator
- ❌ A configuration container
- ❌ Directly accessible for internals

**Rule:** Never access Quest internals directly. All interaction goes through rings.

---

## Rings (Fluent Services)

### What a Ring Is

A ring is a **capability boundary** — a fluent service that:

- Exposes a domain-specific fluent API
- Controls one technical domain (UI, API, DB, or custom business logic)
- Hides implementation details
- Returns itself from every method to enable chaining

### Switching Rings

```java
quest
      .use(RING_OF_API)
      .request(...)
      .drop()              // Exit current ring, return to Quest
      .use(RING_OF_DB)
      .query(...)
      .complete();
```

### Ring Definition Pattern

Rings are defined as class constants in a `Rings` utility class:

```java

@UtilityClass
public class Rings {
   public static final Class<RestServiceFluent> RING_OF_API = RestServiceFluent.class;
   public static final Class<DatabaseServiceFluent> RING_OF_DB = DatabaseServiceFluent.class;
   public static final Class<CustomService> RING_OF_CUSTOM = CustomService.class;
   // Add more as needed
}
```

### Available Ring Types

The framework provides built-in rings for common domains:

- **API Ring** — REST API operations (if API module enabled)
- **DB Ring** — Database query and validation (if DB module enabled)
- **UI Ring** — Browser-based UI interactions (if UI module enabled)
- **Custom Ring** — Domain-specific business logic (user-defined)

### Ring Rules

- ❌ Never mix responsibilities across rings
- ❌ Never instantiate services directly in tests
- ✅ Always access rings via `quest.use()`
- ✅ Use `drop()` to return to Quest when switching rings

---

## Fluent Syntax Pattern

### Core Principle

**Every method returns the same instance** to enable chaining.

```java
quest
      .use(RING_OF_CUSTOM)
      .login(seller)           // returns CustomService
      .createOrder(order)      // returns CustomService
      .validateOrder(order)    // returns CustomService
      .drop()                  // returns Quest
      .use(RING_OF_API)
      .request(endpoint)       // returns ApiService
      .complete();             // finalizes test
```

### Exiting a Ring

Call `drop()` to exit the current ring and return to the Quest object:

```java
quest
      .use(RING_OF_CUSTOM)
      .doSomething()
      .drop()                  // Back to Quest
      .use(RING_OF_API)        // Switch to different ring
      .doSomethingElse()
      .complete();
```

---

## Custom Fluent Services (Custom Rings)

### Creating a Custom Ring

Custom services extend `FluentService` and are annotated with `@Ring`:

```java

@Ring("Custom")
public class CustomService extends FluentService {

   public CustomService login(User user) {
      quest
         .use(RING_OF_UI)
         .input().insert(USERNAME_FIELD, user.getUsername())
         .input().insert(PASSWORD_FIELD, user.getPassword())
         .button().click(SIGN_IN_BUTTON);
      return this;  // Always return this for chaining
   }

   public CustomService createOrder(Order order) {
      // Implementation
      return this;
   }
}
```

### Custom Service Rules

- ✅ Extend `FluentService`
- ✅ Annotate with `@Ring("Name")`
- ✅ Return `this` from every method
- ✅ Can orchestrate multiple rings internally
- ✅ Represent business-level actions (login, createOrder, etc.)

---

## Storage System

### TL;DR (for agentic tools)

ROA Storage is a **per-test, thread-local** data container attached to the active `Quest`. It stores **one or more values per enum key** (append-only), supports **namespaces via sub-storages** (e.g., `PRE_ARGUMENTS`, `ARGUMENTS`, `STATIC_DATA`, `HOOKS`, and tech namespaces like `UI/API/DB`), and is designed to pass data between **journeys, fluent chains, hooks/interceptors, and the test body**. Reads default to the **latest written value**; use index-based reads when you need older entries.

### Read/Write in tests (recommended)

```java
// Read latest values (BaseQuest helpers)
Order order = retrieve(StorageKeysTest.PRE_ARGUMENTS, DataCreator.ORDER, Order.class);

// Read via DataExtractor (best for "stored raw object + custom extraction")
String token = retrieve(
  DataExtractorsUi.responseBodyExtraction(
    RequestsInterceptor.INTERCEPT_AUTH_SESSION.getEndpointSubString(),
    "$.token"
  ),
  String.class
);

// Static test data preload
String value = retrieve(DataExtractorsTest.staticTestData(StaticData.KEY), String.class);
```

### Read outside tests

```java
SuperQuest quest = QuestHolder.get();            // current active quest
Storage storage = quest.getStorage();

Order order = storage.sub(StorageKeysTest.PRE_ARGUMENTS)
                     .getByClass(DataCreator.ORDER, Order.class);
```

### DefaultStorage shortcut

If `config.properties` contains `default.storage=UI`, calls that use `DefaultStorage.retrieve(...)` (or test helpers wired to DefaultStorage) automatically read from `storage.sub(UI)` without you explicitly calling `.sub(UI)`.

```java
Boolean selected = DefaultStorage.retrieve(MyUiKeys.CHECKBOX_SELECTED, Boolean.class);
```

### Index semantics (when multiple values exist)

- Storage keeps **all writes** for a key.
- `get(key, ...)` returns the **latest** value.
- `getByIndex(key, 1, ...)` returns the **latest**, `2` returns the **previous**, etc.


---

## Validation Pattern

Assertions are executed **inside the test method** via fluent services (“Rings”). Rings expose many **custom validation methods** (their names start with `validate...`) and also share two generic `validate(...)` overloads: one for hard assertions and one for soft assertions. Inside these lambdas you typically **retrieve data from Storage** and assert on it.

### In-Ring Validation

Rings provide `validate()` methods that accept:

- `Runnable` — **hard assertions** (fails immediately). Use `org.junit.jupiter.api.Assertions`.
- `Consumer<SoftAssertions>` — **soft assertions** (collects failures and continues). Use `org.assertj.core.api.SoftAssertions`.

```java
quest
      .use(RING_OF_CUSTOM)
      // execute actions first...
      .doAction()
      // then validate (often using values from Storage)
      .validate(() -> org.junit.jupiter.api.Assertions.assertEquals(expected, actual))
      .complete();
```

### Assertion Builder Pattern (optional)

Use the `Assertion.builder()` pattern **only when a specific custom validation method is implemented to accept `Assertion` objects** (commonly for structured API/DB checks). If a ring method does not take `Assertion` parameters, use the generic `validate(...)` overloads above.

```java
.validate(
  response,
  Assertion.builder()
    .target(TARGET)
    .key("jsonPath")
    .type(EQUALS)
    .expected(expectedValue)
    .soft(true)  // soft assertion (continues on failure)
    .build()
)
```


## Lifecycle Annotations

### @Journey (Preconditions)

Executes setup logic **before** the test body:

```java

@Test
@Journey(value = Preconditions.Data.LOGIN_PRECONDITION,
   journeyData = {@JourneyData(DataCreator.Data.SELLER)},
   order = 1)
@Journey(value = Preconditions.Data.ORDER_PRECONDITION,
   journeyData = {@JourneyData(DataCreator.Data.ORDER)},
   order = 2)
void testWithPreconditions(Quest quest) {
   // Test starts with user logged in and order created
}
```

### @Ripper (Cleanup)

Executes cleanup logic **after** test completion (even on failure):

```java

@Test
@Ripper(targets = {DataCleaner.Data.DELETE_CREATED_ORDERS})
void testWithCleanup(Quest quest) {
   // After test, orders will be deleted
}
```

### @Craft (Data Injection)

Injects typed model instances as test parameters:

```java

@Test
void testWithCraftedData(Quest quest,
                         @Craft(model = DataCreator.Data.SELLER) Seller seller,
                         @Craft(model = DataCreator.Data.ORDER) Order order) {
   // seller and order are pre-built by DataCreator
}
```

### @StaticTestData (Preloaded Data)

Loads static data into storage before test:

```java

@Test
@StaticTestData(StaticData.class)
void testWithStaticData(Quest quest) {
   String value = retrieve(staticTestData(StaticData.KEY), String.class);
}
```

---

## Enum-Based Registry Pattern

ROA uses enums with nested `Data` classes for annotation-based references.

### DataCreator Pattern

Defines reusable data factories:

```java
public enum DataCreator implements DataForge<DataCreator> {

   SELLER(DataCreatorFunctions::createSeller),
   ORDER(DataCreatorFunctions::createOrder),
   LATE_ORDER(DataCreatorFunctions::createLateOrder);

   // Nested class for string constants (used in annotations)
   public static final class Data {
      public static final String SELLER = "SELLER";
      public static final String ORDER = "ORDER";
      public static final String LATE_ORDER = "LATE_ORDER";

      private Data() {
      }
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

### Preconditions Pattern

Defines reusable setup journeys:

```java
public enum Preconditions implements PreQuestJourney<Preconditions> {

   LOGIN_PRECONDITION((quest, objects) -> loginUser(quest, (Seller) objects[0])),
   ORDER_PRECONDITION((quest, objects) -> createOrder(quest, (Order) objects[0]));

   public static final class Data {
      public static final String LOGIN_PRECONDITION = "LOGIN_PRECONDITION";
      public static final String ORDER_PRECONDITION = "ORDER_PRECONDITION";

      private Data() {
      }
   }

   private final BiConsumer<SuperQuest, Object[]> function;

   Preconditions(final BiConsumer<SuperQuest, Object[]> function) {
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

### DataCleaner Pattern

Defines reusable cleanup operations:

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

---

## Late Data Creation

For data that depends on runtime state (e.g., extracted from responses):

```java

@Test
void testWithLateData(Quest quest,
                      @Craft(model = DataCreator.Data.LATE_ORDER) Late<Order> lateOrder) {
   quest
      .use(RING_OF_CUSTOM)
      .doSomethingFirst()
      .createOrder(lateOrder.create())  // Data created at this moment
      .complete();
}
```

---

## Test Data Configuration

### External Properties

Use configuration-backed data via OWNER library:

```java
public interface DataProperties extends Config {
   String username();

   String password();

   String customerName();
}

// Access via
Data
      .testData()
      .username();
```

### Static Data Provider

Implement `StaticDataProvider` for `@StaticTestData`:

```java
public class StaticData implements StaticDataProvider {
   public static final String USERNAME = "username";
   public static final String ORDER = "order";

   @Override
   public Map<String, Object> staticTestData() {
      Map<String, Object> data = new HashMap<>();
      data.put(USERNAME, Data.testData().username());
      data.put(ORDER, DataCreatorFunctions.createOrder());
      return data;
   }
}
```

---

## Enabling Technology Modules

Use class-level annotations to enable specific rings:

```java

@UI   // Enables UI ring
@API  // Enables API ring
@DB   // Enables DB ring
class MyTests extends BaseQuest {
   // Tests can now use RING_OF_UI, RING_OF_API, RING_OF_DB
}
```

Additional module-specific annotations may exist (e.g., `@DbHook`, `@AuthenticateViaUi`, `@InterceptRequests`). Refer to
module-specific instruction files.

---

## Project Structure Convention

```
src/main/java/{projectName}/common/
├── base/
│   └── Rings.java                    # Ring class constants
├── data/
│   ├── creator/
│   │   ├── DataCreator.java          # Data factory enum
│   │   └── DataCreatorFunctions.java # Factory implementations
│   ├── cleaner/
│   │   ├── DataCleaner.java          # Cleanup enum
│   │   └── DataCleanerFunctions.java # Cleanup implementations
│   ├── extractor/
│   │   └── DataExtractorFunctions.java
│   └── test_data/
│       ├── Data.java                 # Config accessor
│       ├── DataProperties.java       # Config interface
│       └── StaticData.java           # Static data provider
├── preconditions/
│   ├── Preconditions.java            # Journey enum
│   └── PreconditionFunctions.java    # Journey implementations
└── service/
    └── CustomService.java            # Custom fluent service

src/test/java/
└── tests/
    └── MyTests.java                  # Test classes
```

---

## Summary of Key Rules

| Rule                | Description                                                       |
|---------------------|-------------------------------------------------------------------|
| Extend BaseQuest    | All test classes must extend `BaseQuest` or `BaseQuestSequential` |
| Quest parameter     | Every test method receives `Quest` as first parameter             |
| End with complete() | Every test must call `.complete()` at the end                     |
| Use rings           | Access capabilities via `quest.use(RingClass)`                    |
| Return this         | Custom service methods must return `this` for chaining            |
| drop() to switch    | Call `drop()` to exit a ring and return to Quest                  |
| Enum + Data class   | Use enum pattern with nested `Data` class for annotations         |
| Storage per test    | Each test has isolated storage, access via `retrieve()`           |

---

## Quick Reference: Test Anatomy

```java

@UI
@API
@DB
class ExampleTests extends BaseQuest {

   @Test
   @Journey(value = Preconditions.Data.SETUP,
         journeyData = {@JourneyData(DataCreator.Data.MODEL)})
   @Ripper(targets = {DataCleaner.Data.CLEANUP})
   void completeExample(Quest quest,
                        @Craft(model = DataCreator.Data.MODEL) Model model) {
      quest
            .use(RING_OF_CUSTOM)
            .businessAction(model)
            .validate(() -> Assertions.assertTrue(condition))
            .drop()
            .use(RING_OF_API)
            .request(endpoint)
            .validate(response, Assertion.builder()
                  .target(STATUS).type(IS).expected(200).build())
            .complete();
   }
}
```

---

This document provides the **global rules and patterns** of the ROA framework. Module-specific instruction files explain
**how things work inside each module** (UI, API, DB). This file explains **how everything fits together and stays
consistent**.
