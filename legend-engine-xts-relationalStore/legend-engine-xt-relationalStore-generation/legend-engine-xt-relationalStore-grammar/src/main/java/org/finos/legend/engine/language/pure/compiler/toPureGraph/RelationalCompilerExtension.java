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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validation.RelationalValidator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.validator.MappingValidatorContext;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.DatabaseMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.RelationalMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.SchemaMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapper.TableMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.MiddleTierUserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.ExtractSubQueriesAsCTEsPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.RelationalMapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.DatabaseInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.executionContext.RelationalExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.shared.core.function.Function4;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_MapperPostProcessor;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_PostProcessor;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_data_EmbeddedData;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext;
import org.finos.legend.pure.generated.Root_meta_pure_store_RelationStoreAccessor_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_GroupByMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_RelationalAssociationImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_RootRelationalInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_Database_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_TableAlias_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_runtime_PostProcessorWithParameter;
import org.finos.legend.pure.generated.Root_meta_relational_runtime_RelationalExecutionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_RelationalMapperPostProcessor;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_RelationalMapper;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_DatabaseMapper;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_SchemaMapper;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_TableMapper;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_RelationalMapper_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_DatabaseMapper_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_SchemaMapper_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_TableMapper_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_postProcessor_cteExtraction_ExtractSubQueriesAsCTEsPostProcessor;
import org.finos.legend.pure.generated.Root_meta_relational_postProcessor_cteExtraction_ExtractSubQueriesAsCTEsPostProcessor_Impl;
import org.finos.legend.pure.generated.core_relational_relational_runtime_connection_postprocessor;
import org.finos.legend.pure.generated.core_relational_relational_postprocessor_cteExtractionPostProcessor;
import org.finos.legend.pure.m2.dsl.store.M2StorePaths;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalAssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.AliasAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.*;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.Double;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.Float;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.Integer;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Relation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class RelationalCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "-Core");
    }

    @Override
    public CompilerExtension build()
    {
        return new RelationalCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(
                Processor.newProcessor(
                        Database.class,
                        (Database srcDatabase, CompileContext context) ->
                        {
                            org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = new Root_meta_relational_metamodel_Database_Impl(srcDatabase.name, SourceInformationHelper.toM3SourceInformation(srcDatabase.sourceInformation), null)._name(srcDatabase.name);

                            database._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                                    ._rawType(context.pureModel.getType("meta::relational::metamodel::Database")));
                            return database;
                        },
                        (Database srcDatabase, CompileContext context) ->
                        {
                            org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = HelperRelationalBuilder.getDatabase(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), srcDatabase.sourceInformation, context);
                            if (!srcDatabase.includedStores.isEmpty())
                            {
                                database._includes(ListIterate.collect(srcDatabase.includedStores, include -> HelperRelationalBuilder.resolveDatabase(context.pureModel.addPrefixToTypeReference(include.path), include.sourceInformation, context)));
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
                                    ._filters(srcDatabase.filters == null ? Lists.fixedSize.empty() : ListIterate.collect(srcDatabase.filters, filter -> HelperRelationalBuilder.processDatabaseFilter(filter, context, database)))
                                    ._stereotypes(srcDatabase.stereotypes == null ? Lists.fixedSize.empty() : ListIterate.collect(srcDatabase.stereotypes, stereotypePointer -> context.resolveStereotype(stereotypePointer.profile, stereotypePointer.value, stereotypePointer.profileSourceInformation, stereotypePointer.sourceInformation)));
                        },
                        (Database srcDatabase, CompileContext context) ->
                        {
                            org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = HelperRelationalBuilder.getDatabase(context.pureModel.buildPackageString(srcDatabase._package, srcDatabase.name), srcDatabase.sourceInformation, context);
                            ListIterate.forEach(srcDatabase.schemas, _schema -> HelperRelationalBuilder.processDatabaseSchemaViewsSecondPass(_schema, context, database));
                        }
                ),
                Processor.newProcessor(
                        RelationalMapper.class,
                        Lists.fixedSize.with(PackageableRuntime.class, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store.class),
                        (relationalMapper, context) ->
                        {
                            Root_meta_relational_metamodel_RelationalMapper metamodel = new Root_meta_relational_metamodel_RelationalMapper_Impl(relationalMapper.name, null, context.pureModel.getClass("meta::relational::metamodel::RelationalMapper"))._name(relationalMapper.name);
                            return metamodel;
                        },
                        (relationalMapper, context) ->
                        {
                            Root_meta_relational_metamodel_RelationalMapper metamodel = (Root_meta_relational_metamodel_RelationalMapper) context.pureModel.getPackageableElement(context.pureModel.buildPackageString(relationalMapper._package, relationalMapper.name));
                            metamodel._databaseMappers(ListIterate.collect(relationalMapper.databaseMappers, dbMap ->
                            {
                                return new Root_meta_relational_metamodel_DatabaseMapper_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::DatabaseMapper"))
                                        ._database(dbMap.databaseName);
                            }));
                            metamodel._schemaMappers(ListIterate.collect(relationalMapper.schemaMappers, schMap ->
                            {
                                return new Root_meta_relational_metamodel_SchemaMapper_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::SchemaMapper"))
                                        ._to(schMap.to);
                            }));
                            metamodel._tableMappers(ListIterate.collect(relationalMapper.tableMappers, tblMap ->
                            {
                                return new Root_meta_relational_metamodel_TableMapper_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableMapper"))
                                        ._to(tblMap.to);
                            }));
                        },
                        (relationalMapper, context) ->
                        {
                            Root_meta_relational_metamodel_RelationalMapper metamodel = (Root_meta_relational_metamodel_RelationalMapper) context.pureModel.getPackageableElement(context.pureModel.buildPackageString(relationalMapper._package, relationalMapper.name));
                            checkForDuplicates(relationalMapper.databaseMappers);
                            checkForDuplicates(relationalMapper.schemaMappers);
                            checkForDuplicates(relationalMapper.tableMappers);
                            metamodel._databaseMappers(ListIterate.collect(relationalMapper.databaseMappers, dbMap -> processDatabaseMapper(dbMap, context)));
                            metamodel._schemaMappers(ListIterate.collect(relationalMapper.schemaMappers, schMap -> processSchemaMapper(schMap, context)));
                            metamodel._tableMappers(ListIterate.collect(relationalMapper.tableMappers, tblMap -> processTableMapper(tblMap, context)));
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
                        final RootRelationalInstanceSetImplementation res = new Root_meta_relational_mapping_RootRelationalInstanceSetImplementation_Impl(id, SourceInformationHelper.toM3SourceInformation(cm.sourceInformation), context.pureModel.getClass("meta::relational::mapping::RootRelationalInstanceSetImplementation"))._id(id);
                        MutableList<RelationalOperationElement> groupByColumns = ListIterate.collect(classMapping.groupBy, relationalOperationElement -> HelperRelationalBuilder.processRelationalOperationElement(relationalOperationElement, context, Maps.mutable.empty(), Lists.mutable.empty()));
                        org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias mainTableAlias = null;
                        // user has defined main table
                        if (classMapping.mainTable != null)
                        {
                            Relation pureTable = HelperRelationalBuilder.getRelation(classMapping.mainTable, context);
                            mainTableAlias = new Root_meta_relational_metamodel_TableAlias_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAlias"))
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
                            res._groupBy(new Root_meta_relational_mapping_GroupByMapping_Impl("", null, context.pureModel.getClass("meta::relational::mapping::GroupByMapping"))._columns(groupByColumns));
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
                                mainTableAlias = new Root_meta_relational_metamodel_TableAlias_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAlias"));
                                mainTableAlias._relationalElement(tables.toList().getFirst());
                                mainTableAlias._database(databases.toList().getFirst());
                                res._mainTableAlias(mainTableAlias);
                                HelperRelationalBuilder.enhanceEmbeddedMappingsWithRelationalOperationElement(embeddedRelationalPropertyMappings, res, context);
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
                            asi._aggregateSetImplementations().forEach(c ->
                            {
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
                        RelationalAssociationImplementation base = new Root_meta_relational_mapping_RelationalAssociationImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(associationMapping.sourceInformation), context.pureModel.getClass("meta::relational::mapping::RelationalAssociationImplementation"));
                        final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association pureAssociation = context.resolveAssociation(relationalAssociationImplementation.association.path, relationalAssociationImplementation.association.sourceInformation);
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
    public List<Function2<Connection, CompileContext, Root_meta_core_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.mutable.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof RelationalDatabaseConnection)
                    {
                        RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connectionValue;

                        Root_meta_external_store_relational_runtime_RelationalDatabaseConnection relational = new Root_meta_external_store_relational_runtime_RelationalDatabaseConnection_Impl("", SourceInformationHelper.toM3SourceInformation(relationalDatabaseConnection.sourceInformation), context.pureModel.getClass("meta::external::store::relational::runtime::RelationalDatabaseConnection"));
                        HelperRelationalDatabaseConnectionBuilder.addDatabaseConnectionProperties(relational, relationalDatabaseConnection.type.name(), relationalDatabaseConnection.timeZone, relationalDatabaseConnection.quoteIdentifiers, context);

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

                        MutableList<Pair<Root_meta_pure_alloy_connections_PostProcessor, Root_meta_relational_runtime_PostProcessorWithParameter>> pp = ListIterate.collect(postProcessors, p -> IRelationalCompilerExtension.process(
                                relationalDatabaseConnection,
                                p,
                                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraConnectionPostProcessor),
                                context));

                        List<DatabaseAuthenticationFlowKey> flowKeys = context.getCompilerExtensions().getExtensions().stream().filter(ext -> ext instanceof IRelationalCompilerExtension).map(ext -> ((IRelationalCompilerExtension) ext).getFlowKeys()).flatMap(Collection::stream).collect(Collectors.toList());

                        if (relationalDatabaseConnection.databaseType == null)
                        {
                            relationalDatabaseConnection.databaseType = relationalDatabaseConnection.type;
                        }
                        if (!flowKeys.contains(DatabaseAuthenticationFlowKey.newKey(relationalDatabaseConnection.databaseType, relationalDatabaseConnection.datasourceSpecification.getClass(), relationalDatabaseConnection.authenticationStrategy.getClass())))
                        {
                            context.pureModel.addWarnings(Lists.mutable.with(new Warning(connectionValue.sourceInformation, "Unsupported Database Authentication Flow with Database Type: " + relationalDatabaseConnection.databaseType.name() + ", Datasource: " + relationalDatabaseConnection.datasourceSpecification.getClass().getSimpleName() + ", Authentication: " + relationalDatabaseConnection.authenticationStrategy.getClass().getSimpleName())));
                        }

                        //we currently need to add both as __queryPosNDattProcessorsWithParameter is used for plan generation
                        //and _postProcessors is used for serialization of plan to protocol
                        relational._datasourceSpecification(datasource);
                        relational._authenticationStrategy(authenticationStrategy);
                        List<Root_meta_relational_runtime_PostProcessorWithParameter> postProcessorWithParameters = ListIterate.collect(relationalDatabaseConnection.postProcessorWithParameter, p -> IRelationalCompilerExtension.process(
                                p,
                                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraLegacyPostProcessors),
                                context));
                        List<Root_meta_relational_runtime_PostProcessorWithParameter> translatedForPlanGeneration = ListIterate.collect(pp, Pair::getTwo);
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
                stats.put("tables", Long.toString(Iterate.sumOfInt(db.schemas, s -> s.tables.size())));
                stats.put("views", Long.toString(Iterate.sumOfInt(db.schemas, s -> s.views.size())));
                stats.put("joins", java.lang.Integer.toString(db.joins.size()));
                stats.put("filters", java.lang.Integer.toString(db.filters.size()));
            }
        });
    }

    @Override
    public List<Function2<ExecutionContext, CompileContext, Root_meta_pure_runtime_ExecutionContext>> getExtraExecutionContextProcessors()
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
                    fksByTable.put(table, new Root_meta_pure_functions_collection_List_Impl("", null, context.pureModel.getClass("meta::pure::functions::collection::List"))._values(columns));
                });
                return new Root_meta_relational_runtime_RelationalExecutionContext_Impl("", null, context.pureModel.getClass("meta::relational::runtime::RelationalExecutionContext"))
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
                                handlers.m(handlers.m(handlers.h("meta::pure::tds::extensions::extendWithDigestOnColumns_TabularDataSet_1__String_1__HashType_1__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4)),
                                        handlers.m(handlers.h("meta::pure::tds::extensions::extendWithDigestOnColumns_TabularDataSet_1__String_1__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 2)))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.m(handlers.h("meta::pure::tds::extensions::rowValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 5)),
                                        handlers.m(handlers.h("meta::pure::tds::extensions::rowValueDifference_TabularDataSet_1__TabularDataSet_1__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res("meta::pure::tds::TabularDataSet", "one"), ps -> ps.size() == 4)))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.h("meta::pure::tds::extensions::zScore_TabularDataSet_1__String_MANY__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res(ps.get(0)._genericType(), "one"), ps -> ps.size() == 4))
                        ),
                        new FunctionExpressionBuilderRegistrationInfo(null,
                                handlers.m(handlers.h("meta::pure::tds::extensions::iqrClassify_TabularDataSet_1__String_MANY__String_$1_MANY$__String_$1_MANY$__TabularDataSet_1_", false, ps -> handlers.res(ps.get(0)._genericType(), "one"), ps -> ps.size() == 4))
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
            registerElementForPathToElement.value("meta::relational::contract", Lists.mutable.with(
                    "supports_FunctionExpression_1__Boolean_1_",
                    "supportsStream_FunctionExpression_1__Boolean_1_",
                    "planExecution_StoreQuery_1__RoutedValueSpecification_$0_1$__Mapping_$0_1$__Runtime_$0_1$__ExecutionContext_1__Extension_MANY__DebugContext_1__ExecutionNode_1_"
            ));

            ImmutableList<String> versions = PureClientVersions.versionsSince("v1_20_0");
            versions.forEach(v -> registerElementForPathToElement.value("meta::protocols::pure::" + v + "::extension", Lists.mutable.with("getRelationalExtension_String_1__SerializerExtension_1_")));
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
                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                        ._genericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::relational::metamodel::Database")))
                        ._multiplicity(context.pureModel.getMultiplicity("one"))
                        ._values(Lists.mutable.with(context.resolveStore(databaseInstance.fullPath, databaseInstance.sourceInformation)));
            }
            return null;
        });
    }

    @Override
    public List<Function3<Connection, PostProcessor, CompileContext, Pair<Root_meta_pure_alloy_connections_PostProcessor, Root_meta_relational_runtime_PostProcessorWithParameter>>> getExtraConnectionPostProcessor()
    {
        return Lists.mutable.with((connection, processor, context) ->
        {
            if (processor instanceof MapperPostProcessor)
            {
                MapperPostProcessor mapper = (MapperPostProcessor) processor;

                Root_meta_pure_alloy_connections_MapperPostProcessor p = HelperRelationalDatabaseConnectionBuilder.createMapperPostProcessor(mapper, context);

                Root_meta_relational_runtime_PostProcessorWithParameter f =
                        core_relational_relational_runtime_connection_postprocessor.Root_meta_pure_alloy_connections_tableMapperPostProcessor_MapperPostProcessor_1__PostProcessorWithParameter_1_(p, context.pureModel.getExecutionSupport());

                return Tuples.pair(p, f);
            }
            else if (processor instanceof RelationalMapperPostProcessor)
            {
                RelationalMapperPostProcessor relationalMapper = (RelationalMapperPostProcessor) processor;
                List<String> duplicates = ((RelationalMapperPostProcessor) processor).relationalMappers.stream().map(rm -> rm.path).collect(Collectors.groupingBy(path -> path, Collectors.counting())).entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
                if (!duplicates.isEmpty())
                {
                    throw new EngineException("Found duplicated relational mapper(s) " + duplicates, SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION);
                }
                Root_meta_pure_alloy_connections_RelationalMapperPostProcessor p = HelperRelationalDatabaseConnectionBuilder.createRelationalMapperPostProcessor(relationalMapper, context);

                Root_meta_relational_runtime_PostProcessorWithParameter f =
                        core_relational_relational_runtime_connection_postprocessor.Root_meta_pure_alloy_connections_relationalMapperPostProcessor_RelationalMapperPostProcessor_1__PostProcessorWithParameter_1_(p, context.pureModel.getExecutionSupport());

                return Tuples.pair(p, f);
            }
            else if (processor instanceof ExtractSubQueriesAsCTEsPostProcessor)
            {
                Root_meta_relational_postProcessor_cteExtraction_ExtractSubQueriesAsCTEsPostProcessor p = new Root_meta_relational_postProcessor_cteExtraction_ExtractSubQueriesAsCTEsPostProcessor_Impl("", null, context.pureModel.getClass("meta::relational::postProcessor::cteExtraction::ExtractSubQueriesAsCTEsPostProcessor"));
                Root_meta_relational_runtime_PostProcessorWithParameter f =
                        core_relational_relational_postprocessor_cteExtractionPostProcessor.Root_meta_relational_postProcessor_cteExtraction_extractSubQueriesAsCTEsPostProcessor_ExtractSubQueriesAsCTEsPostProcessor_1__PostProcessorWithParameter_1_(p, context.pureModel.getExecutionSupport());
                return Tuples.pair(p, f);
            }
            return null;
        });
    }

    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((spec, context) ->
        {
            DatasourceSpecificationBuilder datasourceSpecificationVisitor = new DatasourceSpecificationBuilder(context);
            return spec.accept(datasourceSpecificationVisitor);
        });
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((strategy, context) ->
        {
            AuthenticationStrategyBuilder authenticationStrategyBuilder = new AuthenticationStrategyBuilder(context);
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
        return Collections.singletonList(((inputData, compileContext) ->
        {
            if (inputData instanceof RelationalInputData)
            {
                RelationalInputData relationalInputData = (RelationalInputData) inputData;
                compileContext.resolveStore(relationalInputData.database, relationalInputData.sourceInformation);
            }
        }));
    }

    @Override
    public List<Function3<EmbeddedData, CompileContext, ProcessingContext, Root_meta_pure_data_EmbeddedData>> getExtraEmbeddedDataProcessors()
    {
        return Collections.singletonList(RelationalEmbeddedDataCompiler::compileRelationalEmbeddedDataCompiler);
    }

    @Override
    public List<BiConsumer<PureModel, MappingValidatorContext>> getExtraMappingPostValidators()
    {
        return Collections.singletonList(RelationalValidator::validateRelationalMapping);
    }

    @Override
    public List<DatabaseAuthenticationFlowKey> getFlowKeys()
    {
        return Lists.mutable.of(DatabaseAuthenticationFlowKey.newKey(DatabaseType.H2, StaticDatasourceSpecification.class, TestDatabaseAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.H2, LocalH2DatasourceSpecification.class, DefaultH2AuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.H2, LocalH2DatasourceSpecification.class, TestDatabaseAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.SqlServer, StaticDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.Postgres, StaticDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.Postgres, StaticDatasourceSpecification.class, MiddleTierUserNamePasswordAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.MemSQL, StaticDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class)
        );
    }

    @Override
    public List<Function4<RelationStoreAccessor, Store, CompileContext, ProcessingContext, ValueSpecification>> getExtraRelationStoreAccessorProcessors()
    {
        return Lists.mutable.with((accessor, store, context, processingContext) ->
        {
            if (store instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database)
            {
                if (accessor.path.size() <= 1)
                {
                    throw new EngineException("Error in the accessor definition. Please provide a table.", accessor.sourceInformation, EngineErrorType.COMPILATION);
                }
                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database ds = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) store;

                String schemaName = (accessor.path.size() == 3) ? accessor.path.get(1) : null;
                String tableName = (accessor.path.size() == 3) ? accessor.path.get(2) : accessor.path.get(1);

                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema schema = schemaName == null ? ds._schemas().getFirst() : ds._schemas().select(c -> c.getName().equals(schemaName)).getFirst();
                if (schema == null)
                {
                    throw new EngineException(schemaName == null ? "The database " + store._name() + " has no schemas" : "The schema " + schemaName + " can't be found in the store " + store._name(), accessor.sourceInformation, EngineErrorType.COMPILATION);
                }
                Table table = schema._tables().select(c -> c.getName().equals(tableName)).getFirst();
                if (table == null)
                {
                    throw new EngineException("The table " + accessor.path.get(1) + " can't be found in the store " + store._name(), accessor.sourceInformation, EngineErrorType.COMPILATION);
                }

                ProcessorSupport processorSupport = context.pureModel.getExecutionSupport().getProcessorSupport();

                org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = null;

                RelationType<?> type = _RelationType.build(table._columns().collect(c ->
                {
                    Column col = (Column) c;
                    String name = col._name();
                    if (name.startsWith("\""))
                    {
                        name = name.substring(1, name.length() - 1);
                    }
                    return (CoreInstance) _Column.getColumnInstance(name, false, convertTypes(col._type(), processorSupport), (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(col._nullable() ? 0 : 1, 1, processorSupport), sourceInformation, processorSupport);
                }).toList(), sourceInformation, processorSupport);

                GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                        ._rawType(context.pureModel.getType(M2StorePaths.RelationStoreAccessor))
                        ._typeArguments(FastList.newListWith(
                                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(type)
                                )
                        );

                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                        ._genericType(genericType)
                        ._multiplicity(context.pureModel.getMultiplicity("one"))
                        ._values(
                                FastList.newListWith(
                                        new Root_meta_pure_store_RelationStoreAccessor_Impl<>("", new org.finos.legend.pure.m4.coreinstance.SourceInformation("X", 0, 0, 0, 0), null)
                                                ._store(ds)
                                                ._sourceElement(table)
                                                ._classifierGenericType(genericType)
                                )
                        );


            }
            return null;
        });
    }

    private GenericType convertTypes(DataType c, ProcessorSupport processorSupport)
    {
        String primitiveType;
        if (c instanceof Varchar)
        {
            primitiveType = "String";
        }
        else if (c instanceof Integer)
        {
            primitiveType = "Integer";
        }
        else if (c instanceof BigInt)
        {
            primitiveType = "Integer";
        }
        else if (c instanceof Bit)
        {
            primitiveType = "Boolean";
        }
        else if (c instanceof Double || c instanceof Float)
        {
            primitiveType = "Float";
        }
        else if (c instanceof Decimal)
        {
            primitiveType = "Decimal";
        }
        else if (c instanceof Date)
        {
            primitiveType = "Date";
        }
        else if (c instanceof Timestamp)
        {
            primitiveType = "DateTime";
        }
        else
        {
            throw new RuntimeException("Implement support for '" + c.getClass().getName() + "'");
        }
        return (GenericType) processorSupport.type_wrapGenericType(_Package.getByUserPath(primitiveType, processorSupport));
    }

    @Override
    public List<Procedure3<SetImplementation, Set<String>, CompileContext>> getExtraSetImplementationSourceScanners()
    {
        return Collections.singletonList((setImplementation, scannedSources, context) ->
        {
            if (setImplementation instanceof RootRelationalInstanceSetImplementation)
            {
                scannedSources.add(HelperModelBuilder.getElementFullPath(((RootRelationalInstanceSetImplementation) setImplementation)._mainTableAlias()._database(), context.pureModel.getExecutionSupport()));
            }
        });
    }

    private static <T> void checkForDuplicates(List<T> list)
    {
        List<String> duplicates = list.stream()
                .flatMap(item ->
                {
                    if (item instanceof DatabaseMapper)
                    {
                        DatabaseMapper mapper = (DatabaseMapper) item;
                        return mapper.schemas.stream().map(sp -> sp.database + "." + sp.schema);
                    }
                    else if (item instanceof SchemaMapper)
                    {
                        SchemaMapper mapper = (SchemaMapper) item;
                        return Stream.of(mapper.from.database + "." + mapper.from.schema);
                    }
                    else if (item instanceof TableMapper)
                    {
                        TableMapper mapper = (TableMapper) item;
                        return Stream.of(mapper.from.database + "." + mapper.from.schema + "." + mapper.from.table);
                    }
                    else
                    {
                        throw new EngineException("Unsupported type");
                    }
                })
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
        if (!duplicates.isEmpty())
        {
            throw new EngineException("Found duplicated mappers for " + duplicates, SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION);
        }
    }

    public static Root_meta_relational_metamodel_DatabaseMapper processDatabaseMapper(DatabaseMapper dbMap, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database database = dbMap.databaseName != null ? new Root_meta_relational_metamodel_Database_Impl("")._name(dbMap.databaseName) : null;
        Root_meta_relational_metamodel_DatabaseMapper dbMapper = new Root_meta_relational_metamodel_DatabaseMapper_Impl("")
                ._database(database._name())
                ._schemas(ListIterate.collect(dbMap.schemas, sch ->
                {
                    return HelperRelationalBuilder.getSchema(HelperRelationalBuilder.resolveDatabase(sch.database, sch.sourceInformation, context), sch.schema);
                }));
        return dbMapper;
    }

    public static Root_meta_relational_metamodel_SchemaMapper processSchemaMapper(SchemaMapper schMap, CompileContext context)
    {
        Root_meta_relational_metamodel_SchemaMapper schMapper = new Root_meta_relational_metamodel_SchemaMapper_Impl("")
                ._to(schMap.to)
                ._from(HelperRelationalBuilder.getSchema(HelperRelationalBuilder.resolveDatabase(schMap.from.database, schMap.from.sourceInformation, context), schMap.from.schema));
        return schMapper;
    }

    public static Root_meta_relational_metamodel_TableMapper processTableMapper(TableMapper tblMap, CompileContext context)
    {
        SetIterable<Table> tables = HelperRelationalBuilder.getAllTablesInSchema(HelperRelationalBuilder.resolveDatabase(tblMap.from.database, tblMap.from.sourceInformation, context), tblMap.from.schema, org.finos.legend.engine.protocol.pure.v1.model.SourceInformation.getUnknownSourceInformation());
        Table tbl = tables.toList().stream().filter(t -> t._name().equals(tblMap.from.table)).findFirst().orElseThrow(() -> new RuntimeException("Can't find " + tblMap.from.table + " table in " + tblMap.from.schema + " schema."));

        Root_meta_relational_metamodel_TableMapper tblMapper = new Root_meta_relational_metamodel_TableMapper_Impl("")
                ._to(tblMap.to)
                ._from(tbl);
        return tblMapper;
    }
}
