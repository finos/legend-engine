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

import org.junit.BeforeClass;

import static org.junit.Assume.assumeTrue;

/*
    This class is a test/demo of mTLS.
    It exercises various scenarios of a client and server communicating with mTLS.
 */
public class MTLSTestAbstract
{
    public static String SERVER_KEYSTORE_PATH = "serverkeystore.jks";
    public static String CLIENT_KEYSTORE_PATH = "clientkeystore.jks";
    public static String CA_KEYSTORE_PATH = "cakeystore.jks";
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
        CLIENT_KEYSTORE_PATH = certs.clientKeyStorePath;
        CA_KEYSTORE_PATH = certs.caKeyStorePath;
    }
}

