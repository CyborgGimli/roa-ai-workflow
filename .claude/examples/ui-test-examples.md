# ROA UI Test Examples - Comprehensive Reference

This document provides a complete reference of all UI testing patterns and combinations available in the ROA framework. Examples are derived from actual test implementations in `ui-simple-test-framework`.

---

## Table of Contents

1. [Test Class Structure](#1-test-class-structure)
2. [Authentication Patterns](#2-authentication-patterns)
3. [Component Interactions](#3-component-interactions)
4. [Validation Patterns](#4-validation-patterns)
5. [Data Management (@Craft)](#5-data-management-craft)
6. [Preconditions (@Journey)](#6-preconditions-journey)
7. [Custom Service Rings](#7-custom-service-rings)
8. [Table Operations](#8-table-operations)
9. [Complete Test Examples](#9-complete-test-examples)

---

## 1. Test Class Structure

### Basic Test Class
```java
@UI
@DisplayName("Feature Name: Description")
class FeatureTests extends BaseQuest {

   @Test
   @Smoke
   @Regression
   @Description("What this test verifies")
   void testName_scenario_expectedOutcome(Quest quest) {
      quest
            .use(RING_OF_UI)
            // ... test steps
            .complete();
   }
}
```

### Required Imports by Feature

| Feature | Import |
|---------|--------|
| Base class | `io.cyborgcode.roa.framework.base.BaseQuest` |
| Quest parameter | `io.cyborgcode.roa.framework.quest.Quest` |
| @UI annotation | `io.cyborgcode.roa.ui.annotations.UI` |
| @AuthenticateViaUi | `io.cyborgcode.roa.ui.annotations.AuthenticateViaUi` |
| @Craft, @Journey, @Ripper | `io.cyborgcode.roa.framework.annotation.*` |
| Late initialization | `io.cyborgcode.roa.framework.parameters.Late` |
| Table assertions | `io.cyborgcode.roa.validator.core.Assertion` |
| Table types | `io.cyborgcode.roa.ui.validator.TableAssertionTypes.*` |
| Table targets | `io.cyborgcode.roa.ui.validator.UiTablesAssertionTarget.*` |
| TableField | `io.cyborgcode.roa.ui.components.table.base.TableField` |
| Config access | `static io.cyborgcode.roa.ui.config.UiConfigHolder.getUiConfig` |
| Rings | `static {project}.base.Rings.*` |
| Preconditions | `static {project}.preconditions.Preconditions.Data.*` |
| UI elements | `{project}.ui.elements.*` |
| Authentication classes | `{project}.ui.authentication.*` |
| DataCreator | `{project}.data.creator.DataCreator` |

### Test Annotations

| Annotation | Purpose |
|------------|---------|
| `@UI` | Marks class as UI test (enables UI framework) |
| `@DisplayName` | Human-readable test class/method name |
| `@Test` | JUnit test method marker |
| `@Smoke` | Tag for smoke test suite |
| `@Regression` | Tag for regression test suite |
| `@Description` | Allure report description |

---

## 2. Authentication Patterns

### Pattern 1: Manual Login (No Framework Auth)
Hardcoded login steps directly in test - use only for learning/demos.

```java
@Test
void manualLogin_baseline(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, "username")
         .input().insert(InputFields.PASSWORD_FIELD, "password")
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         // ... continue with test
         .complete();
}
```

### Pattern 2: Config-Based Credentials
Retrieve credentials from configuration (better than hardcoding).

```java
@Test
void configLogin_fromProperties(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, Data.testData().username())
         .input().insert(InputFields.PASSWORD_FIELD, Data.testData().password())
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         // ... continue with test
         .complete();
}
```

### Pattern 3: @AuthenticateViaUi (Per-Test, No Cache)
Framework handles login automatically before each test.

```java
@Test
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void authenticatedTest_perTest(Quest quest) {
   // User is already logged in when test starts
   quest
         .use(RING_OF_UI)
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         // ... test steps (no login needed)
         .complete();
}
```

### Pattern 4: @AuthenticateViaUi (Cached Session)
Login once, reuse session across all tests in the class.

```java
@UI
@DisplayName("Tests with Cached Authentication")
class CachedAuthTests extends BaseQuest {

   @Test
   @AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class, cacheCredentials = true)
   void test1_firstTestPerformsLogin(Quest quest) {
      // This test performs login (first execution)
      quest.use(RING_OF_UI)
            .link().click(LinkFields.TRANSFER_FUNDS_LINK)
            .select().selectOption(SelectFields.TF_FROM_ACCOUNT_DDL, LOAN_ACCOUNT)
            .select().selectOption(SelectFields.TF_TO_ACCOUNT_DDL, CREDIT_CARD_ACCOUNT)
            .input().insert(InputFields.AMOUNT_FIELD, "300")
            .button().click(ButtonFields.SUBMIT_BUTTON)
            .alert().validateValue(AlertFields.SUBMITTED_TRANSACTION, SUCCESSFUL_TRANSFER_MESSAGE)
            .complete();
   }

   @Test
   @AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class, cacheCredentials = true)
   void test2_reusesLoginSession(Quest quest) {
      // This test reuses cached session (no login)
      quest.use(RING_OF_UI)
            .link().click(LinkFields.TRANSFER_FUNDS_LINK)
            .select().selectOption(SelectFields.TF_FROM_ACCOUNT_DDL, SAVINGS_ACCOUNT)
            .select().selectOption(SelectFields.TF_TO_ACCOUNT_DDL, CHECKING_ACCOUNT)
            .input().insert(InputFields.AMOUNT_FIELD, "2000")
            .button().click(ButtonFields.SUBMIT_BUTTON)
            .alert().validateValue(AlertFields.SUBMITTED_TRANSACTION, SUCCESSFUL_TRANSFER_MESSAGE)
            .complete();
   }
}
```

---

## 3. Component Interactions

### Browser/Navigation
```java
quest.use(RING_OF_UI)
      .browser().navigate(getUiConfig().baseUrl())           // Navigate to URL
      .browser().navigate(getUiConfig().baseUrl() + "/path") // Navigate with path
      .browser().back()                                       // Browser back
      .browser().refresh()                                    // Refresh page
```

### Button
```java
quest.use(RING_OF_UI)
      .button().click(ButtonFields.SUBMIT_BUTTON)            // Click by element enum
      .button().click(ButtonFields.SIGN_IN_BUTTON)
      .button().click(ButtonFields.CALCULATE_COST_BUTTON)
      .button().validateIsEnabled(ButtonFields.SUBMIT, true) // Validate enabled state
```

### Input/Text Field
```java
quest.use(RING_OF_UI)
      .input().insert(InputFields.USERNAME_FIELD, "value")   // Insert text
      .input().insert(InputFields.AMOUNT_FIELD, "100")       // Numeric input
      .input().validateValue(InputFields.EMAIL, "expected")  // Validate value
```

### Link
```java
quest.use(RING_OF_UI)
      .link().click(LinkFields.TRANSFER_FUNDS_LINK)          // Click link
```

### Select/Dropdown
```java
quest.use(RING_OF_UI)
      .select().selectOption(SelectFields.TF_FROM_ACCOUNT_DDL, "Loan")
```

### List/Tabs/Menu
```java
quest.use(RING_OF_UI)
      .list().select(ListFields.NAVIGATION_TABS, "Pay Bills")
      .list().validateIsSelected(ListFields.ACCOUNT_ACTIVITY_TABS, false, "Find Transactions")
```

### Radio Button
```java
quest.use(RING_OF_UI)
      .radio().select(RadioFields.DOLLARS_RADIO_FIELD)       // Select radio option
```

### Alert/Message
```java
quest.use(RING_OF_UI)
      .alert().validateValue(AlertFields.SUBMITTED_TRANSACTION, "Success message")  // Hard assertion
      .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, "Currency purchased", true)  // Soft
      .alert().validateValue(AlertFields.PAYMENT_MESSAGE, "Payment successful")
```

### Insertion Service (Auto-Fill Form from Model)
```java
quest.use(RING_OF_UI)
      .insertion().insertData(purchaseForeignCurrency)       // Auto-fill entire form from model
```

### Complete Component Flow Example
```java
@Test
@Smoke
@Regression
@Description("Components Covered: Browser, Button, Input, Link, Select, Alert")
void components_browserButtonInputLinkSelectAlert(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, "username")
         .input().insert(InputFields.PASSWORD_FIELD, "password")
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .select().selectOption(SelectFields.TF_FROM_ACCOUNT_DDL, LOAN_ACCOUNT)
         .select().selectOption(SelectFields.TF_TO_ACCOUNT_DDL, CREDIT_CARD_ACCOUNT)
         .input().insert(InputFields.AMOUNT_FIELD, "100")
         .input().insert(InputFields.TF_DESCRIPTION_FIELD, TRANSFER_LOAN_TO_CREDIT_CARD)
         .button().click(ButtonFields.SUBMIT_BUTTON)
         .button().click(ButtonFields.SUBMIT_BUTTON)
         .alert().validateValue(AlertFields.SUBMITTED_TRANSACTION, SUCCESSFUL_TRANSFER_MESSAGE)
         .drop()
         .complete();
}
```

### List and Radio Example
```java
@Test
@Smoke
@Regression
@Description("Components Covered: Browser, Button, Input, Link, List, Select, Radio, Alert")
void components_listRadio(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, "username")
         .input().insert(InputFields.PASSWORD_FIELD, "password")
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .select().selectOption(SelectFields.PC_CURRENCY_DDL, CURRENCY_PESO)
         .input().insert(InputFields.AMOUNT_CURRENCY_FIELD, "100")
         .radio().select(RadioFields.DOLLARS_RADIO_FIELD)
         .button().click(ButtonFields.CALCULATE_COST_BUTTON)
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .drop()
         .complete();
}
```

---

## 4. Validation Patterns

### Hard Assertions (Fail Immediately)
Default behavior - test fails on first assertion failure.

```java
quest.use(RING_OF_UI)
      .validate().validateTextInField(Tag.I, "Expected Text")           // No soft flag = hard
      .validate().validateTextInField(Tag.I, "Another Text", false)     // Explicit hard
      .alert().validateValue(AlertFields.MESSAGE, "Expected")           // Component validation
      .button().validateIsEnabled(ButtonFields.SUBMIT)                  // Button state
      .input().validateValue(InputFields.EMAIL, "expected@email.com")   // Input value
```

### Soft Assertions (Continue on Failure)
All soft assertions execute before reporting failures.

```java
@Test
@Smoke
@Regression
@Description("Component Covered: Validate using Soft Assertions")
void components_validateUsingSoftAssertions(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, "username")
         .input().insert(InputFields.PASSWORD_FIELD, "password")
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .list().select(ListFields.PAY_BILLS_TABS, PAY_SAVED_PAYEE)
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_SPRINT)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_SPRINT_PLACEHOLDER, true)    // Soft
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_BANK_OF_AMERICA)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_BANK_OF_AMERICA_PLACEHOLDER, true)
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_APPLE)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_APPLE_PLACEHOLDER, true)
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_WELLS_FARGO)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_WELLS_FARGO_PLACEHOLDER, true)
         .alert().validateValue(AlertFields.PAYMENT_MESSAGE, SUCCESSFUL_PAYMENT_MESSAGE, true)
         .complete();
}
```

### Mixed Assertions
Combine hard and soft assertions strategically.

```java
@Test
@Smoke
@Regression
@Description("Component Covered: Validate using mixed assertions")
void components_validateUsingMixedAssertions(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, "username")
         .input().insert(InputFields.PASSWORD_FIELD, "password")
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .list().select(ListFields.PAY_BILLS_TABS, PAY_SAVED_PAYEE)
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_SPRINT)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_SPRINT_PLACEHOLDER)          // Hard
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_BANK_OF_AMERICA)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_BANK_OF_AMERICA_PLACEHOLDER, false)  // Hard
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_APPLE)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_APPLE_PLACEHOLDER, true)     // Soft
         .select().selectOption(SelectFields.SP_PAYEE_DDL, PAYEE_WELLS_FARGO)
         .link().click(LinkFields.SP_PAYEE_DETAILS_LINK)
         .validate().validateTextInField(Tag.I, PAYEE_WELLS_FARGO_PLACEHOLDER)     // Hard
         .complete();
}
```

### Custom Validation with Lambda
Use `validate()` for custom assertion logic.

```java
quest.use(RING_OF_UI)
      .table().readTable(Tables.FILTERED_TRANSACTIONS,
            TableField.of(FilteredTransactionEntry::setDescription),
            TableField.of(FilteredTransactionEntry::setWithdrawal))
      .validate(() -> Assertions.assertEquals(
            "50",
            retrieve(tableRowExtractor(Tables.FILTERED_TRANSACTIONS, TRANSACTION_DESCRIPTION_OFFICE_SUPPLY),
                  FilteredTransactionEntry.class).getWithdrawal().getText(),
            "Wrong deposit value")
      )
      .complete();
```

### Component-Specific Validation Methods

| Component | Validation Method | Example |
|-----------|-------------------|---------|
| Alert | `validateValue()` | `.alert().validateValue(AlertFields.SUCCESS, "message")` |
| Alert (soft) | `validateValue(,, true)` | `.alert().validateValue(AlertFields.SUCCESS, "message", true)` |
| Button | `validateIsEnabled()` | `.button().validateIsEnabled(ButtonFields.SUBMIT)` |
| Input | `validateValue()` | `.input().validateValue(InputFields.EMAIL, "test@email.com")` |
| List | `validateIsSelected()` | `.list().validateIsSelected(ListFields.TABS, false, "Tab Name")` |
| Validate | `validateTextInField()` | `.validate().validateTextInField(Tag.I, "text")` |
| Table | `validate()` with Assertion.builder() | See Table Operations section |

---

## 5. Data Management (@Craft)

### Basic @Craft Usage
Inject test data models into test parameters.

```java
@Test
@Regression
@Description("Craft injects a typed model instance for data-driven steps")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void craft_injectsModelDataIntoSteps(
      Quest quest,
      @Craft(model = DataCreator.Data.PURCHASE_CURRENCY) PurchaseForeignCurrency purchaseForeignCurrency) {

   quest
         .use(RING_OF_UI)
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .select().selectOption(SelectFields.PC_CURRENCY_DDL, purchaseForeignCurrency.getCurrency())
         .input().insert(InputFields.AMOUNT_CURRENCY_FIELD, purchaseForeignCurrency.getAmount())
         .radio().select(RadioFields.DOLLARS_RADIO_FIELD)
         .button().click(ButtonFields.CALCULATE_COST_BUTTON)
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .complete();
}
```

### Using @Craft with Insertion Service
Auto-fill entire forms from crafted models.

```java
@Test
@Regression
@Description("Insertion service maps model fields to UI controls in one operation")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void insertionService_populatesFormFromModel(Quest quest,
      @Craft(model = DataCreator.Data.PURCHASE_CURRENCY) PurchaseForeignCurrency purchaseForeignCurrency) {

   quest
         .use(RING_OF_UI)
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .insertion().insertData(purchaseForeignCurrency)  // Auto-fills select, input, radio
         .button().click(ButtonFields.CALCULATE_COST_BUTTON)
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .complete();
}
```

### Late Initialization
For runtime-dependent data creation.

```java
@Test
@Regression
@Description("Create order after runtime data is available")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void createOrderWithLateData(Quest quest,
      Late<@Craft(model = DataCreator.Data.ORDER)> orderLate) {
   quest
         .use(RING_OF_CUSTOM)
         .generateOrderNumber()              // Creates runtime data
         .createOrder(orderLate.create())    // Model uses runtime data
         .validateOrder(orderLate.create())
         .complete();
}
```

---

## 6. Preconditions (@Journey)

### Single @Journey Precondition
Execute setup before test.

```java
@Test
@Regression
@Description("PreQuest with a single @Journey precondition to set required state")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
@Journey(value = PURCHASE_CURRENCY_PRECONDITION,
         journeyData = {@JourneyData(DataCreator.Data.PURCHASE_CURRENCY)})
void journey_singlePrecondition(Quest quest) {
   // Precondition already executed - validate result
   quest
         .use(RING_OF_PURCHASE_CURRENCY)
         .validatePurchase()
         .complete();
}
```

### Multiple @Journey Preconditions
Combine multiple preconditions (executed in order).

```java
@Test
@Regression
@Description("PreQuest with multiple @Journey entries to compose preconditions, no JourneyData")
@Journey(value = USER_LOGIN_PRECONDITION)
@Journey(value = PURCHASE_CURRENCY_PRECONDITION,
         journeyData = {@JourneyData(DataCreator.Data.PURCHASE_CURRENCY)})
void multipleJourneys_combinedPreconditions_noJourneyData(Quest quest) {
   // Login AND purchase preconditions already executed
   quest
         .use(RING_OF_PURCHASE_CURRENCY)
         .validatePurchase()
         .complete();
}
```

---

## 7. Custom Service Rings

### Using Custom Ring - Switch Between Rings
```java
@Test
@Regression
@Description("Usage of custom service, and switching between different services")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void customServiceExample_switchBetweenServices(Quest quest,
      @Craft(model = DataCreator.Data.PURCHASE_CURRENCY) PurchaseForeignCurrency purchaseForeignCurrency) {

   quest
         .use(RING_OF_PURCHASE_CURRENCY)    // Custom ring
         .purchaseCurrency(purchaseForeignCurrency)
         .drop()                             // Release ring before switching
         .use(RING_OF_UI)                    // Switch to UI ring
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .complete();
}
```

### Using Only Custom Ring Methods
```java
@Test
@Regression
@Description("Perform the entire scenario via a custom ring (service) methods only")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void customServiceExample_usingOnlyCustomMethods(Quest quest,
      @Craft(model = DataCreator.Data.PURCHASE_CURRENCY) PurchaseForeignCurrency purchaseForeignCurrency) {

   quest
         .use(RING_OF_PURCHASE_CURRENCY)
         .purchaseCurrency(purchaseForeignCurrency)
         .validatePurchase()   // Custom ring handles everything
         .complete();
}
```

---

## 8. Table Operations

### Read Entire Table
```java
.table().readTable(Tables.FILTERED_TRANSACTIONS)
```

### Read Table with Specific Columns
```java
.table().readTable(Tables.FILTERED_TRANSACTIONS,
      TableField.of(FilteredTransactionEntry::setDescription),
      TableField.of(FilteredTransactionEntry::setWithdrawal))
```

### Read Table with Row Range
```java
.table().readTable(Tables.OUTFLOW, 3, 5)  // Rows 3-5 inclusive
```

### Read Table with Row Range and Specific Columns
```java
.table().readTable(Tables.OUTFLOW, 3, 5,
      TableField.of(OutFlow::setCategory),
      TableField.of(OutFlow::setAmount))
```

### Read Single Row by Index
```java
.table().readRow(Tables.FILTERED_TRANSACTIONS, 1)  // Row index 1
```

### Read Single Row by Index with Columns
```java
.table().readRow(Tables.OUTFLOW, 1,
      TableField.of(OutFlow::setCategory),
      TableField.of(OutFlow::setAmount))
```

### Read Single Row by Search Criteria
```java
.table().readRow(Tables.OUTFLOW, List.of(RETAIL))  // Find row containing "Retail"
```

### Read Single Row by Criteria with Columns
```java
.table().readRow(Tables.OUTFLOW, List.of(RETAIL),
      TableField.of(OutFlow::setCategory),
      TableField.of(OutFlow::setAmount))
```

### Click Element in Cell by Row Index
```java
.table().clickElementInCell(Tables.CREDIT_ACCOUNTS, 1,
      TableField.of(CreditAccounts::setAccount))
```

### Click Element in Cell by Search Criteria
```java
.table().clickElementInCell(Tables.OUTFLOW, List.of(CHECKS_WRITTEN),
      TableField.of(OutFlow::setDetails))
```

### Click Element in Cell with Data Object
```java
@Test
void clickButtonInCertainCell_usingDataObject(
      Quest quest,
      @Craft(model = DataCreator.Data.OUTFLOW_DATA) OutFlow outFlowDetails) {

   quest.use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         // ... login steps ...
         .link().click(LinkFields.MY_MONEY_MAP_LINK)
         .table().clickElementInCell(Tables.OUTFLOW, 4, outFlowDetails)
         .table().readTable(Tables.DETAILED_REPORT)
         .validate(() -> Assertions.assertEquals(
               "$105.00",
               retrieve(tableRowExtractor(Tables.DETAILED_REPORT, TRANSACTION_REPORT_DATE),
                     DetailedReport.class).getAmount().getText(),
               "Wrong Amount")
         )
         .complete();
}
```

### Click Element by Criteria with Data Object
```java
@Test
void clickButtonInCellFoundByCriteria_usingDataObject(
      Quest quest,
      @Craft(model = DataCreator.Data.OUTFLOW_DATA) OutFlow outFlowDetails) {

   quest.use(RING_OF_UI)
         // ... navigation ...
         .table().clickElementInCell(Tables.OUTFLOW, List.of(CHECKS_WRITTEN), outFlowDetails)
         .complete();
}
```

### Complete Table Validation Example
```java
@Test
@Smoke
@Regression
@Description("Read entire table and validate using table assertion types")
void readEntireTable_validateWithAssertionTypes(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, Data.testData().username())
         .input().insert(InputFields.PASSWORD_FIELD, Data.testData().password())
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .button().click(ButtonFields.MORE_SERVICES_BUTTON)
         .link().click(LinkFields.ACCOUNT_ACTIVITY_LINK)
         .list().select(ListFields.ACCOUNT_ACTIVITY_TABS, FIND_TRANSACTIONS)
         .input().insert(InputFields.AA_DESCRIPTION_FIELD, TRANSACTION_DESCRIPTION_ONLINE)
         .input().insert(InputFields.AA_FROM_DATE_FIELD, TRANSACTION_FROM_DATE)
         .input().insert(InputFields.AA_TO_DATE_FIELD, TRANSACTION_TO_DATE)
         .input().insert(InputFields.AA_FROM_AMOUNT_FIELD, TRANSACTION_AMOUNT_100)
         .input().insert(InputFields.AA_TO_AMOUNT_FIELD, TRANSACTION_AMOUNT_1000)
         .select().selectOption(SelectFields.AA_TYPE_DDL, TRANSACTION_TYPE_DEPOSIT)
         .button().click(ButtonFields.FIND_SUBMIT_BUTTON)
         .table().readTable(Tables.FILTERED_TRANSACTIONS)
         .table().validate(
               Tables.FILTERED_TRANSACTIONS,
               Assertion.builder().target(TABLE_VALUES).type(TABLE_NOT_EMPTY).expected(true).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(TABLE_ROW_COUNT).expected(2).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(EVERY_ROW_CONTAINS_VALUES).expected(List.of(ONLINE_TRANSFER_REFERENCE)).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(TABLE_DOES_NOT_CONTAIN_ROW).expected(ROW_VALUES_NOT_CONTAINED).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(ALL_ROWS_ARE_UNIQUE).expected(true).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(NO_EMPTY_CELLS).expected(false).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(COLUMN_VALUES_ARE_UNIQUE).expected(1).soft(true).build(),
               Assertion.builder().target(TABLE_VALUES).type(TABLE_DATA_MATCHES_EXPECTED).expected(ONLINE_TRANSFERS_EXPECTED_TABLE).soft(true).build(),
               Assertion.builder().target(TABLE_ELEMENTS).type(ALL_CELLS_ENABLED).expected(true).soft(true).build(),
               Assertion.builder().target(TABLE_ELEMENTS).type(ALL_CELLS_CLICKABLE).expected(true).soft(true).build())
         .table().readRow(Tables.FILTERED_TRANSACTIONS, 1)
         .table().validate(
               Tables.FILTERED_TRANSACTIONS,
               Assertion.builder().target(ROW_VALUES).type(ROW_NOT_EMPTY).expected(true).soft(true).build(),
               Assertion.builder().target(ROW_VALUES).type(ROW_CONTAINS_VALUES).expected(List.of(TRANSFER_DATE_1, ONLINE_TRANSFER_REFERENCE)).soft(true).build())
         .complete();
}
```

## 9. Complete Test Examples

### Example 1: Baseline Simple Flow (No Advanced Features)
```java
@Test
@Regression
@Description("Baseline simple flow without advanced framework features")
void baseline_simpleFlow_noAdvancedFeatures(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate("http://zero.webappsecurity.com/")
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, "username")
         .input().insert(InputFields.PASSWORD_FIELD, "password")
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .select().selectOption(SelectFields.PC_CURRENCY_DDL, CURRENCY_PESO)
         .input().insert(InputFields.AMOUNT_CURRENCY_FIELD, "100")
         .radio().select(RadioFields.DOLLARS_RADIO_FIELD)
         .button().click(ButtonFields.CALCULATE_COST_BUTTON)
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .complete();
}
```

### Example 2: Config Properties for Credentials
```java
@Test
@Regression
@Description("Retrieves Login credentials from configuration properties")
void config_properties_retrievedLoginCredentials(Quest quest) {
   quest
         .use(RING_OF_UI)
         .browser().navigate(getUiConfig().baseUrl())
         .button().click(ButtonFields.SIGN_IN_BUTTON)
         .input().insert(InputFields.USERNAME_FIELD, Data.testData().username())
         .input().insert(InputFields.PASSWORD_FIELD, Data.testData().password())
         .button().click(ButtonFields.SIGN_IN_FORM_BUTTON)
         .browser().back()
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .select().selectOption(SelectFields.PC_CURRENCY_DDL, CURRENCY_PESO)
         .input().insert(InputFields.AMOUNT_CURRENCY_FIELD, "100")
         .radio().select(RadioFields.DOLLARS_RADIO_FIELD)
         .button().click(ButtonFields.CALCULATE_COST_BUTTON)
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .complete();
}
```

### Example 3: @AuthenticateViaUi Per-Test
```java
@Test
@Regression
@Description("AuthenticateViaUi performs login per test without session caching")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
void authenticateViaUi_perTestNoCache(Quest quest) {
   quest
         .use(RING_OF_UI)
         .link().click(LinkFields.TRANSFER_FUNDS_LINK)
         .list().select(ListFields.NAVIGATION_TABS, PAY_BILLS)
         .select().selectOption(SelectFields.PC_CURRENCY_DDL, CURRENCY_PESO)
         .input().insert(InputFields.AMOUNT_CURRENCY_FIELD, "100")
         .radio().select(RadioFields.DOLLARS_RADIO_FIELD)
         .button().click(ButtonFields.CALCULATE_COST_BUTTON)
         .alert().validateValue(AlertFields.FOREIGN_CURRENCY_CASH, SUCCESSFUL_PURCHASE_MESSAGE)
         .complete();
}
```

### Example 4: Full Lifecycle with Journey and Ripper
```java
@Test
@Regression
@Description("Order creation with precondition setup and automatic cleanup")
@AuthenticateViaUi(credentials = AdminCredentials.class, type = AppUiLogin.class)
@Journey(value = SETUP_CUSTOMER_PRECONDITION,
         journeyData = {@JourneyData(DataCreator.Data.CUSTOMER)})
@Ripper(DataCleaner.Data.ORDER_CLEANER)
void createOrder(Quest quest,
      @Craft(model = DataCreator.Data.ORDER) Order order) {
   quest
         .use(RING_OF_CUSTOM)
         .createOrder(order)
         .validateOrderCreated(order)
         .complete();
}
```
