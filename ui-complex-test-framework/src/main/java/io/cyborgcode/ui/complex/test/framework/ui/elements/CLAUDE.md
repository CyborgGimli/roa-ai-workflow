## description
**Goal:** centralized registry of UI elements (locators + component types) used by ROA UI services.

**Provide in this folder:**
- Enums for each element category (examples: `InputFields`, `ButtonFields`, `Tables`, `DropdownFields`, `RadioFields`, ...).
- Each enum constant represents one UI element:
    - `By locator`
    - `ComponentType` (specific subtype per category, e.g. `InputComponentType`)
- Each enum implements the matching ROA UI element interface (example: `InputUiElement`).

**Why this exists**
- Tests/flows reference enum constants (not raw locators).
- ROA resolves the correct component implementation using the enum’s `componentType()` (type-based resolution).

**Annotation name constants**
- Each enum may have a nested `Data` class containing `String` constants used for annotation-based references (only when needed).

**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.

---

## ⚠️ CRITICAL RULES

### UiElement Interface Contract
**Element enums implement `*UiElement` interface which requires `enumImpl()` method:**

```java
// ✅ CORRECT - UiElement interface
public enum InputFields implements InputUiElement {
    USERNAME(By.id("user"), InputFieldTypes.BOOTSTRAP_INPUT_TYPE);

    @Override
    public Enum<?> enumImpl() { return this; }  // MUST be enumImpl()
}

// ❌ WRONG - This is for ComponentType, NOT UiElement
public enum InputFields implements InputUiElement {
    @Override
    public Enum<?> getType() { return this; }  // WRONG METHOD NAME
    }
```    
**Rule:** `ComponentType` = `getType()` | `UiElement` = `enumImpl()`

### If you add consumer hooks in enum dont provide constructors like this:

```java
// ❌ WRONG
private final By locator;
private final InputComponentType componentType;
private final Consumer<SmartWebDriver> before;
private final Consumer<SmartWebDriver> after;

    ButtonFields(By locator,
                 ButtonComponentType componentType) {
        this.locator = locator;
        this.componentType = componentType;
        this.before = null;
        this.after = null;
    }
```
```java
// ❌ WRONG
private final By locator;
private final InputComponentType componentType;
private final Consumer<SmartWebDriver> before;
private final Consumer<SmartWebDriver> after;

    ButtonFields(By locator,
                 ButtonComponentType componentType,
                 Consumer<SmartWebDriver> before) {
        this.locator = locator;
        this.componentType = componentType;
        this.before = before;
        this.after = null;
    }
```
```java
// ✅ CORRECT
private final By locator;
private final InputComponentType componentType;
private final Consumer<SmartWebDriver> before;
private final Consumer<SmartWebDriver> after;

    ButtonFields(By locator,
                 ButtonComponentType componentType,
                 Consumer<SmartWebDriver> before,
                 Consumer<SmartWebDriver> after) {
        this.locator = locator;
        this.componentType = componentType;
        this.before = before;
        this.after = after;
    }
```
**Rule:** Do not provide consumer hooks with null values in constructors where they are not taken as parameters.

## example

### Element enum (pattern: inputs) - simple example without hooks (no constructor with consumer before and after)
```java
package <your.package>.ui.elements;

import io.cyborgcode.roa.ui.components.base.ComponentType;
import io.cyborgcode.roa.ui.components.input.InputComponentType;
import io.cyborgcode.roa.ui.selenium.InputUiElement;
import <your.package>.ui.types.InputFieldTypes;
import org.openqa.selenium.By;

public enum InputFields implements InputUiElement {

  USERNAME_FIELD(By.id("username"), InputFieldTypes.BOOTSTRAP_INPUT_TYPE),
  PASSWORD_FIELD(By.id("user_password"), InputFieldTypes.BOOTSTRAP_INPUT_TYPE);

   public static final class Data {
      public static final String USERNAME_FIELD = "USERNAME_FIELD";
      public static final String PASSWORD_FIELD = "PASSWORD_FIELD";

      private Data() {
      }
   }
   
  private final By locator;
  private final InputComponentType componentType;

  InputFields(By locator, InputComponentType componentType) {
    this.locator = locator;
    this.componentType = componentType;
  }

  @Override
  public By locator() {
    return locator;
  }

  @Override
  public <T extends ComponentType> T componentType() {
    return (T) componentType;
  }

  @Override
  public Enum<?> enumImpl() {
    return this;
  }
  
}
```
**Why Data class is needed:** The `Data` class provides string constants that match enum names. These constants are required for `@InsertionElement` annotations in model classes, where enum references cannot be used directly. The annotation framework uses these string constants to map model fields to UI elements at runtime.

### Usage (pattern)
```java
quest
  .use(Rings.RING_OF_UI)
  .input().insert(InputFields.USERNAME_FIELD, "admin");
```

### Element enum (pattern: buttons) - example with hooks (constructor with before and after consumer)
**Description:** `SharedUiFunctions` is a static utility class containing reusable functions for UI element lifecycle management.
These functions can be used as `before` or `after` hooks in element enums to handle necessary operations:
- **Before hooks**: operations that must occur before the main action (e.g., waiting for presence, scrolling into view, validating preconditions)
- **After hooks**: operations that must occur after the main action (e.g., waiting for removal, verifying state changes, cleanup)
- Hooks are optional and should only be used when explicit waits or operations are required for reliable interaction with specific elements.
```java
package <your.package>.ui.elements;

import io.cyborgcode.roa.ui.components.base.ComponentType;
import io.cyborgcode.roa.ui.components.input.ButtonComponentType;
import io.cyborgcode.roa.ui.selenium.ButtonUiElement;
import <your.package>.ui.functions.SharedUiFunctions;
import <your.package>.ui.types.ButtonFieldTypes;
import org.openqa.selenium.By;

public enum ButtonFields implements ButtonUiElement {

   // No lifecycle hooks
   SIGN_IN_FORM_BUTTON(By.cssSelector("input[value='Sign in']"), ButtonFieldTypes.BOOTSTRAP_BUTTON_TYPE),
   // Use only before hook
   SIGN_IN_BUTTON(By.id("signin_button"), ButtonFieldTypes.BOOTSTRAP_BUTTON_TYPE,
         driver -> SharedUiFunctions.waitForPresence(driver, By.id("signin_button")),
         driver -> {}),
   // Use only after hook
   PURCHASE_BUTTON(By.id("purchase_cash"), ButtonFieldTypes.BOOTSTRAP_BUTTON_TYPE,
         driver -> {},
         driver -> SharedUiFunctions.waitForPresence(driver, By.id("purchase_cash"))),
   // Use before and after hooks
   SUBMIT_BUTTON(By.id("btn_submit"), ButtonFieldTypes.BOOTSTRAP_BUTTON_TYPE,
         driver -> SharedUiFunctions.waitForPresence(driver, By.id("btn_submit")),
         driver -> SharedUiFunctions.waitToBeRemoved(driver, By.id("btn_submit")));

    private final By locator;
    private final ButtonComponentType componentType;
    private final Consumer<SmartWebDriver> before;
    private final Consumer<SmartWebDriver> after;


    ButtonFields(final By locator, final ButtonComponentType componentType) {
        this(locator, componentType, smartWebDriver -> {
        }, smartWebDriver -> {
        });
    }


    ButtonFields(By locator,
                 ButtonComponentType componentType,
                 Consumer<SmartWebDriver> before,
                 Consumer<SmartWebDriver> after) {
        this.locator = locator;
        this.componentType = componentType;
        this.before = before;
        this.after = after;
    }


    @Override
    public By locator() {
        return locator;
    }


    @Override
    public <T extends ComponentType> T componentType() {
        return (T) componentType;
    }


    @Override
    public Enum<?> enumImpl() {
        return this;
    }


    @Override
    public Consumer<SmartWebDriver> before() {
        return before;
    }


    @Override
    public Consumer<SmartWebDriver> after() {
        return after;
    }

}
```

```java
public class SharedUiFunctions {

   private SharedUiFunctions() {
   }

   public static void waitForPresence(SmartWebDriver smartWebDriver, By locator) {
      // Implement waiting logic for element to become visible
   }
   
   public static void waitToBeRemoved(SmartWebDriver smartWebDriver, By locator) {
      // Implement waiting logic for element to be removed from DOM
   }

   public static void waitUntilAnotherOperationIsCompleted(SmartWebDriver smartWebDriver, By locator, SmartWebElement element) {
      // Implement waiting logic after another operation is completed
   }
}
```

### Usage (pattern)
```java
quest
  .use(Rings.RING_OF_UI)
  .button().click(ButtonFields.SIGN_IN_BUTTON);
```

---

## dos
- Create one enum per **element category** (Input/Button/Table/Radio/...) and keep it in `ui/elements`.
- For every enum constant:
    - use a stable `By` locator
    - assign the correct **component type** for the UI technology (via `*FieldTypes.*`)
- Implement the correct ROA element interface for the category:
    - inputs → `InputUiElement`
    - buttons → `<ButtonUiElement>` (category-specific interface)
    - etc.
- Prefer stable selectors:
    - `By.id(...)`
    - `By.cssSelector(...)` using stable attributes
- Keep element registries declarative:
    - locators + types only (see InputFields example - simple pattern without hooks)
    - no complex interaction logic or assertions in constructors
    - lifecycle hooks (`before`/`after`) are the exception: use them only for necessary waits or operations that specific elements require (see ButtonFields example - pattern with hooks)
- Use the nested `Data` class only when string constants are required by annotations or external mapping.

---

## donts
- Don’t put raw `By` locators directly in tests/flows; always reference the enum constants.
- Don’t mix different element categories into the same enum.
- Don’t use brittle selectors (dynamic classes, deeply nested XPath) when stable attributes exist.
- Don’t add business logic or UI actions into element enums.
- Don’t duplicate locator definitions across multiple enums; keep a single source of truth per element.