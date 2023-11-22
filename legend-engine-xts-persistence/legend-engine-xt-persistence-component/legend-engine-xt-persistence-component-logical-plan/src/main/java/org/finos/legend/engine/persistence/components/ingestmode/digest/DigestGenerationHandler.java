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

package org.finos.legend.engine.persistence.components.ingestmode.digest;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.List;
import java.util.stream.Collectors;

public class DigestGenerationHandler implements DigestGenStrategyVisitor<Void>
{
    private List<Value> fieldsToSelect;
    private List<Value> fieldsToInsert;
    private Dataset stagingDataset;
    private Dataset mainDataset;

    public DigestGenerationHandler(Dataset mainDataset, Dataset stagingDataset, List<Value> fieldsToSelect, List<Value> fieldsToInsert)
    {
        this.mainDataset = mainDataset;
        this.stagingDataset = stagingDataset;
        this.fieldsToSelect = fieldsToSelect;
        this.fieldsToInsert = fieldsToInsert;
    }

    @Override
    public Void visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
    {
        return null;
    }

    @Override
    public Void visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
    {
        Value digestValue = DigestUdf
            .builder()
            .udfName(udfBasedDigestGenStrategy.digestUdfName())
            .addAllFieldNames(stagingDataset.schemaReference().fieldValues().stream().map(fieldValue -> fieldValue.fieldName()).collect(Collectors.toList()))
            .addAllValues(fieldsToSelect)
            .dataset(stagingDataset)
            .build();
        String digestField = udfBasedDigestGenStrategy.digestField();
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(digestField).build());
        fieldsToSelect.add(digestValue);
        return null;
    }

    @Override
    public Void visitUserProvidedDigestGenStrategy(UserProvidedDigestGenStrategyAbstract userProvidedDigestGenStrategy)
    {
        return null;
    }
}
