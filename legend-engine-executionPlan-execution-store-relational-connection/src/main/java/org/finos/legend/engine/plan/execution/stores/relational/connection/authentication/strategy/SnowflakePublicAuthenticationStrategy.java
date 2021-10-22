package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.common.base.Splitter;
import net.snowflake.client.jdbc.internal.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import net.snowflake.client.jdbc.internal.org.bouncycastle.jce.provider.BouncyCastleProvider;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.PEMParser;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import net.snowflake.client.jdbc.internal.org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import net.snowflake.client.jdbc.internal.org.bouncycastle.operator.InputDecryptorProvider;
import net.snowflake.client.jdbc.internal.org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.SnowflakePublicAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionState;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

public class SnowflakePublicAuthenticationStrategy extends AuthenticationStrategy
{
    private final String privateKeyVaultReference;
    private final String passPhraseVaultReference;
    private final String publicUserName;

    public SnowflakePublicAuthenticationStrategy(String privateKeyVaultReference, String passPhraseVaultReference, String publicUserName)
    {
        this.privateKeyVaultReference = privateKeyVaultReference;
        this.passPhraseVaultReference = passPhraseVaultReference;
        this.publicUserName = publicUserName;
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        PrivateKeyCredential keyPairCredential = this.resolveCredential(properties, this.privateKeyVaultReference, this.passPhraseVaultReference, this.publicUserName);
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);
        connectionProperties.put("privateKey", keyPairCredential.getPrivateKey());
        connectionProperties.put("user", keyPairCredential.getUser());
        return Tuples.pair(url, connectionProperties);
    }

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        try
        {
            return ds.getDataSource().getConnection();
        }
        catch (SQLException e)
        {
            throw new ConnectionException(e);
        }
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new SnowflakePublicAuthenticationStrategyKey(this.privateKeyVaultReference, this.passPhraseVaultReference, this.publicUserName);
    }

    private PrivateKeyCredential resolveCredential(Properties properties, String privateKeyVaultReference, String passPhraseVaultReference, String publicUserName)
    {
        ConnectionState connectionState = ConnectionStateManager.getInstance().getStateUsing(properties);
        if (!connectionState.getCredentialSupplier().isPresent())
        {
            PrivateKey privateKey = this.getEncryptedPrivateKey(privateKeyVaultReference, passPhraseVaultReference);
            return new PrivateKeyCredential(publicUserName, privateKey);
        }
        return (PrivateKeyCredential)super.getDatabaseCredential(connectionState);
    }

    private PrivateKey getEncryptedPrivateKey(String privateKeyVaultReference, String passPhraseVaultReference)
    {
        String privateKey = Vault.INSTANCE.getValue(privateKeyVaultReference);
        String passPhrase = Vault.INSTANCE.getValue(passPhraseVaultReference);

        if (privateKey == null || passPhrase == null)
        {
            throw new RuntimeException("Can't find the privateKey ("+privateKeyVaultReference+") or the passPhrase ("+passPhraseVaultReference+") in the vault");
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
                throw new UnsupportedOperationException(pemObject.getClass()+" is not supported yet");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
