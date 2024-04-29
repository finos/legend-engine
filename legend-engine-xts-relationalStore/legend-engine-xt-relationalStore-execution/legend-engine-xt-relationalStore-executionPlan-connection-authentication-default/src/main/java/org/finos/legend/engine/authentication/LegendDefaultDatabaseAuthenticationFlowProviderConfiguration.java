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

import org.finos.legend.engine.authentication.cloud.AWSConfig;
import org.finos.legend.engine.authentication.cloud.GCPWorkloadConfig;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;

public final class LegendDefaultDatabaseAuthenticationFlowProviderConfiguration extends DatabaseAuthenticationFlowProviderConfiguration
{
    private AWSConfig awsConfig;
    private GCPWorkloadConfig gcpWorkloadConfig;

    public LegendDefaultDatabaseAuthenticationFlowProviderConfiguration()
    {
        // jackson
    }

    public LegendDefaultDatabaseAuthenticationFlowProviderConfiguration(Builder builder)
    {
        this.awsConfig = builder.awsConfig;
        this.gcpWorkloadConfig = builder.gcpWorkloadConfig;
    }

    public static class Builder
    {
        private AWSConfig awsConfig;
        private GCPWorkloadConfig gcpWorkloadConfig;

        public static Builder newInstance()
        {
            return new Builder();
        }

        private Builder()
        {
        }

        public Builder withAwsConfig(AWSConfig awsConfig)
        {
            this.awsConfig = awsConfig;
            return this;
        }

        public Builder withGcpWorkloadConfig(GCPWorkloadConfig gcpWorkloadConfig)
        {
            this.gcpWorkloadConfig = gcpWorkloadConfig;
            return this;
        }

        public LegendDefaultDatabaseAuthenticationFlowProviderConfiguration build()
        {
            return new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration(this);
        }
    }


    public AWSConfig getAwsConfig()
    {
        return awsConfig;
    }

    public GCPWorkloadConfig getGcpWorkloadConfig()
    {
        return gcpWorkloadConfig;
    }
}