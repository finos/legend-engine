// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.to;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.*;
import org.finos.legend.engine.shared.core.operational.Assert;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperElasticsearchPropertyComposer extends AbstractPropertyBaseVisitor<Void>
{
    private final PrintWriter builder;
    private final PureGrammarComposerContext context;
    private int indentLevel;

    public HelperElasticsearchPropertyComposer(PrintWriter builder, int indentLevel, PureGrammarComposerContext context)
    {
        this.builder = builder;
        this.indentLevel = indentLevel;
        this.context = context;
    }

    private Void visit(PropertyBase val)
    {
        Assert.assertTrue(val.meta.isEmpty(), () -> "meta not supported yet", null, EngineErrorType.COMPOSER);
        Assert.assertTrue(val.ignore_above == null, () -> "ignore_above not supported yet", null, EngineErrorType.COMPOSER);
        Assert.assertTrue(val.dynamic == null, () -> "dynamic not supported yet", null, EngineErrorType.COMPOSER);

        if (!val.properties.isEmpty() || !val.fields.isEmpty())
        {
            builder.println(" {");

            int currIndent = this.indentLevel++;

            this.render("properties", val.properties);
            this.render("fields", val.fields);

            this.indentLevel = currIndent;
            builder.append(getTabString(indentLevel)).append('}');
        }
        return null;
    }

    private void render(String grammarName, Map<String, Property> properties)
    {
        if (!properties.isEmpty())
        {
            builder.append(context.getIndentationString()).append(getTabString(indentLevel)).append(grammarName).println(": [");

            Iterator<Map.Entry<String, Property>> namesSorted = properties.entrySet().stream().sorted(Map.Entry.comparingByKey()).iterator();

            while (namesSorted.hasNext())
            {
                Map.Entry<String, Property> next = namesSorted.next();
                String name = next.getKey();
                PropertyBase property = (PropertyBase) next.getValue().unionValue();

                builder.append(context.getIndentationString()).append(getTabString(indentLevel + 1)).append(PureGrammarComposerUtility.convertIdentifier(name)).append(": ");

                int currIndent = this.indentLevel++;
                property.accept(this);
                this.indentLevel = currIndent;

                if (namesSorted.hasNext())
                {
                    builder.println(',');
                }
                else
                {
                    builder.println();
                }
            }

            builder.append(context.getIndentationString()).append(getTabString(indentLevel)).println("];");
        }
    }

    @Override
    public Void defaultValue(PropertyBase val)
    {
        throw new UnsupportedOperationException(val.getClass().getCanonicalName() + " not supported");
    }

    @Override
    public Void visit(CorePropertyBase val)
    {
        Assert.assertTrue(val.copy_to.isEmpty(), () -> "`copy_to` not supported yet", null, EngineErrorType.COMPOSER);
        Assert.assertTrue(val.similarity == null, () -> "similarity not supported yet", null, EngineErrorType.COMPOSER);
        Assert.assertTrue(val.store == null, () -> "store not supported yet", null, EngineErrorType.COMPOSER);
        return this.visit((PropertyBase) val);
    }

    @Override
    public Void visit(DocValuesPropertyBase val)
    {
        Assert.assertTrue(val.doc_values == null, () -> "store not supported yet", null, EngineErrorType.COMPOSER);
        return this.visit((CorePropertyBase) val);
    }

    @Override
    public Void visit(StandardNumberProperty val)
    {
        // todo handle any special number properties
        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Void visit(KeywordProperty val)
    {
        this.builder.append("Keyword");
        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Void visit(BooleanProperty val)
    {
        this.builder.append("Boolean");
        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Void visit(ByteNumberProperty val)
    {
        this.builder.append("Byte");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(DateProperty val)
    {
        this.builder.append("Date");
        return this.visit((DocValuesPropertyBase) val);
    }

    @Override
    public Void visit(FloatNumberProperty val)
    {
        this.builder.append("Float");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(HalfFloatNumberProperty val)
    {
        this.builder.append("HalfFloat");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(IntegerNumberProperty val)
    {
        this.builder.append("Integer");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(LongNumberProperty val)
    {
        this.builder.append("Long");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(DoubleNumberProperty val)
    {
        this.builder.append("Double");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(ShortNumberProperty val)
    {
        this.builder.append("Short");
        return this.visit((StandardNumberProperty) val);
    }

    @Override
    public Void visit(TextProperty val)
    {
        this.builder.append("Text");
        return this.visit((CorePropertyBase) val);
    }

    @Override
    public Void visit(NestedProperty val)
    {
        this.builder.append("Nested");
        return this.visit((CorePropertyBase) val);
    }

    @Override
    public Void visit(ObjectProperty val)
    {
        this.builder.append("Object");
        return this.visit((CorePropertyBase) val);
    }
}
