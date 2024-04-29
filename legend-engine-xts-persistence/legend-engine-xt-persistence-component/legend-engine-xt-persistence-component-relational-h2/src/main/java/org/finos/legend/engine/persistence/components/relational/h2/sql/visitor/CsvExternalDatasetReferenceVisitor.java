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

package org.finos.legend.engine.persistence.components.relational.h2.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.expresssions.table.CsvRead;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class CsvExternalDatasetReferenceVisitor implements LogicalPlanVisitor<CsvExternalDatasetReference>
{

    private static final String FIELD_SEPARATOR = "fieldSeparator";
    private static final String CHARSET_UTF8 = "charset=UTF-8";

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, CsvExternalDatasetReference current, VisitorContext context)
    {
        String csvFilePath = current.csvDataPath();
        String csvOptions = FIELD_SEPARATOR + "=" + current.fieldSeparator() + WHITE_SPACE + CHARSET_UTF8;
        String csvColumnNames = current.getDatasetDefinition().schemaReference().fieldValues().stream().map(FieldValue::fieldName).collect(Collectors.joining(","));
        CsvRead csvRead = new CsvRead(csvFilePath, csvColumnNames, csvOptions);
        prev.push(csvRead);
        return new VisitorResult(csvRead);
    }
}