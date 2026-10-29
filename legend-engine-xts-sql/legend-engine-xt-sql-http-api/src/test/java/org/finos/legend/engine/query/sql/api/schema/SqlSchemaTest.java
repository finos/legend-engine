// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.query.sql.api.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.query.sql.api.CatchAllExceptionMapper;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

@ExtendWith(DropwizardExtensionsSupport.class)
public class SqlSchemaTest
{
    static
    {
        System.setProperty(TestProperties.CONTAINER_PORT, "0");
    }

    private static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public static final ResourceExtension resources = ResourceExtension.builder()
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addResource(new SqlSchema(new ModelManager(DeploymentMode.TEST)))
            .addResource(new MockPac4jFeature())
            .addResource(new CatchAllExceptionMapper())
            .bootstrapLogging(false)
            .build();

    @Test
    public void testGetSchemaWithBinaryColumns() throws Exception
    {
        SchemaResult result = getSchema("###Relational\n" +
                "Database test::BinDB\n" +
                "(\n" +
                "  Table binTable(id INTEGER PRIMARY KEY, bin BINARY(16), vbin VARBINARY(256))\n" +
                ")\n");

        AddressableRelation relation = ListIterate.detect(result.addressableRelations, r -> r.packageableElement.equals("test::BinDB"));
        Assertions.assertEquals("tb", relation.tableFunctionName);
        Assertions.assertEquals("[binTable]", relation.pathWithinElement.toString());
        Assertions.assertEquals("[id:meta::pure::precisePrimitives::Int, bin:Byte, vbin:Byte]",
                ListIterate.collect(relation.relationType.columns, c -> c.name + ":" + ((PackageableType) c.genericType.rawType).fullPath).toString());
    }

    @Test
    public void testGetSchemaWithBinaryColumnInAnotherDatabase() throws Exception
    {
        SchemaResult result = getSchema("###Relational\n" +
                "Database test::CleanDB\n" +
                "(\n" +
                "  Table cleanTable(id INTEGER, name VARCHAR(200))\n" +
                ")\n" +
                "###Relational\n" +
                "Database test::DependencyDB\n" +
                "(\n" +
                "  Table depTable(id INTEGER, payload BINARY(16))\n" +
                ")\n");

        Assertions.assertEquals("[test::CleanDB, test::DependencyDB]",
                ListIterate.collect(result.addressableRelations, r -> r.packageableElement).toSortedList().toString());
    }

    private SchemaResult getSchema(String code) throws Exception
    {
        PureModelContextText context = new PureModelContextText();
        context.code = code;
        Response response = resources.target("sql/v1/schema/getSchema").request().post(Entity.json(MAPPER.writeValueAsString(context)));
        String body = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus(), body);
        return MAPPER.readValue(body, SchemaResult.class);
    }
}
