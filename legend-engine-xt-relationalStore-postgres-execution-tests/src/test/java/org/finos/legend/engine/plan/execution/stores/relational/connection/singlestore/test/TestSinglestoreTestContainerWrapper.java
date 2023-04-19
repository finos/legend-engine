package org.finos.legend.engine.plan.execution.stores.relational.connection.singlestore.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class TestSinglestoreTestContainerWrapper
{
    private static final String PASSWORD_ENV_NAME = "SS_PASSWORD";
    private static final String LICENSE_KEY_ENV_NAME = "SS_LICENSE_KEY";

    private SingleStoreContainer singleStoreContainer = null;
    private String password;
    private String licenseKey;

    private void startSingleStoreContainer()
    {
        try
        {
            this.password = System.getenv(PASSWORD_ENV_NAME);
            this.licenseKey = System.getenv(LICENSE_KEY_ENV_NAME);

            this.password = "xxxxxx";
            this.licenseKey = "yyyyyy";

            this.singleStoreContainer = new SingleStoreContainer(SingleStoreContainer.DEFAULT_IMAGE_NAME, password, licenseKey);
            this.singleStoreContainer.start();
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
            assumeTrue("Cannot start SingleStoreContainer", false);
        }
    }

    @Before
    public void setup() throws Exception
    {
        Class.forName("org.mariadb.jdbc.Driver");
        startSingleStoreContainer();
    }

    @After
    public void cleanup()
    {
        if (this.singleStoreContainer != null)
        {
            this.singleStoreContainer.stop();
        }
    }

    @Test
    public void testDirectJDBCConnection() throws Exception
    {
        String jdbcUrl = this.singleStoreContainer.getJdbcUrl();
        Class.forName(this.singleStoreContainer.getDriverClassName());

        Connection connection = DriverManager.getConnection(jdbcUrl, singleStoreContainer.getUsername(), this.password);
        ResultSet resultSet = connection.createStatement().executeQuery("select current_user();");
        resultSet.next();
        String currentUser = resultSet.getString(1);
        assertEquals("root@%", currentUser);
    }
}