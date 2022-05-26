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

package org.finos.legend.engine.language.graphQL.grammar.from;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.graphQL.grammar.from.antlr4.GraphQLLexer;
import org.finos.legend.engine.language.graphQL.grammar.from.antlr4.GraphQLParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.graphQL.metamodel.Definition;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.ExecutableDocument;
import org.finos.legend.engine.protocol.graphQL.metamodel.executable.*;
import org.finos.legend.engine.protocol.graphQL.metamodel.typeSystem.*;
import org.finos.legend.engine.protocol.graphQL.metamodel.value.*;

import java.util.BitSet;
import java.util.Collections;

public class GraphQLGrammarParser
{
    private GraphQLGrammarParser()
    {
    }

    public static GraphQLGrammarParser newInstance()
    {
        return new GraphQLGrammarParser();
    }

    public Document parseDocument(String code)
    {
        return this.parse(code);
    }

    private Document parse(String code)
    {
        ANTLRErrorListener errorListener = new BaseErrorListener()
        {
            @Override
            public void syntaxError(            Recognizer<?, ?> recognizer,
                                                Object offendingSymbol,
                                                int line,
                                                int charPositionInLine,
                                                String msg,
                                                RecognitionException e)
            {
                if (e != null && e.getOffendingToken() != null && e instanceof InputMismatchException)
                {
                    msg = "Unexpected token";
                }
                else if (e == null || e.getOffendingToken() == null)
                {
                    if (e == null && offendingSymbol instanceof Token && (msg.startsWith("extraneous input") || msg.startsWith("missing ")))
                    {
                        // when ANTLR detects unwanted symbol, it will not result in an error, but throw
                        // `null` with a message like "extraneous input ... expecting ..."
                        // NOTE: this is caused by us having INVALID catch-all symbol in the lexer
                        // so anytime, INVALID token is found, it should cause this error
                        // but because it is a catch-all rule, it only produces a lexer token, which is a symbol
                        // we have to construct the source information manually
                        SourceInformation sourceInformation = new SourceInformation(
                                "",
                                line,
                                charPositionInLine + 1 ,
                                line,
                                charPositionInLine + 1 + ((Token) offendingSymbol).getStopIndex() - ((Token) offendingSymbol).getStartIndex());
                        // NOTE: for some reason sometimes ANTLR report the end index of the token to be smaller than the start index so we must reprocess it here
                        sourceInformation.startColumn = Math.min(sourceInformation.endColumn, sourceInformation.startColumn);
                        msg = "Unexpected token";
                        throw new GraphQLParserException(msg, sourceInformation);
                    }
                    SourceInformation sourceInformation = new SourceInformation(
                            "",
                            line,
                            charPositionInLine + 1,
                            line,
                            charPositionInLine + 1);
                    throw new GraphQLParserException(msg, sourceInformation);
                }
                Token offendingToken = e.getOffendingToken();
                SourceInformation sourceInformation = new SourceInformation(
                        "",
                        line,
                        charPositionInLine + 1 ,
                        offendingToken.getLine(),
                        charPositionInLine + offendingToken.getText().length());
                throw new GraphQLParserException(msg, sourceInformation);
            }

            @Override
            public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet)
            {
            }

            @Override
            public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet)
            {
            }
        };
        GraphQLLexer lexer = new GraphQLLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        GraphQLParser parser = new GraphQLParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return visitDocument(parser.document());
    }


    private Document visitDocument(GraphQLParser.DocumentContext documentContext)
    {
        Document document = new ExecutableDocument();
        document.definitions = ListIterate.collect(documentContext.definition(), this::visitDefinition);
        return document;
    }

    private Definition visitDefinition(GraphQLParser.DefinitionContext definitionContext)
    {
        if (definitionContext.typeSystemDefinition() != null)
        {
            if (definitionContext.typeSystemDefinition().typeDefinition() != null)
            {
                if (definitionContext.typeSystemDefinition().typeDefinition().objectTypeDefinition() != null)
                {
                    return visitObjectTypeDefinition(definitionContext.typeSystemDefinition().typeDefinition().objectTypeDefinition());
                }
                else if (definitionContext.typeSystemDefinition().typeDefinition().interfaceTypeDefinition() != null)
                {
                    return visitInterfaceTypeDefinition(definitionContext.typeSystemDefinition().typeDefinition().interfaceTypeDefinition());
                }
                else if (definitionContext.typeSystemDefinition().typeDefinition().scalarTypeDefinition() != null)
                {
                    return visitScalarDefinition(definitionContext.typeSystemDefinition().typeDefinition().scalarTypeDefinition());
                }
                else if (definitionContext.typeSystemDefinition().typeDefinition().unionTypeDefinition() != null)
                {
                    return visitUnionDefinition(definitionContext.typeSystemDefinition().typeDefinition().unionTypeDefinition());
                }
                else if (definitionContext.typeSystemDefinition().typeDefinition().enumTypeDefinition() != null)
                {
                    return visitEnumDefinition(definitionContext.typeSystemDefinition().typeDefinition().enumTypeDefinition());
                }
                else
                {
                    throw new RuntimeException("Error");
                }
            }
            else if (definitionContext.typeSystemDefinition().directiveDefinition() != null)
            {
                return visitDirectiveDefinition(definitionContext.typeSystemDefinition().directiveDefinition());
            }
            else if (definitionContext.typeSystemDefinition().schemaDefinition() != null)
            {
                return visitSchemaDefinition(definitionContext.typeSystemDefinition().schemaDefinition());
            }
            else
            {
                throw new RuntimeException("Error");
            }
        }
        else if (definitionContext.executableDefinition() != null)
        {
            if (definitionContext.executableDefinition().operationDefinition() != null)
            {
                return visitTypeOperationDefinition(definitionContext.executableDefinition().operationDefinition());
            }
            else if (definitionContext.executableDefinition().fragmentDefinition() != null)
            {
                return visitFragmentDefinition(definitionContext.executableDefinition().fragmentDefinition());
            }
            else
            {
                throw new RuntimeException("Error");
            }
        }
        else
        {
            throw new RuntimeException("Error");
        }
    }

    private SchemaDefinition visitSchemaDefinition(GraphQLParser.SchemaDefinitionContext schemaDefinitionContext)
    {
        SchemaDefinition schemaDefinition = new SchemaDefinition();
        schemaDefinition.rootOperationTypeDefinitions = ListIterate.collect(schemaDefinitionContext.rootOperationTypeDefinition(), o -> {
                RootOperationTypeDefinition root = new RootOperationTypeDefinition();
                root.operationType = OperationType.valueOf(o.operationType().getText());
                root.type = o.namedType().getText();
                return root;
        });
        return schemaDefinition;
    }

    private DirectiveDefinition visitDirectiveDefinition(GraphQLParser.DirectiveDefinitionContext directiveDefinitionContext)
    {
        DirectiveDefinition directiveDefinition = new DirectiveDefinition();
        directiveDefinition.name = directiveDefinitionContext.name().getText();
        directiveDefinition.executableLocation = Lists.mutable.empty();
        directiveDefinition.typeSystemLocation = Lists.mutable.empty();
        ListIterate.forEach(directiveDefinitionContext.directiveLocations().directiveLocation(), v -> {
           if (v.executableDirectiveLocation() != null)
           {
               ExecutableDirectiveLocation res = ExecutableDirectiveLocation.valueOf(v.executableDirectiveLocation().getText());
               directiveDefinition.executableLocation.add(res);
           }
           else if (v.typeSystemDirectiveLocation() != null)
           {
               TypeSystemDirectiveLocation res = TypeSystemDirectiveLocation.valueOf(v.typeSystemDirectiveLocation().getText());
               directiveDefinition.typeSystemLocation.add(res);
           }
           else
           {
               throw new RuntimeException("ERROR");
           }
        });
        directiveDefinition.argumentDefinitions = directiveDefinitionContext.argumentsDefinition() == null ? Collections.emptyList() : ListIterate.collect(directiveDefinitionContext.argumentsDefinition().inputValueDefinition(), this::visitInputValueDefinition);
        return directiveDefinition;
    }


    private FragmentDefinition visitFragmentDefinition(GraphQLParser.FragmentDefinitionContext fragmentDefinitionContext)
    {
        FragmentDefinition fragmentDefinition = new FragmentDefinition();
        fragmentDefinition.name = fragmentDefinitionContext.fragmentName().getText();
        fragmentDefinition.selectionSet = ListIterate.collect(fragmentDefinitionContext.selectionSet().selection(), this::visitSelectionSet);
        fragmentDefinition.typeCondition = fragmentDefinitionContext.typeCondition().namedType() == null ? null : fragmentDefinitionContext.typeCondition().namedType().getText();
        return fragmentDefinition;
    }

    private OperationDefinition visitTypeOperationDefinition(GraphQLParser.OperationDefinitionContext operationDefinitionContext)
    {
        OperationDefinition operationDefinition = new OperationDefinition();
        operationDefinition.name = operationDefinitionContext.name() == null ? null : operationDefinitionContext.name().getText();
        operationDefinition.type = operationDefinitionContext.operationType() == null ? null : OperationType.valueOf(operationDefinitionContext.operationType().getText());
        operationDefinition.variables = operationDefinitionContext.variableDefinitions() == null ? Lists.mutable.empty() : ListIterate.collect(operationDefinitionContext.variableDefinitions().variableDefinition(), this::visitVariableDefinition);
        operationDefinition.selectionSet = ListIterate.collect(operationDefinitionContext.selectionSet().selection(), this::visitSelectionSet);
        return operationDefinition;
    }

    private VariableDefinition visitVariableDefinition(GraphQLParser.VariableDefinitionContext variableDefinitionContext)
    {
        VariableDefinition variableDefinition = new VariableDefinition();
        variableDefinition.name = variableDefinitionContext.variable().getText();
        variableDefinition.type = visitType(variableDefinitionContext.type_());
        variableDefinition.defaultValue = variableDefinitionContext.defaultValue() == null ? null : visitValue(variableDefinitionContext.defaultValue().value());
        return variableDefinition;
    }

    private Selection visitSelectionSet(GraphQLParser.SelectionContext selectionContext)
    {
        if (selectionContext.field() != null)
        {
            GraphQLParser.FieldContext fieldContext = selectionContext.field();
            Field field = new Field();
            field.alias = fieldContext.alias() == null ? null : fieldContext.alias().getText();
            field.name = fieldContext.name().getText();
            field.arguments = fieldContext.arguments() == null ? Collections.emptyList() : ListIterate.collect(fieldContext.arguments().argument(), this::visitArgument);
            field.selectionSet = fieldContext.selectionSet() == null ? Collections.emptyList() : ListIterate.collect(fieldContext.selectionSet().selection(), this::visitSelectionSet);
            return field;
        }
        else if (selectionContext.fragmentSpread() != null)
        {
            GraphQLParser.FragmentSpreadContext fragmentSpreadContext = selectionContext.fragmentSpread();
            FragmentSpread fragmentSpread = new FragmentSpread();
            fragmentSpread.name = fragmentSpreadContext.fragmentName().getText();
            return fragmentSpread;
        }
        else
        {
            throw new RuntimeException("");
        }
    }

    private Argument visitArgument(GraphQLParser.ArgumentContext argumentContext)
    {
        Argument argument = new Argument();
        argument.name = argumentContext.name().getText();
        argument.value = visitValue(argumentContext.value());
        return argument;
    }

    private ObjectTypeDefinition visitObjectTypeDefinition(GraphQLParser.ObjectTypeDefinitionContext objectTypeDefinitionContext)
    {
        ObjectTypeDefinition objectTypeDefinition = new ObjectTypeDefinition();
        objectTypeDefinition.name = objectTypeDefinitionContext.name().getText();
        objectTypeDefinition.fields = objectTypeDefinitionContext.fieldsDefinition() == null ? Collections.emptyList() : ListIterate.collect(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition(), this::visitFieldsDefinitionContext);
        objectTypeDefinition._implements = visitImplementInterface(objectTypeDefinitionContext.implementsInterfaces(), Lists.mutable.empty()).reverseThis();
        return objectTypeDefinition;
    }

    private InterfaceTypeDefinition visitInterfaceTypeDefinition(GraphQLParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext)
    {
        InterfaceTypeDefinition interfaceTypeDefinition = new InterfaceTypeDefinition();
        interfaceTypeDefinition.name = interfaceTypeDefinitionContext.name().getText();
        interfaceTypeDefinition.fields = ListIterate.collect(interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition(), this::visitFieldsDefinitionContext);
        return interfaceTypeDefinition;
    }

    private Definition visitScalarDefinition(GraphQLParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext)
    {
        ScalarTypeDefinition scalarTypeDefinition = new ScalarTypeDefinition();
        scalarTypeDefinition.name = scalarTypeDefinitionContext.name().getText();
        return scalarTypeDefinition;
    }

    private Definition visitUnionDefinition(GraphQLParser.UnionTypeDefinitionContext unionTypeDefinitionContext)
    {
        UnionTypeDefinition unionTypeDefinition = new UnionTypeDefinition();
        unionTypeDefinition.name = unionTypeDefinitionContext.name().getText();
        unionTypeDefinition.members = ListIterate.collect(unionTypeDefinitionContext.unionMemberTypes().namedType(), o -> o.name().getText());
        return unionTypeDefinition;
    }

    private EnumTypeDefinition visitEnumDefinition(GraphQLParser.EnumTypeDefinitionContext enumTypeDefinitionContext)
    {
        EnumTypeDefinition enumTypeDefinition = new EnumTypeDefinition();
        enumTypeDefinition.name = enumTypeDefinitionContext.name().getText();
        enumTypeDefinition.values = enumTypeDefinitionContext.enumValuesDefinition() == null ? Collections.emptyList() : ListIterate.collect(enumTypeDefinitionContext.enumValuesDefinition().enumValueDefinition(), o -> {
          EnumValueDefinition enumValueDefinition = new EnumValueDefinition();
            enumValueDefinition.value = o.enumValue().getText();
          return enumValueDefinition;
        });
        return enumTypeDefinition;
    }

    private MutableList<String> visitImplementInterface(GraphQLParser.ImplementsInterfacesContext implementsInterfaces, MutableList<String> empty)
    {
        if (implementsInterfaces != null)
        {
            empty.add(implementsInterfaces.namedType().getText());
            visitImplementInterface(implementsInterfaces.implementsInterfaces(), empty);
        }
        return empty;
    }

    private FieldDefinition visitFieldsDefinitionContext(GraphQLParser.FieldDefinitionContext fieldsDefinitionContext)
    {
        FieldDefinition fieldDefinition = new FieldDefinition();
        fieldDefinition.name = fieldsDefinitionContext.name().getText();
        fieldDefinition.type = visitType(fieldsDefinitionContext.type_());
        if (fieldsDefinitionContext.argumentsDefinition() != null)
        {
            fieldDefinition.argumentDefinitions = ListIterate.collect(fieldsDefinitionContext.argumentsDefinition().inputValueDefinition(), this::visitInputValueDefinition);
        }
        return fieldDefinition;
    }

    private InputValueDefinition visitInputValueDefinition(GraphQLParser.InputValueDefinitionContext inputValueDefinitionContext)
    {
        InputValueDefinition inputValueDefinition = new InputValueDefinition();
        inputValueDefinition.description = inputValueDefinitionContext.description() != null ? inputValueDefinitionContext.description().getText() : null;
        inputValueDefinition.name = inputValueDefinitionContext.name().getText();
        inputValueDefinition.type = visitType(inputValueDefinitionContext.type_());
        inputValueDefinition.defaultValue = inputValueDefinitionContext.defaultValue() == null ? null : visitValue(inputValueDefinitionContext.defaultValue().value());
        return inputValueDefinition;
    }

    private TypeReference visitType(GraphQLParser.Type_Context typeContext)
    {
        TypeReference typeReference;

        if (typeContext.namedType() != null)
        {
            NamedTypeReference namedTypeReference = new NamedTypeReference();
            namedTypeReference.name = typeContext.namedType().getText();
            typeReference = namedTypeReference;
        }
        else if (typeContext.listType() != null)
        {
            ListTypeReference listTypeReference = new ListTypeReference();
            listTypeReference.itemType = visitType(typeContext.listType().type_());
            typeReference = listTypeReference;
        }
        else
        {
            throw new RuntimeException("ERROR");
        }
        typeReference.nullable = !typeContext.getText().trim().endsWith("!");
        return typeReference;
    }

    private Value visitValue(GraphQLParser.ValueContext valueContext)
    {
        if (valueContext.enumValue() != null)
        {
            EnumValue value = new EnumValue();
            value.value = valueContext.enumValue().getText();
            return value;
        }
        if (valueContext.intValue() != null)
        {
            IntValue value = new IntValue();
            value.value = Integer.parseInt(valueContext.intValue().getText());
            return value;
        }
        if (valueContext.stringValue() != null)
        {
            StringValue value = new StringValue();
            String in = valueContext.stringValue().STRING().getText().trim();
            value.value = in.substring(1, in.length() - 1);
            return value;
        }
        if (valueContext.booleanValue() != null)
        {
            BooleanValue value = new BooleanValue();
            value.value = Boolean.parseBoolean(valueContext.booleanValue().getText());
            return value;
        }
        if (valueContext.floatValue() != null)
        {
            FloatValue value = new FloatValue();
            value.value = Double.parseDouble(valueContext.floatValue().getText());
            return value;
        }
        if (valueContext.nullValue() != null)
        {
            return new NullValue();
        }
        if (valueContext.variable() != null)
        {
            Variable var = new Variable();
            String in = valueContext.variable().getText().trim();
            var.name = in.substring(1);
            return var;
        }
        if (valueContext.listValue() != null)
        {
            ListValue listValue = new ListValue();
            listValue.values = ListIterate.collect(valueContext.listValue().value(), this::visitValue);
            return listValue;
        }
        throw new RuntimeException("Error");
    }

}
