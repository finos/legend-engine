package org.finos.legend.engine.language.daml.grammar.to;

import org.finos.legend.engine.language.haskell.grammar.to.HaskellGrammarComposer;
import org.finos.legend.engine.protocol.haskell.metamodel.Field;
import org.finos.legend.engine.protocol.haskell.metamodel.NamedConstructor;
import org.finos.legend.engine.protocol.haskell.metamodel.RecordTypeConstructor;

public class DamlGrammarComposer extends HaskellGrammarComposer {
    private DamlGrammarComposer() {
        super(":", "::");
    }

    public static DamlGrammarComposer newInstance()
    {
        return new DamlGrammarComposer();
    }

    @Override
    protected void renderNamedConstructor(StringBuilder builder, NamedConstructor constructor) {
        builder.append(constructor.name).append(" with");

        if (constructor instanceof RecordTypeConstructor)
        {
            renderRecordTypeConstructor(builder, (RecordTypeConstructor) constructor);
        }
    }

    protected void renderRecordTypeConstructor(StringBuilder builder, RecordTypeConstructor constructor)
    {
        for(Field field: constructor.fields)
        {
            builder.append("\n  ");
            renderFieldConstructor(builder, field);
        }
    }
}
