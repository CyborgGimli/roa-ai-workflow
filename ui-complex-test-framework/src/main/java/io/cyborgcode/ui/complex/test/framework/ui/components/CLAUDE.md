## description

**Goal:** provide ROA UI component implementations (concrete Selenium/Vaadin/Bootstrap/etc. behaviors) behind ROA
component interfaces.

**Concepts**

- **Component interface (ROA):** e.g. `io.cyborgcode.roa.ui.components.input.Input`
- **Concrete implementation (this folder):** e.g. Vaadin / Bootstrap / HTML-specific class implementing that interface
- **Type-based resolution:** ROA selects an implementation via `@ImplementationOfType(<FieldTypes.Data.*>)`

**Folder rules**

- Organize by component kind: `ui/components/<kind>/...`
    - examples: `input`, `button`, `table`, `select`, `modal`, etc.
- Each component implementation:
    - extends `BaseComponent`
    - implements the matching ROA interface
    - has exactly one `@ImplementationOfType(...)` mapping to a field-type constant (from `ui/types/*FieldTypes`)

**Used by**

- Higher-level UI services (e.g. `UiServiceFluent`) calling component interfaces (`getInputField()`, `getButtonField()`,
  etc.)
- Tests and business flows indirectly (tests should not depend on the concrete impl class)

**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.

---

## ⚠️ CRITICAL RULES

### Mandatory Requirements

**Each component MUST implement all methods from the interface it implements:**

- Use .claude/skills/pandora/SKILL.md to find all methods that needs to be implemented and the metadata for the interface the component implements (
  `io.cyborgcode.roa.ui.components.input.Input` / `io.cyborgcode.roa.ui.components.button.Button` \
  `io.cyborgcode.roa.ui.components.select.Select`)
- you must validate usage of the external libraries(`io.cyborgcode.roa`) by checking dependency source code and Javadocs (not assumptions) so the generated code compiles.

**All component implementations MUST have:**

1. **@ImplementationOfType annotation**

```java

@ImplementationOfType(InputFieldTypes.Data.BOOTSTRAP_INPUT_TYPE)
public class InputBootstrapImpl extends BaseComponent implements Input {
```   

2. **Use SmartWebDriver APIs (NOT standard Selenium)**

```java
// ✅ CORRECT
SmartWebElement element = driver.findSmartElement(By.id("username"));
SmartWebElement nested = container.findSmartElement(By.className("btn"));
String value = input.getDomProperty("value");

// ❌ WRONG - Causes compilation errors
WebElement element = driver.findElement(By.id("username"));  // Wrong type
String value = input.getAttribute("value");  // Deprecated
```

### Smart decorators (must use)

`SmartWebDriver` and `SmartWebElement` are **decorators** over Selenium `WebDriver` / `WebElement`.

- They expose the same Selenium APIs for interacting (click, sendKeys, getText, getDomProperty, etc.).
- For locating elements, use:
    - `findSmartElement(...)` / `findSmartElements(...)`
    - **not** `findElement(...)` / `findElements(...)`

All other Selenium-style methods can be used from the smart decorators; only element lookup must use the smart find
methods.

3. **Three-Layer Architecture Required**

- Without ALL three layers, tests compile but fail at runtime:

* Component Type (`ui/types/*FieldTypes.java`) with getType()
* Element Definition (`ui/elements/*Fields.java`) with enumImpl()
* Component Implementation (`ui/components/<component>/*Impl.java`) ← YOU ARE HERE (<component> can be input, button,
  table, etc.)

---

## example

### Pattern: component implementation (Bootstrap input)

**Intent:** implement `Input` interactions for Bootstrap.

Required structure:

```java
package

<your.package>.ui.components.input;

import io.cyborgcode.roa.ui.annotations.ImplementationOfType;
import io.cyborgcode.roa.ui.components.base.BaseComponent;
import io.cyborgcode.roa.ui.components.input.Input;
import io.cyborgcode.roa.ui.selenium.smart.SmartWebDriver;

import <your.package>.ui.types.InputFieldTypes;

@ImplementationOfType(InputFieldTypes.Data.BOOTSTRAP_INPUT)
public class InputBootstrapImpl extends BaseComponent implements Input {

   private static final By INPUT_FIELD_CONTAINER = By.tagName("form");
   private static final By INPUT_FIELD_ERROR_MESSAGE_LOCATOR = By.className("alert-error");
   private static final By INPUT_FIELD_LABEL_LOCATOR = By.tagName("../label");
   public static final String ELEMENT_VALUE_ATTRIBUTE = "value";
   public static final String FIELD_DISABLE_CLASS_INDICATOR = "disabled";


   public InputBootstrapImpl(SmartWebDriver driver) {
      super(driver);
   }


   @Override
   public void insert(final SmartWebElement container, final String value) {
      SmartWebElement inputFieldContainer = findInputField(container, null);
      insertIntoInputField(inputFieldContainer, value);
   }


   @Override
   public void insert(final SmartWebElement container, final String inputFieldLabel, final String value) {
      SmartWebElement inputFieldContainer = findInputField(container, inputFieldLabel);
      insertIntoInputField(inputFieldContainer, value);
   }
   //Implement all the remaining methods from io.cyborgcode.roa.ui.components.input.Input interface
}
```

## dos

- Implement one concrete class per **UI technology + component kind + field type** (i.e., each distinct component
  variant on the screen). If the same UI tech has multiple variants (e.g., Bootstrap select type 1 vs type 2), create
  separate implementations.
- Always:
    - `extends BaseComponent`
    - `implements <ROA component interface>`
    - `@ImplementationOfType(<FieldTypes.Data.*>)`
    - constructor: `(SmartWebDriver driver) { super(driver); }`
- Use **SmartWebDriver / SmartWebElement** APIs exclusively (avoid raw WebDriver/WebElement in component impls).

- Keep locators minimal and maintainable:
    - Prefer a **single, direct** lookup over long chains of nested `findSmartElement(...)` calls when possible.
    - Declare locators at the top of the class as `private static final By ...` (or `public static final` only when
      reused externally).
    - Standardize locator strategy across the project when possible (e.g., prefer `By.id` or `By.name` consistently).
    - Fewer/standardized locators = easier long-term maintenance.

- Keep component impls:
    - low-level and deterministic
    - free of business logic
    - focused on stable DOM strategies for that UI technology
- Fail fast when required elements are missing.
    - Throw an appropriate runtime exception for the component/strategy (e.g., `NoSuchElementException`,
      `NotFoundException`, `IllegalArgumentException`, etc.).
    - Keep exception messages precise:
        - include expected component kind (input/button/radio/...)
        - include field type (e.g., VA_INPUT) when relevant
        - include label/locator/context when lookup uses them

---

## donts

- Don’t reference concrete implementation classes from tests/flows (call the ROA interface via services).
- Don’t hardcode application-specific business steps in components (no navigation, no domain flows).
- Don’t mix multiple field types into one implementation class.
- Don’t use unstable selectors (dynamic classes, translated texts) when stable attributes/parts exist.
- Don’t silently ignore missing required elements.
- Missing elements must fail fast with an exception that matches the component’s semantics.
