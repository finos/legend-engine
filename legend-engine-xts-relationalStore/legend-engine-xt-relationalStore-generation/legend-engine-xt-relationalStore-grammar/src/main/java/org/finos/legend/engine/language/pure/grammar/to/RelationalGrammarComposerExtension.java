// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.data.RelationalEmbeddedDataComposer;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.ExtractSubQueriesAsCTEsPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.RelationalMapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderAnnotations;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;

public class RelationalGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "-Core");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof Database)
        {
            return renderDatabase((Database) element, context);
        }
        return null;
    });

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> postprocessorRenderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof RelationalMapper)
        {
            return renderRelationalMapper((RelationalMapper) element, context);
        }
        return null;
    });

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderer = Lists.mutable.withAll(renderers).withAll(postprocessorRenderers);

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderer;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(
                buildSectionComposer(RelationalGrammarParserExtension.NAME, renderers),
                buildSectionComposer(RelationalGrammarParserExtension.POSTPROCESSOR_NAME, postprocessorRenderers)
        );
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {

        return Lists.mutable.with(
                (elements, context, composedSections) ->
                {
                    List<Database> composableElements = ListIterate.selectInstancesOf(elements, Database.class);
                    return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, x -> RelationalGrammarComposerExtension.renderDatabase(x, context)).makeString("###" + RelationalGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
                },
                (elements, context, composedSections) ->
                {
                    List<RelationalMapper> composableElements = ListIterate.selectInstancesOf(elements, RelationalMapper.class);
                    return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, e -> renderRelationalMapper(e, context)).makeString("###" + RelationalGrammarParserExtension.POSTPROCESSOR_NAME + "\n", "\n\n", ""), composableElements);
                }
        );
    }

    @Override
    public List<Function2<ClassMapping, PureGrammarComposerContext, String>> getExtraClassMappingComposers()
    {
        return Lists.mutable.with((classMapping, context) ->
        {
            if (classMapping instanceof RootRelationalClassMapping)
            {
                RelationalGrammarComposerContext ctx = RelationalGrammarComposerContext.Builder.newInstance(context).build();
                RootRelationalClassMapping rootRelationalClassMapping = (RootRelationalClassMapping) classMapping;
                StringBuilder builder = new StringBuilder();
                builder.append(": Relational\n");
                builder.append(getTabString()).append("{\n");
                if (rootRelationalClassMapping.filter != null)
                {
                    appendTabString(builder, 2).append(HelperRelationalGrammarComposer.renderFilterMapping(rootRelationalClassMapping.filter)).append("\n");
                }
                builder.append(rootRelationalClassMapping.distinct ? (getTabString(2) + "~distinct\n") : "");
                if (!rootRelationalClassMapping.groupBy.isEmpty())
                {
                    appendTabString(builder, 2).append("~groupBy\n");
                    appendTabString(builder, 2).append("(\n");
                    builder.append(LazyIterate.collect(rootRelationalClassMapping.groupBy, op -> getTabString(3) + HelperRelationalGrammarComposer.renderRelationalOperationElement(op, ctx)).makeString(",\n"));
                    builder.append("\n");
                    appendTabString(builder, 2).append(")\n");
                }
                if (!rootRelationalClassMapping.primaryKey.isEmpty())
                {
                    appendTabString(builder, 2).append("~primaryKey\n");
                    appendTabString(builder, 2).append("(\n");
                    builder.append(LazyIterate.collect(rootRelationalClassMapping.primaryKey, op -> getTabString(3) + HelperRelationalGrammarComposer.renderRelationalOperationElement(op, ctx)).makeString(",\n"));
                    builder.append("\n");
                    appendTabString(builder, 2).append(")\n");
                }
                if (rootRelationalClassMapping.mainTable != null)
                {
                    TablePtr tablePtr = rootRelationalClassMapping.mainTable;
                    appendTabString(builder, 2).append("~mainTable ");
                    builder.append("[").append(tablePtr.getDb()).append("]");
                    builder.append((tablePtr.schema != null && !tablePtr.schema.equals("default")) ? (tablePtr.schema + "." + tablePtr.table) : tablePtr.table);
                    builder.append("\n");
                }
                if (!rootRelationalClassMapping.propertyMappings.isEmpty())
                {
                    RelationalGrammarComposerContext indentedContext = RelationalGrammarComposerContext.Builder.newInstance(ctx).withIndentation(4).build();
                    builder.append(LazyIterate.collect(rootRelationalClassMapping.propertyMappings, propertyMapping -> HelperRelationalGrammarComposer.renderAbstractRelationalPropertyMapping(propertyMapping, indentedContext, false)).makeString(",\n"));
                    builder.append("\n");
                }
                appendTabString(builder).append("}");
                return builder.toString();
            }
            return null;
        });
    }

    @Override
    public List<Function2<AssociationMapping, PureGrammarComposerContext, String>> getExtraAssociationMappingComposers()
    {
        return Lists.mutable.with((associationMapping, context) ->
        {
            if (associationMapping instanceof RelationalAssociationMapping)
            {
                RelationalGrammarComposerContext ctx = RelationalGrammarComposerContext.Builder.newInstance(context).build();
                RelationalAssociationMapping relationalAssociationMapping = (RelationalAssociationMapping) associationMapping;
                StringBuilder associationMappingBuilder = new StringBuilder();
                associationMappingBuilder.append(relationalAssociationMapping.association.path).append(": ").append("Relational\n");
                associationMappingBuilder.append(PureGrammarComposerUtility.getTabString()).append("{\n");
                associationMappingBuilder.append(getTabString(2)).append("AssociationMapping").append("\n");
                associationMappingBuilder.append(PureGrammarComposerUtility.getTabString(2)).append("(").append("\n");
                if (!relationalAssociationMapping.propertyMappings.isEmpty())
                {
                    RelationalGrammarComposerContext indentedContext = RelationalGrammarComposerContext.Builder.newInstance(ctx).withIndentation(6).build();
                    associationMappingBuilder.append(LazyIterate.collect(relationalAssociationMapping.propertyMappings, propertyMapping -> HelperRelationalGrammarComposer.renderAbstractRelationalPropertyMapping(propertyMapping, indentedContext, true)).makeString(",\n"));
                    associationMappingBuilder.append("\n");
                }
                associationMappingBuilder.append(PureGrammarComposerUtility.getTabString(2)).append(")\n").append(getTabString()).append("}");
                return associationMappingBuilder.toString();
            }
            return null;
        });
    }

    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof RelationalDatabaseConnection)
            {
                RelationalGrammarComposerContext ctx = RelationalGrammarComposerContext.Builder.newInstance(context).build();
                RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connectionValue;
                int baseIndentation = 0;

                List<IRelationalGrammarComposerExtension> extensions = IRelationalGrammarComposerExtension.getExtensions(context);

                String authenticationStrategy = IRelationalGrammarComposerExtension.process(relationalDatabaseConnection.authenticationStrategy,
                        ListIterate.flatCollect(extensions, IRelationalGrammarComposerExtension::getExtraAuthenticationStrategyComposers),
                        context);

                String specification = IRelationalGrammarComposerExtension.process(relationalDatabaseConnection.datasourceSpecification,
                        ListIterate.flatCollect(extensions, IRelationalGrammarComposerExtension::getExtraDataSourceSpecificationComposers),
                        context);

                List<String> postProcessorStrings = FastList.newList();
                if (relationalDatabaseConnection.postProcessors != null && !relationalDatabaseConnection.postProcessors.isEmpty())
                {
                    postProcessorStrings.addAll(ListIterate.collect(relationalDatabaseConnection.postProcessors, postProcessor -> IRelationalGrammarComposerExtension.process(
                            postProcessor,
                            ListIterate.flatCollect(extensions, IRelationalGrammarComposerExtension::getExtraPostProcessorComposers),
                            context)));
                }

                String postProcessors = !postProcessorStrings.isEmpty()
                        ? "postProcessors:\n" + getTabString() + "[\n" + String.join(",\n", postProcessorStrings) + "\n" + getTabString() + "];\n"
                        : null;

                return Tuples.pair(RelationalGrammarParserExtension.RELATIONAL_DATABASE_CONNECTION_TYPE, context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        (relationalDatabaseConnection.element != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "store: " + relationalDatabaseConnection.element + ";\n") : "") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "type: " + relationalDatabaseConnection.type.name() + ";\n" +
                        (relationalDatabaseConnection.timeZone != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "timezone: " + relationalDatabaseConnection.timeZone + ";\n") : "") +
                        (relationalDatabaseConnection.quoteIdentifiers != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "quoteIdentifiers: " + relationalDatabaseConnection.quoteIdentifiers + ";\n") : "") +
                        // HACKY: this is a hack to make local mode works, we will need to rethink about this
                        (relationalDatabaseConnection.localMode != null && relationalDatabaseConnection.localMode ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "mode: local;\n") : "") +
                        (relationalDatabaseConnection.localMode != null && relationalDatabaseConnection.localMode ? "" : (
                                context.getIndentationString() + getTabString(baseIndentation + 1) + "specification: " + specification + ";\n" +
                                        context.getIndentationString() + getTabString(baseIndentation + 1) + "auth: " + authenticationStrategy + ";\n"
                        )) +
                        (relationalDatabaseConnection.queryTimeOutInSeconds != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "queryTimeOutInSeconds: " + String.valueOf(relationalDatabaseConnection.queryTimeOutInSeconds) + ";\n") : "") +
                        (postProcessors != null
                                ? context.getIndentationString() + getTabString(baseIndentation + 1) + postProcessors
                                : "") +
                        context.getIndentationString() + "}");
            }
            return null;
        });
    }

    private static String renderDatabase(Database database, PureGrammarComposerContext PUREcontext)
    {
        List<Schema> nonDefaultSchema = ListIterate.select(database.schemas, schema -> !"default".equals(schema.name));
        Schema defaultSchema = ListIterate.select(database.schemas, schema -> "default".equals(schema.name)).getFirst();
        RelationalGrammarComposerContext context = RelationalGrammarComposerContext.Builder.newInstance().withCurrentDatabase(PureGrammarComposerUtility.convertPath(database.getPath())).withNoDynaFunctionNames().withRenderStyle(PUREcontext.getRenderStyle()).build();
        StringBuilder builder = new StringBuilder();
        builder.append("Database ").append(renderAnnotations(database.stereotypes, database.taggedValues)).append(PureGrammarComposerUtility.convertPath(database.getPath())).append("\n(\n");
        boolean nonEmpty = false;
        if (!database.includedStores.isEmpty())
        {
            builder.append(LazyIterate.collect(database.includedStores, include -> getTabString(1) + "include " + PureGrammarComposerUtility.convertPath(include.path)).makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (!nonDefaultSchema.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(nonDefaultSchema, schema -> HelperRelationalGrammarComposer.renderDatabaseSchema(schema, context)).makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (defaultSchema != null && !defaultSchema.tables.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(defaultSchema.tables, table -> HelperRelationalGrammarComposer.renderDatabaseTable(table, 1, context)).makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (defaultSchema != null && !defaultSchema.tabularFunctions.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(defaultSchema.tabularFunctions, tabularFunction -> HelperRelationalGrammarComposer.renderDatabaseTabularFunction(tabularFunction, 1, context)).makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (defaultSchema != null && !defaultSchema.views.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(defaultSchema.views, view -> HelperRelationalGrammarComposer.renderDatabaseView(view, 1, context)).makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (!database.joins.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(database.joins, join -> getTabString(1) + "Join " + PureGrammarComposerUtility.convertIdentifier(join.name) + "(" + HelperRelationalGrammarComposer.renderRelationalOperationElement(join.operation, context) + ")").makeString("\n"));
            builder.append("\n");
            nonEmpty = true;
        }
        if (!database.filters.isEmpty())
        {
            builder.append(nonEmpty ? "\n" : "");
            builder.append(LazyIterate.collect(database.filters, filter -> getTabString(1) + (filter._type.equals("multigrain") ? "MultiGrainFilter " : "Filter ") + PureGrammarComposerUtility.convertIdentifier(filter.name) + "(" + HelperRelationalGrammarComposer.renderRelationalOperationElement(filter.operation, context) + ")").makeString("\n"));
            builder.append("\n");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public List<Function2<InputData, PureGrammarComposerContext, String>> getExtraMappingTestInputDataComposers()
    {
        return Lists.mutable.with((inputData, context) ->
        {
            if (inputData instanceof RelationalInputData)
            {
                RelationalInputData relationalInputData = (RelationalInputData) inputData;
                String data;
                if (relationalInputData.inputType == RelationalInputType.SQL)
                {
                    MutableList<String> lines = org.eclipse.collections.api.factory.Lists.mutable.of(relationalInputData.data.replace("\r", "").replace("\n", "").split("(?<!\\\\);"));
                    data = "\n" + lines.collect(l -> getTabString(5) + convertString(l + ";\n", true).replace("\\\\;", "\\;")).makeString("+\n");
                }
                else if (relationalInputData.inputType == RelationalInputType.CSV)
                {
                    MutableList<String> lines = org.eclipse.collections.api.factory.Lists.mutable.of(relationalInputData.data.split("\\n"));
                    lines.add("\n\n");
                    data = "\n" + lines.collect(l -> getTabString(5) + convertString(l + "\n", true)).makeString("+\n");
                }
                else
                {
                    data = relationalInputData.data;
                }
                return "<Relational, " + relationalInputData.inputType + ", " + relationalInputData.database + ", " + data + "\n" + getTabString(4) + ">";
            }
            return null;
        });
    }

    @Override
    public List<Function2<AuthenticationStrategy, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Lists.mutable.with((strategy, context) -> HelperRelationalGrammarComposer.visitRelationalDatabaseConnectionAuthenticationStrategy(strategy, RelationalGrammarComposerContext.Builder.newInstance(context).build()));
    }

    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((specification, context) -> HelperRelationalGrammarComposer.visitRelationalDatabaseConnectionDatasourceSpecification(specification, RelationalGrammarComposerContext.Builder.newInstance(context).build()));
    }

    @Override
    public List<Function2<PostProcessor, PureGrammarComposerContext, String>> getExtraPostProcessorComposers()
    {
        return org.eclipse.collections.api.factory.Lists.mutable.with((postProcessor, context) ->
        {
            if (postProcessor instanceof MapperPostProcessor)
            {
                return HelperRelationalGrammarComposer.visitMapperPostProcessor((MapperPostProcessor) postProcessor, context);
            }
            if (postProcessor instanceof RelationalMapperPostProcessor)
            {
                return visitRelationalMapperPostProcessor((RelationalMapperPostProcessor) postProcessor, context);
            }
            if (postProcessor instanceof ExtractSubQueriesAsCTEsPostProcessor)
            {
                int baseIndentation = 2;
                return getTabString(baseIndentation) + "ExtractSubQueriesAsCTEsPostProcessor\n" +
                        getTabString(baseIndentation) + "{\n" +
                        getTabString(baseIndentation) + "}";
            }
            else
            {
                return null;
            }
        });
    }

    public static String visitRelationalMapperPostProcessor(RelationalMapperPostProcessor relationalMapperPostProcessor, PureGrammarComposerContext context)
    {
        return writeRelationalMappersPostProcessor("relationalMapper", relationalMapperPostProcessor.relationalMappers, context);
    }

    public static String writeRelationalMappersPostProcessor(String mapperName, List<PackageableElementPointer> relationalMappers, PureGrammarComposerContext context)
    {
        List<String> relationalMapperStrings = ListIterate.collect(relationalMappers, relationalMapper -> relationalMapper.path);

        int baseIndent = 2;
        return getTabString(baseIndent) + mapperName + "\n" +
                getTabString(baseIndent) + "{\n" + getTabString(3) +
                String.join(", " + context.getIndentationString(), relationalMapperStrings) +
                "\n" + getTabString(baseIndent) + "}";
    }

    @Override
    public List<Function3<Milestoning, Integer, PureGrammarComposerContext, String>> getExtraMilestoningComposers()
    {
        return Lists.mutable.with((specification, offset, context) -> HelperRelationalGrammarComposer.visitMilestoning(specification, offset, RelationalGrammarComposerContext.Builder.newInstance(context).build()));
    }

    public static String renderRelationalOperationElement(RelationalOperationElement operationElement)
    {
        return HelperRelationalGrammarComposer.renderRelationalOperationElement(operationElement, RelationalGrammarComposerContext.Builder.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).build());
    }

    @Override
    public List<Function2<EmbeddedData, PureGrammarComposerContext, ContentWithType>> getExtraEmbeddedDataComposers()
    {
        return Collections.singletonList(RelationalEmbeddedDataComposer::composeRelationalDataEmbeddedData);
    }

    public static String renderRelationalMapper(RelationalMapper relationalMapper, PureGrammarComposerContext context)
    {
        return "RelationalMapper" + " " + PureGrammarComposerUtility.convertPath(relationalMapper.getPath()) + "\n" +
                "(\n" +
                (relationalMapper.databaseMappers.isEmpty() ? "" : renderDatabaseMapperSection(relationalMapper)) +
                (relationalMapper.schemaMappers.isEmpty() ? "" : renderSchemaMapperSection(relationalMapper)) +
                (relationalMapper.tableMappers.isEmpty() ? "" : renderTableMapperSection(relationalMapper)) +
                ")";
    }

    public static String renderDatabaseMapperSection(RelationalMapper relationalMapper)
    {
        return  "   DatabaseMappers:" + "\n" +
                "   [\n" +
                LazyIterate.collect(relationalMapper.databaseMappers, d -> renderDatabaseMapper(d)).makeString(",\n") + (relationalMapper.databaseMappers.isEmpty() ? "" : "\n") +
                "   ];\n";
    }

    public static String renderSchemaMapperSection(RelationalMapper relationalMapper)
    {
        return  "   SchemaMappers:" + "\n" +
                "   [\n" +
                LazyIterate.collect(relationalMapper.schemaMappers, s -> renderSchemaMapper(s)).makeString(",\n") + (relationalMapper.schemaMappers.isEmpty() ? "" : "\n") +
                "   ];\n";
    }

    public static String renderTableMapperSection(RelationalMapper relationalMapper)
    {
        return  "   TableMappers:" + "\n" +
                "   [\n" +
                LazyIterate.collect(relationalMapper.tableMappers, t -> renderTableMapper(t)).makeString(",\n") + (relationalMapper.tableMappers.isEmpty() ? "" : "\n") +
                "   ];\n";
    }

    public static String renderDatabaseMapper(DatabaseMapper databaseMapper)
    {
        return getTabString(3) +
                "[" +
                String.join(", ", LazyIterate.collect(databaseMapper.schemas, s -> renderSchemaRef(s))) +
                "]" + " -> " + "'" + databaseMapper.databaseName + "'";
    }

    public static String renderSchemaMapper(SchemaMapper schemaMapper)
    {
        return getTabString(3) + renderSchemaRef(schemaMapper.from) + " -> "  + "'" + schemaMapper.to + "'";
    }

    public static String renderSchemaRef(SchemaPtr schemaRef)
    {
        return schemaRef.database + "." + schemaRef.schema;
    }

    public static String renderTableMapper(TableMapper tableMapper)
    {
        return getTabString(3) + tableMapper.from.database + "." + tableMapper.from.schema + "." + tableMapper.from.table + " -> "  + "'" + tableMapper.to + "'";
    }

}
