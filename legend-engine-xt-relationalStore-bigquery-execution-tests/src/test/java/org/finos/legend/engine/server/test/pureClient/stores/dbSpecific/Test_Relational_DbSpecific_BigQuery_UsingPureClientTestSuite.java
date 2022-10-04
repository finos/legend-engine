package org.finos.legend.engine.server.test.pureClient.stores.dbSpecific;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import junit.framework.Test;
import org.finos.legend.engine.authentication.BigQueryTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.BigQueryTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.server.test.shared.Relational_DbSpecific_UsingPureClientTestSuite;

public class Test_Relational_DbSpecific_BigQuery_UsingPureClientTestSuite extends Relational_DbSpecific_UsingPureClientTestSuite
{
    public static Test suite() throws Exception
    {
        return Relational_DbSpecific_UsingPureClientTestSuite.createSuite(
                "meta::relational::tests::dbSpecificTests::bigQuery",
                "org/finos/legend/engine/server/test/userTestConfig_withBigQueryTestConnection.json",
                new NamedType(BigQueryTestDatabaseAuthenticationFlowProviderConfiguration.class, "bigQueryTest")
        );
    }
}