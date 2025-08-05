// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.sql.compiler.test;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCompilerFromGrammar
{
    @Test
    public void testSimple()
    {
        testCompile("function pack::f():Boolean[1]\n" +
                "{\n" +
                "   #SQL{select a from csv('a,b\n1,2\n3,4')}#->filter(x|$x.a == 1);" +
                "   true;" +
                "}");
    }

    @Test
    public void  testSimpleWithError()
    {
        testCompile("function pack::f():Boolean[1]\n" +
                "{\n" +
                "   #SQL{select a from csv('a,b\n1,2\n3,4')}#->filter(x|$x.ba == 1);" +
                "   true;" +
                "}","COMPILATION error at [5:22-23]: The column 'ba' can't be found in the relation (a:Integer)");
    }

    @Test
    public void testTable()
    {
        testCompile("" +
                "###Relational\n" +
                "Database pack::DB" +
                "(\n" +
                "   Table myTab(a Varchar(200))" +
                ")\n" +
                "###Pure\n" +
                "function pack::f():Boolean[1]\n" +
                "{\n" +
                "   #SQL{select a from tb('pack::DB.myTab')}#;" +
                "   true;" +
                "}");
    }

    private void testCompile(String code)
    {
        new PureModel(PureGrammarParser.newInstance().parseModel(code), null, DeploymentMode.PROD);
    }

    private void testCompile(String code, String errorMessage)
    {
        try
        {
            new PureModel(PureGrammarParser.newInstance().parseModel(code), null, DeploymentMode.PROD);
            fail();
        }
        catch (EngineException e)
        {
            assertNotNull("No source information provided in error", e.getSourceInformation());
            assertEquals(errorMessage, e.toPretty());
        }
        catch (Exception e)
        {
            assertEquals(errorMessage, e.getMessage());
        }
    }
}
