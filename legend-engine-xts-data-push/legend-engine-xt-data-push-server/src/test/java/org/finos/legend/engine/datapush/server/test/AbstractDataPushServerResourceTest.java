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

package org.finos.legend.engine.datapush.server.test;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.finos.legend.engine.datapush.server.configuration.DataPushServerConfiguration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.util.Optional;

public class AbstractDataPushServerResourceTest
{
    @ClassRule
    public static final DropwizardAppRule<DataPushServerConfiguration> APP_RULE =
            new DropwizardAppRule<>(
                    DataPushServerForTest.class,
                    ResourceHelpers.resourceFilePath("config-test.yaml"));

    protected Client client;

    @Rule
    public final DataPushServerClientRule clientRule = new DataPushServerClientRule();

    @Before
    public void setUp()
    {
        configureClient();
    }

    private void configureClient()
    {
        this.client = this.clientRule.getClient();
        this.client.target(getServerUrl()).request().get();
    }

    protected DataPushServerForTest getApplicationInstance()
    {
        return APP_RULE.getApplication();
    }

    protected WebTarget clientFor(String url)
    {
        return this.client.target(getServerUrl(url));
    }

    protected String getServerUrl()
    {
        return getServerUrl(null);
    }

    protected String getServerUrl(String path)
    {
        return "http://localhost:" + APP_RULE.getLocalPort() + Optional.ofNullable(path).orElse("");
    }
}
