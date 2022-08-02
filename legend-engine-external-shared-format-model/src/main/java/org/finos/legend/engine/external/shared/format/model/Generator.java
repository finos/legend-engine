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

package org.finos.legend.engine.external.shared.format.model;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.platform_pure_corefunctions_meta;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class Generator
{
    protected final PureModel pureModel;

    protected Generator(PureModel pureModel)
    {
        this.pureModel = pureModel;
    }

    protected Binding transformBinding(Root_meta_external_shared_format_binding_Binding binding)
    {
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = binding._modelUnit()._packageableElementIncludes().collect(this::elementToPath).toList();
        modelUnit.packageableElementExcludes = binding._modelUnit()._packageableElementExcludes().collect(this::elementToPath).toList();

        Binding result = new Binding();
        result.name = binding._name();
        result._package = elementToPath(binding._package());
        result.schemaSet = elementToPath(binding._schemaSet());
        result.schemaId = binding._schemaId();
        result.contentType = binding._contentType();
        result.modelUnit = modelUnit;
        return result;
    }

    protected String elementToPath(PackageableElement element)
    {
        return platform_pure_corefunctions_meta.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_(element, pureModel.getExecutionSupport());
    }
}
