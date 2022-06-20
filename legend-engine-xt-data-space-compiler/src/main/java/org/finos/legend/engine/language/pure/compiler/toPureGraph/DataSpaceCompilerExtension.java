// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportEmail;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceSupportEmail_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceSupportInfo;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;

import java.util.Collections;

public class DataSpaceCompilerExtension implements CompilerExtension
{
    protected final MutableMap<String, Root_meta_pure_metamodel_dataSpace_DataSpace> dataSpacesIndex = Maps.mutable.empty();

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                DataSpace.class,
                (dataSpace, context) ->
                {
                    Root_meta_pure_metamodel_dataSpace_DataSpace metamodel = new Root_meta_pure_metamodel_dataSpace_DataSpace_Impl("")._name(dataSpace.name);
                    this.dataSpacesIndex.put(context.pureModel.buildPackageString(dataSpace._package, dataSpace.name), metamodel);
                    return metamodel;
                },
                (dataSpace, context) ->
                {
                    Root_meta_pure_metamodel_dataSpace_DataSpace metamodel = this.dataSpacesIndex.get(context.pureModel.buildPackageString(dataSpace._package, dataSpace.name));
                    metamodel._stereotypes(ListIterate.collect(dataSpace.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)));
                    metamodel._taggedValues(ListIterate.collect(dataSpace.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)));

                    // execution context
                    if (dataSpace.executionContexts.isEmpty())
                    {
                        throw new EngineException("Data space must have at least one execution context", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    metamodel._executionContexts(ListIterate.collect(dataSpace.executionContexts, item -> new Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext_Impl("")
                            ._name(item.name)
                            ._description(item.description)
                            ._mapping(context.resolveMapping(item.mapping.path, item.mapping.sourceInformation))
                            ._defaultRuntime(context.resolvePackageableRuntime(item.defaultRuntime.path, item.defaultRuntime.sourceInformation))
                    ));
                    if ((dataSpace.defaultExecutionContext != null) && Iterate.noneSatisfy(dataSpace.executionContexts, c -> dataSpace.defaultExecutionContext.equals(c.name)))
                    {
                        throw new EngineException("Default execution context does not match any existing execution contexts", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    }

                    metamodel._description(dataSpace.description);
                    metamodel._featuredDiagrams(ListIterate.collect(dataSpace.featuredDiagrams, item -> HelperDiagramBuilder.resolveDiagram(item.path, item.sourceInformation, context)));

                    // support
                    if (dataSpace.supportInfo != null)
                    {
                        Root_meta_pure_metamodel_dataSpace_DataSpaceSupportInfo supportInfo = null;
                        if (dataSpace.supportInfo instanceof DataSpaceSupportEmail)
                        {
                            supportInfo = new Root_meta_pure_metamodel_dataSpace_DataSpaceSupportEmail_Impl("")._address(((DataSpaceSupportEmail) dataSpace.supportInfo).address);
                        }
                        metamodel._supportInfo(supportInfo);
                    }
                }
        ));
    }
}
