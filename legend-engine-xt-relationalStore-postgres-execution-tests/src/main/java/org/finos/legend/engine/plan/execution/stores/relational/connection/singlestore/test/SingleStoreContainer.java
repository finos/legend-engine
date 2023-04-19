// TODO - Update Copyright header
package org.finos.legend.engine.plan.execution.stores.relational.connection.singlestore.test;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.shaded.com.google.common.collect.Sets;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

public class SingleStoreContainer<SELF extends SingleStoreContainer<SELF>> extends JdbcDatabaseContainer<SELF>
{
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("memsql/cluster-in-a-box");
    public static final String IMAGE;
    public static final Integer SINGLESTORE_PORT;
    static final String DEFAULT_USER = "root";
    private String licenseKey;
    private String password;
    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 240;

    public static SingleStoreContainer newSingleStoreContainer(DockerImageName dockerImageName, String passwordEnvVariable, String licenseKeyEnvVariable)
    {
        if (System.getenv().containsKey(passwordEnvVariable))
        {
            throw new RuntimeException("System env does not contain a value for variable " + passwordEnvVariable);
        }
        if (System.getenv().containsKey(licenseKeyEnvVariable))
        {
            throw new RuntimeException("System env does not contain a value for variable " + licenseKeyEnvVariable);
        }
        return new SingleStoreContainer(dockerImageName, System.getenv().get(passwordEnvVariable), System.getenv().get(licenseKeyEnvVariable));
    }

    public SingleStoreContainer(DockerImageName dockerImageName, String password, String licenseKey) {
        super(dockerImageName);
        this.password = password;
        this.licenseKey = licenseKey;
        dockerImageName.assertCompatibleWith(new DockerImageName[]{DEFAULT_IMAGE_NAME});
        this.withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        this.withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        this.withUrlParam("permitMysqlScheme", null);
        this.addExposedPort(SINGLESTORE_PORT);
    }

    public Set<Integer> getLivenessCheckPortNumbers() {
        return Sets.newHashSet(new Integer[]{SINGLESTORE_PORT});
    }

    protected void configure() {
        this.addEnv("START_AFTER_INIT", "Y");
        this.addEnv("ROOT_PASSWORD", this.password);
        this.addEnv("LICENSE_KEY", this.licenseKey);
    }

    public String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    public String getJdbcUrl()
    {
        String additionalUrlParams = this.constructUrlParameters(";", ";");
        return "jdbc:mysql://" + this.getHost() + ":" + this.getMappedPort(SINGLESTORE_PORT) + "?permitMysqlScheme";
    }

    public String getUsername() {
        return DEFAULT_USER;
    }

    public String getPassword() {
        return this.password;
    }

    public String getTestQueryString() {
        return "SELECT 1";
    }

    public SELF withPassword(String password) {
        this.password = password;
        return this.self();
    }

    static {
        IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();
        SINGLESTORE_PORT = 3306;
    }
}