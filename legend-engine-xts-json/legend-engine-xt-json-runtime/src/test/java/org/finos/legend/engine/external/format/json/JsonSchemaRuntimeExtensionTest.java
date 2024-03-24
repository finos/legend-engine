//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Configuration;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatSerializeResult;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeTDSExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.pure.generated.core_relational_relational_extensions_extension;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionJavaPlatformBinding__Extension_1_;
import static org.mockito.ArgumentMatchers.any;

public class JsonSchemaRuntimeExtensionTest extends TestExternalFormatQueries
{
    private PureModelContextData modelData;

    @Before
    public void setUp()
    {
        String pureCode = resourceAsString("tdsTest/proj-1.pure");
        this.modelData = PureGrammarParser.newInstance().parseModel(pureCode);
        ExecutionSupport executionSupport = Compiler.compile(modelData, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName()).getExecutionSupport();
        formatExtensions = FastList.newListWith(
                Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionJavaPlatformBinding__Extension_1_(executionSupport),
                core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtension__Extension_1_(executionSupport)
        );
    }

    @Test
    public void testExternalize()
    {
        String actualResult = runTest(this.modelData,
                "|demo::employee.all()" +
                        "->project([x|$x.id, x|$x.startDate, x|$x.name],['id', 'startDate', 'name'])" +
                        "->externalize('application/json')" +
                        "->from(demo::DemoRelationalMapping, demo::H2DemoRuntime)");
        String expected = resourceAsString("tdsTest/testExternalizeExpected.json");

        Configuration configuration = JsonAssert.whenIgnoringPaths("activities[0].comment"); // ignore comments such as executionTraceID
        JsonAssert.assertJsonEquals(expected, actualResult, configuration);

    }

    @Test
    public void executeExternalizeTDSExecutionNodeTest() throws Exception
    {
        JsonSchemaRuntimeExtension extension = new JsonSchemaRuntimeExtension();
        ExternalFormatExternalizeTDSExecutionNode node = new ExternalFormatExternalizeTDSExecutionNode();
        //create a real result from H2
        RelationalExecutionNode mockExecutionNode = Mockito.mock(RelationalExecutionNode.class);
        DatabaseConnection mockDatabaseConnection = Mockito.mock(DatabaseConnection.class);

        mockExecutionNode.connection = mockDatabaseConnection;
        TDSResultType resultType = new TDSResultType();
        resultType.tdsColumns = FastList.newListWith(
                new TDSColumn("testInt", "Integer"),
                new TDSColumn("testStringR", "String"),
                new TDSColumn("testString", "String"),
                new TDSColumn("testDate", "DateTime"),
                new TDSColumn("testBool", "Boolean")
        );

        mockExecutionNode.resultType = resultType;
        Mockito.when(mockDatabaseConnection.accept(any())).thenReturn(false);
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/test;TIME ZONE=America/New_York", "sa", ""); ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            //setup table
            conn.createStatement().execute("DROP TABLE IF EXISTS testtable");
            conn.createStatement().execute("DROP TABLE IF EXISTS testtableJoin");

            conn.createStatement().execute("Create Table testtable (testInt INTEGER, testString VARCHAR(255), testDate TIMESTAMP, testBool BOOLEAN)");
            conn.createStatement().execute("Create Table testtableJoin (testIntR INTEGER, testStringR VARCHAR(255)  PRIMARY KEY )");

            conn.createStatement().execute("INSERT INTO  testtable (testInt, testString, testDate, testBool) VALUES(1,'A', '2020-01-01 00:00:00-05:00',true),( 2,null, '2020-01-01 00:00:00-02:00',false ),( 3,'B', '2020-01-01 00:00:00-05:00',false )");
            conn.createStatement().execute("INSERT INTO  testtableJoin (testIntR, testStringR) VALUES(6,'A'), (1,'B')");

            RelationalResult relationalResult = new RelationalResult(FastList.newListWith(
                    new RelationalExecutionActivity("SELECT testInt, testStringR, testString, testDate, testBool FROM testtable left join  testtableJoin on testtable.testInt=testtableJoin.testIntR", null)),
                    mockExecutionNode,
                    FastList.newListWith(
                            new SQLResultColumn("testInt", "INTEGER"),
                            new SQLResultColumn("testStringR", "VARCHAR"),
                            new SQLResultColumn("testString", "VARCHAR"),
                            new SQLResultColumn("testDate", "TIMESTAMP"),
                            new SQLResultColumn("testBool", "BOOLEAN")
                    ), null, "America/New_York", conn,
                    IdentityFactoryProvider.getInstance().getAnonymousIdentity(), null, null, new RequestContext());

            ExternalFormatSerializeResult serializeResult = (ExternalFormatSerializeResult) extension.executeExternalizeTDSExecutionNode(node, relationalResult, IdentityFactoryProvider.getInstance().getAnonymousIdentity(), null);

            serializeResult.stream(outputStream, SerializationFormat.DEFAULT);

            String expected = resourceAsString("tdsTest/executeExternalizeTDSExecutionNodeTestExpected.json");
            JsonAssert.assertJsonEquals(expected, new ObjectMapper().readValue(new ByteArrayInputStream(outputStream.toByteArray()), Object.class));
        }
    }
}