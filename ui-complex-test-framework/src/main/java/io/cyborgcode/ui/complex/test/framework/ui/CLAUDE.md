# AppUiService Class Instructions

## Overview
The `AppUiService` class is the central facade for all UI interactions in the test automation framework. It acts as the "Ring of UI" in the ROA architecture. It extends `UiServiceFluent<AppUiService>` to provide a fluent API for accessing various UI components.

## Purpose
This class aggregates specific component services and exposes them through concise methods. This allows test code to chain commands naturally (e.g., `ui.input()...` or `ui.button()...`).

## Generation Rules

When generating or updating this class, follow these rules:

1.  **Inheritance**:
    *   The class **must** extend `UiServiceFluent<AppUiService>`.

2.  **Constructor**:
    *   It must have a constructor matching: `public AppUiService(SmartWebDriver driver, SuperQuest quest)`.
    *   Inside the constructor:
        *   Call `super(driver);`
        *   Set `this.quest = quest;`
        *   Call `postQuestSetupInitialization();`

3.  **Standard Base Components**:
    *   The project archetype includes these methods by default. **Do not remove them**:
        *   `input()` -> `getInputField()`
        *   `table()` -> `getTable()`
        *   `button()` -> `getButtonField()`
        *   `select()` -> `getSelectField()`
        *   `interceptor()` -> `getInterceptor()`
        *   `insertion()` -> `getInsertionService()`
        *   `browser()` -> `getNavigation()`
        *   `validate()` -> `getValidation()`

4.  **Dynamic Expansion (Add Missing Components)**:
    *   **Crucial**: The base class often lacks specific components like **Links**, **Alerts**, **Radio Buttons**, or **Lists**.
    *   **Analyze the App Knowledge**: If the application under test requires these components, you **must** add the corresponding facade methods.
    *   **Add these if needed**:
        *   **Links**: `public LinkServiceFluent<AppUiService> link() { return getLinkField(); }`
        *   **Alerts/Popups**: `public AlertServiceFluent<AppUiService> alert() { return getAlertField(); }`
        *   **Radio Buttons/Checkboxes**: `public RadioServiceFluent<AppUiService> radio() { return getRadioField(); }`
        *   **Lists**: `public ListServiceFluent<AppUiService> list() { return getListField(); }`

5. **UI Validation Model**:
   *   UI validations are NOT built with Assertion.builder() (All of them, except Table assertions)
   *   UI validations are executed via component fluent services
   *   Validation always flows like this:
           ui.<component>().validateX(...)
           → component service reads UI
           → value is stored in UI storage
           → assertion is executed via appUiService.validate(...)(or custom fluent service)


## Source of truth
**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.

## Code Template

```java
package io.cyborgcode.roa.example.project.ui; // Note: Package may vary based on project

import io.cyborgcode.roa.framework.quest.SuperQuest;
import io.cyborgcode.roa.ui.selenium.smart.SmartWebDriver;
import io.cyborgcode.roa.ui.service.fluent.*;
import io.cyborgcode.roa.ui.service.tables.TableServiceFluent;

public class AppUiService extends UiServiceFluent<AppUiService> {

    public AppUiService(SmartWebDriver driver, SuperQuest quest) {
        super(driver);
        this.quest = quest;
        postQuestSetupInitialization();
    }

    // --- Standard Archetype Components ---

    public InputServiceFluent<AppUiService> input() {
        return getInputField();
    }

    public TableServiceFluent<AppUiService> table() {
        return getTable();
    }

    public ButtonServiceFluent<AppUiService> button() {
        return getButtonField();
    }

    public SelectServiceFluent<AppUiService> select() {
        return getSelectField();
    }

    public InterceptorServiceFluent<AppUiService> interceptor() {
        return getInterceptor();
    }

    public InsertionServiceFluent<AppUiService> insertion() {
        return getInsertionService();
    }

    public NavigationServiceFluent<AppUiService> browser() {
        return getNavigation();
    }

    public ValidationServiceFluent<AppUiService> validate() {
        return getValidation();
    }

    // --- Extended Components (ADD THESE BASED ON APP KNOWLEDGE) ---

    // Example: If app has links
    // public LinkServiceFluent<AppUiService> link() { return getLinkField(); }

    // Example: If app has alerts
    // public AlertServiceFluent<AppUiService> alert() { return getAlertField(); }

    // Example: If app has radio buttons
    // public RadioServiceFluent<AppUiService> radio() { return getRadioField(); }

    // Example: If app has lists
    // public ListServiceFluent<AppUiService> list() { return getListField(); }
}
```
