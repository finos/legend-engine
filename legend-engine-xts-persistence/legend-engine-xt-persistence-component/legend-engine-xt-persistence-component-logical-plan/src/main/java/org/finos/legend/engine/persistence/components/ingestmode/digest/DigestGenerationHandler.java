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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DigestGenerationHandler implements DigestGenStrategyVisitor<Void>
{
    private List<Value> fieldsToSelect;
    private List<Value> fieldsToInsert;
    private List<DataType> fieldTypes;
    private Dataset mainDataset;

    public DigestGenerationHandler(Dataset mainDataset, List<Value> fieldsToSelect, List<Value> fieldsToInsert, List<DataType> fieldTypes)
    {
        this.mainDataset = mainDataset;
        this.fieldsToSelect = fieldsToSelect;
        this.fieldsToInsert = fieldsToInsert;
        this.fieldTypes = fieldTypes;
    }

    @Override
    public Void visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
    {
        return null;
    }

    @Override
    public Void visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
    {
        Set<String> fieldsToExclude = udfBasedDigestGenStrategy.fieldsToExcludeFromDigest();
        List<String> filteredStagingFieldNames = new ArrayList<>();
        List<Value> filteredStagingFieldValues = new ArrayList<>();
        List<DataType> filteredStagingFieldTypes = new ArrayList<>();

        for (int i = 0; i < fieldsToSelect.size(); i++)
        {
            Value value = fieldsToSelect.get(i);
            DataType dataType = fieldTypes.get(i);
            if (value instanceof FieldValue)
            {
                FieldValue fieldValue = (FieldValue) value;
                if (!fieldsToExclude.contains(fieldValue.fieldName()))
                {
                    filteredStagingFieldNames.add(fieldValue.fieldName());
                    filteredStagingFieldValues.add(fieldValue);
                    filteredStagingFieldTypes.add(dataType);
                }
            }
            else if (value instanceof StagedFilesFieldValue)
            {
                StagedFilesFieldValue stagedFilesFieldValue = (StagedFilesFieldValue) value;
                if (!fieldsToExclude.contains(stagedFilesFieldValue.fieldName()))
                {
                    filteredStagingFieldNames.add(stagedFilesFieldValue.fieldName());
                    filteredStagingFieldValues.add(stagedFilesFieldValue);
                    filteredStagingFieldTypes.add(dataType);
                }
            }
            else
            {
                throw new IllegalStateException("Value can either be a FieldValue or StagedFilesFieldValue for UDF based digest generation");
            }
        }

        Value digestValue = DigestUdf
            .builder()
            .udfName(udfBasedDigestGenStrategy.digestUdfName())
            .addAllFieldNames(filteredStagingFieldNames)
            .addAllValues(filteredStagingFieldValues)
            .addAllFieldTypes(filteredStagingFieldTypes)
            .putAllTypeConversionUdfNames(udfBasedDigestGenStrategy.typeConversionUdfNames())
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
