//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.authorization.PlanExecutionAuthorizerInput;
import org.finos.legend.engine.plan.execution.authorization.RelationalMiddleTierConnectionCredentialAuthorizer;
import org.finos.legend.engine.plan.execution.authorization.mac.PlanExecutionAuthorizerMACKeyGenerator;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test.PostgresTestContainerWrapper;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.middletier.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.identity.factory.DefaultIdentityFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.pac4j.core.profile.CommonProfile;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.Assume.assumeTrue;

public class AbstractMiddleTierExecutionTest
{
    protected static PostgresTestContainerWrapper postgresTestContainerWrapper;

    protected MutableList<CommonProfile> profiles;
    public static final String MAC_KEY_VAULT_REFERENCE = "macReference";
    public static final String DB_CREDENTIAL_REFERENCE = "reference1";
    private TestVaultImplementation testVaultImplementation;

    @BeforeClass
    public static void setupClass() throws Exception
    {
        try
        {
            postgresTestContainerWrapper = PostgresTestContainerWrapper.build();
            postgresTestContainerWrapper.postgreSQLContainer.start();
        }
        catch (Exception e)
        {
            assumeTrue("Cannot start PostgreSQLContainer", false);
        }
        setupPostgresDatabase();
    }

    public static void setupPostgresDatabase() throws Exception
    {
        Class.forName("org.postgresql.Driver");
        Connection conn = DriverManager.getConnection(
                postgresTestContainerWrapper.getJdbcUrl(),
                postgresTestContainerWrapper.getUser(),
                postgresTestContainerWrapper.getPassword()
        );

        Statement stmt = conn.createStatement();
        MutableList<String> setupSqls = Lists.mutable.with("drop table if exists PERSON;",
                "create table PERSON(fullName VARCHAR(200));",
                "insert into PERSON (fullName) values ('Mickey Mouse');");
        for (String sql : setupSqls)
        {
            stmt.executeUpdate(sql);
        }
        stmt.close();
        conn.close();
    }

    @AfterClass
    public static void shutdownClass()
    {
        postgresTestContainerWrapper.postgreSQLContainer.stop();
    }

    @Before
    public void setup() throws Exception
    {
        DefaultIdentityFactory defaultIdentityFactory = new DefaultIdentityFactory();
        this.profiles = Lists.mutable.withAll(defaultIdentityFactory.adapt(defaultIdentityFactory.makeUnknownIdentity()));

        this.testVaultImplementation = new TestVaultImplementation();
        testVaultImplementation.setValue(MAC_KEY_VAULT_REFERENCE, new PlanExecutionAuthorizerMACKeyGenerator().generateKeyAsString());
        MiddleTierUserPasswordCredential middleTierUserPasswordCredential = new MiddleTierUserPasswordCredential(postgresTestContainerWrapper.getUser(), postgresTestContainerWrapper.getPassword(), "unused");
        testVaultImplementation.setValue(DB_CREDENTIAL_REFERENCE, new ObjectMapper().writeValueAsString(middleTierUserPasswordCredential));
        Vault.INSTANCE.registerImplementation(testVaultImplementation);
    }

    @After
    public void teardown()
    {
        if (this.testVaultImplementation != null)
        {
            Vault.INSTANCE.unregisterImplementation(testVaultImplementation);
        }
    }

    protected PlanExecutor buildPlanExecutor()
    {
        TemporaryTestDbConfiguration temporaryTestDbConfiguration = new TemporaryTestDbConfiguration(9098);
        LegendDefaultDatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration = new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration();
        flowProviderConfiguration.setMiddleTierMACKeyVaultRefernce(MAC_KEY_VAULT_REFERENCE);

        RelationalExecutionConfiguration relationalExecutionConfiguration = RelationalExecutionConfiguration.newInstance()
                .withTemporaryTestDbConfiguration(temporaryTestDbConfiguration)
                .withDatabaseAuthenticationFlowProvider(LegendDefaultDatabaseAuthenticationFlowProvider.class, flowProviderConfiguration)
                .build();

        StoreExecutor relationalStoreExecutor = Relational.build(relationalExecutionConfiguration);

        PlanExecutor executor = PlanExecutor.newPlanExecutor(InMemory.build(), relationalStoreExecutor);
        return executor;
    }

    public static class AlwaysAllowCredentialAuthorizer implements RelationalMiddleTierConnectionCredentialAuthorizer
    {
        @Override
        public CredentialAuthorization evaluate(Identity currentUser, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception
        {
            return CredentialAuthorization.allow(currentUser.getName(), credentialVaultReference, Lists.immutable.empty());
        }
    }

    public static class AlwaysDenyCredentialAuthorizer implements RelationalMiddleTierConnectionCredentialAuthorizer
    {
        @Override
        public CredentialAuthorization evaluate(Identity currentUser, String credentialVaultReference, PlanExecutionAuthorizerInput.ExecutionMode usageContext, String resourceContext, String policyContext) throws Exception
        {
            return CredentialAuthorization.deny(currentUser.getName(), credentialVaultReference, Lists.immutable.empty());
        }
    }

    protected String readResponse(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamingOutput output = (StreamingOutput) response.getEntity();
        output.write(baos);
        String responseText = baos.toString("UTF-8");
        return responseText;
    }

    protected String readResponseError(Response response) throws IOException
    {
        ExceptionError error = (ExceptionError) response.getEntity();
        return error.getMessage();
    }

    public static class ReflectiveInvocationHandler implements InvocationHandler
    {
        private final Object[] delegates;

        public ReflectiveInvocationHandler(Object... delegates)
        {
            this.delegates = delegates;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            for (Object delegate : delegates)
            {
                try
                {
                    return delegate.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(delegate, args);
                }
                catch (NoSuchMethodException e)
                {
                    // The loop will complete if all delegates fail
                }
            }
            throw new UnsupportedOperationException("Method not simulated: " + method);
        }
    }

    public static class Request
    {
        @SuppressWarnings("unused")
        public String getRemoteUser()
        {
            return "someone";
        }
    }
}