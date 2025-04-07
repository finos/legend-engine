package org.finos.legend.engine.authentication.cloud;

public class GCPWorkforceConfig
{
    private final String poolId;
    private final String providerId;

    public GCPWorkforceConfig(String poolId, String providerId)
    {
        this.poolId = poolId;
        this.providerId = providerId;
    }

    public String getPoolId()
    {
        return poolId;
    }

    public String getProviderId()
    {
        return providerId;
    }
}
