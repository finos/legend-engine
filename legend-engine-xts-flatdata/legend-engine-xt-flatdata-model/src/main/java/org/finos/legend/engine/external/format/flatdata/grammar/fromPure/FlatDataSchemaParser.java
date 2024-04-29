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

package org.finos.legend.engine.external.format.flatdata.grammar.fromPure;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.grammar.antlr4.FlatDataLexerGrammar;
import org.finos.legend.engine.external.format.flatdata.grammar.antlr4.FlatDataParserGrammar;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataString;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility.fromGrammarString;
import static org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility.fromIdentifier;

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
        org.finos.legend.engine.external.format.flatdata.grammar.fromPure.FlatDataErrorListener errorListener = new FlatDataErrorListener();
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
        result.sections = ListIterate.collect(ctx.section(), this::convertSection);

        return result;
    }

    private FlatDataSection convertSection(FlatDataParserGrammar.SectionContext ctx)
    {
        String name = fromIdentifier(ctx.sectionSignature().identifier());

        if (!sectionNames.add(name))
        {
            throw error("Duplicated section name '" + name + "'", ctx.sectionSignature().identifier().getStart());
        }

        FlatDataSection result = new FlatDataSection();
        result.name = name;
        result.driverId = ctx.sectionSignature().driverId().getText();

        result.sectionProperties = ListIterate.collect(ctx.sectionProperty() != null ? ctx.sectionProperty() : Collections.emptyList(), this::convertSectionProperty);

        List<FlatDataParserGrammar.SectionRecordTypeContext> sectionRecordTypeContext = ctx.sectionRecordType();
        if (sectionRecordTypeContext != null && !sectionRecordTypeContext.isEmpty())
        {
            if (sectionRecordTypeContext.size() > 1)
            {
                throw error("Only one Record is permitted in a section", sectionRecordTypeContext.get(1));
            }
            result.recordType = convertFlatDataRecordType(sectionRecordTypeContext.get(0));
        }

        return result;
    }

    private FlatDataProperty convertSectionProperty(FlatDataParserGrammar.SectionPropertyContext ctx)
    {
        FlatDataProperty flatDataProperty = new FlatDataProperty();
        if (ctx.booleanSectionProperty() != null)
        {
            flatDataProperty.name = ctx.booleanSectionProperty().sectionPropertyName().getText();
            flatDataProperty.values = Collections.singletonList(true);
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
                    flatDataProperty.name = name;
                    flatDataProperty.values = Collections.singletonList(fromGrammarString(literal.STRING().getText(), true));
                }
                else if (literal.INTEGER() != null)
                {
                    flatDataProperty.name = name;
                    flatDataProperty.values = Collections.singletonList(Long.parseLong(literal.INTEGER().getText()));
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
                    flatDataProperty.name = name;
                    flatDataProperty.values = Collections.emptyList();
                }
                else if (arrayValues.STRING().size() > 0)
                {
                    flatDataProperty.name = name;
                    flatDataProperty.values = arrayValues.STRING().stream().map(n -> fromGrammarString(n.getText(), true)).collect(Collectors.toList());
                }
                else
                {
                    flatDataProperty.name = name;
                    flatDataProperty.values = arrayValues.INTEGER().stream().map(n -> Long.parseLong(n.getText())).collect(Collectors.toList());
                }
            }
            else
            {
                throw error("No value for section property: " + name, value);
            }
        }
        return flatDataProperty;
    }

    private FlatDataRecordType convertFlatDataRecordType(FlatDataParserGrammar.SectionRecordTypeContext ctx)
    {
        FlatDataRecordType result = new FlatDataRecordType();
        result.fields = ListIterate.collect(ctx.recordTypeFields().recordTypeField(), this::converFlatDataRecordField);
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
                        .map(s -> fromGrammarString(s, true))
                        .collect(Collectors.toList());
                dataTypeAttributes.put(name, values);
            }
        }

        FlatDataDataType dataType;
        switch (ctx.recordTypeDataType().RECORD_DATA_TYPE().getText())
        {
            case "BOOLEAN":
            {
                FlatDataBoolean flatDataBoolean = new FlatDataBoolean();
                flatDataBoolean.optional = optional;
                flatDataBoolean.trueString = singleOptionalAttributeValue(dataTypeAttributes, "trueString", ctx.recordTypeDataType());
                flatDataBoolean.falseString = singleOptionalAttributeValue(dataTypeAttributes, "falseString", ctx.recordTypeDataType());
                dataType = flatDataBoolean;
                break;
            }
            case "STRING":
            {
                FlatDataString flatDataString = new FlatDataString();
                flatDataString.optional = optional;
                dataType = flatDataString;
                break;
            }
            case "INTEGER":
            {
                FlatDataInteger flatDataInteger = new FlatDataInteger();
                flatDataInteger.optional = optional;
                flatDataInteger.format = singleOptionalAttributeValue(dataTypeAttributes, "format", ctx.recordTypeDataType());
                dataType = flatDataInteger;
                break;
            }
            case "DECIMAL":
            {
                FlatDataDecimal flatDataDecimal = new FlatDataDecimal();
                flatDataDecimal.optional = optional;
                flatDataDecimal.format = singleOptionalAttributeValue(dataTypeAttributes, "format", ctx.recordTypeDataType());
                dataType = flatDataDecimal;
                break;
            }
            case "DATE":
            {
                FlatDataDate flatDataDate = new FlatDataDate();
                flatDataDate.optional = optional;
                flatDataDate.format = multiOptionalAttributeValue(dataTypeAttributes, "format");
                dataType = flatDataDate;
                break;
            }
            case "DATETIME":
            {
                FlatDataDateTime flatDataDateTime = new FlatDataDateTime();
                flatDataDateTime.optional = optional;
                flatDataDateTime.timeZone = singleOptionalAttributeValue(dataTypeAttributes, "timeZone", ctx.recordTypeDataType());
                flatDataDateTime.format = multiOptionalAttributeValue(dataTypeAttributes, "format");
                dataType = flatDataDateTime;
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

        FlatDataRecordField flatDataRecordField = new FlatDataRecordField();
        flatDataRecordField.label = label;
        flatDataRecordField.type = dataType;
        flatDataRecordField.address = address;

        return flatDataRecordField;
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
}
