# Legend Engine - XTS - Data Space

## Overview
The Data Space module provides functionality for defining and managing data spaces in Legend Engine. A data space is a packagable element that encapsulates execution contexts, diagrams, and executables, allowing users to organize and interact with their data models in a structured way.

## Key Components
The Data Space module consists of several submodules:

- **legend-engine-xt-data-space-compiler**: Provides compilation functionality for data spaces
- **legend-engine-xt-data-space-generation**: Handles generation of artifacts from data spaces
- **legend-engine-xt-data-space-grammar**: Defines the grammar for data space definitions
- **legend-engine-xt-data-space-http-api**: Exposes HTTP API endpoints for data space functionality
- **legend-engine-xt-data-space-protocol**: Defines the protocol for data space communication
- **legend-engine-xt-data-space-pure**: Contains Pure language implementations for data spaces
- **legend-engine-xt-data-space-pure-metamodel**: Defines the metamodel for data spaces

## Core Concepts

### DataSpace
A DataSpace is a packagable element that contains:
- Execution contexts with mappings and runtimes
- Diagrams for visual representation
- Executables (services, functions) that can be run within the data space
- Support information for documentation and assistance

### DataSpaceExecutionContext
An execution context within a data space that defines:
- A mapping for data transformation
- A default runtime for execution
- Optional test data for testing

### DataSpaceDiagram
A diagram associated with a data space for visual representation of the data model.

### DataSpaceExecutable
An executable element within a data space, which can be:
- A packagable element executable (service, function)
- A template executable with a query

## Integration with Legend Engine
The Data Space module integrates with the core Legend Engine components:
- Uses the Pure language for defining data spaces
- Leverages the mapping and runtime functionality for execution
- Provides HTTP API endpoints for data space analytics
- Supports compilation and generation of data space artifacts

## Usage
Data spaces can be defined using the Pure language. Here's a simple example:

```pure
DataSpace domain::MyDataSpace
{
  executionContexts:
  [
    {
      name: 'myContext';
      mapping: domain::MyMapping;
      defaultRuntime: domain::MyRuntime;
    }
  ];
  defaultExecutionContext: 'myContext';
  title: 'My Data Space';
  description: 'This is my data space for organizing my data models.';
  
  executables:
  [
    {
      title: 'My Executable';
      id: 'myExecutable';
      description: 'This is my executable service';
      executable: domain::MyService;
    }
  ];
}
```

## API Endpoints
The Data Space module provides HTTP API endpoints for analyzing data spaces:

- `POST /pure/v1/analytics/dataSpace/render`: Analyzes a data space to collect information needed for rendering
- `POST /pure/v1/analytics/dataSpace/coverage`: Analyzes a data space to get model coverage information

## Dependencies
The Data Space module depends on:
- Core Legend Engine modules for compilation and execution
- Pure language for defining data spaces
- Mapping and runtime modules for execution contexts
- Diagram module for visual representation

## Contributing
For information on how to contribute to this module, please refer to the [Legend contribution guide](https://github.com/finos/legend/blob/master/CONTRIBUTING.md).
