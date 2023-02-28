package org.finos.legend.authentication;

import org.finos.legend.authentication.credentialprovider.impl.ApikeyCredentialProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDefaultCredentialCreation
{
    private Identity identity;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new ApiTokenCredential("value1"));
    }

    @Test
    public void testProviderWithDefaultIncomingCredential() throws Exception
    {
        ApikeyCredentialProvider credentialProvider = new ApikeyCredentialProvider();
        ApiTokenCredential credential = credentialProvider.makeCredential(new ApiKeyAuthenticationSpecification(), identity);

        assertTrue(credential instanceof ApiTokenCredential);
        assertEquals("value1", credential.getApiToken());
    }
}
