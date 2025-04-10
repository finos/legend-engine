# DataSpace Metamodel with AccessPointProvider - Description

## Overview
The updated DataSpace metamodel introduces the abstract `AccessPointProvider` class to replace the previous `DataSpaceExecutionContext`. This change allows for more flexible and extensible data access patterns within the DataSpace framework.

## Key Classes

**DataSpace** (extends PackageableElement)
- Attributes:
  - title: String[0..1]
  - description: String[0..1]
  - accessPointProviders: AccessPointProvider[1..*]
  - defaultAccessPointProvider: AccessPointProvider[1]
  - diagrams: DataSpaceDiagram[*]
  - elements: PackageableElement[*]
  - executables: DataSpaceExecutable[*]
  - supportInfo: DataSpaceSupportInfo[0..1]

**AccessPointProvider** (abstract)
- Attributes:
  - name: String[1]
  - title: String[0..1]
  - description: String[0..1]

**MappingAccessProvider** (extends AccessPointProvider)
- Attributes:
  - mapping: Mapping[1]
  - defaultRuntime: PackageableRuntime[1]
  - testData: EmbeddedData[0..1]

**StoreAccessProvider** (extends AccessPointProvider)
- Attributes:
  - store: Store[1]
  - defaultRuntime: PackageableRuntime[1]

**IngestAccessProvider** (extends AccessPointProvider)
- Attributes:
  - ingestMode: String[1]
  - targetStore: Store[1]

**DataSpaceDiagram**
- Attributes:
  - title: String[1]
  - description: String[0..1]
  - diagram: Diagram[1]

**DataSpaceExecutable** (abstract)
- Attributes:
  - title: String[1]
  - id: String[1]
  - description: String[0..1]
  - accessPointProviderKey: String[0..1]

**DataSpacePackageableElementExecutable** (extends DataSpaceExecutable)
- Attributes:
  - executable: PackageableElement[1]

**DataSpaceTemplateExecutable** (extends DataSpaceExecutable)
- Attributes:
  - query: FunctionDefinition[1]

**DataSpaceSupportInfo** (abstract)
- Attributes:
  - documentationUrl: String[0..1]

**DataSpaceSupportEmail** (extends DataSpaceSupportInfo)
- Attributes:
  - address: String[1]

**DataSpaceSupportCombinedInfo** (extends DataSpaceSupportInfo)
- Attributes:
  - emails: String[*]
  - website: String[0..1]
  - faqUrl: String[0..1]
  - supportUrl: String[0..1]

## Key Relationships

### Inheritance Relationships
- DataSpace extends PackageableElement
- MappingAccessProvider, StoreAccessProvider, and IngestAccessProvider extend AccessPointProvider
- DataSpacePackageableElementExecutable and DataSpaceTemplateExecutable extend DataSpaceExecutable
- DataSpaceSupportEmail and DataSpaceSupportCombinedInfo extend DataSpaceSupportInfo

### Composition Relationships
- DataSpace contains AccessPointProvider (1..*)
- DataSpace designates one AccessPointProvider as default
- DataSpace contains DataSpaceDiagram (0..*)
- DataSpace contains PackageableElement (0..*)
- DataSpace contains DataSpaceExecutable (0..*)
- DataSpace contains DataSpaceSupportInfo (0..1)

### Association Relationships
- MappingAccessProvider references Mapping (1)
- MappingAccessProvider references PackageableRuntime (1)
- MappingAccessProvider references EmbeddedData (0..1)
- StoreAccessProvider references Store (1)
- StoreAccessProvider references PackageableRuntime (1)
- IngestAccessProvider references Store (1)
- DataSpaceDiagram references Diagram (1)
- DataSpacePackageableElementExecutable references PackageableElement (1)
- DataSpaceTemplateExecutable references FunctionDefinition (1)

## Key Changes from Previous Model

1. Replaced `DataSpaceExecutionContext` with abstract `AccessPointProvider`
2. Added specialized providers:
   - `MappingAccessProvider` - For mapping-based data access (similar to previous ExecutionContext)
   - `StoreAccessProvider` - For direct store access
   - `IngestAccessProvider` - For data ingestion capabilities
3. Renamed references in DataSpace from `executionContexts` to `accessPointProviders`
4. Renamed references in DataSpace from `defaultExecutionContext` to `defaultAccessPointProvider`
5. Updated DataSpaceExecutable to reference `accessPointProviderKey` instead of `executionContextKey`

## Benefits of the New Model

1. **Extensibility**: The abstract AccessPointProvider allows for adding new provider types without changing the core model
2. **Flexibility**: Different access patterns (mapping, direct store, ingestion) are now explicitly modeled
3. **Clarity**: Each provider type clearly communicates its purpose and required properties
4. **Maintainability**: Specialized providers make it easier to add provider-specific functionality
