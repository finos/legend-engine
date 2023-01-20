/*
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

package org.finos.legend.authentication.vault.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestAWSDefaultCredentialsProviderVault
{
    @Before
    public void setup()
    {
        System.setProperty("aws.accessKeyId", "fake_id");
        System.setProperty("aws.secretAccessKey", "fake_secret");
    }

    @After
    public void cleanup()
    {
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
    }

    @Test
    public void getSecretAccessKey() throws Exception
    {
        AWSDefaultCredentialsProviderVault vault = new AWSDefaultCredentialsProviderVault();
        assertEquals("fake_id", vault.getSecret("ACCESS_KEY_ID"));
        assertEquals("fake_secret", vault.getSecret("SECRET_ACCESS_KEY"));
    }
}
*/
