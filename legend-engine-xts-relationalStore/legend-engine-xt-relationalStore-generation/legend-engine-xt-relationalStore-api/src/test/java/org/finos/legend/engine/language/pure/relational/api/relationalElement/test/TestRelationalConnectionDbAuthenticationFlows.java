//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.relational.api.relationalElement.test;

import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.IRelationalCompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.DbTypeDataSourceAuth;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.RelationalConnectionDbAuthenticationFlows;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TestRelationalConnectionDbAuthenticationFlows
{
    private static final RelationalConnectionDbAuthenticationFlows dbDatasourceAuth = new RelationalConnectionDbAuthenticationFlows();
    private static final DatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration = new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration();
    private static final LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();

    @Test
    public void testValidDbTypeDataSourceAuthCombinations()
    {
        flowProvider.configure(flowProviderConfiguration);
        Set<DbTypeDataSourceAuth> dbTypeDataSourceAuthCombinations = new HashSet<>(dbDatasourceAuth.getDbTypeDataSourceAndAuthCombos(flowProvider.getFlows()));
        Assert.assertTrue(dbTypeDataSourceAuthCombinations.containsAll(new HashSet<>(Arrays.asList(new DbTypeDataSourceAuth("Postgres", "static", "middleTierUserNamePassword"), new DbTypeDataSourceAuth("Trino", "Trino", "TrinoDelegatedKerberosAuth"), new DbTypeDataSourceAuth("BigQuery", "bigQuery", "gcpWorkloadIdentityFederation"), new DbTypeDataSourceAuth("Spanner", "spanner", "gcpApplicationDefaultCredentials")))));
    }

    @Test
    public void testDbTypeDataSourceAuthCombinationsMatchValidFlowKeys()
    {
        flowProvider.configure(flowProviderConfiguration);
        Set<DbTypeDataSourceAuth> dbTypeDataSourceAuthCombinations = new HashSet<>(dbDatasourceAuth.getDbTypeDataSourceAndAuthCombos(flowProvider.getFlows()));
        Set<DatabaseAuthenticationFlowKey> flowKeys = CompilerExtensions.fromAvailableExtensions().getExtensions().stream().filter(ext -> ext instanceof IRelationalCompilerExtension).map(ext -> ((IRelationalCompilerExtension) ext).getFlowKeys()).flatMap(Collection::stream).collect(Collectors.toSet());
        Set<DbTypeDataSourceAuth> validAuthenticationFlows = new HashSet<>();
        for (DatabaseAuthenticationFlowKey flowKey : flowKeys)
        {
            validAuthenticationFlows.add(new DbTypeDataSourceAuth(flowKey.getDatabaseType().name(), RelationalConnectionDbAuthenticationFlows.getNameFromClass(flowKey.getDatasourceProtocolSpecClass()), RelationalConnectionDbAuthenticationFlows.getNameFromClass(flowKey.getAuthStrategyProtocolSpecClass())));
        }
        Assert.assertTrue(dbTypeDataSourceAuthCombinations.containsAll(validAuthenticationFlows));
    }
}
