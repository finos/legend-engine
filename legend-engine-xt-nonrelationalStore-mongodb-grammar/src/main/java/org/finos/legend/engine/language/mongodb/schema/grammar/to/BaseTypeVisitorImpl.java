// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.mongodb.schema.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ArrayType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BaseTypeVisitor;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BinaryType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BoolType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.BsonType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DateType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DecimalType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.DoubleType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.IntType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.JavaScriptType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.LongType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MaxKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MinKeyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.NullType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectIdType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.ObjectType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.PropertyType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.RegExType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Schema;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.StringType;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.TimeStampType;

import java.util.stream.Collectors;

import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendJsonArrayKey;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendJsonKey;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendJsonObjectKey;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendStringWithQuotes;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendTabString;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.getTabString;

public class BaseTypeVisitorImpl implements BaseTypeVisitor<String>
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final int indentLevel;

    public BaseTypeVisitorImpl(int indentLevel)
    {
        this.indentLevel = indentLevel;
    }

    @Override
    public String visit(ArrayType val)
    {
        StringBuilder builder = new StringBuilder();
        //appendTabString(builder, indentLevel);
        builder.append("{\n");
        int valuesIndentLevel = indentLevel + 1;

        appendTabString(builder, valuesIndentLevel);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("array", builder);
        builder.append(",\n");

        if (val.description != null)
        {
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("description", builder);
            appendStringWithQuotes(val.description, builder);
            builder.append(",\n");
        }

        // Setting the indent level to earlier point, as it will be incrmenting it to match
        if (val.items != null)
        {
            if (val.items.size() == 1)
            {
                appendTabString(builder, valuesIndentLevel);
                appendJsonObjectKey("items", builder);
                //builder.append("{\n");
                ListIterate.collect(val.items, i -> builder.append(renderItem(i, valuesIndentLevel)));
                // appendTabString(builder, valuesIndentLevel);
                // builder.append(",\n");
            }
            else if (val.items.size() > 1)
            {
                appendTabString(builder, valuesIndentLevel);
                appendJsonObjectKey("items", builder);
                builder.append("[\n");
                builder.append(val.items.stream().map(p -> renderItem(p, valuesIndentLevel + 1))
                        .collect(Collectors.joining(",\n", "", "\n")));
                // ListIterate.collect(val.items, i -> builder.append(renderItem(i, valuesIndentLevel + 1)));
                appendTabString(builder, valuesIndentLevel);
                builder.append("]");
            }
        }
        if (val.minItems != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("minItems", builder);
            builder.append(val.minItems);
        }
        if (val.maxItems != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("maxItems", builder);
            builder.append(val.maxItems);
        }

        if (val.uniqueItems)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("uniqueItems", builder);
            builder.append(String.valueOf(val.uniqueItems));
        }
        builder.append("\n");
        appendTabString(builder, indentLevel);
        builder.append("}");

        return builder.toString();
    }


    @Override
    public String visit(BinaryType val)
    {
        return null;
    }

    @Override
    public String visit(BoolType val)
    {
        return null;
    }

    @Override
    public String visit(BsonType val)
    {
        return null;
    }

    @Override
    public String visit(DateType val)
    {
        return null;
    }

    @Override
    public String visit(DecimalType val)
    {
        StringBuilder builder = new StringBuilder();
        //øappendTabString(builder, indentLevel);
        builder.append("{\n");
        int valuesIndentLevel = indentLevel + 1;

        appendTabString(builder, valuesIndentLevel);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("decimal", builder);
        builder.append(",\n");

        if (val.description != null)
        {
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("description", builder);
            appendStringWithQuotes(val.description, builder);
            builder.append(",\n");
        }

        if (val.minimum != null)
        {
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("minimum", builder);
            builder.append(val.minimum);
            builder.append(",\n");
        }

        if (val.maximum != null)
        {
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("maximum", builder);
            builder.append(val.maximum);
            builder.append("\n");
        }


        appendTabString(builder, indentLevel);
        builder.append("}");

        return builder.toString();
    }

    @Override
    public String visit(DoubleType val)
    {
        return null;
    }

    @Override
    public String visit(IntType val)
    {
        StringBuilder builder = new StringBuilder();
        //appendTabString(builder, indentLevel);
        builder.append("{\n");
        int valuesIndentLevel = indentLevel + 1;

        appendTabString(builder, valuesIndentLevel);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("int", builder);


        if (val.description != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("description", builder);
            appendStringWithQuotes(val.description, builder);
            //builder.append(",\n");
        }

        if (val.minimum != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("minimum", builder);
            builder.append(val.minimum);
            //builder.append(",\n");
        }

        if (val.maximum != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("maximum", builder);
            builder.append(val.maximum);
            //builder.append(",\n");
        }
        builder.append("\n");
        appendTabString(builder, indentLevel);
        builder.append("}");

        return builder.toString();
    }

    @Override
    public String visit(JavaScriptType val)
    {
        return null;
    }

    @Override
    public String visit(LongType val)
    {
        StringBuilder builder = new StringBuilder();
        //appendTabString(builder, indentLevel);
        builder.append("{\n");

        int valuesIndentLevel = indentLevel + 1;

        appendTabString(builder, valuesIndentLevel);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("long", builder);

        if (val.description != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("description", builder);
            appendStringWithQuotes(val.description, builder);
            //builder.append(",\n");
        }

        if (val.minimum != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("minimum", builder);
            builder.append(val.minimum);
            //builder.append(",\n");
        }

        if (val.maximum != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("maximum", builder);
            builder.append(val.maximum);
            //builder.append("\n");
        }

        builder.append("\n");
        appendTabString(builder, indentLevel);
        builder.append("}");

        return builder.toString();
    }

    @Override
    public String visit(MaxKeyType val)
    {
        return null;
    }

    @Override
    public String visit(MinKeyType val)
    {
        return null;
    }

    @Override
    public String visit(NullType val)
    {
        return null;
    }

    @Override
    public String visit(ObjectIdType val)
    {
        return null;
    }

    @Override
    public String visit(ObjectType val)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        int valuesIndentLevel = indentLevel + 1;

        appendTabString(builder, valuesIndentLevel);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("object", builder);

        renderObjectType(val, builder, valuesIndentLevel);
        builder.append("\n");

        appendTabString(builder, indentLevel);
        builder.append("}");

        return builder.toString();

    }


    @Override
    public String visit(RegExType val)
    {
        return null;
    }

    @Override
    public String visit(Schema val)
    {
        StringBuilder builder = new StringBuilder();
        appendTabString(builder, indentLevel);
        builder.append("{\n");

        int schemaValuesTabIndex = indentLevel + 1;
        appendTabString(builder, schemaValuesTabIndex);
        appendJsonKey("$schema", builder);
        appendStringWithQuotes(val.schemaVersion, builder);
        builder.append(",\n");

        appendTabString(builder, schemaValuesTabIndex);
        appendJsonKey("$id", builder);
        appendStringWithQuotes(val.id, builder);
        builder.append(",\n");

        appendTabString(builder, schemaValuesTabIndex);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("object", builder);
        renderObjectType(val, builder, schemaValuesTabIndex);
        builder.append("\n");

        appendTabString(builder, indentLevel);
        builder.append("}");
        return builder.toString();
    }


    @Override
    public String visit(StringType val)
    {
        StringBuilder builder = new StringBuilder();
        //appendTabString(builder, indentLevel);
        builder.append("{\n");
        int valuesIndentLevel = indentLevel + 1;

        appendTabString(builder, valuesIndentLevel);
        appendJsonKey("bsonType", builder);
        appendStringWithQuotes("string", builder);
        //builder.append(",\n");

        if (val.description != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("description", builder);
            appendStringWithQuotes(val.description, builder);
            //builder.append(",\n");
        }

        if (val.minLength != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("minLength", builder);
            builder.append(val.minLength);
            //builder.append(",\n");
        }
        if (val.maxLength != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("maxLength", builder);
            builder.append(val.maxLength);
            //builder.append(",\n");
        }
        builder.append("\n");
        appendTabString(builder, indentLevel);
        builder.append("}");

        return builder.toString();
    }

    private void renderObjectType(ObjectType objType, StringBuilder builder, int valuesIndentLevel)
    {
        if (objType.title != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("title", builder);
            appendStringWithQuotes(objType.title, builder);
            //builder.append(",\n");
        }
        if (objType.description != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("description", builder);
            appendStringWithQuotes(objType.description, builder);
            //builder.append(",\n");
        }

        if (objType.properties != null && objType.properties.size() > 0)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonObjectKey("properties", builder);
            builder.append("{\n");
            //ListIterate.collect(objType.properties, p -> builder.append(render(p, valuesIndentLevel + 1)));
            builder.append(objType.properties.stream().map(p -> render(p, valuesIndentLevel + 1))
                    .collect(Collectors.joining(",\n", "", "\n")));
            appendTabString(builder, valuesIndentLevel);
            builder.append("}");
        }

        if (objType.minProperties != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("minProperties", builder);
            builder.append(objType.minProperties);
            //builder.append(",\n");
        }
        if (objType.maxProperties != null)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("maxProperties", builder);
            builder.append(objType.maxProperties);
            //builder.append(",\n");
        }

        if (objType.required != null && objType.required.size() > 0)
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonArrayKey("required", builder);
            builder.append("[");

            int itemsIndentLevel = valuesIndentLevel + 1;
            String requiredValues = objType.required.stream().map(i -> "\"" + i + "\"")
                    .collect(Collectors.joining(",\n" + getTabString(itemsIndentLevel), "\n" + getTabString(itemsIndentLevel), "\n"));
            builder.append(requiredValues);

            appendTabString(builder, valuesIndentLevel);
            builder.append("]");
        }

        if (objType.additionalPropertiesAllowed)
        {
            builder.append(",\n");
            if (objType.additionalProperties != null)
            {
                appendTabString(builder, valuesIndentLevel);
                appendJsonKey("additionalProperties", builder);
                builder.append("Additional properties with specified schema not supported");
            }
            else if (objType.additionalProperties == null)
            {
                appendTabString(builder, valuesIndentLevel);
                appendJsonKey("additionalProperties", builder);
                builder.append(true);
            }
        }
        else
        {
            builder.append(",\n");
            appendTabString(builder, valuesIndentLevel);
            appendJsonKey("additionalProperties", builder);
            builder.append(false);
        }
    }


    private String render(PropertyType p, int indentLevel)
    {
        LOGGER.debug(p.key);
        StringBuilder builder = new StringBuilder();
        appendTabString(builder, indentLevel);
        appendJsonObjectKey(p.key, builder);
        BaseTypeVisitorImpl typeVisitor = new BaseTypeVisitorImpl(indentLevel);
        builder.append(p.value.accept(typeVisitor));
        return builder.toString();
    }

    private String renderItem(BaseType item, int indentLevel)
    {
        StringBuilder builder = new StringBuilder();
        BaseTypeVisitorImpl typeVisitor = new BaseTypeVisitorImpl(indentLevel);
        builder.append(item.accept(typeVisitor));
        return builder.toString();
    }

    @Override
    public String visit(TimeStampType val)
    {
        return null;
    }
}
