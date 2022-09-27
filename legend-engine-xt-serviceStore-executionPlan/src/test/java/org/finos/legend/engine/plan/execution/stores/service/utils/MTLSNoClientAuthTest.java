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

import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/*
    This class is a test/demo of mTLS.
    It exercises various scenarios of a client and server communicating with mTLS.
 */
public class MTLSNoClientAuthTest extends MTLSTestAbstract
{
    // Create a server with client auth disabled
    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(
            wireMockConfig()
                    .dynamicHttpsPort()
                    .httpDisabled(true)
                    .needClientAuth(false)
                    .fileSource(new SingleRootFileSource(Objects.requireNonNull(ServiceStoreTestSuite.class.getClassLoader().getResource("showcase/json")).getFile()))
                    .keystorePath(SERVER_KEYSTORE_PATH).keystorePassword(CHANGEIT_PASSWORD).keyManagerPassword(CHANGEIT_PASSWORD)
                    .caKeystorePath(CA_KEYSTORE_PATH).caKeystorePassword(CHANGEIT_PASSWORD)
                    .trustStorePath(CA_KEYSTORE_PATH).trustStorePassword(CHANGEIT_PASSWORD)
    );

    @Test
    public void serverWith_ClientAuthDisabled_Allows_UnAuthenticatedClients() throws Exception
    {

        // Create a http client which DOEST NOT present a certificate ... this is ok because the server does not require client authentication
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(SSLContextBuilder.create()
                        .loadTrustMaterial(new TrustAllStrategy())
                        .build())
                .build();

        HttpGet httpGet = new HttpGet(wireMockRule.baseUrl() + "/trades/1");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("mismatch in status code", 200, httpResponse.getStatusLine().getStatusCode());
        String responseText = EntityUtils.toString(httpResponse.getEntity());
        assertEquals("[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"}]", responseText);
    }

    @Test
    public void serverWith_ClientAuthDisabled_Allows_AuthenticatedClients() throws Exception
    {
        // Create a http client which DOES present a certificate ... this is ok because the server does not require client authentication
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(SSLContextBuilder.create()
                        .loadKeyMaterial(new File(CLIENT_KEYSTORE_PATH), CHANGEIT_PASSWORD.toCharArray(), CHANGEIT_PASSWORD.toCharArray())
                        .loadTrustMaterial(new TrustAllStrategy())
                        .build())
                .build();

        HttpGet httpGet = new HttpGet(wireMockRule.baseUrl() + "/trades/1");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("mismatch in status code", 200, httpResponse.getStatusLine().getStatusCode());
        String responseText = EntityUtils.toString(httpResponse.getEntity());
        assertEquals("[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"}]", responseText);
    }
}

