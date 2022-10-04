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

public class BigQueryTestDatabaseAuthenticationFlowProviderConfiguration extends DatabaseAuthenticationFlowProviderConfiguration
{
    private AWSConfig awsConfig;
    private GCPWorkloadConfig gcpWorkloadConfig;

    public AWSConfig getAwsConfig() {
        return awsConfig;
    }

    public GCPWorkloadConfig getGcpWorkloadConfig() {
        return gcpWorkloadConfig;
    }

    private void setAwsConfig(AWSConfig awsConfig) {
        this.awsConfig = awsConfig;
    }

    private void setGcpWorkloadConfig(GCPWorkloadConfig gcpWorkloadConfig) {
        this.gcpWorkloadConfig = gcpWorkloadConfig;
    }

    public static class Builder {
        BigQueryTestDatabaseAuthenticationFlowProviderConfiguration configuration;
        public Builder(){
            configuration = new BigQueryTestDatabaseAuthenticationFlowProviderConfiguration();
        }
        public static Builder newInstance(){
            return new Builder();
        }

        public Builder withAwsConfig(AWSConfig awsConfig){
            configuration.setAwsConfig(awsConfig);
            return this;
        }

        public Builder withGcpWorkloadConfig(GCPWorkloadConfig gcpWorkloadConfig){
            configuration.setGcpWorkloadConfig(gcpWorkloadConfig);
            return this;
        }

        public BigQueryTestDatabaseAuthenticationFlowProviderConfiguration build(){
            return configuration;
        }
    }

}
