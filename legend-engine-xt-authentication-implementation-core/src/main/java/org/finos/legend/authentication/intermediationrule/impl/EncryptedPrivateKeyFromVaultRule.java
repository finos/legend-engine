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

package org.finos.legend.authentication.intermediationrule.impl;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.util.encoders.Base64;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Strings;
import org.finos.legend.authentication.intermediationrule.IntermediationRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.EncryptedPrivateKeyPairAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;

public class EncryptedPrivateKeyFromVaultRule extends IntermediationRule<EncryptedPrivateKeyPairAuthenticationSpecification, Credential, PrivateKeyCredential>
{
    public EncryptedPrivateKeyFromVaultRule(CredentialVaultProvider credentialVaultProvider)
    {
        super(credentialVaultProvider);
    }

    @Override
    public PrivateKeyCredential makeCredential(EncryptedPrivateKeyPairAuthenticationSpecification authenticationSpecification, Credential credential) throws Exception
    {
        String encryptedPrivateKey = super.lookupSecret(authenticationSpecification.privateKey);
        String passphrase = super.lookupSecret(authenticationSpecification.passphrase);
        PrivateKey privateKey = this.getDecryptedPrivateKey(encryptedPrivateKey, passphrase);
        // TODO - epsstan - remove "alice"
        return new PrivateKeyCredential("alice", privateKey);
    }

    private PrivateKey getDecryptedPrivateKey(String privateKey, String passPhrase)
    {
        if (!privateKey.startsWith("-----BEGIN ENCRYPTED PRIVATE KEY-----"))
        {
            privateKey = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" + chunk(privateKey) + "\n-----END ENCRYPTED PRIVATE KEY-----";
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
                    EncryptedPrivateKeyInfo pki = new EncryptedPrivateKeyInfo(Base64.decode(encryptedPrivateKeyString));
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

    public static String chunk(String privateKey)
    {
        RichIterable<String> collect = Strings.asChars(privateKey).chunk(64).collect(c -> c.makeString(""));
        String s = collect.makeString("\n");
        return Strings.asChars(privateKey).chunk(64).collect(c -> c.makeString("")).makeString("\n");
    }
}
