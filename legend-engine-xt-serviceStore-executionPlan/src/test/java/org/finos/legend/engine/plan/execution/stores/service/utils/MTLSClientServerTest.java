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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/*
    This class is a test/demo of mTLS.
    It exercises various scenarios of a client and server communicating with mTLS.
 */
public class MTLSClientServerTest
{
    public static String SERVER_KEYSTORE_PATH = "";
    public static String CA_KEYSTORE_PATH = "";
    public static String CHANGEIT_PASSWORD = "changeit";

    @BeforeClass
    public static void generateCerts() throws Exception
    {
        /*
            This test runs a shell script that generates certs.
            The shell script needs to be ported to Windows to use Windows versions of OpenSSL exe etc.
            For now, the test is run only Linux. You can run the test on Windows by generating the certs as described in certs.sh.
         */
        if (System.getProperty("os.name").toLowerCase().contains("windows"))
        {
            assumeTrue(false);
        }
        CertGenerator.Certs certs = new CertGenerator().generateCerts();
        SERVER_KEYSTORE_PATH = certs.serverKeyStorePath;
        CA_KEYSTORE_PATH = certs.caKeyStorePath;
    }

    private int port;
    private WireMockServer wireMockServer;

    @Before
    public void setup()
    {
        this.port = DynamicPortGenerator.generatePort();
    }

    @After
    public void shutdown()
    {
        if (this.wireMockServer != null)
        {
            wireMockServer.shutdown();
        }
    }

    @Test
    public void serverWith_ClientAuthDisabled_Allows_UnAuthenticatedClients() throws Exception
    {
        // Create a server with client auth disabled
        this.wireMockServer = WireMockServerWrapper.with(port, "showcase/json")
                .withServerKeystore(SERVER_KEYSTORE_PATH, CHANGEIT_PASSWORD, CHANGEIT_PASSWORD)
                .withCAKeyStore(CA_KEYSTORE_PATH, CHANGEIT_PASSWORD)
                .withClientAuth(false)
                .build();
        wireMockServer.start();

        // Create a http client which DOEST NOT present a certificate ... this is ok because the server does not require client authentication
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(SSLContextBuilder.create()
                        .loadTrustMaterial(new TrustAllStrategy())
                        .build())
                .build();

        HttpGet httpGet = new HttpGet(wireMockServer.baseUrl() + "/trades/1");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("mismatch in status code", 200, httpResponse.getStatusLine().getStatusCode());
        String responseText = EntityUtils.toString(httpResponse.getEntity());
        assertEquals("[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"}]", responseText);
    }

    @Test
    public void serverWith_ClientAuthDisabled_Allows_AuthenticatedClients() throws Exception
    {
        // Create a server with client auth disabled
        this.wireMockServer = WireMockServerWrapper.with(port, "showcase/json")
                .withServerKeystore(SERVER_KEYSTORE_PATH, CHANGEIT_PASSWORD, CHANGEIT_PASSWORD)
                .withCAKeyStore(CA_KEYSTORE_PATH, CHANGEIT_PASSWORD)
                .withClientAuth(false)
                .build();
        wireMockServer.start();

        // Create a http client which DOES present a certificate ... this is ok because the server does not require client authentication
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(SSLContextBuilder.create()
                        .loadKeyMaterial(new File(SERVER_KEYSTORE_PATH), CHANGEIT_PASSWORD.toCharArray(), CHANGEIT_PASSWORD.toCharArray())
                        .loadTrustMaterial(new TrustAllStrategy())
                        .build())
                .build();

        HttpGet httpGet = new HttpGet(wireMockServer.baseUrl() + "/trades/1");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("mismatch in status code", 200, httpResponse.getStatusLine().getStatusCode());
        String responseText = EntityUtils.toString(httpResponse.getEntity());
        assertEquals("[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"}]", responseText);
    }

    @Test
    public void serverWith_ClientAuthEnabled_Rejects_UnAuthenticatedClients() throws Exception
    {
        // Create a server with client auth enabled
        this.wireMockServer = WireMockServerWrapper.with(port, "showcase/json")
                .withServerKeystore(SERVER_KEYSTORE_PATH, CHANGEIT_PASSWORD, CHANGEIT_PASSWORD)
                .withCAKeyStore(CA_KEYSTORE_PATH, CHANGEIT_PASSWORD)
                .withClientAuth(true)
                .build();
        wireMockServer.start();

        // Create a http client which DOEST NOT present a certificate ... this is not ok because the server does require client authentication
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(SSLContextBuilder.create()
                        .loadTrustMaterial(new TrustAllStrategy())
                        .build())
                .build();

        try
        {
            HttpGet httpGet = new HttpGet(wireMockServer.baseUrl() + "/trades/1");
            httpClient.execute(httpGet);
            fail("failed to get exception");
        }
        catch (SSLHandshakeException e)
        {
            assertEquals("Received fatal alert: bad_certificate", e.getMessage());
        }
    }

    @Test
    public void serverWith_ClientAuthEnabled_Allows_AuthenticatedClients() throws Exception
    {
        // Create a server with client auth enabled
        this.wireMockServer = WireMockServerWrapper.with(port, "showcase/json")
                .withServerKeystore(SERVER_KEYSTORE_PATH, CHANGEIT_PASSWORD, CHANGEIT_PASSWORD)
                .withCAKeyStore(CA_KEYSTORE_PATH, CHANGEIT_PASSWORD)
                .withClientAuth(true)
                .build();
        wireMockServer.start();

        // Create a http client which DOES present a certificate ... this is ok because the server does require client authentication
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(SSLContextBuilder.create()
                        .loadKeyMaterial(new File(SERVER_KEYSTORE_PATH), CHANGEIT_PASSWORD.toCharArray(), CHANGEIT_PASSWORD.toCharArray())
                        .loadTrustMaterial(new TrustAllStrategy())
                        .build())
                .build();

        HttpGet httpGet = new HttpGet(wireMockServer.baseUrl() + "/trades/1");
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

        assertEquals("mismatch in status code", 200, httpResponse.getStatusLine().getStatusCode());
        String responseText = EntityUtils.toString(httpResponse.getEntity());
        assertEquals("[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"}]", responseText);
    }
}

