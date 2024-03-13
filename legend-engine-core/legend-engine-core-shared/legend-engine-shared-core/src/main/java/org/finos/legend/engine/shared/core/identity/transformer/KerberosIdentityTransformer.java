package org.finos.legend.engine.shared.core.identity.transformer;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

import javax.security.auth.Subject;
import java.util.Objects;

public class KerberosIdentityTransformer implements IdentityTransformer<Subject>
{
    public static KerberosIdentityTransformer INSTANCE = new KerberosIdentityTransformer();

    KerberosIdentityTransformer()
    {
        // Avoiding instantiation of this class
    }

    @Override
    public Subject transform(Identity identity) {
        return LazyIterate.selectInstancesOf(identity.getCredentials(), LegendKerberosCredential.class)
                .select(Objects::nonNull)
                .collect(LegendKerberosCredential::getSubject)
                .getFirst();
    }
}
