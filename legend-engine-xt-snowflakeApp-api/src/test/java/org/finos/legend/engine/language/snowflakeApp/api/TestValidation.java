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

package org.finos.legend.engine.language.snowflakeApp.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.functionActivator.api.FunctionActivatorAPI;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.api.input.FunctionActivatorInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.ServiceLoader;

public class TestValidation
{
    private final FunctionActivatorAPI api = new FunctionActivatorAPI(new ModelManager(DeploymentMode.TEST), (PureModel pureModel) -> Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)).flatCollect(e -> e.getExtraExtensions(pureModel)));

    @Test
    public void testProperPlan()
    {
        String val =
                        "Class a::Person {name : String[1];}\n" +
                        "function a::f():a::Person[*]{a::Person.all()->from(a::m, ^meta::pure::runtime::Runtime(connections=^meta::relational::runtime::TestDatabaseConnection(element=a::db, type=meta::relational::runtime::DatabaseType.H2)))}\n" +
                        "###Mapping\n" +
                        "Mapping a::m(a::Person:Relational{name : [a::db]tb.name})\n" +
                        "###Relational\n" +
                        "Database a::db(Table tb(name VARCHAR(100)))\n" +
                        "###Snowflake\n" +
                        "SnowflakeApp a::myApp{" +
                        "   applicationName: 'name';" +
                        "   description: 'ee';" +
                        "   function: a::f():Person[*];" +
                        "}";
        Response response = api.validate(new FunctionActivatorInput("vX_X_X", "a::myApp", PureGrammarParser.newInstance().parseModel(val)), null);
        Assert.assertEquals("[]", response.getEntity().toString());
    }

    @Test
    public void testImproperPlan()
    {
        String val =
                "Class a::Person {name : String[1]; address : a::Address[1];}\n" +
                        "Class a::Address{zip:String[1];}\n" +
                        "function a::f():a::Person[*]{a::Person.all()->graphFetch(#{a::Person{name,address{zip}}}#)->from(a::m, ^meta::pure::runtime::Runtime(connections=^meta::relational::runtime::TestDatabaseConnection(element=a::db, type=meta::relational::runtime::DatabaseType.H2)))}\n" +
                        "###Mapping\n" +
                        "Mapping a::m(a::Person:Relational{name : [a::db]tb.name, address : [a::db]@j} a::Address:Relational{zip : [a::db]addr.zip})\n" +
                        "###Relational\n" +
                        "Database a::db(Table tb(id INT PRIMARY KEY, name VARCHAR(100)) Table addr(id INT PRIMARY KEY, zip VARCHAR(100)) Join j(tb.id=addr.id))\n" +
                        "###Snowflake\n" +
                        "SnowflakeApp a::myApp{" +
                        "   applicationName: 'name';" +
                        "   description: 'ee';" +
                        "   function: a::f():Person[*];" +
                        "}";
        Response response = api.validate(new FunctionActivatorInput("vX_X_X", "a::myApp", PureGrammarParser.newInstance().parseModel(val)), null);
        Assert.assertEquals("[{\"foundSQLs\":[],\"message\":\"SnowflakeApp can't be used with a plan containing '0' SQL expressions\"}]", response.getEntity().toString());
    }

    @Test
    public void testList() throws Exception
    {
        Response response = api.list(null);
        System.out.println(response.getEntity().toString());
        List<FunctionActivatorInfo> info = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(response.getEntity().toString(), new TypeReference<List<FunctionActivatorInfo>>(){});
        Assert.assertEquals(1, info.size());
        Assert.assertEquals("Snowflake App", info.get(0).name);
        Assert.assertEquals("Create a SnowflakeApp that can activate the function in Snowflake. It then can be used in SQL expressions and be shared with other accounts", info.get(0).description);
        Assert.assertEquals("meta::protocols::pure::vX_X_X::metamodel::functionActivator::snowflakeApp::SnowflakeApp", info.get(0).configuration.topElement);
        Assert.assertEquals(8, info.get(0).configuration.model.size());
    }
}
