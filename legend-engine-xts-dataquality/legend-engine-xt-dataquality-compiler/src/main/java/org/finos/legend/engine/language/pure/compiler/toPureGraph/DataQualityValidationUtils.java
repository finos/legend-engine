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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityRelationComparison;
import org.finos.legend.engine.protocol.dataquality.metamodel.MD5HashStrategy;
import org.finos.legend.engine.protocol.dataquality.metamodel.ReconStrategy;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;

import java.util.List;
import java.util.Set;

public class DataQualityValidationUtils
{
    /**
     * Extracts the set of column names from the return type of a compiled relation lambda.
     * The lambda is expected to return a RelationType, which is enforced by the compiler.
     */
    private static MutableSet<String> extractColumnNamesFromRelationLambda(LambdaFunction<?> compiledLambda)
    {
        RelationType<?> relationType = (RelationType<?>) compiledLambda
                ._expressionSequence().getLast()
                ._genericType()._typeArguments().getOnly()
                ._rawType();
        return Sets.mutable.ofAll(relationType._columns().collect(col -> ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column<?, ?>) col)._name()));
    }

    /**
     * Validates that every key listed in the comparison appears in both source and target column sets.
     */
    private static void validateKeysExistInBothDatasets(
            List<String> keys,
            Set<String> sourceColumns,
            Set<String> targetColumns,
            SourceInformation sourceInformation,
            String fieldName
            )
    {
        if (keys == null || keys.isEmpty())
        {
            return;
        }
        MutableSet<String> missingInSource = Sets.mutable.empty();
        MutableSet<String> missingInTarget = Sets.mutable.empty();
        for (String key : keys)
        {
            if (!sourceColumns.contains(key))
            {
                missingInSource.add(key);
            }
            if (!targetColumns.contains(key))
            {
                missingInTarget.add(key);
            }
        }
        if (missingInSource.notEmpty())
        {
            throw new EngineException(
                    fieldName + " column(s) " + missingInSource + " not found in the source relation",
                    sourceInformation,
                    EngineErrorType.COMPILATION);
        }
        if (missingInTarget.notEmpty())
        {
            throw new EngineException(
                    fieldName + " column(s) " + missingInTarget + " not found in the target relation",
                    sourceInformation,
                    EngineErrorType.COMPILATION);
        }
    }

    /**
     * When the strategy is MD5Hash, validates that the configured sourceHashColumn
     * exists in the source dataset and the targetHashColumn exists in the target dataset.
     */
    private static void validateHashColumnsIfApplicable(
            ReconStrategy strategy,
            Set<String> sourceColumns,
            Set<String> targetColumns,
            SourceInformation sourceInformation)
    {
        if (strategy == null || !(strategy instanceof MD5HashStrategy))
        {
            return;
        }
        MD5HashStrategy hashStrategy = (MD5HashStrategy) strategy;

        if (StringUtils.isNotBlank(hashStrategy.sourceHashColumn) && !sourceColumns.contains(hashStrategy.sourceHashColumn))
        {
            throw new EngineException(
                    "sourceHashColumn '" + hashStrategy.sourceHashColumn + "' is not present in the source relation",
                    sourceInformation,
                    EngineErrorType.COMPILATION);
        }

        if (hashStrategy.targetHashColumn != null
                && !hashStrategy.targetHashColumn.isEmpty()
                && !targetColumns.contains(hashStrategy.targetHashColumn))
        {
            throw new EngineException(
                    "targetHashColumn '" + hashStrategy.targetHashColumn + "' is not present in the target relation",
                    sourceInformation,
                    EngineErrorType.COMPILATION);
        }
    }

    /**
     * Convenience method that runs all relation-comparison validations in one call.
     */
    public static void runAllRelationComparisonChecks(
            DataQualityRelationComparison comparison,
            LambdaFunction<?> compiledSource,
            LambdaFunction<?> compiledTarget,
            SourceInformation sourceInformation)
    {
        Set<String> srcCols = extractColumnNamesFromRelationLambda(compiledSource);
        Set<String> tgtCols = extractColumnNamesFromRelationLambda(compiledTarget);

        validateKeysExistInBothDatasets(comparison.keys, srcCols, tgtCols, sourceInformation, "keys");
        validateKeysExistInBothDatasets(comparison.columnsToCompare, srcCols, tgtCols, sourceInformation, "columnsToCompare");
        validateHashColumnsIfApplicable(comparison.strategy, srcCols, tgtCols, sourceInformation);
    }
}