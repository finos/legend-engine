package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication;

import org.apache.hadoop.minikdc.MiniKdc;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.kerberos.SystemAccountLoginConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLOutput;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

public class TestKerberosAuth
{
    private static Path miniKdcServerWorkingDir;
    private static MiniKdc miniKdcServer;
    private static Path keytabsWorkingDir;
    private static File aliceKeytabFile;
    private static File bobKeytabFile;

    @BeforeClass
    public static void setupClass() throws Exception
    {
        miniKdcServerWorkingDir = Files.createTempDirectory("minikdc-workdir");
        keytabsWorkingDir = miniKdcServerWorkingDir.resolve("keytabs");
        keytabsWorkingDir.toFile().mkdirs();
        miniKdcServer = startKdcServer(miniKdcServerWorkingDir);

        aliceKeytabFile = keytabsWorkingDir.resolve("alice.kt").toFile();
        miniKdcServer.createPrincipal(aliceKeytabFile, "alice@EXAMPLE.COM");

        bobKeytabFile = keytabsWorkingDir.resolve("bob.kt").toFile();
        miniKdcServer.createPrincipal(bobKeytabFile, "bob@EXAMPLE.COM");
    }

    @AfterClass
    public static void afterClass() throws Exception
    {
        if (miniKdcServer != null) {
            miniKdcServer.stop();
        }
    }

    private static Object LOCK = new Object();
    private static int TURN = 0;
    private static int COUNTER = 0;

    @Test
    public void testAuthPingPong() throws InterruptedException
    {
        Thread thread0 = new Thread(new AuthWorker(0, "alice@EXAMPLE.COM", aliceKeytabFile));
        Thread thread1 = new Thread(new AuthWorker(1, "bob@EXAMPLE.COM", bobKeytabFile));

        thread0.start();
        thread1.start();

        System.out.println("Started main thread.");
        while (true)
        {
            synchronized (LOCK)
            {
                if (COUNTER > 20)
                {
                    TURN = -1;
                    LOCK.notifyAll();
                    break;
                }
                else
                {
                    LOCK.wait();
                }
            }
        }

        thread0.join();
        thread1.join();
    }

    public static class AuthWorker implements Runnable
    {
        private int id;
        private final String principal;
        private final File keytabFile;

        public AuthWorker(int id, String principal, File keytabFile)
        {
            this.id = id;
            this.principal = principal;
            this.keytabFile = keytabFile;
        }

        public void run()
        {
            while (true)
            {
                synchronized (LOCK)
                {
                    if (TURN == -1)
                    {
                        break;
                    }
                    if (TURN != id )
                    {
                        try
                        {
                            LOCK.wait();
                        }
                        catch (InterruptedException e) {}
                    }
                    else
                    {
                        authenticate();
                        TURN = TURN == 0 ? 1 : 0;
                        LOCK.notifyAll();
                    }
                }
            }
        }

        private void authenticate()
        {
            try {
                Configuration configuration = new SystemAccountLoginConfiguration(keytabFile.getAbsolutePath(), principal, true);
                LoginContext loginContext = new LoginContext("context " + id, null, null, configuration);
                loginContext.login();
                Subject subject = loginContext.getSubject();
                System.out.println(String.format("%d : Thread %s authenticated as Subject %s with keytab %s ", COUNTER++, this.id, SubjectTools.getPrincipalFromSubject(subject), this.keytabFile));
            }
            catch (Exception e)
            {
                System.out.println(String.format("%d : Thread %s failed with %s", COUNTER++, this.id, e.getMessage()));
            }
        }
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
}
