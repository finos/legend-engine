// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.test.emit;

import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.util.List;

public class CoreEMITModelProvider implements EMITModelProvider
{
    @Override
    public String getModule()
    {
        return "legend-engine-emit";
    }

    @Override
    public List<EMITModelDescriptor> getDescriptors()
    {
        return EMITModelProviderTool.load(
                CoreEMITModelProvider.class.getClassLoader(),
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/artifact-generation.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/class-simple.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/compile-failure.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/file-generation.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/m2m-mixed.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/m2m-passing.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/m2m-with-dep.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/basic/model-generation.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/clash-a.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/clash-b.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/clash-test.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/diamond.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/left-dep.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/right-dep.emit.yaml",
                "legend-engine/legend-engine-core/legend-engine-core-emit/legend-engine-emit/src/test/resources/emit-models/diamond/shared-dep.emit.yaml"
        );
    }
}

