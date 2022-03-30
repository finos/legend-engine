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

package org.finos.legend.engine.authentication;

import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;

public final class LegendDefaultDatabaseAuthenticationFlowProviderConfiguration extends DatabaseAuthenticationFlowProviderConfiguration
{
    public AWSConfig awsConfig;
    public GCPWorkloadConfig gcpWorkloadConfig;

    public static class AWSConfig {
        public String region;
        public String accountId;
        public String role;
        public String awsAccessKeyIdVaultReference;
        public String awsSecretAccessKeyVaultReference;

        public AWSConfig() {
        }
    }

    public static class GCPWorkloadConfig {
        public String projectNumber;
        public String poolId;
        public String providerId;

        public GCPWorkloadConfig() {
        }
    }

    public LegendDefaultDatabaseAuthenticationFlowProviderConfiguration()
    {
        // jackson
    }
}