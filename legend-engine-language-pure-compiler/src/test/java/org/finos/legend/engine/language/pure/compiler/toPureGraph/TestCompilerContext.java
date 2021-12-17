package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

public class TestCompilerContext
{
    @Test
    public void testCommonImportsCache()
    {
        PureModelContextData contextData = PureModelContextData.newPureModelContextData();
        PureModel pureModel = Compiler.compile(contextData, null, null);

        CompileContext compileContext = new CompileContext.Builder(pureModel).build();
        compileContext.resolve("doc", null, path -> pureModel.getProfile(path, null));
        Assert.assertEquals(1, pureModel.getCommonPaths().size());
    }

    @Test
    public void testCommonImportsCache_withAutoImports()
    {
        PureModel pureModel = Compiler.compile(PureModelContextData.newPureModelContextData(), null, null);
        CompileContext compileContext = new CompileContext.Builder(pureModel).build();
        compileContext.resolve("doc", null, path -> pureModel.getProfile(path, null));
        Assert.assertEquals(1, pureModel.getCommonPaths().size());

        String grammar = "import test::*;\n" +
                "\n" +
                "Profile test::doc {}\n" +
                "\n" +
                "Class <<doc.doc>> test::mewo {\n" +
                "}";
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(grammar);
        try
        {
            Compiler.compile(contextData, null, null);
            Assert.fail("Expected compilation error but no error occurred");
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [5:9-11]: Can't resolve element with path 'doc' - multiple matches found [meta::pure::profiles::doc, test::doc]",
                    EngineException.buildPrettyErrorMessage(e.getMessage(), e.getSourceInformation(), e.getErrorType()));
        }
    }

    @Test
    public void testCommonImportsCache_dontCacheProjectSpecificElements()
    {
        String grammar = "import test::*;\n" +
                "\n" +
                "Class test::one {}\n" +
                "Class test::two {\n" +
                "   prop1: one[1];\n" +
                "}";
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModel = Compiler.compile(contextData, null, null);
        Assert.assertTrue(pureModel.getCommonPaths().contains("test::one"));
    }
}
