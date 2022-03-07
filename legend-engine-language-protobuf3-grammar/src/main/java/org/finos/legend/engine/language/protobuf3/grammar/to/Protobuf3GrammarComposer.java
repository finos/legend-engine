package org.finos.legend.engine.language.protobuf3.grammar.to;

import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_metamodel_ProtoFile;
import org.finos.legend.pure.generated.core_external_format_protobuf_serialization_protocolBufferToString;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public class Protobuf3GrammarComposer {

    private Protobuf3GrammarComposer()
    {
    }

    public static Protobuf3GrammarComposer newInstance()
    {
        return new Protobuf3GrammarComposer();
    }

    public String renderProto(Root_meta_external_format_protobuf_metamodel_ProtoFile file, ExecutionSupport executionSupport)
    {
        String output = core_external_format_protobuf_serialization_protocolBufferToString.
                Root_meta_external_format_protobuf_serialization_toString_ProtoFile_1__String_1_(file, executionSupport);
        return output;
    }
}
