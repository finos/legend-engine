package org.finos.legend.engine.authentication.cloud;

public class GCPWorkloadConfig
{
    public String getProjectNumber()
    {
        return projectNumber;
    }

    public String getPoolId()
    {
        return poolId;
    }

    public String getProviderId()
    {
        return providerId;
    }

    public GCPWorkloadConfig(String projectNumber, String poolId, String providerId)
    {
        this.projectNumber = projectNumber;
        this.poolId = poolId;
        this.providerId = providerId;
    }

    private String projectNumber;
    private String poolId;
    private String providerId;

    public GCPWorkloadConfig()
    {
        // jackson
    }
}
