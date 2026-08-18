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

package org.finos.legend.engine.testable.helper;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionOptionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_featureFlag_FeatureFlagOption_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;

public class TestExecutionContextHelper
{
    private static final String EXECUTION_OPTION_CONTEXT = "meta::pure::executionPlan::ExecutionOptionContext";
    private static final String FEATURE_FLAG_OPTION = "meta::pure::executionPlan::featureFlag::FeatureFlagOption";
    private static final String FEATURE = "meta::pure::executionPlan::features::Feature";

    private TestExecutionContextHelper()
    {
    }

    /**
     * Relation-returning tests execute against DuckDB rather than H2 (see the relational
     * connection factory), and are generated with Pure's substring indexing rather than the
     * historical pass-through. This is where users see the difference between the two.
     */
    public static Root_meta_pure_runtime_ExecutionContext executionContextFor(TestConnectionBuildParameters hints, PureModel pureModel)
    {
        if (hints == null || !hints.isRelation())
        {
            return null;
        }
        return new Root_meta_pure_executionPlan_ExecutionOptionContext_Impl("", null, pureModel.getClass(EXECUTION_OPTION_CONTEXT))
                ._executionOptionsAdd(new Root_meta_pure_executionPlan_featureFlag_FeatureFlagOption_Impl("", null, pureModel.getClass(FEATURE_FLAG_OPTION))
                        ._flagsAdd(pureModel.getEnumValue(FEATURE, "CORRECT_SQL_SUBSTRING_INDEXING")));
    }
}
