package org.finos.legend.engine.external.format.daml.extension;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.external.format.daml.generation.model.DamlGenerationConfig;
import org.finos.legend.engine.external.format.daml.generation.model.DamlGenerationConfigFromFileGenerationSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.Root_meta_external_language_daml_generation_DamlConfig;
import org.finos.legend.pure.generated.core_external_language_daml_transformation;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class TestDamlGenerationExtension {

    @Test
    public void testSimpleDaml()
    {
        try
        {
            PureModelContextData pureModelContextData = getProtocol("simpleFileGeneration.json");
            PureModel pureModel = new PureModel(pureModelContextData, null, DeploymentMode.TEST);
            FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
            DamlGenerationConfig config = DamlGenerationConfigFromFileGenerationSpecificationBuilder.build(fileGeneration);
            Root_meta_external_language_daml_generation_DamlConfig metaModelConfig = config.transformToPure(pureModel);
            List<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = core_external_language_daml_transformation.Root_meta_external_language_daml_generation_generateDamlFromPure_DamlConfig_1__GenerationOutput_MANY_(metaModelConfig, pureModel.getExecutionSupport()).toList();
            Assert.assertEquals(outputs.size(), 2);
            Assert.assertTrue(outputs.get(0)._content().startsWith("module"));
            Assert.assertTrue(outputs.get(1)._content().startsWith("module"));
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private PureModelContextData getProtocol(String fileName) throws JsonProcessingException
    {
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(this.getResourceAsString(fileName), PureModelContextData.class);
    }

    private String getResourceAsString(String fileName)
    {
        InputStream inputStream = TestDamlGenerationExtension.class.getResourceAsStream(fileName);
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
