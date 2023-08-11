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

package org.finos.legend.engine.language.pure.relational.api.relationalElement;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RelationalConnectionDbAuthenticationFlows
{
    public static String getNameFromClass(Class<?> obj)
    {
        String[] result = new String[1];
        result[0] = " ";
        PureProtocolExtensionLoader.extensions().forEach(extension ->
                LazyIterate.flatCollect(extension.getExtraProtocolSubTypeInfoCollectors(), Function0::value).forEach(info ->
                        info.getSubTypes().forEach(subType ->
                        {
                            if (obj.getName().equals(subType.getOne().getName()))
                            {
                                result[0] = subType.getTwo();
                            }
                        })));
        return result[0];
    }

    public List<DbTypeDataSourceAuth> getDbTypeDataSourceAndAuthCombos(Map<DatabaseAuthenticationFlowKey, DatabaseAuthenticationFlow> flows)
    {
        return flows.values().stream().map(
                extension ->
                {
                    String dbType = extension.getDatabaseType().name();
                    String dataSource = getNameFromClass(extension.getDatasourceClass());
                    String authStrategy = getNameFromClass(extension.getAuthenticationStrategyClass());
                    return new DbTypeDataSourceAuth(dbType, dataSource, authStrategy);
                }
        ).collect(Collectors.toList());
    }
}
