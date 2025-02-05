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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_data_RelationalCSVData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.generated.core_relational_relational_testDataGeneration_testDataGeneration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.util.Collections;
import java.util.List;

public class TestDataGenerationService
{
    public static List<EmbeddedData> generateEmbeddedData(Lambda query, Mapping mapping, PureModel pureModel)
    {
        Root_meta_relational_metamodel_data_RelationalCSVData relationalCSVData = core_relational_relational_testDataGeneration_testDataGeneration.Root_meta_relational_testDataGeneration_getRelationalCSVDataFromQuery_FunctionDefinition_1__Mapping_1__RelationalCSVData_1_(
                buildPureLambda(query, pureModel), mapping, pureModel.getExecutionSupport());
        RelationalCSVData data = new RelationalCSVData();
        List<RelationalCSVTable> relationalCSVTables = relationalCSVData._tables().collect(table ->
        {
            RelationalCSVTable relationalCSVTable = new RelationalCSVTable();
            relationalCSVTable.schema = table._schema();
            relationalCSVTable.table = table._table();
            relationalCSVTable.values = table._values();
            return relationalCSVTable;
        }).toList();
        data.tables = relationalCSVTables;
        return Collections.singletonList(data);
    }

    private static LambdaFunction<?> buildPureLambda(Lambda lambda, PureModel pureModel)
    {
        return HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
    }
}
