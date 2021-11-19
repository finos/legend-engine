package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import java.time.Clock;
import java.util.Optional;

import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.shared.core.identity.Identity;

public class ConnectionState
{
    private final Optional<CredentialSupplier> credentialSupplier;
    private final Identity identity;
    private final long creationTimeInMillis;

    public ConnectionState(long creationTimeInMillis, Identity identity, Optional<CredentialSupplier> credentialSupplier)
    {
        this.creationTimeInMillis = creationTimeInMillis;
        this.credentialSupplier = credentialSupplier;
        this.identity = identity;
    }

    public long ageInMillis(Clock clock)
    {
        return clock.millis() - this.creationTimeInMillis;
    }

    public Optional<CredentialSupplier> getCredentialSupplier()
    {
        return credentialSupplier;
    }

    public Identity getIdentity()
    {
        return identity;
    }
}