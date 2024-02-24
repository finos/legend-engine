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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph.HelperMasterRecordDefinitionBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;
import java.util.List;

public class MasterRecordGenExtension implements IMasteryModelGenerationExtension
{

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Mastery");
    }

    @Override
    public List<Function3<PackageableElement, CompileContext, String, PureModelContextData>> getPureModelContextDataGenerators()
    {
        return Collections.singletonList((modelGenerationElement, compileContext, version) ->
        {
            if (modelGenerationElement instanceof Root_meta_pure_mastery_metamodel_MasterRecordDefinition)
            {
                return HelperMasterRecordDefinitionBuilder.buildMasterRecordDefinitionGeneratedElements((Root_meta_pure_mastery_metamodel_MasterRecordDefinition) modelGenerationElement, compileContext, version);
            }
            return null;
        });
    }
}
