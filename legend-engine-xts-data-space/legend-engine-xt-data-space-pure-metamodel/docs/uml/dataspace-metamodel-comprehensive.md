# DataSpace Metamodel - Comprehensive Documentation

## Overview
The DataSpace metamodel defines the structure and relationships for data spaces in the Legend Engine. A data space is a packagable element that encapsulates execution contexts, diagrams, and executables, allowing users to organize and interact with their data models in a structured way.

## Core Classes and Their Purpose

### DataSpace
**Purpose**: The main container class that represents a data space in Legend Engine.

**Properties**:
- `title: String[0..1]` - Optional title for the data space
- `description: String[0..1]` - Optional description for the data space
- `executionContexts: DataSpaceExecutionContext[1..*]` - One or more execution contexts
- `defaultExecutionContext: DataSpaceExecutionContext[1]` - Default execution context
- `diagrams: DataSpaceDiagram[*]` - Optional diagrams for visualization
- `elements: PackageableElement[*]` - Optional elements included in the data space
- `executables: DataSpaceExecutable[*]` - Optional executables in the data space
- `supportInfo: DataSpaceSupportInfo[0..1]` - Optional support information

**Inheritance**: Extends `PackageableElement`, inheriting its properties including `name`.

**Significance**: DataSpace is the central class that organizes all components needed for a complete data space definition. It provides a way to package related data models, mappings, and execution contexts together.

### DataSpaceExecutionContext
**Purpose**: Defines an execution context within a data space, specifying how data should be transformed and executed.

**Properties**:
- `name: String[1]` - Required name for the execution context
- `title: String[0..1]` - Optional title for the execution context
- `description: String[0..1]` - Optional description for the execution context
- `mapping: Mapping[1]` - Required mapping for data transformation
- `defaultRuntime: PackageableRuntime[1]` - Required default runtime for execution
- `testData: EmbeddedData[0..1]` - Optional test data for testing

**Significance**: Execution contexts are crucial for defining how data is transformed and executed within a data space. They link to mappings and runtimes, which are essential for data processing.

### DataSpaceDiagram
**Purpose**: Associates a diagram with a data space for visual representation.

**Properties**:
- `title: String[1]` - Required title for the diagram
- `description: String[0..1]` - Optional description for the diagram
- `diagram: Diagram[1]` - Required reference to a diagram

**Significance**: Diagrams provide visual representations of data models, making it easier for users to understand complex data structures.

### DataSpaceExecutable
**Purpose**: Base class for executables in a data space.

**Properties**:
- `title: String[1]` - Required title for the executable
- `id: String[1]` - Required ID for the executable
- `description: String[0..1]` - Optional description for the executable
- `executionContextKey: String[0..1]` - Optional execution context key

**Significance**: Executables define operations that can be performed within a data space, such as running queries or services.

### DataSpacePackageableElementExecutable
**Purpose**: Extends DataSpaceExecutable to reference a packagable element (like a service or function).

**Properties**:
- All properties from DataSpaceExecutable
- `executable: PackageableElement[1]` - Required reference to a packagable element

**Inheritance**: Extends `DataSpaceExecutable`.

**Significance**: Allows referencing existing packagable elements (like services or functions) as executables within a data space.

### DataSpaceTemplateExecutable
**Purpose**: Extends DataSpaceExecutable to define a template with a query.

**Properties**:
- All properties from DataSpaceExecutable
- `query: FunctionDefinition<Any>[1]` - Required query as a function definition

**Inheritance**: Extends `DataSpaceExecutable`.

**Significance**: Allows defining custom queries as executables within a data space.

### DataSpaceSupportInfo
**Purpose**: Abstract base class for support information.

**Properties**:
- `documentationUrl: String[0..1]` - Optional documentation URL

**Significance**: Provides a way to include support information with a data space.

### DataSpaceSupportEmail
**Purpose**: Support information with an email address.

**Properties**:
- All properties from DataSpaceSupportInfo
- `address: String[1]` - Required email address

**Inheritance**: Extends `DataSpaceSupportInfo`.

**Significance**: Provides a simple way to include an email address for support.

### DataSpaceSupportCombinedInfo
**Purpose**: Support information with multiple contact methods.

**Properties**:
- All properties from DataSpaceSupportInfo
- `emails: String[*]` - Zero or more email addresses
- `website: String[0..1]` - Optional website URL
- `faqUrl: String[0..1]` - Optional FAQ URL
- `supportUrl: String[0..1]` - Optional support URL

**Inheritance**: Extends `DataSpaceSupportInfo`.

**Significance**: Provides a comprehensive way to include multiple contact methods for support.

## Key Relationships

### Inheritance Relationships
1. **DataSpace** extends **PackageableElement**
   - Inherits the `name` property and other characteristics of packagable elements
   - Allows data spaces to be organized in packages within the Legend Engine

2. **DataSpacePackageableElementExecutable** extends **DataSpaceExecutable**
   - Specializes the executable concept to reference existing packagable elements

3. **DataSpaceTemplateExecutable** extends **DataSpaceExecutable**
   - Specializes the executable concept to define custom queries

4. **DataSpaceSupportEmail** extends **DataSpaceSupportInfo**
   - Specializes the support information concept to include an email address

5. **DataSpaceSupportCombinedInfo** extends **DataSpaceSupportInfo**
   - Specializes the support information concept to include multiple contact methods

### Composition Relationships
1. **DataSpace** contains **DataSpaceExecutionContext** (1..*)
   - A data space must have at least one execution context
   - One execution context is designated as the default

2. **DataSpace** contains **DataSpaceDiagram** (0..*)
   - A data space can have zero or more diagrams for visualization

3. **DataSpace** contains **PackageableElement** (0..*)
   - A data space can include zero or more packagable elements

4. **DataSpace** contains **DataSpaceExecutable** (0..*)
   - A data space can have zero or more executables

5. **DataSpace** contains **DataSpaceSupportInfo** (0..1)
   - A data space can have optional support information

### Association Relationships
1. **DataSpaceExecutionContext** references **Mapping** (1)
   - An execution context must reference a mapping for data transformation

2. **DataSpaceExecutionContext** references **PackageableRuntime** (1)
   - An execution context must reference a default runtime for execution

3. **DataSpaceExecutionContext** references **EmbeddedData** (0..1)
   - An execution context can have optional test data

4. **DataSpaceDiagram** references **Diagram** (1)
   - A data space diagram must reference a diagram for visualization

5. **DataSpacePackageableElementExecutable** references **PackageableElement** (1)
   - A packagable element executable must reference a packagable element

6. **DataSpaceTemplateExecutable** references **FunctionDefinition** (1)
   - A template executable must define a query as a function definition

## Integration with Legend Engine Architecture

The DataSpace metamodel integrates with the broader Legend Engine architecture in several ways:

1. **Core Legend Engine Integration**
   - DataSpace extends PackageableElement, which is a core concept in Legend Engine
   - This allows data spaces to be organized in packages and referenced by other components

2. **Mapping and Runtime Integration**
   - DataSpaceExecutionContext references Mapping and PackageableRuntime
   - This integrates data spaces with the mapping and runtime functionality of Legend Engine

3. **Diagram Integration**
   - DataSpaceDiagram references Diagram
   - This integrates data spaces with the diagram functionality of Legend Engine

4. **Executable Integration**
   - DataSpacePackageableElementExecutable references PackageableElement
   - This allows data spaces to reference services, functions, and other executables

5. **Module Organization**
   - The DataSpace metamodel is part of the legend-engine-xt-data-space-pure-metamodel module
   - This module is part of the broader legend-engine-xts-data-space module
   - The module structure allows for separation of concerns and modular development

## Usage Patterns

The DataSpace metamodel supports several usage patterns:

1. **Data Model Organization**
   - Data spaces can be used to organize related data models, mappings, and runtimes
   - This makes it easier to manage complex data landscapes

2. **Execution Context Definition**
   - Data spaces define execution contexts with mappings and runtimes
   - This allows for consistent execution of data transformations

3. **Visual Representation**
   - Data spaces can include diagrams for visual representation
   - This makes it easier to understand complex data structures

4. **Executable Definition**
   - Data spaces can define executables for running queries or services
   - This allows for consistent execution of operations on data

5. **Support Information**
   - Data spaces can include support information
   - This makes it easier for users to get help when needed

## Helper Functions

The DataSpace metamodel includes helper functions for working with data spaces:

1. **get(DataSpace, String): DataSpaceExecutionContext**
   - Retrieves an execution context from a data space by name
   - Useful for accessing specific execution contexts within a data space

2. **from(T, DataSpace): T**
   - Allows specifying a data space when using the `from` function
   - Extends the mapping functionality to work with data spaces

3. **from(T, DataSpaceExecutionContext): T**
   - Allows specifying a data space execution context when using the `from` function
   - Provides more granular control over execution contexts

## Conclusion

The DataSpace metamodel provides a comprehensive framework for defining and working with data spaces in Legend Engine. It integrates with core Legend Engine components and supports a wide range of usage patterns. The metamodel's structure, with its clear class hierarchy and relationships, makes it easy to understand and use.
