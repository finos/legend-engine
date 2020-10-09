// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.shared.core.url;

import org.finos.legend.engine.shared.core.encryption.EncryptionStrategy;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.URL_ENCRYPTION_INVALID_ALGORITHM_PARAMETER_EXCEPTION;
import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.URL_ENCRYPTION_INVALID_KEY_EXCEPTION;
import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.URL_ENCRYPTION_INVALID_KEY_SPEC_EXCEPTION;
import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.URL_ENCRYPTION_NO_SUCH_ALGORITHM;
import static org.finos.legend.engine.shared.core.operational.logs.LoggingEventType.URL_ENCRYPTION_NO_SUCH_PADDING;


/**
 * Decrypt Protocol Handler
 * data+des:text/plain,Single Line Text for Testin!!;12345678
 * type is an identifier that specifies what kind of decryption to use.
 * The protocol itself is a simplified version of Data.
 * Once the handler has figured out the decryption type,
 * it executes the chosen
 * decryption algorithm and uses it to
 * retrieve the data.
 */
public class EncryptionProtocolHandler extends URLStreamHandler implements UrlProtocolHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProtocolHandler.class);

    private static final List<String> ENCRYPTION_ALGORITHMS = Arrays.asList("des", "aes");
    private static final String PLUS_SIGN = "+";
    private static final char COLON_SIGN = ';';

    @Override
    public boolean handles(String protocol)
    {
        return isSupportedEncryptionProtocol(protocol);
    }

    @Override
    public URLStreamHandler getHandler(String protocol)
    {
        if (!isSupportedEncryptionProtocol(protocol))
        {
            throw new IllegalArgumentException("Invalid protocol: " + protocol);
        }
        return this;
    }

    @Override
    protected URLConnection openConnection(URL enhancedUrl) throws IOException
    {


        int encryptionSchemeSeparator = enhancedUrl.getProtocol().lastIndexOf(PLUS_SIGN);
        String encryptionScheme = enhancedUrl.getProtocol().substring(encryptionSchemeSeparator + 1);
        String urlString = enhancedUrl.toExternalForm();
        int keySeparator = urlString.lastIndexOf(COLON_SIGN);
        String key = urlString.substring(keySeparator + 1);
        String strippedUrlString = stripUrl(urlString, encryptionScheme, key);
        URL url = new URL(strippedUrlString);
        return new EncryptionUrlConnection(url, encryptionScheme, key);
    }

    private static class EncryptionUrlConnection extends URLConnection
    {
        private CipherInputStream decryptInputStream;
        private final String key;
        private final String encryptionScheme;

        EncryptionUrlConnection(URL url, String encryptionScheme, String key)
        {
            super(url);
            this.key = key;
            this.encryptionScheme = encryptionScheme;
        }

        @Override
        public void connect() throws IOException
        {
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            EncryptionStrategy encryptionStrategy = EncryptionStrategy.valueOf(encryptionScheme.toUpperCase());
            try
            {
                decryptInputStream = encryptionStrategy.execute(inputStream, key);
            }
            catch (InvalidKeyException e)
            {
                LOGGER.error(new LogInfo(null, URL_ENCRYPTION_INVALID_KEY_EXCEPTION).toString());
                throw new EngineException(URL_ENCRYPTION_INVALID_KEY_EXCEPTION.name(), e);
            }
            catch (NoSuchAlgorithmException e)
            {
                LOGGER.error(new LogInfo(null, URL_ENCRYPTION_NO_SUCH_ALGORITHM).toString());
                throw new EngineException(URL_ENCRYPTION_NO_SUCH_ALGORITHM.name(), e);
            }
            catch (InvalidKeySpecException e)
            {
                LOGGER.error(new LogInfo(null, URL_ENCRYPTION_INVALID_KEY_SPEC_EXCEPTION).toString());
                throw new EngineException(URL_ENCRYPTION_INVALID_KEY_SPEC_EXCEPTION.name(), e);
            }
            catch (NoSuchPaddingException e)
            {
                LOGGER.error(new LogInfo(null, URL_ENCRYPTION_NO_SUCH_PADDING).toString());
                throw new EngineException(URL_ENCRYPTION_NO_SUCH_PADDING.name(), e);
            }
            catch (InvalidAlgorithmParameterException e)
            {
                LOGGER.error(new LogInfo(null, URL_ENCRYPTION_INVALID_ALGORITHM_PARAMETER_EXCEPTION).toString());
                throw new EngineException(URL_ENCRYPTION_INVALID_ALGORITHM_PARAMETER_EXCEPTION.name(), e);
            }
            connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            if (!connected)
            {
                connect();
            }
            return decryptInputStream;
        }
    }

    public static boolean isSupportedEncryptionProtocol(String protocol)
    {
        return ENCRYPTION_ALGORITHMS.stream().anyMatch(entry -> protocol.endsWith(PLUS_SIGN + entry));
    }

    private static String stripUrl(String urlString, String encryptionScheme, String key)
    {
        return urlString.replace(COLON_SIGN + key, "").replace(PLUS_SIGN + encryptionScheme, "");
    }
}