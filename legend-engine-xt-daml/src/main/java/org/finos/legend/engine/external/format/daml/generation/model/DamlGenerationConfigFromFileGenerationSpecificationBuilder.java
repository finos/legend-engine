package org.finos.legend.engine.external.format.daml.generation.model;

import org.finos.legend.engine.language.pure.dsl.generation.config.ConfigBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationType;
import org.finos.legend.engine.shared.core.operational.Assert;

public class DamlGenerationConfigFromFileGenerationSpecificationBuilder {

    public static DamlGenerationConfig build(FileGenerationSpecification fileGeneration)
    {
        Assert.assertTrue(fileGeneration.type.equals(FileGenerationType.rosetta.name()), () -> "File generation of type of rosetta expected, got '" + fileGeneration.type + "'");
        DamlGenerationConfig rosettaConfig = new DamlGenerationConfig();
        ConfigBuilder.noConfigurationPropertiesCheck(fileGeneration);
        ConfigBuilder.setScopeElements(fileGeneration, rosettaConfig);
        return rosettaConfig;
    }
}
