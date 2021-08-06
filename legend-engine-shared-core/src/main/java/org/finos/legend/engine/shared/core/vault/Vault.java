package org.finos.legend.engine.shared.core.vault;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public class Vault
{
    public static Vault INSTANCE = new Vault();

    private final MutableList<VaultImplementation> implementations = Lists.mutable.empty();

    private Vault()
    {
    }

    public void registerImplementation(VaultImplementation vaultImplementation)
    {
        this.implementations.add(vaultImplementation);
    }

    public String getValue(String key)
    {
        return this.implementations.collect(i -> i.getValue(key)).getFirst();
    }

    public boolean hasValue(String key)
    {
        return !this.implementations.select(i -> i.hasValue(key)).isEmpty();
    }
}
