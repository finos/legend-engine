package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

public final class TestIdentityFactory implements IdentityFactory
{
    public static TestIdentityFactory INSTANCE = new TestIdentityFactory();

    @Override
    public Identity makeIdentity(Subject subject)
    {
        if (subject == null)
        {
            return this.makeUnknownIdentity();
        }
        Principal principal = getKerberosPrincipalFromSubject(subject);
        if (principal == null)
        {
            throw new IllegalArgumentException("Subject does not contain a KerberosPrincipal");
        }
        String name = principal != null ? principal.getName().split("@")[0] : null;
        return new Identity(name, new LegendKerberosCredential(subject));
    }

    private Principal getKerberosPrincipalFromSubject(Subject subject)
    {
        return SubjectTools.getPrincipalFromSubject(subject);
    }

    @Override
    public Identity makeIdentity(MutableList<CommonProfile> profiles)
    {
        if (profiles == null || profiles.isEmpty())
        {
            return this.makeUnknownIdentity();
        }
        Optional<KerberosProfile> kerberosProfileHolder = this.getKerberosProfile(profiles);
        if (kerberosProfileHolder.isPresent())
        {
            return INSTANCE.makeIdentity(kerberosProfileHolder.get().getSubject());
        }
        return INSTANCE.makeIdentityForTesting(profiles.get(0).getId());
    }

    private Optional<KerberosProfile> getKerberosProfile(MutableList<CommonProfile> profiles)
    {
        return Optional.ofNullable(LazyIterate.selectInstancesOf(profiles, KerberosProfile.class).getFirst());
    }

    public Identity makeUnknownIdentity()
    {
        return new Identity("_UNKNOWN_");
    }

    @Override
    public Identity makeIdentityForTesting(String name)
    {
        return new Identity(name);
    }

    @Override
    public List<CommonProfile> adapt(Identity identity)
    {
        MutableList<CommonProfile> profiles = Lists.mutable.empty();
        ImmutableList<Credential> credentials = identity.getCredentials();
        for (Credential credential : credentials)
        {
            if (credential instanceof LegendKerberosCredential)
            {
                LegendKerberosCredential kerberosCredential = (LegendKerberosCredential) credential;
                profiles.add(new KerberosProfile(kerberosCredential.getSubject(), null));
            }
            else if (credential instanceof AnonymousCredential)
            {
                CommonProfile profile = new CommonProfile();
                profile.setId(identity.getName());
                profiles.add(profile);
            }
        }
        return profiles;
    }
}