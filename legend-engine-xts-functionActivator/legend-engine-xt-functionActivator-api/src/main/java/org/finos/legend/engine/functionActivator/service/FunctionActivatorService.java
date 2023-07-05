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

package org.finos.legend.engine.functionActivator.service;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.pure.generated.Root_meta_external_functionActivator_FunctionActivator;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

public interface FunctionActivatorService<T extends Root_meta_external_functionActivator_FunctionActivator>
{
    FunctionActivatorInfo info(PureModel pureModel, String version);

    boolean supports(Root_meta_external_functionActivator_FunctionActivator packageableElement);

    MutableList<? extends FunctionActivatorError> validate(PureModel pureModel, T functionActivator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions);

    MutableList<? extends FunctionActivatorError> publishToSandbox(PureModel pureModel, T functionActivator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions);

   FunctionActivatorArtifact renderArtifact(PureModel pureModel, T functionActivator, PureModelContext inputModel, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions);
}
