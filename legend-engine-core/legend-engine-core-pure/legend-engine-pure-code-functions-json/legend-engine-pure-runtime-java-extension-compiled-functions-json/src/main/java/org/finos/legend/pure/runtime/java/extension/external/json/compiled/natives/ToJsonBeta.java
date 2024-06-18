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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives;

import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class ToJsonBeta extends AbstractNativeFunctionGeneric
{
    public ToJsonBeta()
    {
        super("org.finos.legend.pure.generated.JsonGen.toJson", new Object[]{Object.class, "org.finos.legend.pure.generated.Root_meta_json_JSONSerializationConfig", SourceInformation.class, ExecutionSupport.class},
                true, true, false, "toJsonBeta_Any_MANY__JSONSerializationConfig_1__String_1_");
    }

    @Override
    protected String buildM4SourceInformation(SourceInformation si)
    {
        return NativeFunctionProcessor.buildM4SourceInformation(si);
    }
}
