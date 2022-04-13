package org.finos.legend.engine.language.pure.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsS3ConnectionParserGrammar;
import org.junit.Test;

import java.util.List;


public class TestAwsS3ConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
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
                AwsS3ConnectionParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "AwsS3Connection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  partition: AWS;\n" +
                "  region: 'US';\n" +
                "  bucket: 'abc';\n" +
                "}\n\n";
    }

    @Test
    public void testS3Connection()
    {
        // Missing fields
        test("###Connection\n" +
                "AwsS3Connection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "}\n\n", "PARSER error at [2:1-5:1]: Field 'partition' is required");
        test("###Connection\n" +
                "AwsS3Connection meta::mySimpleConnection\n" +
                "{\n" +
                "  partition: AWS;\n" +
                "  store: model::firm::Person;\n" +
                "}\n\n", "PARSER error at [2:1-6:1]: Field 'region' is required");
        // Correctly written
        test("###Connection\n" +
                "AwsS3Connection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  partition: AWS;\n" +
                "  region: 'US';\n" +
                "  bucket: 'abc';\n" +
                "}\n");

    }
}
