// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.sql;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class TestRetry
{
    private static final String EXECUTE_QUERY_PATH = "/api/sql/v1/execution/executeQueryString";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void testLegendHttpRetrySuccess() throws Exception
    {
        stubFor(post(EXECUTE_QUERY_PATH)
                .inScenario("Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(502))
                .willSetStateTo("Retried"));

        stubFor(post(EXECUTE_QUERY_PATH)
                .inScenario("Retry")
                .whenScenarioStateIs("Retried")
                .willReturn(aResponse().withStatus(200).withBody("success")));

        LegendHttpClient client = new LegendHttpClient("http", "localhost", String.valueOf(wireMockRule.port()));
        String content = IOUtils.toString(client.executeQueryApi("SELECT 1"), StandardCharsets.UTF_8);

        Assert.assertEquals("success", content);
        verify(2, postRequestedFor(urlEqualTo(EXECUTE_QUERY_PATH)));
    }

    @Test
    public void testLegendHttpRetryFailAfterMax()
    {
        stubFor(post(EXECUTE_QUERY_PATH).willReturn(aResponse().withStatus(502)));

        LegendHttpClient client = new LegendHttpClient("http", "localhost", String.valueOf(wireMockRule.port()));

        try
        {
            client.executeQueryApi("SELECT 1");
            Assert.fail("Expected LegendTdsClientException");
        }
        catch (LegendTdsClientException e)
        {
            Assert.assertTrue(e.getMessage().contains("Unable to parse json"));
        }

        // 6 requests: initial + 5 retries
        verify(6, postRequestedFor(urlEqualTo(EXECUTE_QUERY_PATH)));
    }
}
