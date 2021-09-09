package org.finos.legend.engine.shared.core.vault;

import java.util.Properties;

public class PropertiesVaultImplementation implements VaultImplementation
{
    private final Properties properties;

    public PropertiesVaultImplementation(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public String getValue(String vaultKey)
    {
        return properties.getProperty(vaultKey);
    }

    @Override
    public boolean hasValue(String vaultKey)
    {
        return properties.containsKey(vaultKey);
    }
}
