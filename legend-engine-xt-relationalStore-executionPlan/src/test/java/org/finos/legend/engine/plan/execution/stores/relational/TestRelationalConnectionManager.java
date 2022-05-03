package org.finos.legend.engine.plan.execution.stores.relational;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.Assert.assertFalse;

public class TestRelationalConnectionManager
{
    @Test
    public void testConnection() throws Exception
    {
        RelationalConnectionManager manager = new RelationalConnectionManager(22, Lists.mutable.empty());
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
        Identity identity = DefaultIdentityFactory.INSTANCE.makeUnknownIdentity();
        try(Connection connection = manager.getDataSourceSpecification(connectionSpec).getConnectionUsingIdentity(identity, Optional.empty()))
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

    @Test
    public void testResolveEmptyCredentialForUnsupportedFlow() throws JsonProcessingException
    {
        String connectionStr =
                "{\n" +
                        "  \"_type\": \"RelationalDatabaseConnection\",\n" +
                        "  \"type\": \"H2\",\n" +
                        "  \"authenticationStrategy\" : {\n" +
                        "    \"_type\" : \"test\"\n" +
                        "  },\n" +
                        "  \"datasourceSpecification\" : {\n" +
                        "    \"_type\" : \"static\",\n" +
                        "    \"host\" : \"127.0.0.1\",\n" +
                        "    \"port\" : \"111\"\n" +
                        "  }\n" +
                        "}";

        RelationalDatabaseConnection connectionSpec = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(connectionStr, RelationalDatabaseConnection.class);
        DatabaseAuthenticationFlowProvider flowProvider = new NoOpFlowProvider();

        Identity identity = DefaultIdentityFactory.INSTANCE.makeUnknownIdentity();
        Optional<CredentialSupplier> credential = RelationalConnectionManager.getCredential(flowProvider, connectionSpec, identity);
        assertFalse(credential.isPresent());
    }

    static class NoOpFlowProvider implements DatabaseAuthenticationFlowProvider
    {
        @Override
        public Optional<DatabaseAuthenticationFlow> lookupFlow(RelationalDatabaseConnection connection)
        {
            return Optional.empty();
        }

        @Override
        public void configure(DatabaseAuthenticationFlowProviderConfiguration configuration) {

        }

        @Override
        public int count()
        {
            return 0;
        }
    }
}