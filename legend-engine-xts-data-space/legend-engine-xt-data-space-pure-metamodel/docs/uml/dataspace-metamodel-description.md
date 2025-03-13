# DataSpace Metamodel UML Diagram

## Class Structure

### Core Classes

**DataSpace** (extends PackageableElement)
- Attributes:
  - title: String[0..1]
  - description: String[0..1]
  - executionContexts: DataSpaceExecutionContext[1..*]
  - defaultExecutionContext: DataSpaceExecutionContext[1]
  - diagrams: DataSpaceDiagram[*]
  - elements: PackageableElement[*]
  - executables: DataSpaceExecutable[*]
  - supportInfo: DataSpaceSupportInfo[0..1]

**DataSpaceExecutionContext**
- Attributes:
  - name: String[1]
  - title: String[0..1]
  - description: String[0..1]
  - mapping: Mapping[1]
  - defaultRuntime: PackageableRuntime[1]
  - testData: EmbeddedData[0..1]

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
  - executionContextKey: String[0..1]

**DataSpacePackageableElementExecutable** (extends DataSpaceExecutable)
- Attributes:
  - executable: PackageableElement[1]

**DataSpaceTemplateExecutable** (extends DataSpaceExecutable)
- Attributes:
  - query: FunctionDefinition<Any>[1]

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

## Relationships

1. **DataSpace** extends **PackageableElement**
2. **DataSpace** contains multiple **DataSpaceExecutionContext** instances (1..*)
3. **DataSpace** has one default **DataSpaceExecutionContext** (1)
4. **DataSpace** contains multiple **DataSpaceDiagram** instances (0..*)
5. **DataSpace** references multiple **PackageableElement** instances (0..*)
6. **DataSpace** contains multiple **DataSpaceExecutable** instances (0..*)
7. **DataSpace** has optional **DataSpaceSupportInfo** (0..1)
8. **DataSpaceExecutionContext** references one **Mapping** (1)
9. **DataSpaceExecutionContext** references one **PackageableRuntime** (1)
10. **DataSpaceExecutionContext** has optional **EmbeddedData** for testing (0..1)
11. **DataSpaceDiagram** references one **Diagram** (1)
12. **DataSpacePackageableElementExecutable** extends **DataSpaceExecutable**
13. **DataSpaceTemplateExecutable** extends **DataSpaceExecutable**
14. **DataSpacePackageableElementExecutable** references one **PackageableElement** (1)
15. **DataSpaceTemplateExecutable** contains one **FunctionDefinition** query (1)
16. **DataSpaceSupportEmail** extends **DataSpaceSupportInfo**
17. **DataSpaceSupportCombinedInfo** extends **DataSpaceSupportInfo**

## Visual Layout

The diagram layout shows:
- **DataSpace** at the center, connected to its components
- **DataSpaceExecutionContext** below DataSpace, showing the execution environment
- **DataSpaceDiagram** to the right of DataSpace for visualization
- **DataSpaceExecutable** and its subtypes to the left of DataSpace
- **DataSpaceSupportInfo** and its subtypes to the far right
- External dependencies (Mapping, PackageableRuntime, etc.) at the bottom of the diagram
