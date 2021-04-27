package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestRelationalConnectionCompilationRoundtrip
{
    @Test
    public void testJoinInferenceWithClass()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: DelegatedKerberos\n" +
                "  {\n" +
                "    serverPrincipal: 'dummyPrincipal'" +
                "  };\n" +
                "}\n");
    }
}
