// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;

import java.util.Collections;

public class ServiceCompilerExtensionImpl implements ServiceCompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                Service.class,
                Lists.fixedSize.with(PackageableConnection.class, PackageableRuntime.class),
                (service, context) -> new Root_meta_legend_service_metamodel_Service_Impl("")
                        ._name(service.name)
                        ._stereotypes(ListIterate.collect(service.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                        ._taggedValues(ListIterate.collect(service.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)))
                        ._pattern(service.pattern)
                        ._owners(Lists.mutable.withAll(service.owners))
                        ._documentation(service.documentation),
                (service, context) ->
                {
                    Root_meta_legend_service_metamodel_Service pureService = (Root_meta_legend_service_metamodel_Service) context.pureModel.getOrCreatePackage(service._package)._children().detect(c -> service.name.equals(c._name()));
                    pureService._execution(HelperServiceBuilder.processServiceExecution(service.execution, context))
                            ._test(HelperServiceBuilder.processServiceTest(service.test, context, service.execution));
                }));
    }
}
