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

import com.google.common.base.Splitter;
import net.snowflake.client.jdbc.internal.apache.commons.codec.binary.Base64;
import net.snowflake.client.jdbc.internal.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import net.snowflake.client.jdbc.internal.org.bouncycastle.jce.provider.BouncyCastleProvider;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.PEMParser;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import net.snowflake.client.jdbc.internal.org.bouncycastle.operator.InputDecryptorProvider;
import net.snowflake.client.jdbc.internal.org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.EncryptedPrivateKeyPairAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;

public class SnowflakeWithKeyPairFlow implements DatabaseAuthenticationFlow<SnowflakeDatasourceSpecification, SnowflakePublicAuthenticationStrategy>
{
    public CredentialProviderProvider credentialProviderProvider = null;

    public SnowflakeWithKeyPairFlow()
    {
    }

    public SnowflakeWithKeyPairFlow(CredentialProviderProvider credentialProviderProvider)
    {
        this.credentialProviderProvider = credentialProviderProvider;
    }

    @Override
    public Class<SnowflakeDatasourceSpecification> getDatasourceClass()
    {
        return SnowflakeDatasourceSpecification.class;
    }

    @Override
    public Class<SnowflakePublicAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return SnowflakePublicAuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Snowflake;
    }

    @Override
    public Credential makeCredential(Identity identity, SnowflakeDatasourceSpecification datasourceSpecification, SnowflakePublicAuthenticationStrategy authenticationStrategy) throws Exception
    {
        if (this.credentialProviderProvider != null)
        {
            return this.makeCredentialWithCredentialProvider(authenticationStrategy, identity);
        }
        else
        {
            return this.makeCredential(authenticationStrategy);
        }
    }

    protected EncryptedPrivateKeyPairAuthenticationSpecification adapt(SnowflakePublicAuthenticationStrategy authenticationStrategy)
    {
        EncryptedPrivateKeyPairAuthenticationSpecification authenticationSpecification = new EncryptedPrivateKeyPairAuthenticationSpecification();
        authenticationSpecification.userName = authenticationStrategy.publicUserName;
        authenticationSpecification.privateKey = new PropertiesFileSecret(authenticationStrategy.privateKeyVaultReference);
        authenticationSpecification.passphrase = new PropertiesFileSecret(authenticationStrategy.passPhraseVaultReference);
        return authenticationSpecification;
    }

    private PrivateKeyCredential makeCredentialWithCredentialProvider(SnowflakePublicAuthenticationStrategy authenticationStrategy, Identity identity) throws Exception
    {
        EncryptedPrivateKeyPairAuthenticationSpecification authenticationSpecification = this.adapt(authenticationStrategy);
        return (PrivateKeyCredential)CredentialBuilder.makeCredential(this.credentialProviderProvider, authenticationSpecification, identity);
    }

    private PrivateKeyCredential makeCredential(SnowflakePublicAuthenticationStrategy authenticationStrategy)
    {
        String publicUserName = authenticationStrategy.publicUserName;
        String privateKeyVaultReference = authenticationStrategy.privateKeyVaultReference;
        String passPhraseVaultReference = authenticationStrategy.passPhraseVaultReference;
        PrivateKey privateKey = this.getEncryptedPrivateKey(privateKeyVaultReference, passPhraseVaultReference);
        return new PrivateKeyCredential(publicUserName, privateKey);
    }

    private PrivateKey getEncryptedPrivateKey(String privateKeyVaultReference, String passPhraseVaultReference)
    {
        String privateKey = Vault.INSTANCE.getValue(privateKeyVaultReference);
        String passPhrase = Vault.INSTANCE.getValue(passPhraseVaultReference);

        if (privateKey == null || passPhrase == null)
        {
            throw new RuntimeException("Can't find the privateKey (" + privateKeyVaultReference + ") or the passPhrase (" + passPhraseVaultReference + ") in the vault");
        }

        if (!privateKey.startsWith("-----BEGIN ENCRYPTED PRIVATE KEY-----"))
        {
            privateKey = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" + Iterate.makeString(Splitter.fixedLength(64).split(privateKey), "\n") + "\n-----END ENCRYPTED PRIVATE KEY-----";
        }

        try (PEMParser pemParser = new PEMParser(new StringReader(privateKey)))
        {
            Object pemObject = pemParser.readObject();
            if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo)
            {
                PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) pemObject;

                if ("1.2.840.113549.1.5.3".equals(encryptedPrivateKeyInfo.getEncryptionAlgorithm().getAlgorithm().toString()))
                {
                    String encryptedPrivateKeyString = privateKey;
                    encryptedPrivateKeyString = encryptedPrivateKeyString.replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "");
                    encryptedPrivateKeyString = encryptedPrivateKeyString.replace("-----END ENCRYPTED PRIVATE KEY-----", "");
                    EncryptedPrivateKeyInfo pki = new EncryptedPrivateKeyInfo(Base64.decodeBase64(encryptedPrivateKeyString));
                    PBEKeySpec privateKeySpec = new PBEKeySpec(passPhrase.toCharArray());
                    SecretKeyFactory pbeKeyFactory = SecretKeyFactory.getInstance(pki.getAlgName());
                    PKCS8EncodedKeySpec encodedKeySpec = pki.getKeySpec(pbeKeyFactory.generateSecret(privateKeySpec));
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    return keyFactory.generatePrivate(encodedKeySpec);
                }
                else
                {
                    Security.addProvider(new BouncyCastleProvider());
                    InputDecryptorProvider pkcs8Prov = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(passPhrase.toCharArray());
                    PrivateKeyInfo privateKeyInfo = encryptedPrivateKeyInfo.decryptPrivateKeyInfo(pkcs8Prov);
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
                    return converter.getPrivateKey(privateKeyInfo);
                }
            }
            else
            {
                throw new UnsupportedOperationException(pemObject.getClass() + " is not supported yet");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}