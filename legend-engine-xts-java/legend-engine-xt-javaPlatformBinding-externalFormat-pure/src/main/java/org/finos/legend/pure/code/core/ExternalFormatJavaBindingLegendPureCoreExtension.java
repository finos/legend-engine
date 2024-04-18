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

package org.finos.legend.pure.code.core;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.code.core.JavaBindingLegendPureCoreExtension;

public class ExternalFormatJavaBindingLegendPureCoreExtension implements JavaBindingLegendPureCoreExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "-Core");
    }

    @Override
    public String functionFile()
    {
        return "core_java_platform_binding_external_format/legendJavaPlatformBinding/externalFormat/bindingLegendJavaPlatformBindingExtension.pure";
    }

    @Override
    public String functionSignature()
    {
        return "meta::external::format::shared::executionPlan::platformBinding::legendJava::bindingExtensionsJavaPlatformBinding__Extension_1_";
    }
}
