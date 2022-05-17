package org.finos.legend.engine.language.pure.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsFinCloudConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestAwsFinCloudConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ConnectionParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                AwsFinCloudConnectionParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "AwsFinCloudConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  datasetId: 'aws';\n" +
                "  authenticationStrategy: awsOAuth\n" +
                "  {\n" +
                "    secretArn: 'name';\n" +
                "    discoveryUrl: 'name';\n" +
                "  };\n" +
                "  apiUrl: 'abc';\n" +
                "  queryInfo: 'info';\n" +
                "}\n\n";
    }

    @Test
    public void testFinCloudConnection()
    {
        // Correctly written
        test("###Connection\n" +
                "AwsFinCloudConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  datasetId: 'aws';\n" +
                "  authenticationStrategy: awsOAuth\n" +
                "  {\n" +
                "    secretArn: 'name';\n" +
                "    discoveryUrl: 'name';\n" +
                "  };\n" +
                "  apiUrl: 'test';\n" +
                "  queryInfo: 'info';\n" +
                "}\n");

    }
}
