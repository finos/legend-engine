// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.grammar;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.shared.antlr4.FlatDataLexerGrammar;
import org.finos.legend.engine.external.format.flatdata.shared.antlr4.FlatDataParserGrammar;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataTemporal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FlatDataSchemaParser
{
    private final String schema;
    private final Set<String> sectionNames = Sets.mutable.empty();

    public FlatDataSchemaParser(String schema)
    {
        this.schema = schema;
    }

    public FlatData parse()
    {
        FlatDataParserGrammar.DefinitionContext ctx = parseWithAntlr();
        return convert(ctx);
    }

    private FlatDataParserGrammar.DefinitionContext parseWithAntlr()
    {
        CharStream input = CharStreams.fromString(schema);
        FlatDataErrorListener errorListener = new FlatDataErrorListener();
        FlatDataLexerGrammar lexer = new FlatDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        FlatDataParserGrammar parser = new FlatDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return parser.definition();
    }

    FlatData convert(FlatDataParserGrammar.DefinitionContext ctx)
    {
        if (ctx.section() == null || ctx.section().isEmpty())
        {
            throw error("Must specify at least one section", ctx);
        }

        FlatData result = new FlatData();

        if (ctx.section() == null || ctx.section().isEmpty())
        {
            throw error("Must specify at least one section", ctx);
        }
        ListIterate.forEach(ctx.section(), s -> result.withSection(convertSection(s)));

        return result;
    }

    private FlatDataSection convertSection(FlatDataParserGrammar.SectionContext ctx)
    {
        String name = fromIdentifier(ctx.sectionSignature().identifier());

        if (!sectionNames.add(name))
        {
            throw error("Duplicated section name '" + name + "'", ctx.sectionSignature().identifier().getStart());
        }

        FlatDataSection result = new FlatDataSection(name, ctx.sectionSignature().driverId().getText());
        ListIterate.forEach(ctx.sectionProperty() != null ? ctx.sectionProperty() : Collections.emptyList(), p -> result.withProperty(convertSectionPropery(p)));

        List<FlatDataParserGrammar.SectionRecordTypeContext> sectionRecordTypeContext = ctx.sectionRecordType();
        if (sectionRecordTypeContext != null && !sectionRecordTypeContext.isEmpty())
        {
            if (sectionRecordTypeContext.size() > 1)
            {
                throw error("Only one Record is permitted in a section", sectionRecordTypeContext.get(1));
            }
            result.withRecordType(convertFlatDataRecordType(sectionRecordTypeContext.get(0)));
        }

        return result;
    }

    private FlatDataProperty convertSectionPropery(FlatDataParserGrammar.SectionPropertyContext ctx)
    {
        if (ctx.booleanSectionProperty() != null)
        {
            return new FlatDataProperty(ctx.booleanSectionProperty().sectionPropertyName().getText(), true);
        }
        else
        {
            String name = ctx.nonBooleanSectionProperty().sectionPropertyName().getText();
            FlatDataParserGrammar.SectionPropertyValueContext value = ctx.nonBooleanSectionProperty().sectionPropertyValue();

            if (value.sectionPropertyValueLiteral() != null)
            {
                FlatDataParserGrammar.SectionPropertyValueLiteralContext literal = value.sectionPropertyValueLiteral();
                if (literal.STRING() != null)
                {
                    return new FlatDataProperty(name, fromGrammarString(literal.STRING()));
                }
                else if (literal.INTEGER() != null)
                {
                    return new FlatDataProperty(name, fromGrammarInteger(literal.INTEGER()));
                }
                else
                {
                    throw error("No value for section property: " + name, value);
                }
            }
            else if (value.sectionPropertyValueArray() != null)
            {
                FlatDataParserGrammar.SectionPropertyValueLiteralsContext arrayValues = value.sectionPropertyValueArray().sectionPropertyValueLiterals();
                if (arrayValues == null)
                {
                    return new FlatDataProperty(name, Collections.emptyList());
                }
                else if (arrayValues.STRING().size() > 0)
                {
                    return new FlatDataProperty(name, arrayValues.STRING().stream().map(FlatDataSchemaParser::fromGrammarString).collect(Collectors.toList()));
                }
                else
                {
                    return new FlatDataProperty(name, arrayValues.INTEGER().stream().map(FlatDataSchemaParser::fromGrammarInteger).collect(Collectors.toList()));
                }
            }
            else
            {
                throw error("No value for section property: " + name, value);
            }
        }
    }

    private FlatDataRecordType convertFlatDataRecordType(FlatDataParserGrammar.SectionRecordTypeContext ctx)
    {
        FlatDataRecordType result = new FlatDataRecordType();
        ListIterate.forEach(ctx.recordTypeFields().recordTypeField(), f -> result.withField(converFlatDataRecordField(f)));
        return result;
    }

    private FlatDataRecordField converFlatDataRecordField(FlatDataParserGrammar.RecordTypeFieldContext ctx)
    {
        String label = fromIdentifier(ctx.recordTypeLabel().identifier());

        String address = ctx.recordTypeAddress() == null
                ? null
                : ctx.recordTypeAddress().getText().substring(1, ctx.recordTypeAddress().getText().length() - 1);

        List<FlatDataParserGrammar.RecordTypeDataTypeAttributeContext> attributes = ctx.recordTypeDataType().recordTypeDataTypeAttributes() != null
                ? ctx.recordTypeDataType().recordTypeDataTypeAttributes().recordTypeDataTypeAttribute()
                : Collections.emptyList();
        Map<String, List<String>> dataTypeAttributes = Maps.mutable.empty();
        boolean optional = false;
        for (FlatDataParserGrammar.RecordTypeDataTypeAttributeContext a : attributes)
        {
            if (a.OPTIONAL() != null)
            {
                if (optional)
                {
                    throw error("Attribute 'optional' duplicated for record type property", a.getStart());
                }
                optional = true;
            }
            else
            {
                String name = a.recordTypeDataTypeAttributeName().getText();
                if (dataTypeAttributes.containsKey(name))
                {
                    throw error("Attribute '" + name + "' duplicated for record type property", a.getStart());
                }
                List<String> values = a.recordTypeDataTypeAttributeValue().STRING().stream()
                        .map(TerminalNode::getText)
                        .map(FlatDataSchemaParser::fromGrammarString)
                        .collect(Collectors.toList());
                dataTypeAttributes.put(name, values);
            }
        }

        FlatDataDataType dataType;
        switch (ctx.recordTypeDataType().RECORD_DATA_TYPE().getText())
        {
            case "BOOLEAN":
            {
                dataType = new FlatDataBoolean(optional)
                        .withTrueString(singleOptionalAttributeValue(dataTypeAttributes, "trueString", ctx.recordTypeDataType()))
                        .withFalseString(singleOptionalAttributeValue(dataTypeAttributes, "falseString", ctx.recordTypeDataType()));
                break;
            }
            case "STRING":
            {
                dataType = new FlatDataString(optional);
                break;
            }
            case "INTEGER":
            {
                dataType = new FlatDataInteger(optional)
                        .withFormat(singleOptionalAttributeValue(dataTypeAttributes, "format", ctx.recordTypeDataType()));
                break;
            }
            case "DECIMAL":
            {
                dataType = new FlatDataDecimal(optional)
                        .withFormat(singleOptionalAttributeValue(dataTypeAttributes, "format", ctx.recordTypeDataType()));
                break;
            }
            case "DATE":
            {
                FlatDataDate dateType = new FlatDataDate(optional);
                multiOptionalAttributeValue(dataTypeAttributes, "format").forEach(dateType::addFormat);
                dataType = dateType;
                break;
            }
            case "DATETIME":
            {
                FlatDataTemporal dateTimeType = new FlatDataDateTime(optional)
                        .withTimeZone(singleOptionalAttributeValue(dataTypeAttributes, "timeZone", ctx.recordTypeDataType()));
                multiOptionalAttributeValue(dataTypeAttributes, "format").forEach(dateTimeType::addFormat);
                dataType = dateTimeType;
                break;
            }
            default:
            {
                throw error("Unknown FlatData data type '" + ctx.recordTypeDataType().RECORD_DATA_TYPE().getText() + "'", ctx.recordTypeDataType().getStart());
            }
        }
        if (!dataTypeAttributes.isEmpty())

        {
            throw error("Unknown attributes [" + String.join(", ", dataTypeAttributes.keySet()) + "] specified for record type field", ctx.recordTypeDataType());
        }

        return new FlatDataRecordField(label, dataType, address);
    }

    private String singleOptionalAttributeValue(Map<String, List<String>> dataTypeAttributes, String attributeName, ParserRuleContext context)
    {
        if (dataTypeAttributes.containsKey(attributeName))
        {
            List<String> values = dataTypeAttributes.remove(attributeName);
            if (values.size() != 1)
            {
                throw error("Attribute " + attributeName + " should only have one value, found " + values.size(), context);
            }
            return values.get(0);
        }
        else
        {
            return null;
        }
    }

    private List<String> multiOptionalAttributeValue(Map<String, List<String>> dataTypeAttributes, String attributeName)
    {
        if (dataTypeAttributes.containsKey(attributeName))
        {
            return dataTypeAttributes.remove(attributeName);
        }
        else
        {
            return Collections.emptyList();
        }
    }

    private FlatDataSchemaParseException error(String message, ParserRuleContext parserRuleContext)
    {
        int startLine = parserRuleContext.getStart().getLine();
        int startColumn = parserRuleContext.getStart().getCharPositionInLine() + 1;
        int endLine = parserRuleContext.getStop().getLine();
        int endColumn = parserRuleContext.getStop().getCharPositionInLine() + ("<EOF>".equals(parserRuleContext.getStop().getText()) ? 1 : parserRuleContext.getStop().getText().length());
        return new FlatDataSchemaParseException(message, startLine, startColumn, endLine, endColumn);
    }

    private FlatDataSchemaParseException error(String message, Token token)
    {
        return new FlatDataSchemaParseException(message, token.getLine(), token.getCharPositionInLine() + 1);
    }

    public static String fromIdentifier(ParserRuleContext identifier)
    {
        String text = identifier.getText();
        return text.startsWith("'") ? fromGrammarString(text) : text;
    }

    public static Long fromGrammarInteger(TerminalNode node)
    {
        return Long.parseLong(node.getText());
    }

    public static String fromGrammarString(TerminalNode node)
    {
        return fromGrammarString(node.getText());
    }

    public static String fromGrammarString(String val)
    {
        val = StringEscapeUtils.unescapeJava(val);
        return val.substring(1, val.length() - 1);
    }
}
