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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_MasterRecordDefinition_Impl;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;

import java.util.Collections;

public class MasteryCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                MasterRecordDefinition.class,
                Lists.fixedSize.with(Service.class, Mapping.class, Binding.class, PackageableConnection.class),
                //First Pass instantiate - does not include Pure class properties on classes
                (masterRecordDefinition, context) -> new Root_meta_pure_mastery_metamodel_MasterRecordDefinition_Impl("")
                        ._modelClass(HelperMasterRecordDefinitionBuilder.buildModelClass(masterRecordDefinition, context)),
                //Second Pass stitched together - can access Pure class properties now.
                (masterRecordDefinition, context) ->
                {
                    Root_meta_pure_mastery_metamodel_MasterRecordDefinition pureMasteryMetamodelMasterRecordDefinition = (Root_meta_pure_mastery_metamodel_MasterRecordDefinition) context.pureModel.getOrCreatePackage(masterRecordDefinition._package)._children().detect(c -> masterRecordDefinition.name.equals(c._name()));
                    pureMasteryMetamodelMasterRecordDefinition._identityResolution(HelperMasterRecordDefinitionBuilder.buildIdentityResolution(masterRecordDefinition.identityResolution, context));
                    pureMasteryMetamodelMasterRecordDefinition._sources(HelperMasterRecordDefinitionBuilder.buildRecordSources(masterRecordDefinition.sources, context));
                }
        ));
    }
}
