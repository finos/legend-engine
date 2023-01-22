// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.authentication;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.authentication.credentialprovider.impl.PrivateKeyCredentialProvider;
import org.finos.legend.authentication.intermediationrule.impl.EncryptedPrivateKeyFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.CredentialVaultProviderForTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.EncryptedPrivateKeyPairAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestCredentialCreation_EncryptedApiKey
{
    private Identity identity;

    private CredentialVaultProvider credentialVaultProvider;

    @Before
    public void setup()
    {
        this.identity = new Identity("alice", new AnonymousCredential());
        this.credentialVaultProvider = CredentialVaultProviderForTest.buildForTest()
                .withProperties("reference1", PK)
                .withProperties("reference2", PASS)
                .build();
    }

    @Test
    public void makeCredentialFromPropertiesVault() throws Exception
    {
        PrivateKeyCredentialProvider credentialProvider = new PrivateKeyCredentialProvider();
        credentialProvider.configureWithRules(FastList.newListWith(new EncryptedPrivateKeyFromVaultRule(credentialVaultProvider)));

        EncryptedPrivateKeyPairAuthenticationSpecification AuthenticationSpecification = new EncryptedPrivateKeyPairAuthenticationSpecification(
                "fred",
                new PropertiesFileSecret("reference1"),
                new PropertiesFileSecret("reference2")
        );
        PrivateKeyCredential credential = credentialProvider.makeCredential(AuthenticationSpecification, identity);
        assertEquals("fred", credential.getUser());
        assertNotNull(credential.getPrivateKey());
    }

    // Key creation instructions can be found here https://docs.snowflake.com/en/user-guide/key-pair-auth.html
    private static String PK = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" +
            "MIIFHDBOBgkqhkiG9w0BBQ0wQTApBgkqhkiG9w0BBQwwHAQIFHVXMxWCnpYCAggA\n" +
            "MAwGCCqGSIb3DQIJBQAwFAYIKoZIhvcNAwcECPOF7ZWgWMA5BIIEyFJUV/G78SDU\n" +
            "x4snwHIJiE9f0b9TZ2Rz6cISMSuDp+PJrdM4YKFSgZPDSNW4VUryTjYatsOgKROT\n" +
            "osw5S+Ynvw0PQjXFS1p93hsWjBdTmW956/xtt1qAQS9LdZRZaH+mAEk+efXiF3cP\n" +
            "+n+KGmJLF0EvuA9EuJdAGegx8WnKn8nO4Dy/a/eOYYJZ34K+CoId4EWvq5yhPi68\n" +
            "6F/THZagiNbVCx7GzneSjbPnlvZUvxNxnQ5blHM0KNp2xC6PT7F5upfD0+gxIrV1\n" +
            "sRaUATlDuIEl0bUinQKTzEOWO+sNPRM3z+kUN5lNSTJ1QvocjE5PuDJkqgGiHMop\n" +
            "iogY95IcmyyxlizH9qwvDbbogcl6oFoHFdzz8ikEITFmb1PDzGlx/iG81TBM5G9o\n" +
            "vOr7J5nLKLV9NpPtp66oMbcgvq8duou5mRDzXPxhdlsZ6/u03ybI3WDvw7EnRx7C\n" +
            "DZ+m9IiSxtUFebd2Ef2JMlIKyQNnC7TKGxdadVXJwWS6B5IVf0c7lxeIWGXyuoeV\n" +
            "LmzVldBInyhA50cHjGx4Te+e7iAbP1qz/lfevgDeUlV8Upv0RhgT/sHLViuc22Kb\n" +
            "iO9Ck+Pm+AKAIXsj28Npvsv7rxauOGrwEP4D/u1BAqQ5XcG5ihdMhpylNNQwo2XH\n" +
            "KQvcu5Q7rEixS5TaKAtngnuX9x6f72rIVt9bhwIM/kH+MUCEdM4bnG3FXtZGVnAK\n" +
            "RNtRzLVN6ognb8B+6DQPYIMa6FBDYgNa6AkVJZZs9YL8+FnJCq6pkyyckgIZ9x6w\n" +
            "QUfwVU6JRnaGKyoJClcXjP+uMi2po8BuzWQwxWPa8YvxQDg/BK1C7WS+mzIfr4gx\n" +
            "bch5hsFRtRGPS7ggbTqcvaCtEJbFkopC7HPTfLLeTdnOtfRALFPJst2xCBl7Otl6\n" +
            "3Rr+WCDnr4rAXgcsSxN2Bllv4sC8fWO+4F1vq5Awi7nxauBq/pah1Ojfeuuoi0e6\n" +
            "JLxUlQJu+MceY/OuQN0BayZsEGKQC/ufhenM6zhFbchvk92ZFTuC0mV7MJ2cYc2w\n" +
            "PfoeRRIoMy0shlhKBOm+YyjoM8xvFPazqPNZVdCqbvJSmkeuG8TypnlsW8wy/Y1P\n" +
            "U19pWasbom4ltt3gv06FrSzH9bTx2vcokhHBPlixWtXvWfFNXm5ZAAdWDNhYb8mD\n" +
            "X+N6i3IXlLGKaFBzAM0InjEV6HOtEIFqVLoXaGTfAVn4JviqpBno++GI2N5U1vKl\n" +
            "TA0V1ahjOC492Mft16H4H3OXR0mSKc32dMY+w8QwtByIlXDmGV+NpfZiTHcMzuKP\n" +
            "M6uDbPSw9li4tyLmDe1T75HlJjjeFmFMFqFAGHApS1yq69SMNAlv3mv/Rt+SiUpr\n" +
            "Wj59mz9QMkQOtDxMxNEPpeaEr6tNeMTV+DPU+zUeXFU/uNXR2yHmmzvNKUKSitYp\n" +
            "91TdOjXHCl3Fqdf5aeOfbwa0xpnNQzOcdwrm4KSOUsoMv1OZ222XH6DNauLCRbf/\n" +
            "4s5XrEq69OpIjMMa4jXKGw47nKj4S/C4Zqo1W2UP9z+VY8+v2vlzAAPzWEmA+Rg7\n" +
            "0a3ddHiaMNBVAGkaFzlIM7CbwVUwvGydCBgixPcwBNtMRmp/11a4UBh/HQqF7DmL\n" +
            "Sfa3ElWgijNkMqCKqry96g==\n" +
            "-----END ENCRYPTED PRIVATE KEY-----";

    private static String PASS = "changeme";

    @Test
    public void testChunk()
    {
        String[] tokens = PK.split("\n");
        String privateKeyData = Arrays.stream(Arrays.copyOfRange(tokens, 1, tokens.length - 1)).collect(Collectors.joining());
        String chunked = EncryptedPrivateKeyFromVaultRule.chunk(privateKeyData);
        assertEquals(PK, tokens[0] + "\n" + chunked + "\n" + tokens[tokens.length - 1]);
    }
}
