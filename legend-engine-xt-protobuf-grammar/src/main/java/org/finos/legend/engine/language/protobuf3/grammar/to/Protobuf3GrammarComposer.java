//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.protobuf3.grammar.to;

import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_metamodel_ProtoFile;
import org.finos.legend.pure.generated.core_external_format_protobuf_serialization_protocolBufferToString;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public class Protobuf3GrammarComposer
{

    private Protobuf3GrammarComposer()
    {
    }

    public static Protobuf3GrammarComposer newInstance()
    {
        return new Protobuf3GrammarComposer();
    }

    public String renderProto(Root_meta_external_format_protobuf_metamodel_ProtoFile file, ExecutionSupport executionSupport)
    {
        return core_external_format_protobuf_serialization_protocolBufferToString
                .Root_meta_external_format_protobuf_serialization_toString_ProtoFile_1__String_1_(file, executionSupport);
    }
}
