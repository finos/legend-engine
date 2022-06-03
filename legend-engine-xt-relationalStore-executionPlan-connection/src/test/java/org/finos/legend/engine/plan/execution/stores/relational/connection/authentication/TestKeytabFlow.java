package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication;

import org.apache.hadoop.minikdc.MiniKdc;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.middletier.MemSqlStaticWithMiddletierKeytabAuthenticationFlow;
import org.finos.legend.engine.authentication.middletier.MiddletierKeytabMetadata;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierKeytabAuthenticationStrategy;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.junit.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestKeytabFlow {
    private static Path miniKdcServerWorkingDir;
    private static MiniKdc miniKdcServer;
    private static Path keytabsWorkingDir;
    private CustomVaultForTesting vault;

    @BeforeClass
    public static void setupClass() throws Exception {
        miniKdcServerWorkingDir = Files.createTempDirectory("minikdc-workdir");
        keytabsWorkingDir = miniKdcServerWorkingDir.resolve("keytabs");
        keytabsWorkingDir.toFile().mkdirs();
        miniKdcServer = startKdcServer(miniKdcServerWorkingDir);
        miniKdcServer.createPrincipal(keytabsWorkingDir.resolve("fred.kt").toFile(), "fred@EXAMPLE.COM");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (miniKdcServer != null) {
            miniKdcServer.stop();
        }
    }

    @Before
    public void setup() {
        vault = new CustomVaultForTesting(keytabsWorkingDir);
        Vault.INSTANCE.registerImplementation(vault);
    }

    @After
    public void cleanup() {
        Vault.INSTANCE.unregisterImplementation(vault);
    }

    @Test
    public void testKeytabUsageMetadataNotInVault() throws Exception {
        MiddleTierKeytabAuthenticationStrategy middleTierKeytabAuthenticationStrategy = new MiddleTierKeytabAuthenticationStrategy("fred@EXAMPLE.COM", "file::reference1", "reference2");
        try {
            DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "context1").castToMap());
            new MemSqlStaticWithMiddletierKeytabAuthenticationFlow().makeCredential(null, null, middleTierKeytabAuthenticationStrategy, runtimeContext);
            fail("failed to throw");
        } catch (Exception e) {
            assertEquals("Failed to locate keytab metadata using vault reference 'reference2'", e.getMessage());
        }
    }

    @Test
    public void testKeytabMetadataDoesNotMatchKeytab() throws Exception {
        this.vault.add("file::reference1", "fred.kt");
        this.vault.add("reference2", new MiddletierKeytabMetadata("file::some other reference", new String[]{"service1"}).toJSON());

        MiddleTierKeytabAuthenticationStrategy middleTierKeytabAuthenticationStrategy = new MiddleTierKeytabAuthenticationStrategy("fred@EXAMPLE.COM", "file::reference1", "reference2");
        try {
            DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "context1").castToMap());
            new MemSqlStaticWithMiddletierKeytabAuthenticationFlow().makeCredential(null, null, middleTierKeytabAuthenticationStrategy, runtimeContext);
            fail("failed to throw");
        } catch (Exception e) {
            assertEquals("Use of keytab with reference 'file::reference1' not authorized. Mismatch between keytab vault reference and it's associated vault metadata", e.getMessage());
        }
    }

    @Test
    public void testKeytabMetadataContextDoesNotMatchRuntimeContext() throws Exception {
        this.vault.add("file::reference1", "fred.kt");
        this.vault.add("reference2", new MiddletierKeytabMetadata("file::reference1", new String[]{"service1"}).toJSON());

        MiddleTierKeytabAuthenticationStrategy middleTierKeytabAuthenticationStrategy = new MiddleTierKeytabAuthenticationStrategy("fred@EXAMPLE.COM", "file::reference1", "reference2");
        try {
            DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "some other context").castToMap());
            new MemSqlStaticWithMiddletierKeytabAuthenticationFlow().makeCredential(null, null, middleTierKeytabAuthenticationStrategy, runtimeContext);
            fail("failed to throw");
        } catch (Exception e) {
            assertEquals("Use of keytab with reference 'file::reference1' not authorized. Mismatch between runtime context and keytab metadata context", e.getMessage());
        }
    }

    @Test
    public void testUseKeytabWithoutRuntimeContext() throws Exception {
        this.vault.add("file::reference1", "fred.kt");
        this.vault.add("reference2", new MiddletierKeytabMetadata("file::reference1", new String[]{"service1"}).toJSON());

        MiddleTierKeytabAuthenticationStrategy middleTierKeytabAuthenticationStrategy = new MiddleTierKeytabAuthenticationStrategy("fred@EXAMPLE.COM", "file::reference1", "reference2");
        try {
            new MemSqlStaticWithMiddletierKeytabAuthenticationFlow().makeCredential(null, null, middleTierKeytabAuthenticationStrategy);
            fail("failed to throw");
        } catch (UnsupportedOperationException e) {
            assertEquals("Unsafe attempt to make a credential without a runtime context", e.getMessage());
        }

        try {
            new MemSqlStaticWithMiddletierKeytabAuthenticationFlow().makeCredential(null, null, middleTierKeytabAuthenticationStrategy, DatabaseAuthenticationFlow.RuntimeContext.empty());
            fail("failed to throw");
        } catch (UnsupportedOperationException e) {
            assertEquals("Unsafe attempt to make a credential with a (Java) null runtime context", e.getMessage());
        }
    }

    @Test
    public void testCreateAndUseKeytabSuccessfully() throws Exception {
        this.vault.add("file::reference1", "fred.kt");
        this.vault.add("reference2", new MiddletierKeytabMetadata("file::reference1", new String[]{"service1"}).toJSON());

        MiddleTierKeytabAuthenticationStrategy middleTierKeytabAuthenticationStrategy = new MiddleTierKeytabAuthenticationStrategy("fred@EXAMPLE.COM", "file::reference1", "reference2");
        DatabaseAuthenticationFlow.RuntimeContext runtimeContext = DatabaseAuthenticationFlow.RuntimeContext.newWith(Maps.immutable.of("context", "service1").castToMap());
        LegendKerberosCredential credential = (LegendKerberosCredential) new MemSqlStaticWithMiddletierKeytabAuthenticationFlow().makeCredential(null, null, middleTierKeytabAuthenticationStrategy, runtimeContext);

        assertEquals("fred@EXAMPLE.COM", SubjectTools.getPrincipalFromSubject(credential.getSubject()).getName());
        assertEquals("fred", SubjectTools.getKerberos(credential.getSubject()));
    }

    // This code has been adapted from https://github.com/c9n/hadoop/blob/master/hadoop-common-project/hadoop-minikdc/src/main/java/org/apache/hadoop/minikdc/MiniKdc.java
    private static MiniKdc startKdcServer(Path miniKdcWorkDir) throws Exception {
        MutableMap<String, String> propertiesMap = Lists.immutable.of(
                "org.name=EXAMPLE",
                "org.domain=COM",
                "kdc.bind.address=localhost",
                "kdc.port=0",
                "instance=DefaultKrbServer",
                "max.ticket.lifetime=86400000",
                "max.renewable.lifetime=604800000",
                "transport=TCP",
                "debug=false"
        ).toMap(p -> p.split("=")[0], p -> p.split("=")[1]);
        Properties properties = new Properties();
        properties.putAll(propertiesMap);

        MiniKdc miniKdc = new MiniKdc(properties, miniKdcWorkDir.toFile());
        miniKdc.start();
        return miniKdc;
    }

    static class CustomVaultForTesting implements VaultImplementation {
        Properties properties = new Properties();
        private Path keytabsDir;

        public CustomVaultForTesting(Path keytabsDir) {
            this.keytabsDir = keytabsDir;
        }

        public void add(String key, String value) {
            this.properties.put(key, value);
        }

        @Override
        public String getValue(String key) {
            if (!key.startsWith("file::")) {
                return this.properties.getProperty(key);
            }
            String fileName = this.properties.getProperty(key);
            Path keytabFile = this.keytabsDir.resolve(fileName);
            return keytabFile.toAbsolutePath().toString();
        }

        @Override
        public boolean hasValue(String key) {
            return this.properties.getProperty(key) != null;
        }
    }
}