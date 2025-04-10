# DataSpace Metamodel Comparison: Before and After

## Overview
This document compares the original DataSpace metamodel with the updated version that introduces the abstract `AccessPointProvider` class to replace the previous `DataSpaceExecutionContext`.

## Key Changes

### Class Structure Changes

| Original Model | Updated Model | Notes |
|----------------|---------------|-------|
| `DataSpaceExecutionContext` | `AccessPointProvider` (abstract) | Replaced with abstract base class |
| - | `MappingAccessProvider` | New subclass that maintains mapping-based functionality |
| - | `StoreAccessProvider` | New subclass for direct store access |
| - | `IngestAccessProvider` | New subclass for data ingestion |

### Property Changes

| Class | Original Property | Updated Property | Notes |
|-------|------------------|------------------|-------|
| `DataSpace` | `executionContexts: DataSpaceExecutionContext[1..*]` | `accessPointProviders: AccessPointProvider[1..*]` | Renamed to reflect new class |
| `DataSpace` | `defaultExecutionContext: DataSpaceExecutionContext[1]` | `defaultAccessPointProvider: AccessPointProvider[1]` | Renamed to reflect new class |
| `DataSpaceExecutable` | `executionContextKey: String[0..1]` | `accessPointProviderKey: String[0..1]` | Renamed to reflect new class |

### Functionality Distribution

| Original Functionality | New Location | Notes |
|------------------------|--------------|-------|
| Mapping reference | `MappingAccessProvider.mapping` | Maintained in specialized provider |
| Default runtime | `MappingAccessProvider.defaultRuntime` and `StoreAccessProvider.defaultRuntime` | Maintained in relevant providers |
| Test data | `MappingAccessProvider.testData` | Maintained in mapping provider |
| - | `StoreAccessProvider.store` | New property for direct store access |
| - | `IngestAccessProvider.ingestMode` | New property for ingest configuration |
| - | `IngestAccessProvider.targetStore` | New property for ingest target |

## Detailed Comparison

### Original Model Core Classes
```
DataSpace
  - executionContexts: DataSpaceExecutionContext[1..*]
  - defaultExecutionContext: DataSpaceExecutionContext[1]
  - (other properties...)

DataSpaceExecutionContext
  - name: String[1]
  - title: String[0..1]
  - description: String[0..1]
  - mapping: Mapping[1]
  - defaultRuntime: PackageableRuntime[1]
  - testData: EmbeddedData[0..1]
```

### Updated Model Core Classes
```
DataSpace
  - accessPointProviders: AccessPointProvider[1..*]
  - defaultAccessPointProvider: AccessPointProvider[1]
  - (other properties...)

AccessPointProvider (abstract)
  - name: String[1]
  - title: String[0..1]
  - description: String[0..1]

MappingAccessProvider extends AccessPointProvider
  - mapping: Mapping[1]
  - defaultRuntime: PackageableRuntime[1]
  - testData: EmbeddedData[0..1]

StoreAccessProvider extends AccessPointProvider
  - store: Store[1]
  - defaultRuntime: PackageableRuntime[1]

IngestAccessProvider extends AccessPointProvider
  - ingestMode: String[1]
  - targetStore: Store[1]
```

## Impact on Existing Code

### Helper Functions
The existing helper function `get(DataSpace, String): DataSpaceExecutionContext` would need to be updated to return an `AccessPointProvider` instead.

### Mapping Extensions
The mapping extension functions that work with DataSpace and DataSpaceExecutionContext would need to be updated to work with AccessPointProvider and its subclasses.

### Compiler Extensions
The compiler would need to be updated to handle the new class hierarchy and ensure proper validation of the different provider types.

### Grammar
The grammar would need to be extended to support the new provider types and their specific properties.

## Benefits of the New Model

1. **Extensibility**: The abstract AccessPointProvider allows for adding new provider types without changing the core model
2. **Flexibility**: Different access patterns (mapping, direct store, ingestion) are now explicitly modeled
3. **Clarity**: Each provider type clearly communicates its purpose and required properties
4. **Maintainability**: Specialized providers make it easier to add provider-specific functionality
