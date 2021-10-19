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

package org.finos.legend.engine.authentication.flows;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import java.util.stream.Collectors;

import net.snowflake.client.jdbc.internal.org.bouncycastle.jce.provider.BouncyCastleProvider;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.PKCS8Generator;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import net.snowflake.client.jdbc.internal.org.bouncycastle.operator.OutputEncryptor;
import net.snowflake.client.jdbc.internal.org.bouncycastle.util.io.pem.PemObject;
import org.finos.legend.engine.authentication.vaults.InMemoryVaultForTesting;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestSnowflakeWithKeyPairFlow
{
    private InMemoryVaultForTesting inMemoryVault = new InMemoryVaultForTesting();
    private Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");

    @Before
    public void setup()
    {
        Security.addProvider(new BouncyCastleProvider());
        Vault.INSTANCE.registerImplementation(inMemoryVault);
    }

    /*
        To run this test, first generate a private key and add it to src/test/resources/test_encrypted_privatekey.p8.
        The key should be encrypted with the passphrase "test"

        Key gen instructions : https://docs.snowflake.com/en/user-guide/key-pair-auth.html
     */

    @Ignore
    public void testFlow() throws Exception
    {
        String passphrase = "test";
        String privateKeyFromFile = this.loadEncryptedPrivateKey("/test-secrtes/encrypted_privatekey1.p8");
        inMemoryVault.setValue("key1", privateKeyFromFile);
        inMemoryVault.setValue("passphrase1", passphrase);

        SnowflakeDatasourceSpecification datasourceSpec = new SnowflakeDatasourceSpecification();
        SnowflakePublicAuthenticationStrategy authSpec = new SnowflakePublicAuthenticationStrategy();
        authSpec.passPhraseVaultReference = "passphrase1";
        authSpec.privateKeyVaultReference = "key1";
        authSpec.publicUserName = identity1.getName();

        SnowflakeWithKeyPairFlow flow = new SnowflakeWithKeyPairFlow();
        PrivateKeyCredential credential = (PrivateKeyCredential) flow.makeCredential(identity1, datasourceSpec, authSpec);

        assertEquals(identity1.getName(), credential.getUser());
        String privateKeyFromFlow = this.serializePrivateKey(credential.getPrivateKey());
        // TODO : need a better assert
        assertNotNull(privateKeyFromFlow);
    }

    private String loadEncryptedPrivateKey(String file) throws URISyntaxException, IOException
    {
        URL resource = TestSnowflakeWithKeyPairFlow.class.getResource(file);
        List<String> lines = Files.readAllLines(Paths.get(resource.toURI()));
        return lines.stream().collect(Collectors.joining(System.lineSeparator()));
    }

    private String encryptAndSerializePrivateKey(PrivateKey privateKey, String passphrase) throws Exception
    {
        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_RC2_128);
        encryptorBuilder.setRandom(new SecureRandom());

        encryptorBuilder.setPasssword(passphrase.toCharArray());
        OutputEncryptor encryptor = encryptorBuilder.build();

        JcaPKCS8Generator jcaPKCS8Generator = new JcaPKCS8Generator(privateKey, encryptor);
        PemObject pemObject = jcaPKCS8Generator.generate();
        StringWriter stringWriter = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter))
        {
            pemWriter.writeObject(pemObject);
        }
        return stringWriter.toString();
    }

    private String serializePrivateKey(PrivateKey privateKey) throws IOException
    {
        JcaPKCS8Generator jcaPKCS8Generator = new JcaPKCS8Generator(privateKey, null);
        PemObject pemObject = jcaPKCS8Generator.generate();
        StringWriter stringWriter = new StringWriter();
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter))
        {
            pemWriter.writeObject(pemObject);
        }
        return stringWriter.toString();
    }
}