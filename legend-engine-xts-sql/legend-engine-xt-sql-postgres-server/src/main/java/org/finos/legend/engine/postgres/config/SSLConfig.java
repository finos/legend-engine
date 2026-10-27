// Copyright 2026 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.config;

import java.util.List;

public class SSLConfig
{
    private String keyStorePath;
    private String keyStorePassword;
    private String keyStoreType;
    private String keyManagerPassword;
    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType;
    private boolean needClientAuth;
    private boolean wantClientAuth;
    private boolean requireSsl;
    private List<String> supportedProtocols;
    private List<String> supportedCipherSuites;

    public String getKeyStorePath()
    {
        return keyStorePath;
    }

    public String getKeyStorePassword()
    {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword)
    {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreType()
    {
        return keyStoreType;
    }

    public String getKeyManagerPassword()
    {
        return keyManagerPassword;
    }

    public void setKeyManagerPassword(String keyManagerPassword)
    {
        this.keyManagerPassword = keyManagerPassword;
    }

    public String getTrustStorePath()
    {
        return trustStorePath;
    }

    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    public boolean isNeedClientAuth()
    {
        return needClientAuth;
    }

    public boolean isWantClientAuth()
    {
        return wantClientAuth;
    }

    public boolean isRequireSsl()
    {
        return requireSsl;
    }

    public List<String> getSupportedProtocols()
    {
        return supportedProtocols;
    }

    public List<String> getSupportedCipherSuites()
    {
        return supportedCipherSuites;
    }
}

