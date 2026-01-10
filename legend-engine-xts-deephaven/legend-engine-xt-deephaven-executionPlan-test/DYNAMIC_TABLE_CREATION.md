# Dynamic Table Creation for Deephaven PCT Tests

## Overview

This document describes the Java-based dynamic table creation approach for PCT tests, replacing the static `testDataSetup.py` script.

## Problem Statement

Previously, Deephaven tables were created using a static Python script (`testDataSetup.py`) that was loaded when the Deephaven container started. This approach had limitations:

1. **Static Data**: Tables had fixed schemas and data
2. **Not PCT-Friendly**: Couldn't adapt to different PCT test requirements
3. **Manual Updates**: Required editing Python scripts for each new test

## Solution

The new approach uses **Java to dynamically generate and execute Python scripts** based on PCT test data requirements.

## Architecture

```
PCT Test Data (Java)
        ↓
TableDefinition (Java)
        ↓
PythonScriptGenerator (Java)
        ↓
Generated Python Script
        ↓
Deephaven Session Execution
        ↓
Dynamic Tables in Deephaven
```

## Key Components

### 1. TableDefinition
Defines table structure and data in Java:
```java
TableDefinition table = TableDefinition.create("myTable")
    .withIntColumn("val", 1, 3, 4, 5, 6)
    .withStringColumn("str", "a", "ewe", "qw", "wwe", "weq")
    .withStringColumn("other", "a", "b", "c", "d", "e");
```

### 2. ColumnDefinition
Defines individual columns with type-safe builders:
```java
ColumnDefinition.intColumn("id", 1, 2, 3)
ColumnDefinition.stringColumn("name", "Alice", "Bob", "Charlie")
ColumnDefinition.doubleColumn("score", 95.5, 87.3, 92.1)
```

### 3. PythonScriptGenerator
Converts Java table definitions to Python scripts:
```java
String script = PythonScriptGenerator.generateTableCreationScript(tableDefinition);
// Generates:
// from deephaven import new_table
// from deephaven.column import int_col, string_col
// myTable = new_table([
//     int_col("val", [1, 3, 4, 5, 6]),
//     string_col("str", ["a", "ewe", "qw", "wwe", "weq"]),
//     ...
// ])
```

### 4. DeephavenPCTTableManager
Orchestrates table creation and execution:
```java
DeephavenPCTTableManager manager = new DeephavenPCTTableManager(deephavenSession);
manager.createTable(tableDefinition);
```

## Usage Examples

### Example 1: Create Table for PCT Test

For `meta::pure::functions::relation::tests::select::testMultiColsSelectShared`:

```java
@Test
public void testMultiColsSelect() throws Exception {
    // Define table matching PCT test data
    TableDefinition testTable = TableDefinition.create("testTable")
        .withIntColumn("val", 1, 3, 4, 5, 6)
        .withStringColumn("str", "a", "ewe", "qw", "wwe", "weq")
        .withStringColumn("other", "a", "b", "c", "d", "e");
    
    // Create table in Deephaven
    tableManager.createTable(testTable);
    
    // Execute your PCT test
    // The test will find the table ready in Deephaven
}
```

### Example 2: Multiple Tables

```java
List<TableDefinition> tables = Arrays.asList(
    TableDefinition.create("customers")
        .withIntColumn("id", 1, 2, 3)
        .withStringColumn("name", "Alice", "Bob", "Charlie"),
    
    TableDefinition.create("orders")
        .withIntColumn("orderId", 101, 102, 103)
        .withIntColumn("customerId", 1, 2, 1)
        .withDoubleColumn("amount", 99.99, 149.99, 79.99)
);

tableManager.createTables(tables);
```

### Example 3: Programmatic Table Generation

```java
// Generate test data programmatically
Integer[] ids = IntStream.range(1, 101).boxed().toArray(Integer[]::new);
String[] names = IntStream.range(1, 101)
    .mapToObj(i -> "User_" + i)
    .toArray(String[]::new);

TableDefinition largeTable = TableDefinition.create("largeTestTable")
    .withIntColumn("id", ids)
    .withStringColumn("name", names);

tableManager.createTable(largeTable);
```

## Integration with PCT Tests

### Step 1: Setup Test Infrastructure

```java
@BeforeClass
public static void setUp() {
    // Start Deephaven
    DeephavenTestContainer.startDeephaven("0.37.4");
    
    // Create session
    String host = DeephavenTestContainer.deephavenContainer.getHost();
    int port = DeephavenTestContainer.deephavenContainer.getMappedPort(10441);
    DeephavenTarget target = DeephavenTarget.of(host, port);
    deephavenSession = new DeephavenSession(target, "Anonymous " + DeephavenTestContainer.getPsk());
    
    // Create table manager
    tableManager = new DeephavenPCTTableManager(deephavenSession);
}
```

### Step 2: Create Tables Before Each Test

```java
@Before
public void createTestData() {
    // Create tables needed for the specific test
    TableDefinition testData = extractPCTTestData();
    tableManager.createTable(testData);
}
```

### Step 3: Execute PCT Test

```java
@Test
public void test_Deephaven_PCT_Select() {
    // Your PCT test execution logic
    // Tables are already created and ready
}
```

### Step 4: Cleanup

```java
@After
public void cleanup() {
    tableManager.dropTable("testTable");
}

@AfterClass
public static void tearDown() throws Exception {
    deephavenSession.close();
    DeephavenTestContainer.stopDeephaven();
}
```

## Advantages

1. **Dynamic**: Tables created on-demand for each test
2. **Type-Safe**: Java builders with compile-time checking
3. **Flexible**: Easy to parameterize and generate test data
4. **Maintainable**: No manual Python script editing
5. **PCT-Aligned**: Can extract/mirror data from PCT tests
6. **Testable**: Table creation logic can be unit tested

## Type Mapping

| Legend Type | Java Type | Python Type | Deephaven Type |
|------------|-----------|-------------|----------------|
| Integer    | Integer   | int_col     | int            |
| Long       | Long      | int_col     | long           |
| Float      | Float     | float_col   | float          |
| Double     | Double    | float_col   | double         |
| String     | String    | string_col  | String         |
| Boolean    | Boolean   | bool_col    | Boolean        |
| DateTime   | Long      | datetime_col| DateTime       |

## Future Enhancements

1. **Automatic PCT Data Extraction**: Parse Pure TDS literals from PCT tests
2. **Schema Inference**: Auto-detect types from data
3. **Bulk Operations**: Optimize for creating many tables
4. **Direct Arrow Integration**: Use Arrow Flight for data transfer (no Python bridge)
5. **Table Templates**: Pre-defined table structures for common test patterns

## Migration from Static Python

### Old Approach (testDataSetup.py):
```python
stockTrades = new_table([
    int_col("TradeID", [1, 2, 3, 4, 5]),
    string_col("StockSymbol", ["AAPL", "GOOG", "AAPL", "MSFT", "GOOG"]),
    # ... more columns
])
```

### New Approach (Java):
```java
TableDefinition stockTrades = TableDefinition.create("stockTrades")
    .withIntColumn("TradeID", 1, 2, 3, 4, 5)
    .withStringColumn("StockSymbol", "AAPL", "GOOG", "AAPL", "MSFT", "GOOG");

tableManager.createTable(stockTrades);
```

## Troubleshooting

### Issue: Script Execution Fails
- Check Deephaven session is active
- Verify Python script syntax in logs
- Ensure column types are supported

### Issue: Table Not Found
- Verify table was created successfully
- Check table name matches exactly
- Ensure script execution completed

### Issue: Type Mismatch
- Review type mapping table
- Check data values match declared types
- Validate null handling

## See Also

- `ExampleDynamicPCTTest.java` - Working examples
- `DeephavenPCTTableManager.java` - Main API
- `TableDefinition.java` - Table structure API
- `PythonScriptGenerator.java` - Script generation

