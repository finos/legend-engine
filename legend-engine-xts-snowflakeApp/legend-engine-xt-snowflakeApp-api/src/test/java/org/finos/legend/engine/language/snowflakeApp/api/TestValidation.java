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
import org.finos.legend.engine.functionActivator.api.FunctionActivatorAPI;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.api.input.FunctionActivatorInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;

public class TestValidation
{
    private final FunctionActivatorAPI api = new FunctionActivatorAPI(new ModelManager(DeploymentMode.TEST), (PureModel pureModel) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())));

    @Test
    public void testProperPlan()
    {
        String val =
                        "Class a::Person {name : String[1];}\n" +
                        "function a::f():TabularDataSet[1]{a::Person.all()->project([p|$p.name], ['name'])->from(a::m, ^meta::core::runtime::Runtime(connectionStores=^meta::core::runtime::ConnectionStore(element=a::db, connection=^meta::external::store::relational::runtime::TestDatabaseConnection(type=meta::relational::runtime::DatabaseType.H2))))}\n" +
                        "###Mapping\n" +
                        "Mapping a::m(a::Person:Relational{name : [a::db]tb.name})\n" +
                        "###Relational\n" +
                        "Database a::db(Table tb(name VARCHAR(100) PRIMARY KEY))\n" +
                        "###Snowflake\n" +
                        "SnowflakeApp a::myApp{" +
                        "   applicationName: 'name';" +
                        "   description: 'ee';" +
                        "   ownership : Deployment { identifier: 'ownership' };" +
                        "   function: a::f():TabularDataSet[1];" +
                        "}";
        Response response = api.validate(new FunctionActivatorInput("vX_X_X", "a::myApp", PureGrammarParser.newInstance().parseModel(val)), null);
        Assert.assertEquals("[]", response.getEntity().toString());
    }

    @Test
    @Ignore
    public void testImproperPlan()
    {
        String val =
                "Class a::Person {name : String[1]; address : a::Address[1];}\n" +
                        "Class a::Address{zip:String[1];}\n" +
                        "function a::f():a::Person[*]{a::Person.all()->graphFetch(#{a::Person{name,address{zip}}}#)->from(a::m, ^meta::core::runtime::Runtime(connectionStores=^meta::core::runtime::ConnectionStore(element=a::db, connection=^meta::external::store::relational::runtime::TestDatabaseConnection(type=meta::relational::runtime::DatabaseType.H2))))}\n" +
                        "###Mapping\n" +
                        "Mapping a::m(a::Person:Relational{name : [a::db]tb.name, address : [a::db]@j} a::Address:Relational{zip : [a::db]addr.zip})\n" +
                        "###Relational\n" +
                        "Database a::db(Table tb(id INT PRIMARY KEY, name VARCHAR(100)) Table addr(id INT PRIMARY KEY, zip VARCHAR(100)) Join j(tb.id=addr.id))\n" +
                        "###Snowflake\n" +
                        "SnowflakeApp a::myApp{" +
                        "   applicationName: 'name';" +
                        "   description: 'ee';" +
                        "   ownership : Deployment { identifier: 'ownership' };" +
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
        Assert.assertEquals("meta::protocols::pure::vX_X_X::metamodel::function::activator::snowflakeApp::SnowflakeApp", info.get(0).configuration.topElement);
        Assert.assertEquals(8, info.get(0).configuration.model.size());
    }
}
