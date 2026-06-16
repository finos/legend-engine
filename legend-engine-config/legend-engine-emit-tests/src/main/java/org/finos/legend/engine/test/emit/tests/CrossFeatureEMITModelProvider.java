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

package org.finos.legend.engine.test.emit.tests;

import org.finos.legend.engine.test.emit.EMITModelProvider;
import org.finos.legend.engine.test.emit.EMITModelProviderTool;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.util.List;

public class CrossFeatureEMITModelProvider implements EMITModelProvider
{
    @Override
    public String getModule()
    {
        return "legend-engine-emit-tests";
    }

    @Override
    public List<EMITModelDescriptor> getDescriptors()
    {
        return EMITModelProviderTool.load(
                CrossFeatureEMITModelProvider.class.getClassLoader(),
                "legend-engine/legend-engine-config/legend-engine-emit-tests/src/test/resources/emit-models/service-with-binding.emit.yaml"
        );
    }
}

