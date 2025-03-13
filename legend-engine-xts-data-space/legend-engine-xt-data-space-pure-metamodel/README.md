# Legend Engine - XT - Data Space - Pure Metamodel

## Overview
This module defines the Pure language metamodel for DataSpace functionality in Legend Engine. It contains the core classes and relationships that make up the DataSpace metamodel, providing the foundation for defining and working with data spaces.

## Metamodel Structure
The DataSpace metamodel is defined in Pure language and consists of several key classes:

### DataSpace
The main class that represents a data space in Legend Engine. It extends `PackageableElement` and includes:
- Title and description for documentation
- Collection of execution contexts with at least one default context
- Optional diagrams for visual representation
- Collection of elements (PackageableElements) included in the data space
- Collection of executables (services, functions) that can be run within the data space
- Optional support information for assistance

### DataSpaceExecutionContext
Defines an execution context within a data space, containing:
- Name, title, and description
- Reference to a mapping for data transformation
- Reference to a default runtime for execution
- Optional test data for testing

### DataSpaceDiagram
Associates a diagram with a data space for visual representation:
- Title and description
- Reference to a diagram

### DataSpaceExecutable
Base class for executables in a data space:
- Title and ID
- Description
- Optional execution context key

### DataSpacePackageableElementExecutable
Extends DataSpaceExecutable to reference a packagable element (like a service or function):
- All properties from DataSpaceExecutable
- Reference to a PackageableElement

### DataSpaceTemplateExecutable
Extends DataSpaceExecutable to define a template with a query:
- All properties from DataSpaceExecutable
- Query as a FunctionDefinition

### DataSpaceSupportInfo
Abstract base class for support information:
- Optional documentation URL

### DataSpaceSupportEmail
Support information with an email address:
- Extends DataSpaceSupportInfo
- Email address

### DataSpaceSupportCombinedInfo
Support information with multiple contact methods:
- Extends DataSpaceSupportInfo
- Collection of email addresses
- Optional website, FAQ URL, and support URL

## Helper Functions
The module includes helper functions for working with DataSpace objects:

### get(DataSpace, String): DataSpaceExecutionContext
Retrieves an execution context from a data space by name:
```pure
function meta::pure::metamodel::dataSpace::get(ds: DataSpace[1], key: String[1]): DataSpaceExecutionContext[1]
{
  let context = $ds.executionContexts->cast(@DataSpaceExecutionContext)->filter(x| $x.name == $key->toOne());
  assert($context->isNotEmpty(),| 'The key value provided is not present in the dataspace contexts');
  $context->at(0);
}
```

## Mapping Extensions
The module extends mapping functionality to work with data spaces:

### from(T, DataSpace): T
Allows specifying a data space when using the `from` function:
```pure
function <<functionType.NotImplementedFunction>> meta::pure::mapping::from<T|m>(t: T[m], dataSpace: meta::pure::metamodel::dataSpace::DataSpace[1]): T[m]
{
   $t
}
```

### from(T, DataSpaceExecutionContext): T
Allows specifying a data space execution context when using the `from` function:
```pure
function <<functionType.NotImplementedFunction>> meta::pure::mapping::from<T|m>(t: T[m], dataSpace: meta::pure::metamodel::dataSpace::DataSpaceExecutionContext[1]): T[m]
{
   $t
}
```

## Class Diagram
The metamodel includes a class diagram that visualizes the relationships between the DataSpace classes:

- DataSpace extends PackageableElement
- DataSpace has many DataSpaceExecutionContext instances, with one designated as default
- DataSpace has many DataSpaceDiagram instances
- DataSpace has many PackageableElement instances
- DataSpace has many DataSpaceExecutable instances
- DataSpace has an optional DataSpaceSupportInfo
- DataSpaceExecutionContext references a Mapping and a PackageableRuntime
- DataSpaceSupportEmail and DataSpaceSupportCombinedInfo extend DataSpaceSupportInfo

## Integration with Legend Engine
This metamodel integrates with other Legend Engine components:
- Uses the Pure language for defining the metamodel
- Integrates with mapping and runtime functionality
- Supports compilation and generation of data space artifacts
- Provides the foundation for data space analytics

## Dependencies
The module depends on:
- Core Legend Engine modules
- Pure language modules
- Mapping and runtime modules
- Diagram modules

## Usage
The DataSpace metamodel is used by other Legend Engine modules to:
- Define data spaces in Pure language
- Compile data space definitions
- Generate artifacts from data spaces
- Analyze data spaces for coverage and other metrics
