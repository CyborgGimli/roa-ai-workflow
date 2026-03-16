# **ROA UI Framework Instructions**

## Purpose of This Document

This file provides the **UI architecture and basic patterns** of the ROA framework for **agentic code platforms**.

**For advanced patterns and edge cases**, check subfolder MD files (e.g. `CLAUDE.md`).

### Source of Truth
- **Skill metadata:** skill-name:pandora `.claude/skills/pandora/SKILL.md`
- **Subfolder MD files:** Advanced patterns and detailed examples

### Prerequisites
Read [core-framework-instructions.md](core-framework-instructions.md) first for Quest, Rings, lifecycle, and storage fundamentals.

---

## The Three Mandatory UI Layers

Every UI component **must exist in all three layers**:

| Layer | Location | Purpose |
|-------|----------|---------|
| **Component Types** | `ui/types/` | Identify UI technology variants (Bootstrap, Vaadin, etc.) |
| **UI Elements** | `ui/elements/` | Define locators, assign types, optional sync hooks |
| **Component Implementations** | `ui/components/` | Implement Selenium interactions |

---

## App UI Service

Entry point for all UI interactions:

```java
public class AppUiService extends UiServiceFluent<AppUiService> {

   public AppUiService(SmartWebDriver driver, SuperQuest quest) {
      super(driver);
      this.quest = quest;
      postQuestSetupInitialization();
   }

   public InputServiceFluent<AppUiService> input() { return getInputField(); }
   public ButtonServiceFluent<AppUiService> button() { return getButtonField(); }
   public SelectServiceFluent<AppUiService> select() { return getSelectField(); }
   public TableServiceFluent<AppUiService> table() { return getTable(); }
   public InsertionServiceFluent<AppUiService> insertion() { return getInsertionService(); }
   public NavigationServiceFluent<AppUiService> browser() { return getNavigation(); }
}
```

---

## Component Types

Enums identifying UI technology/style:

```java
public enum ButtonFieldTypes implements ButtonComponentType {
   VA_BUTTON_TYPE,        // Vaadin
   BOOTSTRAP_BUTTON_TYPE; // Bootstrap

   public static final class Data {
      public static final String VA_BUTTON = "VA_BUTTON_TYPE";
      public static final String BOOTSTRAP_BUTTON = "BOOTSTRAP_BUTTON_TYPE";
      private Data() {}
   }

   @Override
   public Enum getType() { return this; }
}
```

---

## UI Elements

Enums with locators, types, and optional hooks:

```java
public enum InputFields implements InputUiElement {
   USERNAME_FIELD(By.id("user_login"), InputFieldTypes.BOOTSTRAP_INPUT_TYPE),
   PASSWORD_FIELD(By.id("user_password"), InputFieldTypes.BOOTSTRAP_INPUT_TYPE);

   public static final class Data {
      public static final String USERNAME_FIELD = "USERNAME_FIELD";
      public static final String PASSWORD_FIELD = "PASSWORD_FIELD";
      private Data() {}
   }

   private final By locator;
   private final InputComponentType componentType;

   InputFields(By locator, InputComponentType componentType) {
      this.locator = locator;
      this.componentType = componentType;
   }

   @Override public By locator() { return locator; }
   @Override public <T extends ComponentType> T componentType() { return (T) componentType; }
   @Override public Enum<?> enumImpl() { return this; }
}
```

### With Hooks (for synchronization)
```java
SIGN_IN_BUTTON(By.tagName("button"), ButtonFieldTypes.VA_BUTTON_TYPE,
      SharedUi.WAIT_FOR_LOADING,      // before
      driver -> {});                   // after
```

---

## Component Implementations

Use `@ImplementationOfType` and **SmartWebDriver**:

```java
@ImplementationOfType(InputFieldTypes.Data.BOOTSTRAP_INPUT)
public class InputBootstrapImpl extends BaseComponent implements Input {

   public InputBootstrapImpl(SmartWebDriver driver) {
      super(driver);
   }

   @Override
   public void insert(By locator, String value) {
      SmartWebElement inputField = driver.findSmartElement(locator);
      inputField.clearAndSendKeys(value);
   }

   @Override
   public String getValue(By locator) {
      SmartWebElement inputField = driver.findSmartElement(locator);
      return inputField.getDomAttribute("value");
   }
}
```

**Rules:**
- Use `findSmartElement()` not `findElement()`
- Use `getDomAttribute()` not `getAttribute()`

---

## Authentication

```java
// Credentials
public class AdminCredentials implements LoginCredentials {
   @Override public String username() { return Data.testData().username(); }
   @Override public String password() { return Data.testData().password(); }
}

// Login implementation
public class AppUiLogin extends BaseLoginClient {
   @Override
   protected <T extends UiServiceFluent<?>> void loginImpl(T uiService, String username, String password) {
      uiService
            .getNavigation().navigate(getUiConfig().baseUrl())
            .getInputField().insert(InputFields.USERNAME_FIELD, username)
            .getInputField().insert(InputFields.PASSWORD_FIELD, password)
            .getButtonField().click(ButtonFields.SIGN_IN_BUTTON);
   }

   @Override
   protected By successfulLoginElementLocator() {
      return By.tagName("app-layout");
   }
}

// Usage
@Test
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void testWithAuth(Quest quest) { }
```

---

## Insertion Service

Annotate model fields for automatic form filling:

```java
public class Order {
   @InsertionElement(locatorClass = InputFields.class, elementEnum = "CUSTOMER_FIELD", order = 1)
   private String customerName;

   @InsertionElement(locatorClass = SelectFields.class, elementEnum = "LOCATION_DDL", order = 2)
   private String location;
}

// Usage
quest.use(RING_OF_UI).insertion().insertData(order);
```

---

## Table Service

```java
// Row model
@TableInfo(
    tableContainerLocator = @FindBy(css = "table#orders"),
    rowsLocator = @FindBy(css = "tbody tr"),
    headerRowLocator = @FindBy(css = "thead tr"))
public class OrderTableEntry {
   @TableCellLocator(cellLocator = @FindBy(css = "td.customer"), headerCellLocator = @FindBy(css = "th.customer"))
   private TableCell customerCell;
}

// Table element
public enum Tables implements TableElement<Tables> {
   ORDERS(OrderTableEntry.class);
   // ... standard enum pattern
}

// Usage - tables CAN use Assertion.builder()
quest.use(RING_OF_UI)
    .table().readTable(Tables.ORDERS)
    .table().validate(Tables.ORDERS, Assertion.builder()
        .target(TABLE_VALUES).type(TABLE_NOT_EMPTY).expected(true).build());
```

---

## Request Interception

```java
public enum RequestsInterceptor implements DataIntercept<RequestsInterceptor> {
   INTERCEPT_REQUEST_AUTH("?v-r=uidl");

   public static final class Data {
      public static final String INTERCEPT_REQUEST_AUTH = "INTERCEPT_REQUEST_AUTH";
      private Data() {}
   }

   private final String endpointSubString;
   RequestsInterceptor(String s) { this.endpointSubString = s; }
   @Override public String getEndpointSubString() { return endpointSubString; }
   @Override public RequestsInterceptor enumImpl() { return this; }
}

// Usage
@Test
@InterceptRequests(requestUrlSubStrings = {RequestsInterceptor.Data.INTERCEPT_REQUEST_AUTH})
void testWithInterception(Quest quest) { }
```

---

## Validation

```java
// Direct validation methods (preferred for UI)
quest.use(RING_OF_UI)
    .button().validateIsVisible(ButtonFields.SUBMIT)
    .input().validateValue(InputFields.NAME, "expected")
    .validate(() -> Assertions.assertTrue(condition));

// Assertion.builder() - ONLY for tables
.table().validate(Tables.ORDERS, Assertion.builder()...);
```

---

## Non-Negotiable Rules

| Rule | Description |
|------|-------------|
| **Smart API Only** | Use `findSmartElement()`, not `findElement()` |
| **Three Layers Required** | Types + Elements + Implementations |
| **@ImplementationOfType** | Links implementation to type via `Data.TYPE_NAME` |
| **Hooks for Sync Only** | Never encode business logic in hooks |
| **Direct Validation** | UI uses direct methods, not `Assertion.builder()` (except tables) |

---

## Where to Find More

| Feature | Look In |
|---------|---------|
| Advanced component patterns | Subfolder MD files, existing `*Impl.java` files |
| Complex hooks | `ui/functions/` + subfolder MD files |
| Edge cases | Subfolder MD files |

**Check subfolder MD files for advanced patterns beyond these basics.**
