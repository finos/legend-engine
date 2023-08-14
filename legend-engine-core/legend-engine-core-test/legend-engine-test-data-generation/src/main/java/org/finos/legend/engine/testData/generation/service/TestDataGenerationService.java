/**
 * Copyright (c) 2020-present, Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.legend.engine.testData.generation.service;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.generated.core_relational_relational_testDataGeneration_testDataGeneration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

import java.util.Collections;
import java.util.List;

public class TestDataGenerationService
{
    public static List<EmbeddedData> generateEmbeddedData(Lambda query, Root_meta_pure_runtime_Runtime runtime, Mapping mapping, PureModel pureModel)
    {
        RichIterable<? extends Table> tables = core_relational_relational_testDataGeneration_testDataGeneration.Root_meta_relational_testDataGeneration_getTableFromQuery_FunctionDefinition_1__Mapping_1__Runtime_1__Table_MANY_(
                buildPureLambda(query, pureModel), mapping, runtime, pureModel.getExecutionSupport());
        if (tables.isEmpty())
        {
            return null;
        }
        List<RelationalCSVTable> relationalCSVTables = tables.collect(table ->
        {
            RelationalCSVTable relationalCSVTable = new RelationalCSVTable();
            relationalCSVTable.schema = table._schema()._name();
            relationalCSVTable.table = table._name();
            relationalCSVTable.values = table._columns().select(c -> c instanceof Column).collect(c -> c.getName()).makeString(",");
            return relationalCSVTable;
        }).toList();
        RelationalCSVData data = new RelationalCSVData();
        data.tables = relationalCSVTables;
        return Collections.singletonList(data);
    }

    private static LambdaFunction<?> buildPureLambda(Lambda lambda, PureModel pureModel)
    {
        return HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
    }
}
