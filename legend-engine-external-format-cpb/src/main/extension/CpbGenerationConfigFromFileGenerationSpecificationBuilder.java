package org.finos.legend.engine.external.language.cpb.extension;

import org.finos.legend.engine.external.format.cpb.schema.generations.CpbGenerationConfig;
import org.finos.legend.engine.language.pure.dsl.generation.config.ConfigBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationType;
import org.finos.legend.engine.shared.core.operational.Assert;

public class CpbGenerationConfigFromFileGenerationSpecificationBuilder
{
    public static CpbGenerationConfig build(FileGenerationSpecification fileGeneration)
    {
        Assert.assertTrue(fileGeneration.type.equals(FileGenerationType.cpb.name()), () -> "File generation of type of cpb expected, got '" + fileGeneration.type + "'");
        CpbGenerationConfig cpbConfig = new CpbGenerationConfig();
        ConfigBuilder.duplicateCheck(fileGeneration.configurationProperties);
        //ConfigBuilder.noConfigurationPropertiesCheck(fileGeneration);
        ConfigBuilder.setScopeElements(fileGeneration, cpbConfig);
        fileGeneration.configurationProperties.forEach(e -> ConfigBuilder.setConfigurationProperty(fileGeneration, e, cpbConfig));
        return cpbConfig;
    }
}