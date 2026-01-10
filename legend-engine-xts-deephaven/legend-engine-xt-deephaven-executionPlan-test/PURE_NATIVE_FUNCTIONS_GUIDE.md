# Pure Native Functions for Dynamic Deephaven Table Creation

## Overview

This solution allows **PCT tests written in Pure** to dynamically create Deephaven tables by calling **native functions** that trigger Java code for table creation.

## How It Works

```
Pure PCT Test
    ↓ calls
Pure Native Function (createTableWithData)
    ↓ implemented in
Java (DeephavenPCTNativeFunctions)
    ↓ uses
Python Script Generator
    ↓ executes
Deephaven Session
    ↓ creates
Tables in Deephaven
```

## Pure Native Functions Available

### 1. `createTableWithData` - Create a table with explicit data

```pure
meta::external::store::deephaven::pct::createTableWithData(
    tableName: String[1],
    columnNames: String[*],
    columnTypes: String[*],
    rows: Any[*]
): Boolean[1]
```

### 2. `dropTestTable` - Remove a table

```pure
meta::external::store::deephaven::pct::dropTestTable(
    tableName: String[1]
): Boolean[1]
```

### 3. `initializePCTEnvironment` - Initialize Deephaven session

```pure
meta::external::store::deephaven::pct::initializePCTEnvironment(): Boolean[1]
```

### 4. `shutdownPCTEnvironment` - Cleanup after tests

```pure
meta::external::store::deephaven::pct::shutdownPCTEnvironment(): Boolean[1]
```

## Usage in PCT Tests

### Example 1: testMultiColsSelectShared Pattern

```pure
function <<PCT.test>> meta::pure::functions::relation::tests::select::testMultiColsSelectShared<T|m>(f:Function<{Function<{->T[m]}>[1]->T[m]}>[1]):Boolean[1]
{
    // Create test table dynamically from Pure!
    meta::external::store::deephaven::pct::createTableWithData(
        'testTable',
        ['val', 'str', 'other'],
        ['Integer', 'String', 'String'],
        [
            [1, 'a', 'a'],
            [3, 'ewe', 'b'],
            [4, 'qw', 'c'],
            [5, 'wwe', 'd'],
            [6, 'weq', 'e']
        ]
    );
    
    // Now query the table that was just created
    let expr = {
        | #>{DeephavenStore.testTable}#->select(~[val, other]);
    };
    
    let res = $f->eval($expr);
    
    // Cleanup
    meta::external::store::deephaven::pct::dropTestTable('testTable');
    
    // Verify results
    assertEquals('#TDS\n' +
                 '   val,other\n' +
                 '   1,a\n' +
                 '   3,b\n' +
                 '   4,c\n' +
                 '   5,d\n' +
                 '   6,e\n' +
                 '#', $res->sort(~val->ascending())->toString());
}
```

### Example 2: Using Helper Function

```pure
function <<PCT.test>> myTest<T|m>(f:Function<{Function<{->T[m]}>[1]->T[m]}>[1]):Boolean[1]
{
    // Use pre-defined helper
    meta::external::store::deephaven::pct::setupMultiColsSelectTestData();
    
    // Your test logic here...
    let expr = {| #>{DeephavenStore.testTable}#->select(~[val, str]) };
    let res = $f->eval($expr);
    
    // Cleanup
    meta::external::store::deephaven::pct::dropTestTable('testTable');
    
    // Assertions...
    true;
}
```

### Example 3: Multiple Tables (Join Test)

```pure
function <<PCT.test>> myJoinTest<T|m>(f:Function<{Function<{->T[m]}>[1]->T[m]}>[1]):Boolean[1]
{
    // Create customers table
    meta::external::store::deephaven::pct::createTableWithData(
        'customers',
        ['customerId', 'name', 'city'],
        ['Integer', 'String', 'String'],
        [[1, 'Alice', 'NYC'], [2, 'Bob', 'LA'], [3, 'Charlie', 'Chicago']]
    );
    
    // Create orders table
    meta::external::store::deephaven::pct::createTableWithData(
        'orders',
        ['orderId', 'customerId', 'amount'],
        ['Integer', 'Integer', 'Double'],
        [[101, 1, 99.99], [102, 2, 149.99], [103, 1, 79.99]]
    );
    
    // Perform join test...
    let expr = {| #>{DeephavenStore.customers}#->join(...) };
    
    // Cleanup
    meta::external::store::deephaven::pct::dropTestTable('customers');
    meta::external::store::deephaven::pct::dropTestTable('orders');
    
    true;
}
```

## Integration with welcome.pure

Add initialization to your test adapter:

```pure
import meta::external::store::deephaven::pct::*;

function <<PCT.adapter>> {PCT.adapterName='Deephaven'} 
    meta::external::store::deephaven::pct::testAdapterForDeephavenExecution<X|o>(f:Function<{->X[o]}>[1]):X[o]
{
    // Initialize PCT environment (starts Deephaven, creates session)
    initializePCTEnvironment();
    
    // Run the test
    let result = meta::external::store::deephaven::pct::testAdapterForExecution(
        $f,
        meta::external::store::deephaven::executionPlan::platformBinding::legendJava::deephavenOnlyLegendJavaPlatformBindingExtensions()
    );
    
    // Cleanup (optional - can keep session alive for multiple tests)
    // shutdownPCTEnvironment();
    
    $result;
}
```

## What Happens Behind the Scenes

### When You Call `createTableWithData(...)` from Pure:

1. **Pure code** calls the native function
2. **Java native implementation** (`DeephavenPCTNativeFunctions.createTableWithData`) is invoked
3. **TableDefinition** object is built from the parameters
4. **PythonScriptGenerator** converts the TableDefinition to Python script:
   ```python
   from deephaven import new_table
   from deephaven.column import int_col, string_col
   
   testTable = new_table([
       int_col("val", [1, 3, 4, 5, 6]),
       string_col("str", ["a", "ewe", "qw", "wwe", "weq"]),
       string_col("other", ["a", "b", "c", "d", "e"])
   ])
   ```
5. **Python script is executed** via `session.executeScript()`
6. **Table is created** in Deephaven
7. **Function returns** `true` to Pure
8. **Your PCT test** can now query the table!

## File Structure

### Pure Files (legend-engine-xt-deephaven-pure):
- `pct/pct_table_setup.pure` - Native function declarations
- `pct/pct_environment.pure` - Environment init/shutdown
- `pct/pct_test_examples.pure` - Example tests

### Java Files (legend-engine-xt-deephaven-javaPlatformBinding-pure):
- `CoreDeephavenPCTNative_createTableWithData.java` - Native binding
- `CoreDeephavenPCTNative_dropTestTable.java` - Native binding
- `CoreDeephavenPCTNative_initializePCTEnvironment.java` - Native binding
- `CoreDeephavenPCTNative_shutdownPCTEnvironment.java` - Native binding

### Java Implementation (legend-engine-xt-deephaven-executionPlan-test):
- `DeephavenPCTNativeFunctions.java` - Native function implementations
- `DeephavenPCTTestInitializer.java` - Session management
- `DeephavenPCTTableManager.java` - Table creation
- `PythonScriptGenerator.java` - Python code generation
- `TableDefinition.java` - Table structure
- `ColumnDefinition.java` - Column structure

## Supported Column Types

| Pure Type | Java Type | Deephaven Type |
|-----------|-----------|----------------|
| Integer   | Integer   | int            |
| Long      | Long      | long           |
| Float     | Float     | float          |
| Double    | Double    | double         |
| String    | String    | String         |
| Boolean   | Boolean   | Boolean        |
| DateTime  | Long      | DateTime       |

## Best Practices

### 1. Always Cleanup
```pure
function myTest():Boolean[1]
{
    createTableWithData('myTable', ...);
    
    // ... test logic ...
    
    dropTestTable('myTable');  // Always cleanup!
    true;
}
```

### 2. Use Descriptive Table Names
```pure
// Good
createTableWithData('customers_for_join_test', ...);

// Bad
createTableWithData('t1', ...);
```

### 3. Initialize Once for Multiple Tests
```pure
// In test setup
initializePCTEnvironment();

// Run multiple tests...

// In test teardown
shutdownPCTEnvironment();
```

## Advantages Over Static Approach

✅ **Pure-Native**: Call from Pure code, no external scripts  
✅ **Dynamic**: Each test creates exactly what it needs  
✅ **Type-Safe**: Column types validated  
✅ **Clean**: Tables created and dropped per test  
✅ **Flexible**: Easy to parameterize test data  
✅ **Maintainable**: All test data visible in Pure code  

## Migration from testDataSetup.py

### Before (Static Python):
```python
# testDataSetup.py (loaded at startup)
stockTrades = new_table([
    int_col("TradeID", [1, 2, 3]),
    ...
])
```

### After (Pure Native):
```pure
// In your PCT test
function myTest():Boolean[1]
{
    createTableWithData(
        'stockTrades',
        ['TradeID', 'Symbol'],
        ['Integer', 'String'],
        [[1, 'AAPL'], [2, 'GOOG'], [3, 'MSFT']]
    );
    
    // test logic...
    
    dropTestTable('stockTrades');
    true;
}
```

## Troubleshooting

### "Session not initialized" Error
**Solution**: Call `initializePCTEnvironment()` before creating tables

### Tables Not Found
**Solution**: Ensure `createTableWithData()` completed successfully before querying

### Type Mismatch Errors
**Solution**: Check column types match the data provided

## See Also

- `pct_test_examples.pure` - Complete working examples
- `DeephavenPCTNativeFunctions.java` - Java implementation
- `PythonScriptGenerator.java` - Script generation logic

