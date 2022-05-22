package org.finos.legend.engine.language.haskell.grammar.to;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.haskell.metamodel.*;

public class HaskellGrammarComposer {

    private HaskellGrammarComposer()
    {
    }

    public static HaskellGrammarComposer newInstance()
    {
        return new HaskellGrammarComposer();
    }

    public String renderModule(HaskellModule module)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("module ").append(module.id).append("\n  where\n\n");

        for( TopLevelDeclaration dataType: module.elements)
        {
            if (dataType instanceof DataType)
            {
                renderTopLevelDecl(builder, (DataType) dataType);
            }
        }

        builder.append("\n");

        return builder.toString();
    }

    private void renderTopLevelDecl(StringBuilder builder, DataType dataType)
    {
        builder.append("data ").append(dataType.name);

        if(!Iterate.isEmpty(dataType.constructors ))
        {
            builder.append(" = ");
            boolean isFirst = true;
            for(NamedConstructor constructor: dataType.constructors)
            {
                if(!isFirst)
                    builder.append("\n | ");

                builder.append(constructor.name);
                isFirst = false;
                //TODO: ADD FOR DAML.append(" with");

                if (constructor instanceof RecordTypeConstructor)
                {
                    builder.append(" { ");
                    boolean isFirstField = true;
                    for(Field field: ((RecordTypeConstructor) constructor).fields)
                    {
                        if(!isFirstField)
                            builder.append(", ");

                        renderFieldConstructor(builder, field);
                        isFirstField = false;
                    }
                    builder.append(" }");
                }
            }
        }

        for(Deriving deriving: dataType.derivings)
        {
            builder.append("\n    deriving (");
            renderDeriving(builder, deriving);
            builder.append(")");
        }
    }


    private void renderFieldConstructor(StringBuilder builder, Field field) {
        builder.append(field.name).append(" :: ");
        renderType(builder, field.type);
    }

    private void renderType(StringBuilder builder, HaskellType type) {
        if (type instanceof NamedType)
        {
            NamedType namedType = (NamedType)type;
            builder.append(namedType.name);
        }
    }

    private void renderDeriving(StringBuilder builder, Deriving deriving) {
        builder.append(String.join(", ",deriving.deriving));
    }
}
