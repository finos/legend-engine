// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.deephaven.from;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.utility.ListIterate;

import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.IAuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammarBaseVisitor;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.FloatType;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenSourceSpecification;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.DeephavenStore;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Table;
import org.finos.legend.engine.protocol.deephaven.metamodel.store.Column;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.BooleanType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.IntType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.StringType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.DateTimeType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.Type;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;
import java.util.function.Consumer;

public class DeephavenParseTreeWalker
{
    private final SourceCodeParserInfo parserInfo;
    private final PureGrammarParserExtensions extension;

    public DeephavenParseTreeWalker(PureGrammarParserExtensions extension, SourceCodeParserInfo parserInfo)
    {
        this.parserInfo = parserInfo;
        this.extension = extension;
    }

    public Section visit(String sectionType, Consumer<PackageableElement> elementConsumer, DeephavenParserGrammar.DefinitionContext definitionContext)
    {
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = sectionType;
        section.sourceInformation = parserInfo.sourceInformation;
        section.imports = ListIterate.collect(definitionContext.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));

        Consumer<PackageableElement> sectionConsumer = x ->
        {
            section.elements.add(x.getPath());
            elementConsumer.accept(x);
        };

        this.visit(definitionContext.deephavenDefinition(), sectionConsumer);

        return section;
    }

    private void visit(List<DeephavenParserGrammar.DeephavenDefinitionContext> deephavenStoreDefinition, Consumer<PackageableElement> elementConsumer)
    {
        deephavenStoreDefinition.stream().map(this::visitDeephavenStore).forEach(elementConsumer);
    }

    private DeephavenStore visitDeephavenStore(DeephavenParserGrammar.DeephavenDefinitionContext ctx)
    {
        DeephavenStore store = new DeephavenStore();
        // required fields for all stores
        store.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        store._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        store.sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(ctx);
        
        store.tables = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.tables(), DeephavenLexerGrammar.VOCABULARY.getLiteralName(DeephavenLexerGrammar.TABLES), store.sourceInformation)
                .tableDefinition()
                .stream()
                .map(this::visitTableDefinition)
                .collect(Collectors.toList());
        return store;
    }

    private Table visitTableDefinition(DeephavenParserGrammar.TableDefinitionContext tableDefinitionContext)
    {
        Table table = new Table();
        table.name = PureGrammarParserUtility.fromIdentifier(tableDefinitionContext.tableName());
        table.columns = PureGrammarParserUtility.validateAndExtractRequiredField(
                    tableDefinitionContext.columns(),
                    DeephavenLexerGrammar.VOCABULARY.getLiteralName(DeephavenLexerGrammar.COLUMNS),
                    this.parserInfo.walkerSourceInformation.getSourceInformation(tableDefinitionContext)
                )
                .columnDefinition()
                .stream()
                .map(this::visitColumn)
                .collect(Collectors.toList());
        return table;
    }

    private Column visitColumn(DeephavenParserGrammar.ColumnDefinitionContext columnDefinitionContext)
    {
        Column column = new Column();
        column.name = PureGrammarParserUtility.fromIdentifier(columnDefinitionContext.columnName());
        column.type = new ColumnDefinitionParseTreeWalker().visitColumnType(columnDefinitionContext.columnType());
        return column;
    }

    private class ColumnDefinitionParseTreeWalker extends DeephavenParserGrammarBaseVisitor<Type>
    {
        @Override
        public Type visitColumnType(DeephavenParserGrammar.ColumnTypeContext columnTypeContext)
        {
            TerminalNode type = columnTypeContext.getChild(TerminalNode.class, 0);
            Type columnType;

            switch (type.getSymbol().getType())
            {
                case DeephavenParserGrammar.DATE_TIME:
                    columnType = new DateTimeType();
                    break;
                case DeephavenParserGrammar.STRING:
                    columnType = new StringType();
                    break;
                case DeephavenParserGrammar.INT:
                    columnType = new IntType();
                    break;
                case DeephavenParserGrammar.BOOLEAN:
                    columnType = new BooleanType();
                    break;
                case DeephavenParserGrammar.FLOAT:
                    columnType = new FloatType();
                    break;
                default:
                    throw new EngineException("Unsupported column type: " + type.getText(), parserInfo.walkerSourceInformation.getSourceInformation(columnTypeContext), EngineErrorType.PARSER);
            }

            return columnType;
        }
    }

    public DeephavenConnection visit(DeephavenConnectionParserGrammar.DeephavenConnectionDefinitionContext deephavenConnectionCtx)
    {
        SourceInformation sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(deephavenConnectionCtx);

        DeephavenConnection storeConnection = new DeephavenConnection();
        storeConnection.sourceInformation = sourceInformation;

        DeephavenConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                deephavenConnectionCtx.connectionStore(),
                DeephavenConnectionLexerGrammar.VOCABULARY.getLiteralName(DeephavenConnectionLexerGrammar.STORE),
                sourceInformation
        );

        storeConnection.element = PureGrammarParserUtility.fromQualifiedName(Optional.ofNullable(storeContext.qualifiedName().packagePath()).map(DeephavenConnectionParserGrammar.PackagePathContext::identifier).orElse(Collections.emptyList()), storeContext.qualifiedName().identifier());
        storeConnection.elementSourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());

        DeephavenConnectionParserGrammar.ServerUrlDefinitionContext serverUrlContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                deephavenConnectionCtx.serverUrlDefinition(),
                DeephavenConnectionLexerGrammar.VOCABULARY.getLiteralName(DeephavenConnectionLexerGrammar.SERVER_URL_DEFINITION),
                sourceInformation
        );

        DeephavenSourceSpecification sourceSpecification = new DeephavenSourceSpecification();
        String maybeUri = serverUrlContext.serverUrl().getText();
        try
        {
            sourceSpecification.url = new URI(maybeUri.trim().replaceAll("(^')|('$)",""));
        }
        catch (URISyntaxException e)
        {
            throw new EngineException("URL format is not valid", this.parserInfo.walkerSourceInformation.getSourceInformation(serverUrlContext), EngineErrorType.PARSER, e);
        }
        storeConnection.sourceSpec = sourceSpecification;

        DeephavenConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                deephavenConnectionCtx.authentication(),
                DeephavenConnectionLexerGrammar.VOCABULARY.getLiteralName(DeephavenConnectionLexerGrammar.AUTHENTICATION),
                sourceInformation
        );
        storeConnection.authSpec = IAuthenticationGrammarParserExtension.parseAuthentication(authenticationContext.islandDefinition(), this.parserInfo.walkerSourceInformation, this.extension);

        return storeConnection;
    }
}