// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.code.core;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.code.core.FeatureLegendPureCoreExtension;

public class SQLLegendPureCoreExtension implements FeatureLegendPureCoreExtension
{
    @Override
    public String functionFile()
    {
        return "core_external_query_sql_expression/frameworkExtension.pure";
    }

    @Override
    public String functionSignature()
    {
        return "meta::external::query::sql::expression::sqlExpressionFeatureExtension__Extension_1_";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Query", "SQL");
    }
}
