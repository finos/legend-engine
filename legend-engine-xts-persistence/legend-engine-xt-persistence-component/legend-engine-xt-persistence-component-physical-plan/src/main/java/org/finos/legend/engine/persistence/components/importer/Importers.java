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

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.ResultData;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.Transformer;

public class Importers
{
    private Importers()
    {
    }

    public static <C extends PhysicalPlanNode, P extends PhysicalPlan<C>, R extends ResultData>
    Importer forExternalDatasetReference(ExternalDatasetReference externalDatasetReference, Transformer<C, P> transformer, Executor<C, R, P> executor)
    {
        if (externalDatasetReference instanceof JsonExternalDatasetReference)
        {
            return new JsonDataImporter<>(transformer, executor);
        }
        else if (externalDatasetReference instanceof CsvExternalDatasetReference)
        {
            return new CsvDataImporter<>(transformer, executor);
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized external dataset reference type: " + externalDatasetReference.getClass());
        }
    }
}
