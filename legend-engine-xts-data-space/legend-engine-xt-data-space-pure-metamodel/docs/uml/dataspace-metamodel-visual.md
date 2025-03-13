# DataSpace Metamodel - Visual Representation

## Class Diagram

```
                                 ┌───────────────────────┐
                                 │  PackageableElement   │
                                 └───────────┬───────────┘
                                             │
                                             │ extends
                                             ▼
┌───────────────────┐           ┌───────────────────────────────────┐           ┌───────────────────────┐
│  DataSpaceExecutable │◄────────┤              DataSpace            │─────────►│    DataSpaceDiagram    │
└─────────┬─────────┘           │                                   │           └───────────┬───────────┘
          │                     │ - title: String[0..1]             │                       │
          │                     │ - description: String[0..1]       │                       │ references
    ┌─────┴─────┐               │ - executionContexts: [1..*]       │                       │
    │           │               │ - defaultExecutionContext: [1]    │                       ▼
    ▼           ▼               │ - diagrams: [*]                   │           ┌───────────────────────┐
┌─────────────┐ ┌─────────────┐ │ - elements: [*]                   │           │         Diagram        │
│PackageableElement│ │ Template   │ │ - executables: [*]               │           └───────────────────────┘
│  Executable  │ │  Executable │ │ - supportInfo: [0..1]            │
└─────────────┘ └─────────────┘ └─────────────┬─────────────────────┘
                                              │
                                              │ has
                                              ▼
┌───────────────────────┐           ┌───────────────────────────────────┐
│  DataSpaceSupportInfo  │◄──────────┤     DataSpaceExecutionContext     │
└─────────┬─────────────┘           │                                   │
          │                         │ - name: String[1]                 │
          │                         │ - title: String[0..1]             │
    ┌─────┴─────┐                   │ - description: String[0..1]       │
    │           │                   │ - mapping: Mapping[1]             │
    ▼           ▼                   │ - defaultRuntime: Runtime[1]      │
┌─────────────┐ ┌─────────────┐     │ - testData: EmbeddedData[0..1]    │
│SupportEmail │ │CombinedInfo │     └───────────────────────────────────┘
└─────────────┘ └─────────────┘
```

## Key Relationships

1. **DataSpace** extends **PackageableElement**
   - Inherits name and other properties from PackageableElement

2. **DataSpace** contains **DataSpaceExecutionContext** (1..*)
   - At least one execution context is required
   - One execution context is designated as the default

3. **DataSpace** contains **DataSpaceDiagram** (0..*)
   - Optional diagrams for visual representation
   - Each diagram references a Diagram object

4. **DataSpace** references **PackageableElement** (0..*)
   - Elements included in the data space

5. **DataSpace** contains **DataSpaceExecutable** (0..*)
   - Executables can be either:
     - DataSpacePackageableElementExecutable (references a PackageableElement)
     - DataSpaceTemplateExecutable (contains a query)

6. **DataSpace** has optional **DataSpaceSupportInfo** (0..1)
   - Support information can be either:
     - DataSpaceSupportEmail (with an email address)
     - DataSpaceSupportCombinedInfo (with multiple contact methods)

7. **DataSpaceExecutionContext** references:
   - A Mapping (1)
   - A PackageableRuntime (1)
   - Optional EmbeddedData for testing (0..1)

## Layout Information

The diagram layout in the metamodel_diagram.pure file positions:
- DataSpace in the center (position=(335.00000, 189.00000))
- DataSpaceExecutionContext below DataSpace (position=(286.00000, 545.00000))
- PackageableElement above DataSpace (position=(460.65093, 55.91568))
- DataSpaceExecutable to the left of DataSpace (position=(37.70584, 220.20489))
- DataSpaceSupportInfo to the right of DataSpace (position=(916.02361, 299.03541))
- Mapping and PackageableRuntime at the bottom (positions=(338.22260, 705.49410) and (698.42496, 705.49410))
