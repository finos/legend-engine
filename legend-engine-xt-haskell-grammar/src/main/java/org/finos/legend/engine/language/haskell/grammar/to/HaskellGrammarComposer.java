package org.finos.legend.engine.language.haskell.grammar.to;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.haskell.metamodel.*;

import java.util.List;

public class HaskellGrammarComposer {
    private static final String HASKELL_TYPE_COLON_CONVENTION = "::";
    private static final String HASKELL_CONS_COLON_CONVENTION = ":";

    //Variants of haskell flip these conventions, make them configurable
    private final String typeColonConvention;
    private final String consColonConvention;

    protected HaskellGrammarComposer(String typeColonConvention, String consColonConvention)
    {
        this.typeColonConvention = typeColonConvention;
        this.consColonConvention = consColonConvention;
    }

    public static HaskellGrammarComposer newInstance()
    {
        return new HaskellGrammarComposer(HASKELL_TYPE_COLON_CONVENTION, HASKELL_CONS_COLON_CONVENTION);
    }

    public String renderModule(HaskellModule module)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("module ").append(module.id).append("\n  where\n\n");

        for( ModuleElement dataType: module.elements)
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
        renderDocumentation(builder, dataType.documentation);
        builder.append("data ").append(dataType.name);

        if(!Iterate.isEmpty(dataType.constructors ))
        {
            builder.append(" = ");
            boolean isFirst = true;
            for(NamedConstructor constructor: dataType.constructors)
            {
                if(!isFirst)
                    builder.append("\n  | ");

                isFirst = false;
                renderNamedConstructor(builder, constructor);
            }
        }

        for(Deriving deriving: dataType.derivings)
        {
            builder.append("\n    deriving (");
            renderDeriving(builder, deriving);
            builder.append(")");
        }
    }

    protected void renderNamedConstructor(StringBuilder builder, NamedConstructor constructor) {
        renderDocumentation(builder, constructor.documentation);
        builder.append(constructor.name);

        if (constructor instanceof RecordTypeConstructor)
        {
            renderRecordTypeConstructor(builder, (RecordTypeConstructor) constructor);
        }
    }

    protected void renderRecordTypeConstructor(StringBuilder builder, RecordTypeConstructor constructor)
    {
        if(!Iterate.isEmpty(constructor.fields))
        {
            builder.append(" { ");
            boolean isFirstField = true;
            for (Field field : constructor.fields)
            {
                renderDocumentation(builder, field.documentation);
                if (!isFirstField)
                    builder.append(", ");

                renderFieldConstructor(builder, field);
                isFirstField = false;
            }
            builder.append(" }");
        }
    }

    protected void renderFieldConstructor(StringBuilder builder, Field field) {
        builder.append(field.name).append(" ").append(this.typeColonConvention).append(" ");
        renderTypes(builder, field.type);
    }

    private void renderTypes(StringBuilder builder, List<HaskellType> types) {
        boolean isFirst = true;
        for(HaskellType type: types) {
            if(!isFirst) builder.append(" ");
            isFirst = false;

            renderType(builder, type);
        }
    }

    private void renderType(StringBuilder builder, HaskellType type) {
        if (type instanceof NamedType)
        {
            NamedType namedType = (NamedType)type;
            builder.append(namedType.name);
        }
        else if (type instanceof ListType)
        {
            builder.append("[");
            renderTypes(builder, ((ListType)type).type);
            builder.append("]");
        }
    }

    private void renderDeriving(StringBuilder builder, Deriving deriving) {
        builder.append(String.join(", ",deriving.deriving));
    }

    private void renderDocumentation(StringBuilder builder, String docs)
    {
        if (docs != null)
        {
            builder.append("-- |").append(docs).append("\n");
        }
    }
}
