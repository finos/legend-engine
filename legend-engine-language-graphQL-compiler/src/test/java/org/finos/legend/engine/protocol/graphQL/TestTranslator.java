package org.finos.legend.engine.protocol.graphQL;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.graphQL.grammar.test.roundtrip.TestGraphQLRoundtrip;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;

public class TestTranslator extends TestGraphQLRoundtrip
{
    @Override
    protected void check(String value)
    {
        PureModel pureModel = new PureModel(PureModelContextData.newBuilder().build(), Lists.mutable.empty(), DeploymentMode.TEST);
        GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
        Document document = parser.parseDocument(value);
        Assert.assertEquals(value, org.finos.legend.pure.generated.core_external_query_graphQL_serialization.Root_meta_external_query_graphQL_serialization_graphQLtoString_Document_1__String_1_(new Translator().translate(document, pureModel), pureModel.getExecutionSupport()));
    }
}
