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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.DatabaseInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.executionContext.RelationalExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.*;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Relation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.PostProcessorWithParameter;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class RelationalCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(Processor.newProcessor(
                Database.class,
                (Database srcDatabase, CompileContext context) ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = new Root_meta_relational_metamodel_Database_Impl(srcDatabase.name);

                    database._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")
                            ._rawType(context.pureModel.getType("meta::relational::metamodel::Database")));

                    context.pureModel.storesIndex.put(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), database);
                    return database;
                },
                (Database srcDatabase, CompileContext context) ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = HelperRelationalBuilder.getDatabase(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), srcDatabase.sourceInformation, context);
                    if (!srcDatabase.includedStores.isEmpty())
                    {
                        database._includes(ListIterate.collect(srcDatabase.includedStores, include -> HelperRelationalBuilder.resolveDatabase(context.pureModel.addPrefixToTypeReference(include), srcDatabase.sourceInformation, context)));
                    }
                    database._schemas(ListIterate.collect(srcDatabase.schemas, _schema -> HelperRelationalBuilder.processDatabaseSchema(_schema, context, database)));
                },
                (Database srcDatabase, CompileContext context) ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = HelperRelationalBuilder.getDatabase(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), srcDatabase.sourceInformation, context);
                    ListIterate.forEach(srcDatabase.schemas, _schema -> HelperRelationalBuilder.processDatabaseSchemaViewsFirstPass(_schema, context, database));
                },
                (Database srcDatabase, CompileContext context) ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = HelperRelationalBuilder.getDatabase(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), srcDatabase.sourceInformation, context);
                    // TODO checkForDuplicatesByName for filters/joins
                    database._joins(srcDatabase.joins == null ? Lists.fixedSize.empty() : ListIterate.collect(srcDatabase.joins, join -> HelperRelationalBuilder.processDatabaseJoin(join, context, database)))
                            ._filters(srcDatabase.filters == null ? Lists.fixedSize.empty() : ListIterate.collect(srcDatabase.filters, filter -> HelperRelationalBuilder.processDatabaseFilter(filter, context, database)));
                },
                (Database srcDatabase, CompileContext context) ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = HelperRelationalBuilder.getDatabase(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), srcDatabase.sourceInformation, context);
                    ListIterate.forEach(srcDatabase.schemas, _schema -> HelperRelationalBuilder.processDatabaseSchemaViewsSecondPass(_schema, context, database));
                }
        ));
    }

    @Override
    public List<Function3<ClassMapping, Mapping, CompileContext, Pair<SetImplementation, RichIterable<EmbeddedSetImplementation>>>> getExtraClassMappingFirstPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    if (cm instanceof RootRelationalClassMapping)
                    {
                        RootRelationalClassMapping classMapping = (RootRelationalClassMapping) cm;
                        String id = classMapping.id != null ? classMapping.id : getElementFullPath(context.resolveClass(classMapping._class, classMapping.classSourceInformation), context.pureModel.getExecutionSupport()).replaceAll("::", "_");
                        final RootRelationalInstanceSetImplementation res = new Root_meta_relational_mapping_RootRelationalInstanceSetImplementation_Impl(id)._id(id);
                        MutableList<RelationalOperationElement> groupByColumns = ListIterate.collect(classMapping.groupBy, relationalOperationElement -> HelperRelationalBuilder.processRelationalOperationElement(relationalOperationElement, context, Maps.mutable.empty(), Lists.mutable.empty()));
                        org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias mainTableAlias = null;
                        // user has defined main table
                        if (classMapping.mainTable != null)
                        {
                            Relation pureTable = HelperRelationalBuilder.getRelation(classMapping.mainTable, context);
                            mainTableAlias = new Root_meta_relational_metamodel_TableAlias_Impl("")
                                    ._relationalElement(pureTable)
                                    ._database(HelperRelationalBuilder.resolveDatabase(classMapping.mainTable.getDb(), classMapping.mainTable.sourceInformation, context));
                        }
                        res._distinct(classMapping.distinct)
                                ._superSetImplementationId(classMapping.extendsClassMappingId)
                                ._root(classMapping.root)
                                ._mainTableAlias(mainTableAlias)
                                ._parent(parentMapping);
                        if (classMapping.mappingClass != null)
                        {
                            res._mappingClass(HelperMappingBuilder.processMappingClass(classMapping.mappingClass, context, parentMapping));
                        }
                        if (!classMapping.groupBy.isEmpty())
                        {
                            res._groupBy(new Root_meta_relational_mapping_GroupByMapping_Impl("")._columns(groupByColumns));
                        }
                        MutableList<org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings = Lists.mutable.empty();
                        MutableMap<String, TableAlias> tableAliasesMap = Maps.mutable.empty();
                        HelperRelationalBuilder.processRelationalClassMapping(classMapping, context, res, res, parentMapping, embeddedRelationalPropertyMappings, HelperMappingBuilder.getAllEnumerationMappings(parentMapping), tableAliasesMap);
                        // user has not defined mainTable and the processing of mainTableAlias is complete (done with processing of class mapping)
                        if (res._mainTableAlias() == null)
                        {
                            MutableSet<TableAlias> tableAliases = tableAliasesMap.valuesView().toSet();
                            MutableSet<RelationalOperationElement> tables = tableAliases.collect(AliasAccessor::_relationalElement);
                            MutableSet<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database> databases = tableAliases.collect(TableAliasAccessor::_database);

                            // if classMapping is extending another class the main table can be resolved in ExtraClassMappingSecondPassProcessors
                            if ((tables.size() == 0 && classMapping.extendsClassMappingId == null) || tables.size() > 1)
                            {
                                throw new EngineException("Can't find the main table for class '" + classMapping.id + "'. Please specify a main table using the ~mainTable directive.");
                            }
                            if ((databases.size() == 0 && classMapping.extendsClassMappingId == null) || databases.size() > 1)
                            {
                                throw new EngineException("Can't find the main table for class '" + classMapping.id + "'. Inconsistent database definitions for the mapping");
                            }

                            if (tables.size() == 1 && databases.size() == 1)
                            {
                                mainTableAlias = new Root_meta_relational_metamodel_TableAlias_Impl("");
                                mainTableAlias._relationalElement(tables.toList().getFirst());
                                mainTableAlias._database(databases.toList().getFirst());
                                res._mainTableAlias(mainTableAlias);
                                HelperRelationalBuilder.enhanceEmbeddedMappingsWithRelationalOperationElement(embeddedRelationalPropertyMappings, res);
                            }
                        }

                        parentMapping._classMappingsAddAll(embeddedRelationalPropertyMappings);
                        embeddedRelationalPropertyMappings.addAll(HelperRelationalBuilder.generateMilestoningRangeEmbeddedPropertyMapping(res, res, context));

                        return Tuples.pair(res, Lists.immutable.empty());
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Procedure3<ClassMapping, Mapping, CompileContext>> getExtraClassMappingSecondPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    if (cm instanceof RootRelationalClassMapping)
                    {
                        RootRelationalClassMapping classMapping = (RootRelationalClassMapping) cm;
                        RootRelationalInstanceSetImplementation rsi = (RootRelationalInstanceSetImplementation) parentMapping._classMappings().detect(c -> HelperRelationalBuilder.getClassMappingId(c).equals(HelperMappingBuilder.getClassMappingId(classMapping, context)));

                        HelperRelationalBuilder.processRootRelationalClassMapping(rsi, classMapping, context);
                    }
                }
        );
    }

    @Override
    public List<Procedure3<AggregationAwareClassMapping, Mapping, CompileContext>> getExtraAggregationAwareClassMappingFirstPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    if (cm.mainSetImplementation instanceof RootRelationalClassMapping)
                    {
                        RootRelationalClassMapping relationalClassMapping = (RootRelationalClassMapping) cm.mainSetImplementation;
                        cm.propertyMappings = ListIterate.collect(relationalClassMapping.propertyMappings, x -> HelperRelationalBuilder.visitAggregationAwarePropertyMapping(x, relationalClassMapping.id));
                    }
                    for (AggregateSetImplementationContainer agg : cm.aggregateSetImplementations)
                    {
                        if (agg.setImplementation instanceof RootRelationalClassMapping)
                        {
                            RootRelationalClassMapping relationalClassMapping = (RootRelationalClassMapping) agg.setImplementation;
                            relationalClassMapping.propertyMappings.forEach(prop -> prop.source = relationalClassMapping.id);
                        }
                    }
                }
        );
    }

    @Override
    public List<Procedure3<AggregationAwareClassMapping, Mapping, CompileContext>> getExtraAggregationAwareClassMappingSecondPassProcessors()
    {
        return Collections.singletonList(
                (cm, parentMapping, context) ->
                {
                    AggregationAwareSetImplementation asi = (AggregationAwareSetImplementation) parentMapping._classMappings().detect(c -> HelperRelationalBuilder.getClassMappingId(c).equals(HelperMappingBuilder.getClassMappingId(cm, context)));
                    if (cm.mainSetImplementation instanceof RootRelationalClassMapping)
                    {
                        RootRelationalClassMapping classMapping = (RootRelationalClassMapping) cm.mainSetImplementation;
                        HelperRelationalBuilder.processRootRelationalClassMapping((RootRelationalInstanceSetImplementation) asi._mainSetImplementation(), classMapping, context);
                    }
                    for (AggregateSetImplementationContainer agg : cm.aggregateSetImplementations)
                    {
                        if (agg.setImplementation instanceof RootRelationalClassMapping)
                        {
                            RootRelationalClassMapping classMapping = (RootRelationalClassMapping) agg.setImplementation;
                            asi._aggregateSetImplementations().forEach(c -> {
                                if (HelperRelationalBuilder.getClassMappingId(c._setImplementation()).equals(HelperMappingBuilder.getClassMappingId(classMapping, context)))
                                {
                                    HelperRelationalBuilder.processRootRelationalClassMapping((RootRelationalInstanceSetImplementation) c._setImplementation(), classMapping, context);
                                }
                            });
                        }
                    }
                }
        );
    }

    @Override
    public List<Function3<AssociationMapping, Mapping, CompileContext, AssociationImplementation>> getExtraAssociationMappingProcessors()
    {
        return Collections.singletonList(
                (associationMapping, parentMapping, context) ->
                {
                    if (associationMapping instanceof RelationalAssociationMapping)
                    {
                        RelationalAssociationMapping relationalAssociationImplementation = (RelationalAssociationMapping) associationMapping;
                        RelationalAssociationImplementation base = new Root_meta_relational_mapping_RelationalAssociationImplementation_Impl("");
                        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association pureAssociation = context.resolveAssociation(relationalAssociationImplementation.association, associationMapping.sourceInformation);
                        MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings = Lists.mutable.empty();
                        MutableList<Store> stores = ListIterate.collect(relationalAssociationImplementation.stores, context::resolveStore);
                        // NOTE: we set the association before processing the property mappings, so we can resolve the correct property in the association
                        // in the case where the user does not provide the class name of where the association property comes from
                        base._association(pureAssociation);
                        RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> rpm = ListIterate.collect(relationalAssociationImplementation.propertyMappings, propertyMapping -> HelperRelationalBuilder.processAbstractRelationalPropertyMapping(propertyMapping, context, base, null, embeddedRelationalPropertyMappings, HelperMappingBuilder.getAllEnumerationMappings(parentMapping), Maps.mutable.empty()));
                        base._stores(stores)._propertyMappings(rpm)._parent(parentMapping);
                        parentMapping._classMappingsAddAll(embeddedRelationalPropertyMappings);
                        return base;
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Function2<Connection, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.mutable.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof RelationalDatabaseConnection)
                    {
                        RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connectionValue;

                        Root_meta_pure_alloy_connections_RelationalDatabaseConnection relational = new Root_meta_pure_alloy_connections_RelationalDatabaseConnection_Impl("");
                        HelperRelationalDatabaseConnectionBuilder.addDatabaseConnectionProperties(relational, relationalDatabaseConnection.element, relationalDatabaseConnection.elementSourceInformation, relationalDatabaseConnection.type.name(), relationalDatabaseConnection.timeZone, relationalDatabaseConnection.quoteIdentifiers, context);

                        List<IRelationalCompilerExtension> extensions = IRelationalCompilerExtension.getExtensions(context);

                        Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy authenticationStrategy = IRelationalCompilerExtension.process(
                                relationalDatabaseConnection.authenticationStrategy,
                                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraAuthenticationStrategyProcessors),
                                context);

                        Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification datasource = IRelationalCompilerExtension.process(
                                relationalDatabaseConnection.datasourceSpecification,
                                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraDataSourceSpecificationProcessors),
                                context);

                        List<PostProcessor> postProcessors = relationalDatabaseConnection.postProcessors == null ? FastList.newList() : relationalDatabaseConnection.postProcessors;

                        MutableList<Pair<Root_meta_pure_alloy_connections_PostProcessor, PostProcessorWithParameter>> pp = ListIterate.collect(postProcessors, p -> IRelationalCompilerExtension.process(
                                p,
                                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraConnectionPostProcessor),
                                context));

                        //we currently need to add both as __queryPostProcessorsWithParameter is used for plan generation
                        //and _postProcessors is used for serialization of plan to protocol
                        relational._datasourceSpecification(datasource);
                        relational._authenticationStrategy(authenticationStrategy);
                        List<PostProcessorWithParameter> postProcessorWithParameters = ListIterate.collect(relationalDatabaseConnection.postProcessorWithParameter, p -> IRelationalCompilerExtension.process(
                                p,
                                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraLegacyPostProcessors),
                                context));
                        List<PostProcessorWithParameter> translatedForPlanGeneration = ListIterate.collect(pp, Pair::getTwo);
                        relational._queryPostProcessorsWithParameter(Lists.mutable.withAll(postProcessorWithParameters).withAll(translatedForPlanGeneration));
                        relational._postProcessors(ListIterate.collect(pp, Pair::getOne));

                        return relational;
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Procedure2<PackageableElement, MutableMap<String, String>>> getExtraStoreStatBuilders()
    {
        return Collections.singletonList((element, stats) ->
        {
            if (element instanceof Database)
            {
                Database db = (Database) element;
                stats.put("type", "Database");
                stats.put("tables", "" + ListIterate.injectInto(0, db.schemas, (IntObjectToIntFunction<Schema>) (a, b) -> a + b.tables.size()));
                stats.put("views", "" + ListIterate.injectInto(0, db.schemas, (IntObjectToIntFunction<Schema>) (a, b) -> a + b.views.size()));
                stats.put("joins", "" + db.joins.size());
                stats.put("filters", "" + db.filters.size());
            }
        });
    }

    @Override
    public List<Function2<ExecutionContext, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext>> getExtraExecutionContextProcessors()
    {
        return Collections.singletonList((executionContext, context) ->
        {
            if (executionContext instanceof RelationalExecutionContext)
            {
                RelationalExecutionContext relationalContext = (RelationalExecutionContext) executionContext;
                MutableMap<Relation, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<Column>> fksByTable = Maps.mutable.empty();
                ListIterate.forEach(relationalContext.importDataFlowFkCols, fks ->
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema schema = ((org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) context.pureModel.getStore(fks.table.getDb()))
                            ._schemas().detect(s -> s._name().equals(fks.table.schema));
                    RichIterable<? extends NamedRelation> relations = fks.table._type.equals("Table")
                            ? schema._tables()
                            : fks.table._type.equals("View") ? schema._views() : null;
                    Relation table = relations != null ? relations.detect(a -> a._name().equals(fks.table.table)) : null;
                    RichIterable<Column> columns = ListIterate.collect(fks.columns, col -> table != null
                            ? table._columns().select(c -> c instanceof Column).collect(c -> (Column) c).detect(c -> c._name().equals(col))
                            : null
                    ).select(Objects::nonNull);
                    fksByTable.put(table, new Root_meta_pure_functions_collection_List_Impl("")._values(columns));
                });
                return new Root_meta_relational_runtime_RelationalExecutionContext_Impl("")
                        ._queryTimeOutInSeconds(relationalContext.queryTimeOutInSeconds)
                        ._enableConstraints(relationalContext.enableConstraints)
                        ._addDriverTablePkForProject(relationalContext.addDriverTablePkForProject)
                        ._insertDriverTablePkInTempTable(relationalContext.insertDriverTablePkInTempTable)
                        ._useTempTableAsDriver(relationalContext.useTempTableAsDriver)
                        ._preserveJoinOrder(relationalContext.preserveJoinOrder)
                        ._importDataFlow(relationalContext.importDataFlow)
                        ._importDataFlowAddFks(relationalContext.importDataFlowAddFks)
                        ._importDataFlowFksByTable(relationalContext.importDataFlowAddFks != null && relationalContext.importDataFlowAddFks ? new PureMap(fksByTable) : null)
                        ._importDataFlowImplementationCount(relationalContext.importDataFlowImplementationCount);
            }
            return null;
        });
    }

    @Override
    public List<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> getExtraFunctionHandlerDispatchBuilderInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionHandlerDispatchBuilderInfo("meta::relational::functions::database::tableReference_Database_1__String_1__String_1__Table_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && handlers.isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Database".equals(ps.get(0)._genericType()._rawType()._name())) && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && handlers.isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name()))),
                        new FunctionHandlerDispatchBuilderInfo("meta::relational::functions::database::viewReference_Database_1__String_1__String_1__View_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && handlers.isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "Database".equals(ps.get(0)._genericType()._rawType()._name())) && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && handlers.isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name()))),
                        new FunctionHandlerDispatchBuilderInfo("meta::relational::milestoning::unknownDefaultBusinessDate__Date_1_", (List<ValueSpecification> ps) -> ps.size() == 0)
                ));
    }

    @Override
    public List<Function<Handlers, List<FunctionHandlerRegistrationInfo>>> getExtraFunctionHandlerRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::relational::milestoning::unknownDefaultBusinessDate__Date_1_", false, ps -> handlers.res("Date", "one"))
                        ),
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::pure::tds::tableToTDS_Table_1__TableTDS_1_", false, ps -> handlers.res("meta::relational::mapping::TableTDS", "one"))
                        ),
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::pure::tds::viewToTDS_View_1__TableTDS_1_", false, ps -> handlers.res("meta::relational::mapping::TableTDS", "one"))
                        ),
                        new FunctionHandlerRegistrationInfo(Lists.mutable.with(2, 0),
                                // meta::pure::tds::project(tds:meta::relational::mapping::TableTDS[1], columnFunctions:ColumnSpecification<TDSRow>[*]):TabularDataSet[1]
                                handlers.h("meta::pure::tds::project_TableTDS_1__ColumnSpecification_MANY__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> handlers.typeOne(ps.get(0), "TableTDS"))
                        ),
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::pure::functions::asserts::assertJsonStringsEqual_String_1__String_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> true)
                        ),
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::relational::functions::database::tableReference_Database_1__String_1__String_1__Table_1_", false, ps -> handlers.res("meta::relational::metamodel::relation::Table", "one"))
                        ),
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::relational::functions::database::viewReference_Database_1__String_1__String_1__View_1_", false, ps -> handlers.res("meta::relational::metamodel::relation::View", "one"))
                        )
                ));
    }

    @Override
    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(
                                        handlers.grp(Handlers.JoinInference, handlers.h("meta::pure::tds::join_TabularDataSet_1__TabularDataSet_1__JoinType_1__Function_1__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4 && (ps.get(3)._genericType()._rawType()._name()).contains("Function"))),
                                        handlers.m(handlers.h("meta::pure::tds::join_TabularDataSet_1__TabularDataSet_1__JoinType_1__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4 && "String".equals(ps.get(3)._genericType()._rawType()._name()))),
                                        handlers.m(handlers.h("meta::pure::tds::join_TabularDataSet_1__TabularDataSet_1__JoinType_1__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> true))
                                )
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.m(handlers.h("meta::pure::tds::extensions::columnValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 5)),
                                        handlers.m(handlers.h("meta::pure::tds::extensions::columnValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> true)))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.m(handlers.h("meta::pure::tds::extensions::rowValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 5)),
                                        handlers.m(handlers.h("meta::pure::tds::extensions::rowValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4)))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(Lists.mutable.with(3),
                                handlers.m(handlers.h("meta::pure::functions::asserts::assertEq_Any_1__Any_1__Function_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 3 && !handlers.typeOne(ps.get(2), "String")))
                        )
                ));
    }

    @Override
    public List<Procedure<Procedure2<String, List<String>>>> getExtraElementForPathToElementRegisters()
    {
        return Collections.singletonList(registerElementForPathToElement ->
        {
            registerElementForPathToElement.value("meta::relational::mapping", Lists.mutable.with(
                    "supports_FunctionExpression_1__Boolean_1_",
                    "supportsStream_FunctionExpression_1__Boolean_1_",
                    "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionNode_1_"
            ));
            registerElementForPathToElement.value("meta::pure::mapping::aggregationAware", Lists.mutable.with(
                    "supports_FunctionExpression_1__Boolean_1_",
                    "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__ExecutionNode_1_",
                    "execution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_1__Runtime_1__ExecutionContext_1__RouterExtension_MANY__DebugContext_1__Result_1_"
            ));

            ImmutableList<String> versions = PureClientVersions.versionsSince("v1_20_0");
            versions.forEach(v -> registerElementForPathToElement.value("meta::protocols::pure::"+v+"::extension", Lists.mutable.with("getRelationalExtension_String_1__SerializerExtension_1_")));
         });
    }

    @Override
    public List<Function4<org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification, CompileContext, List<String>, ProcessingContext, ValueSpecification>> getExtraValueSpecificationProcessors()
    {
        return Collections.singletonList((valueSpecification, context, openVariables, processingContext) ->
        {
            if (valueSpecification instanceof DatabaseInstance)
            {
                DatabaseInstance databaseInstance = (DatabaseInstance) valueSpecification;
                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("")
                        ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(context.pureModel.getType("meta::relational::metamodel::Database")))
                        ._multiplicity(context.pureModel.getMultiplicity("one"))
                        ._values(Lists.mutable.with(context.resolveStore(databaseInstance.fullPath, databaseInstance.sourceInformation)));
            }
            return null;
        });
    }

    @Override
    public List<Function2<PostProcessor, CompileContext, Pair<Root_meta_pure_alloy_connections_PostProcessor, PostProcessorWithParameter>>> getExtraConnectionPostProcessor()
    {
        return Lists.mutable.with((processor, context) -> {
            if (processor instanceof MapperPostProcessor)
            {
                MapperPostProcessor mapper = (MapperPostProcessor) processor;

                Root_meta_pure_alloy_connections_MapperPostProcessor p = HelperRelationalDatabaseConnectionBuilder.createMapperPostProcessor(mapper);

                PostProcessorWithParameter f =
                        core_relational_relational_runtime_connection_postprocessor.Root_meta_pure_alloy_connections_tableMapperPostProcessor_MapperPostProcessor_1__PostProcessorWithParameter_1_(p, context.pureModel.getExecutionSupport());

                return Tuples.pair(p, f);
            }

            return null;
        });
    }

    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((spec, context) -> {
            DatasourceSpecificationBuilder datasourceSpecificationVisitor = new DatasourceSpecificationBuilder(context);
            return spec.accept(datasourceSpecificationVisitor);
        });
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((strategy, context) -> {
            AuthenticationStrategyBuilder authenticationStrategyBuilder = new AuthenticationStrategyBuilder();
            return strategy.accept(authenticationStrategyBuilder);
        });
    }

    @Override
    public List<Function3<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning, CompileContext, Multimap<String, Column>, org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning>> getExtraMilestoningProcessors()
    {
        return Lists.mutable.with((spec, context, columnMap) -> HelperRelationalBuilder.visitMilestoning(spec, context, columnMap));
    }

    @Override
    public List<Procedure2<InputData, CompileContext>> getExtraMappingTestInputDataProcessors()
    {
        return Collections.singletonList(((inputData, compileContext) -> {
            if (inputData instanceof RelationalInputData)
            {
                RelationalInputData relationalInputData = (RelationalInputData)inputData;
                compileContext.resolveStore(relationalInputData.database, relationalInputData.sourceInformation);
            }
        }));
    }
}
