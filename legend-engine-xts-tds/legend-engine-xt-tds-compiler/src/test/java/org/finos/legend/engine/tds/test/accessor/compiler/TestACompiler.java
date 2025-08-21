// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.tds.test.accessor.compiler;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

public class TestACompiler
{
    @Test
    public void testTDSAccessor()
    {
        test("###Pure\n" +
                "function pack::f():Boolean[1]\n" +
                "{\n" +
                "   #TDS{\n" +
                "   val,ok\n" +
                "   a,b\n" +
                "   }#->extend(~n:z|$z.ok->toOne()+'ww');\n" +
                "   true;\n" +
                "}");
    }

    @Test
    public void testTDSAccessorError()
    {
        test("###Pure\n" +
                "function pack::f():Boolean[1]\n" +
                "{\n" +
                "   #TDS{\n" +
                "   val,ok\n" +
                "   a,b\n" +
                "   }#->extend(~n:z|$z.oks->toOne()+'ww');\n" +
                "   true;\n" +
                "}", "The column 'oks' can't be found in the relation (val:String, ok:String)");
    }

    private static void test(String code)
    {
        try
        {
            new PureModel(PureGrammarParser.newInstance().parseModel(code, "", 0, 0, true), null, DeploymentMode.PROD);
        }
        catch (EngineException e)
        {
            throw new RuntimeException("An error occurred while parsing the test code.", e);
        }
    }

    private static void test(String code, String error)
    {
        try
        {
            new PureModel(PureGrammarParser.newInstance().parseModel(code, "", 0, 0, true), null, DeploymentMode.PROD);
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals(error, e.getMessage());
        }
    }
}
