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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.DeriveMainDatasetSchemaFromStaging;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetsCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;

import java.util.List;

public class ApiUtils
{
    public static Dataset deriveMainDatasetFromStaging(Datasets datasets, IngestMode ingestMode)
    {
        Dataset mainDataset = datasets.mainDataset();
        List<Field> mainDatasetFields = mainDataset.schema().fields();
        if (mainDatasetFields == null || mainDatasetFields.isEmpty())
        {
            mainDataset = ingestMode.accept(new DeriveMainDatasetSchemaFromStaging(datasets.mainDataset(), datasets.stagingDataset()));
        }
        return mainDataset;
    }

    public static Datasets applyCaseOnDatasets(Datasets datasets, CaseConversion caseConversion)
    {
        DatasetsCaseConverter converter = new DatasetsCaseConverter();
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return converter.applyCaseOnDatasets(datasets, String::toUpperCase);
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return converter.applyCaseOnDatasets(datasets, String::toLowerCase);
        }
        return datasets;
    }

    public static IngestMode applyCaseOnIngestMode(IngestMode ingestMode, CaseConversion caseConversion)
    {
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return ingestMode.accept(new IngestModeCaseConverter(String::toUpperCase));
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return ingestMode.accept(new IngestModeCaseConverter(String::toLowerCase));
        }
        return ingestMode;
    }
}
