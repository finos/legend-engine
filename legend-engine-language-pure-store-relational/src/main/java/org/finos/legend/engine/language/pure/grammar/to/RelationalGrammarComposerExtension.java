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
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;

import java.util.List;
import java.util.Set;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.appendTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class RelationalGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!RelationalGrammarParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof Database)
                {
                    return renderDatabase((Database) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<Set<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with((elements, context, composedSections) ->
        {
            List<Database> composableElements = ListIterate.selectInstancesOf(FastList.newList(elements), Database.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, RelationalGrammarComposerExtension::renderDatabase).makeString("###" + RelationalGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
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
                    builder.append(LazyIterate.collect(rootRelationalClassMapping.propertyMappings, propertyMapping -> HelperRelationalGrammarComposer.renderAbstractRelationalPropertyMapping(propertyMapping, indentedContext)).makeString(",\n"));
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
                associationMappingBuilder.append(relationalAssociationMapping.association).append(": ").append("Relational\n");
                associationMappingBuilder.append(PureGrammarComposerUtility.getTabString()).append("{\n");
                associationMappingBuilder.append(getTabString(2)).append("AssociationMapping").append("\n");
                associationMappingBuilder.append(PureGrammarComposerUtility.getTabString(2)).append("(").append("\n");
                if (!relationalAssociationMapping.propertyMappings.isEmpty())
                {
                    RelationalGrammarComposerContext indentedContext = RelationalGrammarComposerContext.Builder.newInstance(ctx).withIndentation(6).build();
                    associationMappingBuilder.append(LazyIterate.collect(relationalAssociationMapping.propertyMappings, propertyMapping -> HelperRelationalGrammarComposer.renderAbstractRelationalPropertyMapping(propertyMapping, indentedContext)).makeString(",\n"));
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
                if (relationalDatabaseConnection.postProcessors != null && !relationalDatabaseConnection.postProcessors.isEmpty()) {
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
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "specification: " + specification + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "auth: " + authenticationStrategy + ";\n" +
                        (postProcessors != null
                                ? context.getIndentationString() + getTabString(baseIndentation + 1) + postProcessors
                                : "") +
                        context.getIndentationString() + "}");
            }
            return null;
        });
    }

    private static String renderDatabase(Database database)
    {
        List<Schema> nonDefaultSchema = ListIterate.select(database.schemas, schema -> !"default".equals(schema.name));
        Schema defaultSchema = ListIterate.select(database.schemas, schema -> "default".equals(schema.name)).getFirst();
        RelationalGrammarComposerContext context = RelationalGrammarComposerContext.Builder.newInstance().withCurrentDatabase(PureGrammarComposerUtility.convertPath(database.getPath())).build();
        StringBuilder builder = new StringBuilder();
        builder.append("Database ").append(PureGrammarComposerUtility.convertPath(database.getPath())).append("\n(\n");
        boolean nonEmpty = false;
        if (!database.includedStores.isEmpty())
        {
            builder.append(LazyIterate.collect(database.includedStores, include -> getTabString(1) + "include " + PureGrammarComposerUtility.convertPath(include)).makeString("\n"));
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
            builder.append(LazyIterate.collect(database.filters, filter -> getTabString(1) + (filter._type.equals("multiGrainFilter") ? "MultiGrainFilter " : "Filter ") + PureGrammarComposerUtility.convertIdentifier(filter.name) + "(" + HelperRelationalGrammarComposer.renderRelationalOperationElement(filter.operation, context) + ")").makeString("\n"));
            builder.append("\n");
        }
        builder.append(")");
        return builder.toString();
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
        return org.eclipse.collections.api.factory.Lists.mutable.with((postProcessor, context) -> {
            if (postProcessor instanceof MapperPostProcessor)
            {
                return HelperRelationalGrammarComposer.visitMapperPostProcessor((MapperPostProcessor) postProcessor, context);
            }
            else
            {
                return null;
            }
        });
    }

    @Override
    public List<Function3<Milestoning, Integer, PureGrammarComposerContext, String>> getExtraMilestoningComposers()
    {
        return Lists.mutable.with((specification, offset, context) -> HelperRelationalGrammarComposer.visitMilestoning(specification, offset, RelationalGrammarComposerContext.Builder.newInstance(context).build()));
    }
}