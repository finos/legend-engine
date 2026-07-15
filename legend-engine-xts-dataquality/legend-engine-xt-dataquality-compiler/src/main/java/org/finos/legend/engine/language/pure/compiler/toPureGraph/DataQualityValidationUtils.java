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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityRelationComparison;
import org.finos.legend.engine.protocol.dataquality.metamodel.MD5HashStrategy;
import org.finos.legend.engine.protocol.dataquality.metamodel.ReconStrategy;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;

import java.util.List;
import java.util.Set;

import static org.eclipse.collections.impl.utility.Iterate.isEmpty;

public class DataQualityValidationUtils
{
    /**
     * Extracts the set of column names from the return type of a compiled relation lambda.
     * The lambda is expected to return a RelationType, which is enforced by the compiler.
     */
    private static MutableSet<String> extractColumnNamesFromRelationLambda(LambdaFunction<?> compiledLambda)
    {
        RelationType<?> relationType = extractRelationTypeFromLambda(compiledLambda);
        if (relationType == null)
        {
            throw new IllegalStateException("Expected relation lambda to return a RelationType");
        }
        return columnsOf(relationType).collect(Column::_name, Sets.mutable.empty());
    }

    /**
     * Validates that the given list has no duplicate entries (case-insensitive).
     */
    private static void validateNoDuplicates(
            List<String> values,
            SourceInformation sourceInformation,
            String fieldName)
    {
        if (values == null || values.isEmpty())
        {
            return;
        }
        MutableSet<String> seenLowerCased = Sets.mutable.empty();
        MutableSet<String> duplicates = Sets.mutable.empty();
        for (String value : values)
        {
            String key = value.toLowerCase();
            if (!seenLowerCased.add(key))
            {
                duplicates.add(value);
            }
        }
        if (duplicates.notEmpty())
        {
            throw new EngineException(
                    "Duplicate " + fieldName + " column(s) found: " + duplicates,
                    sourceInformation,
                    EngineErrorType.COMPILATION);
        }
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

        validateNoDuplicates(comparison.keys, sourceInformation, "keys");
        validateNoDuplicates(comparison.columnsToCompare, sourceInformation, "columnsToCompare");
        validateKeysExistInBothDatasets(comparison.keys, srcCols, tgtCols, sourceInformation, "keys");
        validateKeysExistInBothDatasets(comparison.columnsToCompare, srcCols, tgtCols, sourceInformation, "columnsToCompare");
        validateHashColumnsIfApplicable(comparison.strategy, srcCols, tgtCols, sourceInformation);
    }

    public static List<String> findFloatingPointReconColumns(
            DataQualityRelationComparison comparison,
            LambdaFunction<?> compiledSource,
            PureModel pureModel)
    {
        RelationType<?> sourceRelationType = extractRelationTypeFromLambda(compiledSource);
        if (sourceRelationType == null)
        {
            return Lists.mutable.empty();
        }

        Type floatType = pureModel.getType_safe("Float");
        ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
        MutableSet<String> reconColumnNames = resolveReconColumnNames(comparison, sourceRelationType);

        return columnsOf(sourceRelationType)
                .select(column -> reconColumnNames.contains(column._name()))
                .select(column -> isFloatingPointColumn(column, floatType, processorSupport))
                .collect(Column::_name)
                .toList();
    }

    /**
     * Resolves the set of column names to check for floating-point types:
     * - If columnsToCompare is empty, all source columns are considered.
     * - Otherwise, the union of columnsToCompare and keys is considered.
     */
    private static MutableSet<String> resolveReconColumnNames(
            DataQualityRelationComparison comparison,
            RelationType<?> sourceRelationType)
    {
        if (isEmpty(comparison.columnsToCompare))
        {
            return columnsOf(sourceRelationType).collect(Column::_name, Sets.mutable.empty());
        }
        return Sets.mutable
                .withAll(comparison.columnsToCompare)
                .withAll(nullToEmpty(comparison.keys));
    }

    private static boolean isFloatingPointColumn(
            Column<?, ?> column,
            Type floatType,
            ProcessorSupport processorSupport)
    {
        Type rawType = columnRawType(column);
        return rawType != null && isFloatingPointType(rawType, floatType, processorSupport);
    }

    private static RelationType<?> extractRelationTypeFromLambda(LambdaFunction<?> compiledLambda)
    {
        Object rawType = compiledLambda
                ._expressionSequence().getLast()
                ._genericType()._typeArguments().getOnly()
                ._rawType();
        return (rawType instanceof RelationType) ? (RelationType<?>) rawType : null;
    }

    private static RichIterable<Column<?, ?>> columnsOf(RelationType<?> relationType)
    {
        return relationType._columns().collect(col -> (Column<?, ?>) col);
    }

    private static Type columnRawType(Column<?, ?> column)
    {
        return column._classifierGenericType()._typeArguments().getLast()._rawType();
    }

    private static boolean isFloatingPointType(
            Type colType,
            Type floatType,
            ProcessorSupport processorSupport)
    {
        return floatType != null && org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(colType, floatType, processorSupport);
    }

    private static List<String> nullToEmpty(List<String> list)
    {
        return list == null ? Lists.mutable.empty() : list;
    }
}