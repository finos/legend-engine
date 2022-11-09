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

package org.finos.legend.engine.language.haskell.grammar.to;

import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.haskell.metamodel.DataType;
import org.finos.legend.engine.protocol.haskell.metamodel.Deriving;
import org.finos.legend.engine.protocol.haskell.metamodel.Field;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellType;
import org.finos.legend.engine.protocol.haskell.metamodel.ListType;
import org.finos.legend.engine.protocol.haskell.metamodel.ModuleElement;
import org.finos.legend.engine.protocol.haskell.metamodel.NamedConstructor;
import org.finos.legend.engine.protocol.haskell.metamodel.NamedTypeRef;
import org.finos.legend.engine.protocol.haskell.metamodel.RecordTypeConstructor;

import java.util.List;

public class HaskellGrammarComposer
{
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

        for (ModuleElement dataType : module.elements)
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

        if (!Iterate.isEmpty(dataType.constructors))
        {
            builder.append(" = ");
            boolean isFirst = true;
            for (NamedConstructor constructor : dataType.constructors)
            {
                if (!isFirst)
                {
                    builder.append("\n  | ");
                }

                isFirst = false;
                renderNamedConstructor(builder, constructor);
            }
        }

        for (Deriving deriving : dataType.deriving)
        {
            builder.append("\n    ");
            renderDeriving(builder, deriving);
        }
    }

    protected void renderNamedConstructor(StringBuilder builder, NamedConstructor constructor)
    {
        renderDocumentation(builder, constructor.documentation);
        builder.append(constructor.name);

        if (constructor instanceof RecordTypeConstructor)
        {
            renderRecordTypeConstructor(builder, (RecordTypeConstructor) constructor);
        }
    }

    protected void renderRecordTypeConstructor(StringBuilder builder, RecordTypeConstructor constructor)
    {
        if (!Iterate.isEmpty(constructor.fields))
        {
            builder.append(" { ");
            boolean isFirstField = true;
            for (Field field : constructor.fields)
            {
                renderDocumentation(builder, field.documentation);
                if (!isFirstField)
                {
                    builder.append(", ");
                }

                renderFieldConstructor(builder, field);
                isFirstField = false;
            }
            builder.append(" }");
        }
    }

    protected void renderFieldConstructor(StringBuilder builder, Field field)
    {
        builder.append(field.name).append(" ").append(this.typeColonConvention).append(" ");
        renderTypes(builder, field.type, " ");
    }

    private void renderTypes(StringBuilder builder, List<HaskellType> types, String separator)
    {
        boolean isFirst = true;
        for (HaskellType type : types)
        {
            if (!isFirst)
            {
                builder.append(separator);
            }
            isFirst = false;

            renderType(builder, type);
        }
    }

    private void renderType(StringBuilder builder, HaskellType type)
    {
        if (type instanceof NamedTypeRef)
        {
            NamedTypeRef namedType = (NamedTypeRef) type;
            builder.append(namedType.name);
        }
        else if (type instanceof ListType)
        {
            builder.append("[");
            renderTypes(builder, ((ListType) type).type, " ");
            builder.append("]");
        }
    }

    private void renderDeriving(StringBuilder builder, Deriving deriving)
    {
        builder.append("deriving (");
        renderTypes(builder, deriving.types, ", ");
        builder.append(")");
    }

    private void renderDocumentation(StringBuilder builder, String docs)
    {
        if (docs != null)
        {
            builder.append("-- |").append(docs).append("\n");
        }
    }
}
