package org.finos.legend.engine.shared.core.vault;

public class VoidVault implements VaultImplementation
{
    public static VoidVault INSTANCE = new VoidVault();

    private VoidVault()
    {
    }

    @Override
    public String getValue(String vaultKey)
    {
        throw new RuntimeException("No vault was provided");
    }
}
