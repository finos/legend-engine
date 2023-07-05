// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.importer;

import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.ResultData;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDatasetReference;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.Transformer;

class CsvDataImporter<C extends PhysicalPlanNode, P extends PhysicalPlan<C>, R extends ResultData> implements Importer
{
    private final Transformer<C, P> transformer;
    private final Executor<C, R, P> executor;

    CsvDataImporter(Transformer<C, P> transformer, Executor<C, R, P> executor)
    {
        this.transformer = transformer;
        this.executor = executor;
    }

    @Override
    public void importData(ExternalDatasetReference csvExternalDatasetReference, DigestInfo digestInfo)
    {
        if (!(csvExternalDatasetReference instanceof CsvExternalDatasetReference))
        {
            throw new IllegalArgumentException("Input to CSV data importer is of type " + csvExternalDatasetReference.getClass());
        }
        LogicalPlan csvLoadLogicalPlan = LogicalPlanFactory.getLoadCsvPlan((CsvExternalDatasetReference) csvExternalDatasetReference);
        P csvLoadPhysicalPlan = transformer.generatePhysicalPlan(csvLoadLogicalPlan);
        executor.executePhysicalPlan(csvLoadPhysicalPlan);
    }
}
