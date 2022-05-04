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

package org.finos.legend.engine.plan.execution.stores.relational.config;

import io.dropwizard.Configuration;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;

import java.util.List;

public class RelationalExecutionConfiguration extends Configuration implements StoreExecutorConfiguration
{
    public TemporaryTestDbConfiguration temporarytestdb;
    public String tempPath;
    public List<OAuthProfile> oauthProfiles = Lists.mutable.empty();
    private DatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration;
    private Class<? extends DatabaseAuthenticationFlowProvider> flowProviderClass;

    @Override
    public StoreType getStoreType()
    {
        return StoreType.Relational;
    }

    public RelationalExecutionConfiguration()
    {
    }

    public  RelationalExecutionConfiguration(String tempPath)
    {
        this.tempPath = tempPath;
    }

    public DatabaseAuthenticationFlowProviderConfiguration getFlowProviderConfiguration() {
        return flowProviderConfiguration;
    }

    public Class<? extends DatabaseAuthenticationFlowProvider> getFlowProviderClass() {
        return flowProviderClass;
    }

    public TemporaryTestDbConfiguration getTemporaryTestDbConfiguration() {
        return temporarytestdb;
    }

    public static Builder newInstance()
    {
        return new Builder();
    }

    public static class Builder
    {
        public String tempPath;
        public List<OAuthProfile> oauthProfiles = Lists.mutable.empty();
        private Class<? extends DatabaseAuthenticationFlowProvider> flowProviderClass;
        private DatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration;
        private TemporaryTestDbConfiguration temporaryTestDbConfiguration;

        public Builder withTempPath(String tempPath)
        {
            this.tempPath = tempPath;
            return this;
        }

        public Builder withTemporaryTestDbConfiguration(TemporaryTestDbConfiguration temporaryTestDbConfiguration)
        {
            this.temporaryTestDbConfiguration = temporaryTestDbConfiguration;
            return this;
        }

        public Builder withOAuthProfiles(List<OAuthProfile> oauthProfiles)
        {
            this.oauthProfiles = oauthProfiles;
            return this;
        }


        public Builder withDatabaseAuthenticationFlowProvider(Class<? extends DatabaseAuthenticationFlowProvider> flowProviderClass, DatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration)
        {
            this.flowProviderClass = flowProviderClass;
            this.flowProviderConfiguration = flowProviderConfiguration;
            return this;
        }

        public Builder withDatabaseAuthenticationFlowProvider(Class<? extends DatabaseAuthenticationFlowProvider>  flowProviderClass)
        {
            this.flowProviderClass = flowProviderClass;
            this.flowProviderConfiguration = null;
            return this;
        }

        public RelationalExecutionConfiguration build()
        {
            RelationalExecutionConfiguration relationalExecutionConfiguration = new RelationalExecutionConfiguration();
            relationalExecutionConfiguration.tempPath = this.tempPath;
            relationalExecutionConfiguration.oauthProfiles = this.oauthProfiles;
            relationalExecutionConfiguration.flowProviderClass = this.flowProviderClass;
            relationalExecutionConfiguration.flowProviderConfiguration = this.flowProviderConfiguration;
            relationalExecutionConfiguration.temporarytestdb = this.temporaryTestDbConfiguration;
            return relationalExecutionConfiguration;
        }
    }
}
