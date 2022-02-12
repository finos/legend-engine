package org.finos.legend.engine.language.protobuf3.grammar.to;

import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;

public class Protobuf3GrammarComposer {

    private Protobuf3GrammarComposer()
    {
    }

    public static Protobuf3GrammarComposer newInstance()
    {
        return new Protobuf3GrammarComposer();
    }

    public String renderProto(ProtoFile file)
    {
        return "";
    }
}
