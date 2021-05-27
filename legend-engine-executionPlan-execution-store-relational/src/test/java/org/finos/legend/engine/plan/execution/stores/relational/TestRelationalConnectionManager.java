package org.finos.legend.engine.plan.execution.stores.relational;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestRelationalConnectionManager
{
    @Test
    public void testConnection() throws Exception
    {
        RelationalConnectionManager manager = new RelationalConnectionManager(22, Lists.mutable.empty(), ConcurrentHashMap.newMap(), new RelationalExecutorInfo());
        String connectionStr =
                "{\n" +
                "  \"_type\": \"RelationalDatabaseConnection\",\n" +
                "  \"type\": \"H2\",\n" +
                "  \"authenticationStrategy\" : {\n" +
                "    \"_type\" : \"test\"\n" +
                "  },\n" +
                "  \"datasourceSpecification\" : {\n" +
                "    \"_type\" : \"h2Local\",\n" +
                "    \"testDataSetupSqls\" : [\n" +
                "       \"Drop schema if exists schemaA cascade;\"," +
                "       \"create schema schemaA;\"," +
                "       \"Drop table if exists schemaA.firmSet;\"," +
                "       \"Create Table schemaA.firmSet(id INT, name VARCHAR(200));\"," +
                "       \"Insert into schemaA.firmSet (id, name) values (1, 'FirmA');\"" +
                "     ]" +
                "  }\n" +
                "}";

        RelationalDatabaseConnection connectionSpec = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(connectionStr, RelationalDatabaseConnection.class);
        try(Connection connection = manager.getDataSourceSpecification(connectionSpec).getConnectionUsingSubject(null))
        {
            try (Statement statement = connection.createStatement())
            {
                ResultSet rs = statement.executeQuery("select * from schemaA.firmSet");
                while (rs.next())
                {
                    Assert.assertEquals("FirmA", rs.getString("name"));
                }
            }
        }
    }
}