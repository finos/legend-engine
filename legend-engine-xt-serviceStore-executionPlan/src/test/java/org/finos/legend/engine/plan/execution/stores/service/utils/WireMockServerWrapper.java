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

package org.finos.legend.engine.plan.execution.stores.service.utils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.util.Objects;

public class WireMockServerWrapper
{
    private int port;
    private String source;
    private boolean enableClientAuth;
    private String keyStorePath;
    private String keyStorePassword;
    private String keyManagerPassword;
    private String caKeyStorePath;
    private String caKeyStorePassword;

    public WireMockServerWrapper(int port, String source)
    {
        this.port = port;
        this.source = source;
    }

    static WireMockServerWrapper with(int port, String source)
    {
        return new WireMockServerWrapper(port, source);
    }

    WireMockServerWrapper withClientAuth(boolean enableClientAuth)
    {
        this.enableClientAuth = enableClientAuth;
        return this;
    }

    WireMockServerWrapper withServerKeystore(String keyStorePath, String keyStorePassword, String keyManagerPassword)
    {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.keyManagerPassword = keyManagerPassword;
        return this;
    }

    WireMockServerWrapper withCAKeyStore(String caKeyStorePath, String caKeyStorePassword)
    {
        this.caKeyStorePath = caKeyStorePath;
        this.caKeyStorePassword = caKeyStorePassword;
        return this;
    }

    public WireMockServer build()
    {
        WireMockConfiguration wireMockConfiguration = new WireMockConfiguration();
        wireMockConfiguration.port(port);

        // enable tls
        wireMockConfiguration.httpsPort(port);
        wireMockConfiguration.httpDisabled(true);
        wireMockConfiguration.keystorePath(this.keyStorePath);
        wireMockConfiguration.keystorePassword(this.keyStorePassword);
        wireMockConfiguration.keyManagerPassword(this.keyManagerPassword);

        // trust store specifies the certificates we want to trust. In this case all the certs signed by the cert authority in this trust store will be accepted
        wireMockConfiguration.trustStorePath(this.caKeyStorePath);
        wireMockConfiguration.trustStorePassword(this.caKeyStorePassword);
        wireMockConfiguration.caKeystorePath(this.caKeyStorePath);
        wireMockConfiguration.caKeystorePassword(this.caKeyStorePassword);

        wireMockConfiguration.needClientAuth(this.enableClientAuth);

        wireMockConfiguration.fileSource(new SingleRootFileSource(Objects.requireNonNull(ServiceStoreTestSuite.class.getClassLoader().getResource(this.source)).getFile()));
        wireMockConfiguration.enableBrowserProxying(false);
        return new WireMockServer(wireMockConfiguration);
    }
}
