package org.finos.legend.engine.shared.core.vault;

public interface VaultImplementation
{
    String getValue(String key);

    boolean hasValue(String key);
}
