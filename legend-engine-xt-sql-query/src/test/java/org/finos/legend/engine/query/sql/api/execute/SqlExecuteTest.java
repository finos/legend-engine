// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.query.sql.api.execute;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.api.sources.TestSQLSourceProvider;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_Enum;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_EnumValueSchemaColumn_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_Enum_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_PrimitiveValueSchemaColumn_Impl;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_Schema;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_SchemaColumn;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_Schema_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enum_Impl;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.util.ServiceLoader;

public class SqlExecuteTest
{
    @ClassRule
    public static final ResourceTestRule resources;
    private static final PureModel pureModel;


    static
    {
        DeploymentMode deploymentMode = DeploymentMode.TEST;
        ModelManager modelManager = new ModelManager(deploymentMode);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithConfigurations();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        TestSQLSourceProvider testSQLSourceProvider = new TestSQLSourceProvider();
        SqlExecute sqlExecute = new SqlExecute(modelManager, executor, (pm) -> generatorExtensions.flatCollect(g -> g.getExtraExtensions(pm)), FastList.newListWith(testSQLSourceProvider), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));

        pureModel = modelManager.loadModel(testSQLSourceProvider.getPureModelContextData(), PureClientVersions.production, null, "");
        resources = ResourceTestRule.builder()
                .addResource(sqlExecute)
                .addResource(new MockPac4jFeature())
                .build();
    }


    @Test
    public void getSchemaFromQueryString()
    {
        String actualSchema = resources.target("sql/v1/execution/getSchemaFromQueryString")
                .request()
                .post(Entity.text("SELECT * FROM service.\"/testService\"")).readEntity(String.class);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum integerType = new Root_meta_pure_metamodel_type_Enum_Impl("Integer");
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum stringType = new Root_meta_pure_metamodel_type_Enum_Impl("String");
        Root_meta_external_query_sql_SchemaColumn idColumn = new Root_meta_external_query_sql_PrimitiveValueSchemaColumn_Impl((String) null)._name("Id")._type(integerType);
        Root_meta_external_query_sql_SchemaColumn nameColumn = new Root_meta_external_query_sql_PrimitiveValueSchemaColumn_Impl((String) null)._name("Name")._type(stringType);
        Root_meta_external_query_sql_SchemaColumn typeColumn = new Root_meta_external_query_sql_EnumValueSchemaColumn_Impl((String) null)._name("Employee Type")._type("demo::employeeType");
        Root_meta_external_query_sql_Enum employeeType = new Root_meta_external_query_sql_Enum_Impl((String) null)._type("demo::employeeType")._valuesAdd("Type1")._valuesAdd("Type2");
        Root_meta_external_query_sql_Schema schema = new Root_meta_external_query_sql_Schema_Impl((String) null)._columnsAdd(idColumn)._columnsAdd(nameColumn)._columnsAdd(typeColumn)._enumsAdd(employeeType);
        String expectedSchema = SqlExecute.serializeToJSON(schema, pureModel);
        Assert.assertEquals(expectedSchema, actualSchema);
    }

    @Test
    public void getSchemaFromQuery()
    {
        String actualSchema = resources.target("sql/v1/execution/getSchemaFromQuery")
                .request()
                .post(Entity.json("{ \"_type\": \"query\", \"orderBy\": [], \"queryBody\": { \"_type\": \"querySpecification\", \"from\": [ { \"_type\": \"table\", \"name\": { \"parts\": [ \"service\", \"/testService\" ] } } ], \"groupBy\": [], \"orderBy\": [], \"select\": { \"_type\": \"select\", \"distinct\": false, \"selectItems\": [ { \"_type\": \"allColumns\" } ] } } }")).readEntity(String.class);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum integerType = new Root_meta_pure_metamodel_type_Enum_Impl("Integer");
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum stringType = new Root_meta_pure_metamodel_type_Enum_Impl("String");
        Root_meta_external_query_sql_SchemaColumn idColumn = new Root_meta_external_query_sql_PrimitiveValueSchemaColumn_Impl((String) null)._name("Id")._type(integerType);
        Root_meta_external_query_sql_SchemaColumn nameColumn = new Root_meta_external_query_sql_PrimitiveValueSchemaColumn_Impl((String) null)._name("Name")._type(stringType);
        Root_meta_external_query_sql_SchemaColumn typeColumn = new Root_meta_external_query_sql_EnumValueSchemaColumn_Impl((String) null)._name("Employee Type")._type("demo::employeeType");
        Root_meta_external_query_sql_Enum employeeType = new Root_meta_external_query_sql_Enum_Impl((String) null)._type("demo::employeeType")._valuesAdd("Type1")._valuesAdd("Type2");
        Root_meta_external_query_sql_Schema schema = new Root_meta_external_query_sql_Schema_Impl((String) null)._columnsAdd(idColumn)._columnsAdd(nameColumn)._columnsAdd(typeColumn)._enumsAdd(employeeType);
        String expectedSchema = SqlExecute.serializeToJSON(schema, pureModel);
        Assert.assertEquals(expectedSchema, actualSchema);
    }

}