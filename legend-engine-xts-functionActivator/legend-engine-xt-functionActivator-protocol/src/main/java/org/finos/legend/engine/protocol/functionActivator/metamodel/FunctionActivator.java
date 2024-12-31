// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.functionActivator.metamodel;

import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.domain.TaggedValue;

import java.util.Collections;
import java.util.List;

//------------------------------------------------------------
// Should be generated out of the Pure protocol specification
//------------------------------------------------------------
public abstract class FunctionActivator extends PackageableElement
{
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();
    public PackageableElementPointer function;
    public DeploymentConfiguration activationConfiguration;
    public Ownership ownership;
    public List<PostDeploymentAction> actions = Collections.emptyList();
}
