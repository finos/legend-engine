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


package org.finos.legend.engine.functionActivator.validation;

import org.finos.legend.engine.functionActivator.service.FunctionActivatorError;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.functionActivator.deployment.FunctionActivatorArtifact;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_FunctionActivator;

import java.util.List;

public interface FunctionActivatorValidator<T extends Root_meta_external_function_activator_FunctionActivator, V extends FunctionActivatorArtifact>
{
    boolean supports(T activator);

    boolean supports(V artifact);

    List<FunctionActivatorError> validate(Identity identity, T activator);

    List<FunctionActivatorError> validate(Identity identity, V artifact);

    List<FunctionActivatorError> validate(T activator, PureModel pureModel);
}
