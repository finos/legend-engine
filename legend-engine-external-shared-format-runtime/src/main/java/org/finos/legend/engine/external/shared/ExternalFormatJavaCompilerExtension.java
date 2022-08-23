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

package org.finos.legend.engine.external.shared;

import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataBooleanAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataLongAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalData;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;
import org.finos.legend.engine.plan.compilation.GeneratePureConfig;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassListFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExternalFormatJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    public static final String PURE_PACKAGE = "meta::external::shared::format::executionPlan::engine::java::";

    static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        DEPENDENCIES.put(PURE_PACKAGE + "_ExternalDataAdder", ExternalDataAdder.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ExternalDataBooleanAdder", ExternalDataBooleanAdder.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ExternalDataDoubleAdder", ExternalDataDoubleAdder.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ExternalDataLongAdder", ExternalDataLongAdder.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ExternalDataObjectAdder", ExternalDataObjectAdder.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IExternalData", IExternalData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IExternalDataFactory", IExternalDataFactory.class);
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return new ClassListFilter(DEPENDENCIES.values());
    }

    /**
     * Use to generate content of core/pure/binding/executionPlan/engine.pure (see GeneratePureConfig)
     */
    public static void main(String[] args)
    {
        GeneratePureConfig extension = new GeneratePureConfig("externalShared", ExternalFormatJavaCompilerExtension.class, PURE_PACKAGE);
        DEPENDENCIES.forEach(extension::addClass);
        System.out.println(extension.generate());
    }
}
