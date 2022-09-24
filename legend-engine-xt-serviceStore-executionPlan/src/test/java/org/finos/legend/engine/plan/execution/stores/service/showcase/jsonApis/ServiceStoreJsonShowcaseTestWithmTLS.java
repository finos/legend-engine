// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.service.showcase.jsonApis;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.service.config.ServiceStoreExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStore;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class ServiceStoreJsonShowcaseTestWithmTLS extends ServiceStoreTestSuite
{
    private static String pureGrammar;
    private static PlanExecutor planExecutor;

    @BeforeClass
    public static void setup() throws Exception
    {
        /*
            This test runs a shell script that generates certs.
            The shell script needs to be ported to Windows to use Windows versions of OpenSSL exe etc.
            For now, the test is run only Linux. You can run the test on Windows by generating the certs as described in certs.sh.
         */
        /*if (System.getProperty("os.name").toLowerCase().contains("windows"))
        {
            assumeTrue(false);
        }

        CertGenerator.Certs certs = new CertGenerator().generateCerts();
        String caKeyStorePath = certs.caKeyStorePath;
        String serverKeyStorePath = certs.serverKeyStorePath;*/

        // Uncomment for running on Windows

        String serverKeyStorePath = "D:\\ephrim-idea-projects\\certs\\certs4\\generated\\serverkeystore.jks";
        String caKeyStorePath = "D:\\ephrim-idea-projects\\certs\\certs4\\generated\\cakeystore.jks";

        setupServerWithMTLS("showcase/json", serverKeyStorePath, caKeyStorePath);

        System.setProperty("javax.net.ssl.keyStore", serverKeyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");

        System.setProperty("javax.net.ssl.trustStore", caKeyStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::ShowcaseServiceStore;\n" +
                        "    baseUrl : 'https://localhost:" + getPort() + "';\n" +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/showcase/json/testGrammar.pure") + "\n\n" + serviceStoreConnection;

        ServiceStoreExecutionConfiguration configuration = new ServiceStoreExecutionConfiguration();

        // enable feature flag
        configuration.setPropagateJVMSSLContext(true);
        // include URIs that require mTLS
        configuration.setMtlsServiceUriPrefixes(FastList.newListWith("https://localhost"));
        StoreExecutor serviceStoreExecutor = ServiceStore.build(configuration);
        planExecutor = PlanExecutor.newPlanExecutor(serviceStoreExecutor, InMemory.build());
    }

    @Test
    public void serviceStoreSimpleExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Trade.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Trade {\n" +
                "               s_tradeId,\n" +
                "               s_traderDetails,\n" +
                "               s_tradeDetails\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Trade {\n" +
                "               s_tradeId,\n" +
                "               s_traderDetails,\n" +
                "               s_tradeDetails\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"31:200\"},{\"s_tradeId\":\"3\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"30:300\"},{\"s_tradeId\":\"4\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"31:400\"}]}";

        Assert.assertEquals(expectedRes, executePlan(plan, planExecutor));
    }
}
