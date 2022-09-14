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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.ResultData;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.optimizer.CaseConversionOptimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.DigestContext;
import org.finos.legend.engine.persistence.components.util.DigestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

class JsonDataImporter<C extends PhysicalPlanNode, P extends PhysicalPlan<C>, R extends ResultData> implements Importer
{
    private final Transformer<C, P> transformer;
    private final Executor<C, R, P> executor;

    JsonDataImporter(Transformer<C, P> transformer, Executor<C, R, P> executor)
    {
        this.transformer = transformer;
        this.executor = executor;
    }

    @Override
    public void importData(ExternalDatasetReference externalDatasetReference, DigestInfo digestInfo)
    {
        if (!(externalDatasetReference instanceof JsonExternalDatasetReference))
        {
            throw new IllegalArgumentException("Input to CSV data importer is of type " + externalDatasetReference.getClass());
        }

        JsonExternalDatasetReference jsonExternalDatasetReference = (JsonExternalDatasetReference) externalDatasetReference;

        boolean populateDigest = digestInfo != null && digestInfo.populateDigest();
        String jsonData = jsonExternalDatasetReference.data();
        List<Field> schemaFields = jsonExternalDatasetReference.schema().fields();
        List<List<Value>> valuesToInsert = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> rows;
        try
        {
            rows = Arrays.asList(mapper.readValue(jsonData, Map[].class));
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }

        for (Map<String, Object> row : rows)
        {
            List<Value> values = new ArrayList<>();
            Object[] objArray = new Object[schemaFields.size()];
            int index = 0;
            for (Field field : schemaFields)
            {
                if (populateDigest && digestInfo.digestField().orElse("").equals(field.name()))
                {
                    continue;
                }
                Object value = row.get(field.name());
                String stringValue = value == null ? null : String.valueOf(value);
                values.add(StringValue.of(stringValue));
                if (populateDigest)
                {
                    objArray[index++] = value;
                }
            }
            if (populateDigest)
            {
                //TODO: ledav -- confirm we *actually* want to capitalize (vs decapitalize if this is a lower case optimizer)
                boolean convertFieldNamesToUpperCase = transformer.options().optimizers().stream().anyMatch(opt -> opt instanceof CaseConversionOptimizer);
                DigestContext context = DigestUtils.getDigestContext(jsonExternalDatasetReference.schema(), digestInfo.metaFields());
                String digest = DigestUtils.getDigest(objArray, context, convertFieldNamesToUpperCase);
                values.add(StringValue.of(digest));
            }
            valuesToInsert.add(values);
        }

        List<FieldValue> fieldValues = jsonExternalDatasetReference.getDatasetDefinition().schemaReference().fieldValues();
        List<Value> fieldsToInsert = new ArrayList<>(fieldValues);
        if (rows.size() > 0)
        {
            LogicalPlan insertPlan = LogicalPlanFactory.getInsertPlan(jsonExternalDatasetReference, fieldsToInsert, valuesToInsert, fieldsToInsert.size());
            P insertPhysicalPlan = transformer.generatePhysicalPlan(insertPlan);
            executor.executePhysicalPlan(insertPhysicalPlan);
        }
    }
}
