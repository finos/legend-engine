package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.middletier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.middletier.MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.shared.core.identity.credential.MiddleTierUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestMiddleTierUserNamePasswordFlow {
    private CustomVaultForTesting vault;

    @Before
    public void setup() {
        this.vault = new CustomVaultForTesting();
        Vault.INSTANCE.registerImplementation(vault);
    }

    @After
    public void cleanup() {
        Vault.INSTANCE.unregisterImplementation(vault);
    }

    @Test
    public void testCredentialNotInVault() throws Exception
    {
        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        try {
            DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "context1").castToMap());
            new MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow().makeCredential(null, null, authenticationStrategy, runtimeContext);
            fail("failed to throw");
        } catch (Exception e) {
            assertEquals("Failed to locate credential using vault reference 'reference1'", e.getMessage());
        }
    }

    @Test
    public void testCredentialContextDoesNotMatchRuntimeContext() throws Exception {
        this.vault.add("reference1", toJSON(new MiddleTierUserPasswordCredential("user", "password", new String[]{"service1"})));

        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        try {
            DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "context1").castToMap());
            new MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow().makeCredential(null, null, authenticationStrategy, runtimeContext);
            fail("failed to throw");
        } catch (Exception e) {
            assertEquals("Use of credential with reference 'reference1' not authorized. Mismatch between runtime context and credential contexts", e.getMessage());
        }
    }

    private String toJSON(MiddleTierUserPasswordCredential credential) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(credential);
    }

    @Test
    public void testUseCredentialWithoutRuntimeContext() throws Exception {
        this.vault.add("reference1", toJSON(new MiddleTierUserPasswordCredential("user", "password", new String[]{"service1"})));

        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        try {
            new MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow().makeCredential(null, null, authenticationStrategy);
            fail("failed to throw");
        } catch (Exception e) {
            assertEquals("Unsafe attempt to make a credential without a runtime context", e.getMessage());
        }
    }

    @Test
    public void testUseCredentialSuccessfully() throws Exception {
        this.vault.add("reference1", toJSON(new MiddleTierUserPasswordCredential("user", "password", new String[]{"service1"})));

        MiddleTierUserNamePasswordAuthenticationStrategy authenticationStrategy = new MiddleTierUserNamePasswordAuthenticationStrategy("reference1");
        DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "service1").castToMap());
        new MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow().makeCredential(null, null, authenticationStrategy, runtimeContext);

        MiddleTierUserPasswordCredential credential = (MiddleTierUserPasswordCredential) new MemSqlStaticWithMiddletierUserNamePasswordAuthenticationFlow().makeCredential(null, null, authenticationStrategy, runtimeContext);

        assertEquals("user", credential.getUser());
        assertEquals("password", credential.getPassword());
        assertArrayEquals(new String[]{"service1"}, credential.getUsageContexts());
    }

    static class CustomVaultForTesting implements VaultImplementation {
        Properties properties = new Properties();


        public void add(String key, String value) {
            this.properties.put(key, value);
        }

        @Override
        public String getValue(String key) {
            return this.properties.getProperty(key);
        }

        @Override
        public boolean hasValue(String key) {
            return this.properties.getProperty(key) != null;
        }
    }
}
