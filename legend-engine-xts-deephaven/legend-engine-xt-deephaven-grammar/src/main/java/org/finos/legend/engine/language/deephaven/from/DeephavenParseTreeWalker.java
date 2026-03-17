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

import org.finos.legend.engine.protocol.deephaven.metamodel.DeephavenApp;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.IAuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammarBaseVisitor;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.DecimalType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.DoubleType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.FloatType;
import org.finos.legend.engine.protocol.deephaven.metamodel.type.TimestampType;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
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
        this.visitDeephavenApps(definitionContext.deephavenAppDefinition(), sectionConsumer);

        return section;
    }

    private void visit(List<DeephavenParserGrammar.DeephavenDefinitionContext> deephavenStoreDefinition, Consumer<PackageableElement> elementConsumer)
    {
        deephavenStoreDefinition.stream().map(this::visitDeephavenStore).forEach(elementConsumer);
    }

    private void visitDeephavenApps(List<DeephavenParserGrammar.DeephavenAppDefinitionContext> deephavenAppDefinitions, Consumer<PackageableElement> elementConsumer)
    {
        if (deephavenAppDefinitions != null)
        {
            deephavenAppDefinitions.stream().map(this::visitDeephavenApp).forEach(elementConsumer);
        }
    }

    private DeephavenApp visitDeephavenApp(DeephavenParserGrammar.DeephavenAppDefinitionContext ctx)
    {
        DeephavenApp app = new DeephavenApp();
        app.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        app._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        app.sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(ctx);

        DeephavenParserGrammar.AppApplicationNameContext appNameCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.appApplicationName(), "applicationName", app.sourceInformation);
        app.applicationName = PureGrammarParserUtility.fromGrammarString(appNameCtx.STRING().getText(), true);

        DeephavenParserGrammar.AppFunctionContext functionCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.appFunction(), "function", app.sourceInformation);
        app.function = new PackageableElementPointer(
                PackageableElementType.FUNCTION,
                functionCtx.appFunctionIdentifier().getText(),
                this.parserInfo.walkerSourceInformation.getSourceInformation(functionCtx.appFunctionIdentifier())
        );

        DeephavenParserGrammar.AppOwnershipContext ownerCtx = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.appOwnership(), "ownership", app.sourceInformation);
        app.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(ownerCtx.STRING().getText(), true));

        DeephavenParserGrammar.AppDescriptionContext descCtx = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.appDescription(), "description", app.sourceInformation);
        if (descCtx != null)
        {
            app.description = PureGrammarParserUtility.fromGrammarString(descCtx.STRING().getText(), true);
        }

        return app;
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
            if (columnTypeContext.decimalType() != null)
            {
                return parseDecimalType(columnTypeContext.decimalType());
            }

            TerminalNode type = columnTypeContext.getChild(TerminalNode.class, 0);
            Type columnType;

            switch (type.getSymbol().getType())
            {
                case DeephavenParserGrammar.DECIMAL_TYPE:
                    columnType = parseDecimalType((DeephavenParserGrammar.DecimalTypeContext) columnTypeContext.getChild(0));
                    break;
                case DeephavenParserGrammar.DATETIME_TYPE:
                    columnType = new DateTimeType();
                    break;
                case DeephavenParserGrammar.STRING_TYPE:
                    columnType = new StringType();
                    break;
                case DeephavenParserGrammar.INT_TYPE:
                    columnType = new IntType();
                    break;
                case DeephavenParserGrammar.BOOLEAN_TYPE:
                    columnType = new BooleanType();
                    break;
                case DeephavenParserGrammar.FLOAT_TYPE:
                    columnType = new FloatType();
                    break;
                case DeephavenParserGrammar.DOUBLE_TYPE:
                    columnType = new DoubleType();
                    break;
                case DeephavenParserGrammar.TIMESTAMP_TYPE:
                    columnType = new TimestampType();
                    break;
                default:
                    throw new EngineException("Unsupported column type: " + type.getText(), parserInfo.walkerSourceInformation.getSourceInformation(columnTypeContext), EngineErrorType.PARSER);
            }

            return columnType;
        }

        private Type parseDecimalType(DeephavenParserGrammar.DecimalTypeContext decimalTypeContext)
        {
            DecimalType decimalType = new DecimalType();
            decimalType.precision = Long.parseLong(decimalTypeContext.precision.getText());
            decimalType.scale = Long.parseLong(decimalTypeContext.scale.getText());
            return decimalType;
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