package org.finos.legend.engine.plan.execution.stores.relational.connection.singlestore.test;

public class SinglestoreTestContainerWrapper
{
    public SingleStoreContainer singleStoreContainer = SingleStoreContainer.newSingleStoreContainer(SingleStoreContainer.DEFAULT_IMAGE_NAME, "myPassword", "myLicenseKey");

    public static SinglestoreTestContainerWrapper build()
    {
        SinglestoreTestContainerWrapper singlestoreTestContainerWrapper = new SinglestoreTestContainerWrapper();
        return singlestoreTestContainerWrapper;
    }

    public void start()
    {
        this.singleStoreContainer.start();
    }

    public void stop()
    {
        this.singleStoreContainer.stop();
    }

    public int getPort()
    {
        return this.singleStoreContainer.getMappedPort(SingleStoreContainer.SINGLESTORE_PORT);
    }

    public String getUser()
    {
        return this.singleStoreContainer.getUsername();
    }

    public String getPassword()
    {
        return this.singleStoreContainer.getPassword();
    }

    public String getJdbcUrl()
    {
        return this.singleStoreContainer.getJdbcUrl();
    }

}
