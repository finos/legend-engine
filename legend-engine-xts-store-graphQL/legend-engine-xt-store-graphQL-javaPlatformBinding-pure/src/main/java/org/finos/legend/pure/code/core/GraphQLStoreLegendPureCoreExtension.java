// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.code.core;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.code.core.StoreLegendPureCoreExtension;

public class GraphQLStoreLegendPureCoreExtension implements StoreLegendPureCoreExtension
{
    @Override
    public String functionFile()
    {
        return "core_external_store_graphql_java_platform_binding/graphQLStoreLegendJavaPlatformBindingExtension.pure";
    }

    @Override
    public String functionSignature()
    {
        return "meta::external::store::graphQL::executionPlan::platformBinding::legendJava::graphQLStoreOnlyLegendJavaPlatformBindingExtensions__Extension_MANY_";
    }

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Store", "GraphQL");
    }
}
