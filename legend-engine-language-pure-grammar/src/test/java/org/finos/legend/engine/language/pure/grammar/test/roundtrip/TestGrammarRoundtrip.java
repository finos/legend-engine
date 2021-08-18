package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestGrammarRoundtrip
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testGrammarRoundtripWithoutSectionIndex()
    {
        // NOTE: stress test to account for flakiness
        for (int i = 0; i < 500; i++)
        {
            StringBuilder code = new StringBuilder();
            for (int t = 200; t > 0; t--)
            {
                code.append("Class model::class").append(t).append("\n").append("{\n").append("  prop: String[1];\n").append("}\n");
                if (t != 1)
                {
                    code.append("\n");
                }
            }
            PureModelContextData modelData = null;
            try
            {
                modelData = PureGrammarParser.newInstance().parseModel(code.toString());
                String json = objectMapper.writeValueAsString(modelData);
                modelData = objectMapper.readValue(json, PureModelContextData.class);
                List<PackageableElement> elements = ListIterate.select(modelData.getElements(), element -> !(element instanceof SectionIndex));
                modelData = PureModelContextData.newPureModelContextData(modelData.getSerializer(), modelData.getOrigin(), elements);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build());
            Assert.assertEquals(code.toString(), grammarTransformer.renderPureModelContextData(modelData));
        }
    }
}
