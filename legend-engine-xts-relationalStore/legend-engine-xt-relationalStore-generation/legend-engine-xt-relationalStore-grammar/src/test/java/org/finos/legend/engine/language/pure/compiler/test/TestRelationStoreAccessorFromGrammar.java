// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test;

import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationStoreAccessorFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Test
    public void testCompilationOfRelationStoreAccessor()
    {
        test("###Relational\n" +
                "Database my::Store" +
                "(" +
                "   Table myTable" +
                "   (" +
                "       id INT," +
                "       name VARCHAR(200)" +
                "   )" +
                ")\n" +
                "###Pure\n" +
                "function my::func():Any[*]" +
                "{" +
                "   #>{my::Store.myTable}#->filter(c|$c.name == 'ok');" +
                "}");
    }

    @Test
    public void testCompilationOfRelationStoreAccessorUnknownTable()
    {
        try
        {
            test("###Relational\n" +
                    "Database my::Store" +
                    "(" +
                    "   Table myTable" +
                    "   (" +
                    "       id INT," +
                    "       name VARCHAR(200)" +
                    "   )" +
                    ")\n" +
                    "###Pure\n" +
                    "function my::func():Any[*]" +
                    "{" +
                    "   #>{my::Store.myTabe}#->filter(c|$c.name == 'ok');" +
                    "}");
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [4:31-51]: The table myTabe can't be found in the store Store", e.toPretty());
        }
    }

    @Test
    public void testCompilationOfRelationStoreAccessorUnknownColumn()
    {
        try
        {
            test("###Relational\n" +
                    "Database my::Store" +
                    "(" +
                    "   Table myTable" +
                    "   (" +
                    "       id INT," +
                    "       name VARCHAR(200)" +
                    "   )" +
                    ")\n" +
                    "###Pure\n" +
                    "function my::func():Any[*]" +
                    "{" +
                    "   #>{my::Store.myTable}#->filter(c|$c.naeme == 'ok');" +
                    "}");
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [4:67-71]: The column 'naeme' can't be found in the relation (id:Integer, name:String)", e.toPretty());
        }
    }


    @Test
    public void testCompilationErrorMissingTable()
    {
        try
        {
            test("###Relational\n" +
                    "Database my::Store" +
                    "(" +
                    "   Table myTable" +
                    "   (" +
                    "       id INT," +
                    "       name VARCHAR(200)" +
                    "   )" +
                    ")\n" +
                    "###Pure\n" +
                    "function my::func():Any[*]" +
                    "{" +
                    "   #>{my::Store}#->filter(c|$c.naeme == 'ok');" +
                    "}");
            Assert.fail();
        }
        catch (EngineException e)
        {
            Assert.assertEquals("COMPILATION error at [4:31-44]: Error in the accessor definition. Please provide a table.", e.toPretty());
        }
    }

    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Relational\n" +
                "Database anything::somethingelse\n" +
                "(\n" +
                ")";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }
}
