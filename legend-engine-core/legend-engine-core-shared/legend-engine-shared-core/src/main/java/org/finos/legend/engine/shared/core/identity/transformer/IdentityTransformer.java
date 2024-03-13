package org.finos.legend.engine.shared.core.identity.transformer;

import org.finos.legend.engine.shared.core.identity.Identity;

public interface IdentityTransformer<T>
{
    public T transform(Identity identity);
}
