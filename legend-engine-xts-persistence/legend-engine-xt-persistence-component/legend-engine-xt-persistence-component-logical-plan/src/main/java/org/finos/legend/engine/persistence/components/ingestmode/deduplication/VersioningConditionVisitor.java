// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode.deduplication;

import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;


public class VersioningConditionVisitor implements VersioningStrategyVisitor<Condition>
{

    Dataset mainDataset;
    Dataset stagingDataset;
    boolean invertComparison;
    String digestField;

    public VersioningConditionVisitor(Dataset mainDataset, Dataset stagingDataset, boolean invertComparison, String digestField)
    {
        this.mainDataset = mainDataset;
        this.stagingDataset = stagingDataset;
        this.invertComparison = invertComparison;
        this.digestField = digestField;
    }

    @Override
    public Condition visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
    {
        if (invertComparison)
        {
            return LogicalPlanUtils.getDigestMatchCondition(mainDataset, stagingDataset, digestField);
        }
        else
        {
            return LogicalPlanUtils.getDigestDoesNotMatchCondition(mainDataset, stagingDataset, digestField);
        }
    }

    @Override
    public Condition visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        FieldValue mainVersioningField = FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(maxVersionStrategy.versioningField()).build();
        FieldValue stagingVersioningField = FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(maxVersionStrategy.versioningField()).build();

        switch (maxVersionStrategy.versioningComparator())
        {
            case GREATER_THAN:
                if (invertComparison)
                {
                    return LessThanEqualTo.of(stagingVersioningField, mainVersioningField);
                }
                else
                {
                    return GreaterThan.of(stagingVersioningField, mainVersioningField);
                }
            case GREATER_THAN_EQUAL_TO:
                if (invertComparison)
                {
                    return LessThan.of(stagingVersioningField, mainVersioningField);
                }
                else
                {
                    return GreaterThanEqualTo.of(stagingVersioningField, mainVersioningField);
                }
            default:
                throw new IllegalStateException("Unsupported versioning comparator type");
        }
    }
}
