package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys;

import java.util.Objects;

public class GCPWorkforceIdentityFederationAuthenticationStrategyKey implements AuthenticationStrategyKey {

    public static final String TYPE = "GCPWorkforceIdentityFederation";

    public GCPWorkforceIdentityFederationAuthenticationStrategyKey()
    {
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        GCPWorkforceIdentityFederationAuthenticationStrategyKey that = (GCPWorkforceIdentityFederationAuthenticationStrategyKey) o;
        return Objects.equals(GCPWorkforceIdentityFederationAuthenticationStrategyKey.class, that.getClass());
    }

    @Override
    public String shortId()
    {
        return "type:" + type();
    }

    @Override
    public String type()
    {
        return TYPE;
    }

}
