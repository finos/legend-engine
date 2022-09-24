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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportEmail;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceSupportEmail_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpaceSupportInfo;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_dataSpace_DataSpace_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_PackageableRuntime;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.Collections;

public class DataSpaceCompilerExtension implements CompilerExtension
{
    protected final MutableMap<String, Root_meta_pure_metamodel_dataSpace_DataSpace> dataSpacesIndex = Maps.mutable.empty();

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                DataSpace.class,
                Lists.fixedSize.with(PackageableRuntime.class, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class, Diagram.class),
                (dataSpace, context) ->
                {
                    Root_meta_pure_metamodel_dataSpace_DataSpace metamodel = new Root_meta_pure_metamodel_dataSpace_DataSpace_Impl(dataSpace.name, null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpace"))._name(dataSpace.name);
                    this.dataSpacesIndex.put(context.pureModel.buildPackageString(dataSpace._package, dataSpace.name), metamodel);
                    return metamodel;
                },
                (dataSpace, context) ->
                {
                    Root_meta_pure_metamodel_dataSpace_DataSpace metamodel = this.dataSpacesIndex.get(context.pureModel.buildPackageString(dataSpace._package, dataSpace.name));
                    metamodel._stereotypes(ListIterate.collect(dataSpace.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)));
                    metamodel._taggedValues(ListIterate.collect(dataSpace.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)));

                    // execution context
                    if (dataSpace.executionContexts.isEmpty())
                    {
                        throw new EngineException("Data space must have at least one execution context", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    metamodel._executionContexts(ListIterate.collect(dataSpace.executionContexts, executionContext ->
                    {
                        Mapping mapping = context.resolveMapping(executionContext.mapping.path, executionContext.mapping.sourceInformation);
                        Root_meta_pure_runtime_PackageableRuntime runtime = context.resolvePackageableRuntime(executionContext.defaultRuntime.path, executionContext.defaultRuntime.sourceInformation);
                        if (!HelperRuntimeBuilder.isRuntimeCompatibleWithMapping(runtime, mapping))
                        {
                            throw new EngineException("Execution context '" + executionContext.name + "' default runtime is not compatible with mapping", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                        }
                        return new Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceExecutionContext"))
                                ._name(executionContext.name)
                                ._description(executionContext.description)
                                ._mapping(mapping)
                                ._defaultRuntime(runtime);
                    }));
                    Assert.assertTrue(dataSpace.defaultExecutionContext != null, () -> "Default execution context is missing", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    Root_meta_pure_metamodel_dataSpace_DataSpaceExecutionContext defaultExecutionContext = metamodel._executionContexts().toList().select(c -> dataSpace.defaultExecutionContext.equals(c._name())).getFirst();
                    if (defaultExecutionContext == null)
                    {
                        throw new EngineException("Default execution context '" + dataSpace.defaultExecutionContext + "' does not match any existing execution contexts", dataSpace.sourceInformation, EngineErrorType.COMPILATION);
                    }
                    metamodel._defaultExecutionContext(defaultExecutionContext);

                    metamodel._title(dataSpace.title);
                    metamodel._description(dataSpace.description);
                    metamodel._featuredDiagrams(dataSpace.featuredDiagrams != null ? ListIterate.collect(dataSpace.featuredDiagrams, item -> HelperDiagramBuilder.resolveDiagram(item.path, item.sourceInformation, context)) : null);

                    // support
                    if (dataSpace.supportInfo != null)
                    {
                        Root_meta_pure_metamodel_dataSpace_DataSpaceSupportInfo supportInfo = null;
                        if (dataSpace.supportInfo instanceof DataSpaceSupportEmail)
                        {
                            supportInfo = new Root_meta_pure_metamodel_dataSpace_DataSpaceSupportEmail_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::dataSpace::DataSpaceSupportEmail"))._address(((DataSpaceSupportEmail) dataSpace.supportInfo).address);
                        }
                        metamodel._supportInfo(supportInfo);
                    }
                }
        ));
    }
}
