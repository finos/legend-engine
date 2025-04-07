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

package org.finos.legend.engine.language.stores.elasticsearch.v7.from;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.from.IAuthenticationGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ElasticsearchLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ElasticsearchParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ElasticsearchParserGrammarBaseVisitor;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ElasticsearchConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ElasticsearchConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreURLSourceSpecification;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7Store;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndex;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.Elasticsearch7StoreIndexProperty;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.specification.types.mapping.*;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class ElasticsearchStoreParseTreeWalker
{
    private final SourceCodeParserInfo parserInfo;
    private final PropertyDefinitionParseTreeWalker propertyDefinitionParseTreeWalker = new PropertyDefinitionParseTreeWalker();
    private final PureGrammarParserExtensions extension;

    public ElasticsearchStoreParseTreeWalker(PureGrammarParserExtensions extension, SourceCodeParserInfo parserInfo)
    {
        this.parserInfo = parserInfo;
        this.extension = extension;
    }

    public Section visit(String sectionType, Consumer<PackageableElement> elementConsumer, ElasticsearchParserGrammar.DefinitionContext definitionContext)
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

        this.visit(definitionContext.v7StoreDefinition(), sectionConsumer);

        return section;
    }

    private void visit(List<ElasticsearchParserGrammar.V7StoreDefinitionContext> v7StoreDefinition, Consumer<PackageableElement> elementConsumer)
    {
        v7StoreDefinition.stream().map(this::visitV7Store).forEach(elementConsumer);
    }

    private Elasticsearch7Store visitV7Store(ElasticsearchParserGrammar.V7StoreDefinitionContext ctx)
    {
        Elasticsearch7Store store = new Elasticsearch7Store();
        store.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        store._package = PureGrammarParserUtility.fromPath(Optional.ofNullable(ctx.qualifiedName().packagePath()).map(ElasticsearchParserGrammar.PackagePathContext::identifier).orElse(Collections.emptyList()));
        store.sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(ctx);
        store.indices = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.indices(), ElasticsearchLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchLexerGrammar.INDICES), store.sourceInformation)
                .indexDefinition()
                .stream()
                .map(this::visitIndex)
                .collect(Collectors.toList());
        return store;
    }

    private Elasticsearch7StoreIndex visitIndex(ElasticsearchParserGrammar.IndexDefinitionContext indicesContext)
    {
        Elasticsearch7StoreIndex index = new Elasticsearch7StoreIndex();
        index.indexName = PureGrammarParserUtility.fromIdentifier(indicesContext.indexName());
        index.properties = PureGrammarParserUtility.validateAndExtractRequiredField(
                        indicesContext.propertiesDefinition(),
                        ElasticsearchLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchLexerGrammar.PROPERTIES),
                        this.parserInfo.walkerSourceInformation.getSourceInformation(indicesContext)
                )
                .propertiesArrayDefinition()
                .namedPropertyDefinition()
                .stream()
                .map(this::visitIndexProperty)
                .collect(Collectors.toList());
        return index;
    }

    private Elasticsearch7StoreIndexProperty visitIndexProperty(ElasticsearchParserGrammar.NamedPropertyDefinitionContext propertyDefinitionContext)
    {
        Pair<String, Property> propertyPair = this.visitNamedPropertyDefinition(propertyDefinitionContext);

        Elasticsearch7StoreIndexProperty property = new Elasticsearch7StoreIndexProperty();
        property.propertyName = propertyPair.getOne();
        property.property = propertyPair.getTwo();

        return property;
    }

    private Pair<String, Property> visitNamedPropertyDefinition(ElasticsearchParserGrammar.NamedPropertyDefinitionContext propertyDefinitionContext)
    {
        String name = PureGrammarParserUtility.fromIdentifier(propertyDefinitionContext.propertyName());
        Property property = this.visitProperty(propertyDefinitionContext.propertyTypeDefinition().getRuleContext(ParserRuleContext.class, 0));
        return Tuples.pair(name, property);
    }

    private Property visitProperty(ParserRuleContext propertyCtx)
    {
        Property property = propertyCtx.accept(this.propertyDefinitionParseTreeWalker);
        if (property == null)
        {
            throw new EngineException("Parsing unsupported type: " + propertyCtx.getStart().getText(), parserInfo.walkerSourceInformation.getSourceInformation(propertyCtx), EngineErrorType.PARSER);
        }
        return property;
    }

    private void processScalarPropertyContent(ElasticsearchParserGrammar.ScalarPropertyContentContext scalarPropertyContentContext, CorePropertyBase docValuesPropertyBase)
    {
        if (scalarPropertyContentContext == null)
        {
            return;
        }

        // process inner fields
        List<ElasticsearchParserGrammar.NamedPropertyDefinitionContext> propertyDefinitionContexts = Optional.ofNullable(
                        PureGrammarParserUtility.validateAndExtractOptionalField(
                                scalarPropertyContentContext.fieldsDefinition(),
                                ElasticsearchLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchLexerGrammar.FIELDS),
                                parserInfo.walkerSourceInformation.getSourceInformation(scalarPropertyContentContext)
                        )
                ).map(ElasticsearchParserGrammar.FieldsDefinitionContext::propertiesArrayDefinition)
                .map(ElasticsearchParserGrammar.PropertiesArrayDefinitionContext::namedPropertyDefinition)
                .orElse(Collections.emptyList());

        docValuesPropertyBase.fields = propertyDefinitionContexts.stream()
                .map(ElasticsearchStoreParseTreeWalker.this::visitNamedPropertyDefinition)
                .collect(Collectors.toMap(Pair::getOne, Pair::getTwo));
    }

    private void processComplexPropertyContent(ElasticsearchParserGrammar.ComplexPropertyContentContext complexPropertyContentContext, CorePropertyBase complexObject)
    {
        // process inner fields
        List<ElasticsearchParserGrammar.NamedPropertyDefinitionContext> propertyDefinitionContexts = Optional.ofNullable(
                        PureGrammarParserUtility.validateAndExtractRequiredField(
                                complexPropertyContentContext.propertiesDefinition(),
                                ElasticsearchLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchLexerGrammar.PROPERTIES),
                                parserInfo.walkerSourceInformation.getSourceInformation(complexPropertyContentContext)
                        )
                ).map(ElasticsearchParserGrammar.PropertiesDefinitionContext::propertiesArrayDefinition)
                .map(ElasticsearchParserGrammar.PropertiesArrayDefinitionContext::namedPropertyDefinition)
                .orElse(Collections.emptyList());

        complexObject.properties = propertyDefinitionContexts.stream()
                .map(ElasticsearchStoreParseTreeWalker.this::visitNamedPropertyDefinition)
                .collect(Collectors.toMap(Pair::getOne, Pair::getTwo));
    }

    private class PropertyDefinitionParseTreeWalker extends ElasticsearchParserGrammarBaseVisitor<Property>
    {
        @Override
        public Property visitScalarPropertyDefinition(ElasticsearchParserGrammar.ScalarPropertyDefinitionContext ctx)
        {
            ElasticsearchParserGrammar.ScalarPropertyTypesContext scalarPropertyTypesContext = ctx.scalarPropertyTypes();
            TerminalNode type = scalarPropertyTypesContext.getChild(TerminalNode.class, 0);

            Property property = new Property();

            switch (type.getSymbol().getType())
            {
                case ElasticsearchParserGrammar.KEYWORD:
                    property.keyword = new KeywordProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.keyword);
                    break;
                case ElasticsearchParserGrammar.TEXT:
                    property.text = new TextProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.text);
                    break;
                case ElasticsearchParserGrammar.DATE:
                    property.date = new DateProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.date);
                    break;
                case ElasticsearchParserGrammar.SHORT:
                    property._short = new ShortNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._short);
                    break;
                case ElasticsearchParserGrammar.BYTE:
                    property._byte = new ByteNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._byte);
                    break;
                case ElasticsearchParserGrammar.INTEGER:
                    property.integer = new IntegerNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.integer);
                    break;
                case ElasticsearchParserGrammar.LONG:
                    property._long = new LongNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._long);
                    break;
                case ElasticsearchParserGrammar.FLOAT:
                    property._float = new FloatNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._float);
                    break;
                case ElasticsearchParserGrammar.HALF_FLOAT:
                    property.half_float = new HalfFloatNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property.half_float);
                    break;
                case ElasticsearchParserGrammar.DOUBLE:
                    property._double = new DoubleNumberProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._double);
                    break;
                case ElasticsearchParserGrammar.BOOLEAN:
                    property._boolean = new BooleanProperty();
                    ElasticsearchStoreParseTreeWalker.this.processScalarPropertyContent(ctx.scalarPropertyContent(), property._boolean);
                    break;
            }

            return property;
        }

        @Override
        public Property visitComplexPropertyDefinition(ElasticsearchParserGrammar.ComplexPropertyDefinitionContext ctx)
        {
            ElasticsearchParserGrammar.ComplexPropertyTypesContext complexPropertyTypesContext = ctx.complexPropertyTypes();
            TerminalNode type = complexPropertyTypesContext.getChild(TerminalNode.class, 0);

            Property property = new Property();

            switch (type.getSymbol().getType())
            {
                case ElasticsearchParserGrammar.OBJECT:
                    property.object = new ObjectProperty();
                    ElasticsearchStoreParseTreeWalker.this.processComplexPropertyContent(ctx.complexPropertyContent(), property.object);
                    break;
                case ElasticsearchParserGrammar.NESTED:
                    property.nested = new NestedProperty();
                    ElasticsearchStoreParseTreeWalker.this.processComplexPropertyContent(ctx.complexPropertyContent(), property.nested);
                    break;
            }

            return property;
        }
    }

    public Elasticsearch7StoreConnection visit(ElasticsearchConnectionParserGrammar.V7ConnectionDefinitionContext v7ConnDefContext)
    {
        SourceInformation sourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(v7ConnDefContext);

        Elasticsearch7StoreConnection storeConnection = new Elasticsearch7StoreConnection();
        storeConnection.sourceInformation = sourceInformation;

        ElasticsearchConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                v7ConnDefContext.connectionStore(),
                ElasticsearchConnectionLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchConnectionLexerGrammar.STORE),
                sourceInformation
        );

        storeConnection.element = PureGrammarParserUtility.fromQualifiedName(Optional.ofNullable(storeContext.qualifiedName().packagePath()).map(ElasticsearchConnectionParserGrammar.PackagePathContext::identifier).orElse(Collections.emptyList()), storeContext.qualifiedName().identifier());
        storeConnection.elementSourceInformation = this.parserInfo.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());

        ElasticsearchConnectionParserGrammar.ClusterDetailsContext clusterDetailsContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                v7ConnDefContext.clusterDetails(),
                ElasticsearchConnectionLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchConnectionLexerGrammar.CLUSTER_DETAILS),
                sourceInformation
        );

        storeConnection.sourceSpec = this.visit(clusterDetailsContext);

        ElasticsearchConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractRequiredField(
                v7ConnDefContext.authentication(),
                ElasticsearchConnectionLexerGrammar.VOCABULARY.getLiteralName(ElasticsearchConnectionLexerGrammar.AUTHENTICATION),
                sourceInformation
        );
        storeConnection.authSpec = IAuthenticationGrammarParserExtension.parseAuthentication(authenticationContext.islandDefinition(), this.parserInfo.walkerSourceInformation, this.extension);

        return storeConnection;
    }

    private Elasticsearch7StoreURLSourceSpecification visit(ElasticsearchConnectionParserGrammar.ClusterDetailsContext clusterDetailsContext)
    {
        Elasticsearch7StoreURLSourceSpecification specification = new Elasticsearch7StoreURLSourceSpecification();
        String clusterDetailsType = clusterDetailsContext.islandDefinition().ISLAND_OPEN().getText();

        String maybeUrlType = clusterDetailsType.substring(1, clusterDetailsType.length() - 1).trim();
        Assert.assertTrue("URL".equals(maybeUrlType), () -> "Unsupported cluster details type: " + maybeUrlType + ".  Supported: URL", this.parserInfo.walkerSourceInformation.getSourceInformation(clusterDetailsContext.islandDefinition().ISLAND_OPEN().getSymbol()), EngineErrorType.PARSER);

        ElasticsearchConnectionParserGrammar.IslandContentContext urlContext = clusterDetailsContext.islandDefinition().islandContent();
        urlContext.removeLastChild();
        String maybeUri = urlContext.getText();
        try
        {
            specification.url = new URI(maybeUri.trim());
        }
        catch (URISyntaxException e)
        {
            throw new EngineException("URL is not valid", this.parserInfo.walkerSourceInformation.getSourceInformation(urlContext), EngineErrorType.PARSER, e);
        }

        return specification;
    }
}
