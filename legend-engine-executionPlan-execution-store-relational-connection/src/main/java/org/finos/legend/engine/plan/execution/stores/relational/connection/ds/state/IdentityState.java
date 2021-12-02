package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;


import java.util.Optional;

import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.shared.core.identity.Identity;

public class IdentityState
{
    private final Optional<CredentialSupplier> credentialSupplier;
    private final Identity identity;

    public IdentityState(Identity identity, Optional<CredentialSupplier> credentialSupplier)
    {
        this.credentialSupplier = credentialSupplier;
        this.identity = identity;
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