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

package org.finos.legend.engine.language.pure.dsl.mastery.extension;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ModelGenerationExtension;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public interface IMasteryModelGenerationExtension extends ModelGenerationExtension
{
    static List<IMasteryModelGenerationExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IMasteryModelGenerationExtension.class));
    }

    static PureModelContextData generate(Root_meta_pure_mastery_metamodel_MasterRecordDefinition item, List<Function3<Root_meta_pure_mastery_metamodel_MasterRecordDefinition, CompileContext, String, PureModelContextData>> generators, CompileContext context, String version)
    {
        PureModelContextData.Builder builder = PureModelContextData.newBuilder().withSerializer(new Protocol("pure", version));
        for (Function3<Root_meta_pure_mastery_metamodel_MasterRecordDefinition, CompileContext, String, PureModelContextData> generator : generators)
        {
            PureModelContextData pureModelContextData = generator.value(item, context, version);
            builder.addPureModelContextData(pureModelContextData);
        }
        return builder.build();
    }

    default List<Function3<Root_meta_pure_mastery_metamodel_MasterRecordDefinition, CompileContext, String, PureModelContextData>> getExtraMasteryModelGenerators()
    {
        return Collections.emptyList();
    }

}
