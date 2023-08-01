//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testData.services;


import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.testData.model.TestRelationalCSVDataGenerationInput;
import org.finos.legend.pure.generated.core_relational_relational_testDataGeneration_testDataGeneration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.testData.model.TestEmbeddedDataGenerationInput;
import org.finos.legend.testData.service.TestEmbeddedDataGenerationServiceExtension;

import java.util.Collections;
import java.util.List;

public class RelationalCSVTestEmbeddedDataGenerationServiceExtension implements TestEmbeddedDataGenerationServiceExtension
{
    public RelationalCSVTestEmbeddedDataGenerationServiceExtension()
    {
    }

    @Override
    public List<EmbeddedData> generateTestEmbeddedData(TestEmbeddedDataGenerationInput input, PureModel pureModel)
    {
        if(input instanceof TestRelationalCSVDataGenerationInput)
        {
            RichIterable<? extends Table> tables = core_relational_relational_testDataGeneration_testDataGeneration.Root_meta_relational_testDataGeneration_getTableFromQuery_FunctionDefinition_1__Mapping_1__Runtime_1__Table_MANY_(
                    buildPureLambda(((TestRelationalCSVDataGenerationInput) input).query, pureModel), ((TestRelationalCSVDataGenerationInput) input).mapping, ((TestRelationalCSVDataGenerationInput) input).runtime, pureModel.getExecutionSupport());
            if (tables.isEmpty())
            {
                return null;
            }
            List<RelationalCSVTable> relationalCSVTables = tables.collect(table ->
            {
                RelationalCSVTable relationalCSVTable = new RelationalCSVTable();
                relationalCSVTable.schema = table._schema()._name();
                relationalCSVTable.table = table._name();
                relationalCSVTable.values = "";
                return relationalCSVTable;
            }).toList();
            RelationalCSVData data = new RelationalCSVData();
            data.tables = relationalCSVTables;
            return Collections.singletonList(data);
        }
        return Collections.emptyList();
    }

    private LambdaFunction<?> buildPureLambda(Lambda lambda, PureModel pureModel)
    {
        return HelperValueSpecificationBuilder.buildLambda(lambda, pureModel.getContext());
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.fixedSize.with(
                ProtocolSubTypeInfo.newBuilder(TestEmbeddedDataGenerationInput.class)
                        .withSubtype(TestRelationalCSVDataGenerationInput.class, "TestRelationalCSVDataGenerationInput")
                        .build()
        ));
    }
}