package org.finos.legend.engine.external.format.cpb.tests;

import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.external.format.cpb.extension.CpbGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestCpbFileGeneration
{

    @Test
    public void testCpbGenerationOutput() throws IOException
    {
        CpbGenerationExtension generationExtension = new CpbGenerationExtension();

        String pureCode = IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("org/finos/legend/engine/external/format/cpb/tests/TestCpbGeneration.pure")), StandardCharsets.UTF_8);
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(pureCode);
        PureModel pureModel = new PureModel(pureModelContextData, null, DeploymentMode.TEST);
        FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);

        List<Root_meta_pure_generation_metamodel_GenerationOutput> outputs = generationExtension.generateFromElement(fileGeneration, pureModel.getContext());
        Map<String, String> fileToContent = outputs.stream().collect(Collectors.toMap(Root_meta_pure_generation_metamodel_GenerationOutput::_fileName, Root_meta_pure_generation_metamodel_GenerationOutput::_content));

        Assert.assertEquals(fileToContent.size(), 1);
        Assert.assertEquals(fileToContent.keySet().toArray()[0], "cnas/TestPipe.cpb");

    }
}