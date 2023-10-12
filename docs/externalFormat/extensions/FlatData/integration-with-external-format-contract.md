# Flatdata integration with External Format

This page explains the integration of Flatdata with External Format Contract 

### Flatdata contract

Flatdata integrates with external format ecosystem via following contracts :

1. [Flatdata ExternalFormat contract instance](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure/src/main/resources/core_external_format_flatdata/externalFormatContract.pure)
2. [Flatdata ExternalFormat LegendJavaPlatformBindingDescriptor](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-javaPlatformBinding-pure/src/main/resources/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/descriptor.pure)
3. [Flatdata External Format Extension](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/src/main/java/org/finos/legend/engine/external/format/flatdata/FlatDataExternalFormatExtension.java)
4. [Flatadata Runtime extension](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-runtime/src/main/java/org/finos/legend/engine/external/format/flatdata/FlatDataRuntimeExtension.java)

### Flatdata module structure

Flatdata source code is distributed across 6 core modules as explained below :

1. `legend-engine-xt-flatdata-pure`: This module contains pure code for flatdata format. It hosts
    - [Flatdata ExternalFormat contract instance](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure/src/main/resources/core_external_format_flatdata/externalFormatContract.pure)
    - [Flatdata metamodel](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure/src/main/resources/core_external_format_flatdata/metamodel/metamodel.pure)
    - [Flatdata to Pure models transformation logic](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure/src/main/resources/core_external_format_flatdata/transformation/toPure)
    - [Flatdata from Pure models transformation logic](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure/src/main/resources/core_external_format_flatdata/transformation/fromPure)
    - [Flatdata binding validation logic](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-pure/src/main/resources/core_external_format_flatdata/binding/validation)
    
2. `legend-engine-xt-flatdata-shared`: This is a lightweight module containing contracts/code shared by generation/execution logic. It hosts
    - [Flatdata protocol classes](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-shared/pom.xml) : These are generated using a plugin.
    - [Flatdata driver contracts](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-shared/src/main/java/org/finos/legend/engine/external/format/flatdata/driver/spi)
    - [Flatdata core drivers](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-shared/src/main/java/org/finos/legend/engine/external/format/flatdata/driver/core)
    
3. `legend-engine-xt-flatdata-model`: This module links flatdata to external format ecosystem. It hosts
    - [Flatdata External Format Extension](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/src/main/java/org/finos/legend/engine/external/format/flatdata/FlatDataExternalFormatExtension.java)
    - [Flatdata Parser](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/src/main/java/org/finos/legend/engine/external/format/flatdata/grammar/fromPure)
    - [Flatdata Composer](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/src/main/java/org/finos/legend/engine/external/format/flatdata/grammar/toPure)
    - [Flatdata Compiler](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/pom.xml) : This transformation is generated using a plugin
    - [Flatdata Transformer](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/pom.xml) : This transformation is generated using a plugin
    - [Flatdata Extension Service File](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-model/src/main/resources/META-INF/services/org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension)

4. `legend-engine-xt-flatdata-javaPlatformBinding-pure`: This module contains pure code to generate java classes for serialization/deserialization to/from flatdata. It hosts
    - [Flatdata ExternalFormat LegendJavaPlatformBindingDescriptor](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-javaPlatformBinding-pure/src/main/resources/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/descriptor.pure)
    - [Flatdata internalize descriptor](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-javaPlatformBinding-pure/src/main/resources/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/internalize.pure)
    - [Flatdata externalize descriptor](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-javaPlatformBinding-pure/src/main/resources/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/externalize.pure)
    
5. `legend-engine-xt-flatdata-runtime`: This module contains logic to execute externalize/internalize on Flatdata. It hosts
   - [Flatadata runtime extension](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-runtime/src/main/java/org/finos/legend/engine/external/format/flatdata/FlatDataRuntimeExtension.java)
   - [Flatdata runtime extension Service File](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-runtime/src/main/resources/META-INF/services)

6. `legend-engine-xt-flatdata-javaPlatformBinding-test`: This module contains logic to execute flatdata tests in maven pipeline.

Apart from these core modules we believe flatdata is a generic format which should cater to variety of file formats. Hence we have structured flatdata to be extensible and support variety of other file formats (like bloomberg) via [FlatDataDriverDescription](../../../../legend-engine-xts-flatdata/legend-engine-xt-flatdata-shared/src/main/java/org/finos/legend/engine/external/format/flatdata/driver/spi/FlatDataDriverDescription.java) implementations <br>
Module: `legend-engine-xt-flatdata-driver-bloomberg` serves as an example how flatdata can be extended for more formats. 
