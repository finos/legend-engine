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

package org.finos.legend.engine.persistence.components.ingestmode.versioning;

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
        return getDigestBasedVersioningCondition();
    }

    @Override
    public Condition visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
    {
        FieldValue mainVersioningField = FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(maxVersionStrategy.versioningField()).build();
        FieldValue stagingVersioningField = FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(maxVersionStrategy.versioningField()).build();
        VersionResolver versionResolver = maxVersionStrategy.versionResolver().orElseThrow(IllegalStateException::new);
        return getVersioningCondition(mainVersioningField, stagingVersioningField, versionResolver);
    }

    @Override
    public Condition visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategy)
    {
        FieldValue mainVersioningField = FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(allVersionsStrategy.versioningField()).build();
        FieldValue stagingVersioningField = FieldValue.builder().datasetRef(stagingDataset.datasetReference()).fieldName(allVersionsStrategy.versioningField()).build();
        VersionResolver versionResolver = allVersionsStrategy.versionResolver().orElseThrow(IllegalStateException::new);
        return getVersioningCondition(mainVersioningField, stagingVersioningField, versionResolver);
    }

    private Condition getVersioningCondition(FieldValue mainVersioningField, FieldValue stagingVersioningField, VersionResolver versionResolver)
    {
        switch (versionResolver)
        {
            case GREATER_THAN_ACTIVE_VERSION:
                if (invertComparison)
                {
                    return LessThanEqualTo.of(stagingVersioningField, mainVersioningField);
                }
                else
                {
                    return GreaterThan.of(stagingVersioningField, mainVersioningField);
                }
            case GREATER_THAN_EQUAL_TO_ACTIVE_VERSION:
                if (invertComparison)
                {
                    return LessThan.of(stagingVersioningField, mainVersioningField);
                }
                else
                {
                    return GreaterThanEqualTo.of(stagingVersioningField, mainVersioningField);
                }
            case DIGEST_BASED:
                return getDigestBasedVersioningCondition();
            default:
                throw new IllegalStateException("Unsupported versioning comparator type");
        }
    }

    private Condition getDigestBasedVersioningCondition()
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
}
