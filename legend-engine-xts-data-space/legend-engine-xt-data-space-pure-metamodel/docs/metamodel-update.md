# DataSpace Metamodel Update: AccessPointProvider

## Overview
This update replaces the `DataSpaceExecutionContext` concept with an abstract `AccessPointProvider` class and specialized provider implementations. This change allows for more flexible and extensible data access patterns within the DataSpace framework.

## Key Changes

### Class Structure Changes
- Replaced `DataSpaceExecutionContext` with abstract `AccessPointProvider`
- Added specialized providers:
  - `MappingAccessProvider` - For mapping-based data access (similar to previous ExecutionContext)
  - `StoreAccessProvider` - For direct store access
  - `IngestAccessProvider` - For data ingestion capabilities

### Property Changes
- Renamed references in DataSpace from `executionContexts` to `accessPointProviders`
- Renamed references in DataSpace from `defaultExecutionContext` to `defaultAccessPointProvider`
- Updated DataSpaceExecutable to reference `accessPointProviderKey` instead of `executionContextKey`

## Benefits
1. **Extensibility**: The abstract AccessPointProvider allows for adding new provider types without changing the core model
2. **Flexibility**: Different access patterns (mapping, direct store, ingestion) are now explicitly modeled
3. **Clarity**: Each provider type clearly communicates its purpose and required properties
4. **Maintainability**: Specialized providers make it easier to add provider-specific functionality

## Implementation Details

### Abstract AccessPointProvider
The abstract `AccessPointProvider` class serves as the base class for all provider implementations:

```pure
Class << typemodifiers.abstract >> meta::pure::metamodel::dataSpace::AccessPointProvider
{
  name: String[1];
  title: String[0..1];
  description: String[0..1];
}
```

### Specialized Providers

#### MappingAccessProvider
Maintains the mapping, defaultRuntime, and testData properties from the original ExecutionContext:

```pure
Class meta::pure::metamodel::dataSpace::MappingAccessProvider extends AccessPointProvider
{
  mapping: meta::pure::mapping::Mapping[1];
  defaultRuntime: meta::pure::runtime::PackageableRuntime[1];
  testData: meta::pure::data::EmbeddedData[0..1];
}
```

#### StoreAccessProvider
Provides direct access to a store with a default runtime:

```pure
Class meta::pure::metamodel::dataSpace::StoreAccessProvider extends AccessPointProvider
{
  store: meta::pure::store::Store[1];
  defaultRuntime: meta::pure::runtime::PackageableRuntime[1];
}
```

#### IngestAccessProvider
Provides capabilities for data ingestion with a target store:

```pure
Class meta::pure::metamodel::dataSpace::IngestAccessProvider extends AccessPointProvider
{
  ingestMode: String[1];
  targetStore: meta::pure::store::Store[1];
}
```

## UML Diagrams
UML diagrams illustrating the updated metamodel structure are available in the `docs/uml/updated` directory.
