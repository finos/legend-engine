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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;

import java.util.ArrayList;
import java.util.List;

public class DatasetDeduplicationHandler implements DeduplicationStrategyVisitor<Dataset>
{

    public static final String COUNT = "legend_persistence_count";

    Dataset stagingDataset;

    public DatasetDeduplicationHandler(Dataset stagingDataset)
    {
        this.stagingDataset = stagingDataset;
    }

    @Override
    public Dataset visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
    {
        return stagingDataset;
    }

    @Override
    public Dataset visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
    {
        return selectionWithGroupByAllColumns();
    }

    @Override
    public Dataset visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
    {
        return selectionWithGroupByAllColumns();
    }

    private Dataset selectionWithGroupByAllColumns()
    {
        List<Value> allColumns = new ArrayList<>(stagingDataset.schemaReference().fieldValues());
        List<Value> allColumnsWithCount = new ArrayList<>(stagingDataset.schemaReference().fieldValues());

        Value count = FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(COUNT).build();
        allColumnsWithCount.add(count);
        Selection selectionWithGroupByAllColumns = Selection.builder()
                .source(stagingDataset)
                .addAllFields(allColumnsWithCount)
                .groupByFields(allColumns)
                .build();
        return selectionWithGroupByAllColumns;
    }

}
