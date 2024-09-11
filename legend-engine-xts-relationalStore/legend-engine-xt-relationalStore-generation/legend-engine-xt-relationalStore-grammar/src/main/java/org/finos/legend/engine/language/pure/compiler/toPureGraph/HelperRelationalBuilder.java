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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwarePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.EmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.InlineEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.OtherwiseEmbeddedRelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.BigInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Binary;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Bit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Char;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.DataType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Date;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Decimal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Double;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Float;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Json;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Numeric;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Other;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Real;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SemiStructured;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.SmallInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Timestamp;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.TinyInt;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.VarChar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Varbinary;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.DynaFunc;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.ElementWithJoins;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.JoinPointer;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_binding_BindingTransformer_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_Pair_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_MappingClass_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_Property_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_relationship_Generalization_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_ColumnMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_EmbeddedRelationalInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_FilterMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_GroupByMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_InlineEmbeddedRelationalInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_OtherwiseEmbeddedRelationalInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_RelationalPropertyMapping_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_mapping_SemiStructuredEmbeddedRelationalInstanceSetImplementation_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_Column_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_DynaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_Filter_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_LiteralList_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_Literal_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_MultiGrainFilter_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_RelationalOperationElementWithJoin_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_SQLNull_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_Schema_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_TableAliasColumn_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_TableAlias_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Binary_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Char_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Decimal_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Json_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Numeric_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Other_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Real_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_SemiStructured_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_SmallInt_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Timestamp_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_TinyInt_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Varbinary_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Varchar_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_join_JoinTreeNode_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_join_Join_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_relation_BusinessMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_relation_BusinessSnapshotMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_relation_ProcessingMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_relation_Table_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_relation_View_Impl;
import org.finos.legend.pure.generated.core_pure_model_modelUnit;
import org.finos.legend.pure.m2.relational.M2RelationalPaths;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningStereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.ColumnMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.GroupByMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.InlineEmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.OtherwiseEmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.ColumnAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.DynaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Filter;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Literal;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElementWithJoin;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SQLNull;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.JoinTreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.operation.Operation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.BusinessMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.NamedRelation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.ProcessingMilestoning;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Relation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.View;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.finos.legend.pure.generated.platform_dsl_mapping_functions_PropertyMappingsImplementation.Root_meta_pure_mapping_superMapping_PropertyMappingsImplementation_1__PropertyMappingsImplementation_$0_1$_;
import static org.finos.legend.pure.generated.platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_;

public class HelperRelationalBuilder
{
    private static final String DEFAULT_SCHEMA_NAME = "default";
    private static final String SELF_JOIN_TABLE_NAME = "{target}";

    public static Database getDatabase(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        try
        {
            Store store = context.pureModel.getStore(fullPath, sourceInformation);
            if (store instanceof Database)
            {
                return (Database) store;
            }
            throw new RuntimeException("Store found but not a database");
        }
        catch (Exception e)
        {
            throw new EngineException("Can't find database '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION, e);
        }
    }

    public static Database resolveDatabase(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        return context.resolve(fullPath, sourceInformation, (String path) -> getDatabase(path, sourceInformation, context));
    }

    public static Column getColumn(Relation tb, final String _column, SourceInformation sourceInformation)
    {
        Column column = (Column) tb._columns().detect(col -> _column.equals(col.getName()));
        Assert.assertTrue(column != null, () -> "Can't find column '" + _column + "'", sourceInformation, EngineErrorType.COMPILATION);
        return column;
    }

    public static Join getJoin(JoinPointer joinPointer, CompileContext context)
    {
        String _joinName = joinPointer.name;
        Database db = resolveDatabase(joinPointer.db, joinPointer.sourceInformation, context);
        SourceInformation sourceInformation = joinPointer.sourceInformation;

        Join join;
        try
        {
            join = findJoin(db, _joinName);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error finding join '").append(_joinName).append("' in database '").append(db.getName()).append("'");
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw new EngineException(builder.toString(), sourceInformation, EngineErrorType.COMPILATION, e);
        }
        if (join == null)
        {
            throw new EngineException("Can't find join '" + _joinName + "' in database '" + db.getName() + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return join;
    }

    private static Join findJoin(Database db, String _joinName)
    {
        return findJoin(db, _joinName, Sets.mutable.empty());
    }

    private static Join findJoin(Database db, String _joinName, MutableSet<Database> visited)
    {
        if (visited.add(db))
        {
            // Check the DB itself
            Join join = db._joins().detect(j -> _joinName.equals(j._name()));
            if (join != null)
            {
                return join;
            }

            // If it's not there, check included joins
            for (Store included : db._includes())
            {
                join = findJoin((Database) included, _joinName, visited);
                if (join != null)
                {
                    return join;
                }
            }
        }
        return null;
    }

    public static Filter getFilter(Database db, String _filterName, SourceInformation sourceInformation)
    {
        Filter filter = db._filters().detect(join -> _filterName.equals(join._name()));
        Assert.assertTrue(filter != null, () -> "Can't find filter '" + _filterName + "' in database '" + db.getName() + "'", sourceInformation, EngineErrorType.COMPILATION);
        return filter;
    }

    public static Schema getSchema(Database db, final String _schema)
    {
        Schema s = db._schemas().detect(schema -> _schema.equals(schema.getName()));
        Assert.assertTrue(s != null, () -> "Can't find schema '" + _schema + "' in database '" + db.getName() + "'");
        return s;
    }

    public static Relation getRelation(Schema s, final String _table)
    {
        return getRelation(s, _table, SourceInformation.getUnknownSourceInformation());
    }

    public static Relation getRelation(Schema s, final String _table, SourceInformation sourceInformation)
    {
        Relation res = s._tables().detect(table -> _table.equals(table.getName()));
        if (res == null)
        {
            res = s._views().detect(view -> _table.equals(view.getName()));
            Assert.assertTrue(res != null, () -> "Can't find table '" + _table + "' in schema '" + s._name() + "' and database '" + s._database()._name() + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return res;
    }

    public static Relation getRelation(Database db, final String _schema, final String _table)
    {
        return getRelation(db, _schema, _table, SourceInformation.getUnknownSourceInformation());
    }

    public static Relation getRelation(Database db, final String _schema, final String _table, SourceInformation sourceInformation)
    {
        validateSchemaExists(db, _schema, sourceInformation);
        Relation table = findRelation(db, _schema, _table, sourceInformation);
        if (table == null)
        {
            throw new EngineException("Can't find table '" + _table + "' in schema '" + _schema + "' and database '" + db.getName() + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
        return table;
    }

    private static Relation findRelation(Database database, final String schemaName, final String tableName, SourceInformation sourceInformation)
    {
        MutableList<Relation> tables = Lists.mutable.empty();
        for (Database db : getAllIncludedDBs(database))
        {
            Schema schema = db._schemas().detect(s -> schemaName.equals(s._name()));
            if (schema != null)
            {
                Relation table = schema._tables().detect(t -> tableName.equals(t._name()));
                if (table == null)
                {
                    table = schema._views().detect(v -> tableName.equals(v._name()));
                }
                if (table != null)
                {
                    tables.add(table);
                }
            }
        }
        switch (tables.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return tables.get(0);
            }
            default:
            {
                StringBuilder message = new StringBuilder("The relation '").append(tableName).append("' has been found ")
                        .append(tables.size()).append(" times in the schema '").append(schemaName).append("' of the database '");
                PackageableElement.writeUserPathForPackageableElement(message, database).append('\'');
                throw new EngineException(message.toString(), sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    public static SetIterable<Table> getAllTables(Database database, SourceInformation sourceInformation)
    {
        return getAllTables(database, x -> true);
    }

    public static SetIterable<Table> getAllTablesInSchema(Database db, String _schema, SourceInformation sourceInformation)
    {
        validateSchemaExists(db, _schema, sourceInformation);
        return getAllTables(db, s -> _schema.equals(s._name()));
    }

    private static void validateSchemaExists(Database db, String _schema, SourceInformation sourceInformation)
    {
        if (!schemaExists(db, _schema))
        {
            throw new EngineException("Can't find schema '" + _schema + "' in database '" + db + "'", sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static SetIterable<Table> getAllTables(Database db, Predicate<Schema> schemaPredicate)
    {
        MutableSet<Table> tables = Sets.mutable.empty();
        getAllIncludedDBs(db).forEach(_db -> _db._schemas().forEach(schema ->
        {
            if (schemaPredicate.test(schema))
            {
                tables.addAllIterable(schema._tables());
            }
        }));
        return tables;
    }

    private static boolean schemaExists(Database database, String schemaName)
    {
        return DEFAULT_SCHEMA_NAME.equals(schemaName) || schemaExists(Sets.mutable.empty(), database, schemaName);
    }

    private static boolean schemaExists(MutableSet<CoreInstance> visited, Database database, String schemaName)
    {
        return visited.add(database) &&
                (database._schemas().anySatisfy(s -> schemaName.equals(s._name())) ||
                        database._includes().anySatisfy(incl -> schemaExists(visited, (Database) incl, schemaName)));
    }

    private static SetIterable<Database> getAllIncludedDBs(Database database)
    {
        return collectIncludedDBs(database, Sets.mutable.empty());
    }

    private static MutableSet<Database> collectIncludedDBs(Store store, MutableSet<Database> results)
    {
        if (results.add((Database) store))
        {
            store._includes().forEach(incl -> collectIncludedDBs(incl, results));
        }
        return results;
    }

    public static Schema processDatabaseSchema(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema srcSchema, CompileContext context, Database database)
    {
        Schema schema = new Root_meta_relational_metamodel_Schema_Impl(srcSchema.name, SourceInformationHelper.toM3SourceInformation(srcSchema.sourceInformation), context.pureModel.getClass("meta::relational::metamodel::Schema"))._name(srcSchema.name);
        RichIterable<Table> tables = ListIterate.collect(srcSchema.tables, _table -> processDatabaseTable(_table, context, schema));
        return schema._tables(tables)._database(database);
    }

    public static Schema processDatabaseSchemaViewsFirstPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema srcSchema, CompileContext context, Database database)
    {
        Schema schema = getSchema(database, srcSchema.name);
        RichIterable<View> views = ListIterate.collect(srcSchema.views, _view -> processDatabaseViewFirstPass(_view, context, schema));
        return schema._views(views);
    }

    public static Schema processDatabaseSchemaViewsSecondPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema srcSchema, CompileContext context, Database database)
    {
        Schema schema = getSchema(database, srcSchema.name);
        RichIterable<View> views = ListIterate.collect(srcSchema.views, _view -> processDatabaseViewSecondPass(_view, context, schema));
        return schema._views(views);
    }

    public static Table processDatabaseTable(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table databaseTable, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema schema)
    {
        Table table = new Root_meta_relational_metamodel_relation_Table_Impl(databaseTable.name, SourceInformationHelper.toM3SourceInformation(databaseTable.sourceInformation), context.pureModel.getClass("meta::relational::metamodel::relation::Table"))._name(databaseTable.name);
        MutableList<Column> columns = Lists.mutable.empty();
        MutableSet<String> validColumnNames = Sets.mutable.empty();
        MutableSet<String> duplicateColumns = Sets.mutable.empty();
        for (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Column column : databaseTable.columns)
        {
            if (validColumnNames.contains(column.name))
            {
                duplicateColumns.add(column.name);
            }
            else
            {
                validColumnNames.add(column.name);
                columns.add(new Root_meta_relational_metamodel_Column_Impl(column.name, SourceInformationHelper.toM3SourceInformation(column.sourceInformation), context.pureModel.getClass("meta::relational::metamodel::Column"))._name(column.name)._name(column.name)._nullable(column.nullable)._type(transformDatabaseDataType(column.type, context))._owner(table));
            }
        }

        if (!duplicateColumns.isEmpty())
        {
            context.pureModel.addWarnings(org.eclipse.collections.impl.factory.Lists.mutable.with(new Warning(databaseTable.sourceInformation, "Duplicate column definitions " + duplicateColumns + " in table: " + table._name())));
        }
        RichIterable<Column> pk = ListIterate.collect(databaseTable.primaryKey, s -> columns.select(column -> s.equals(column._name())).getFirst());
        RichIterable<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning> milestoning = ListIterate.collect(databaseTable.milestoning, m -> processMilestoning(m, context, columns.groupBy(ColumnAccessor::_name)));
        return table._columns(columns)._primaryKey(pk)._schema(schema)._milestoning(milestoning);
    }

    public static View processDatabaseViewFirstPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.View srcView, CompileContext context, Schema schema)
    {
        View view = new Root_meta_relational_metamodel_relation_View_Impl(srcView.name, SourceInformationHelper.toM3SourceInformation(srcView.sourceInformation), null)._name(srcView.name);
        MutableList<Column> columns = ListIterate.collect(srcView.columnMappings, columnMapping -> new Root_meta_relational_metamodel_Column_Impl(columnMapping.name, SourceInformationHelper.toM3SourceInformation(columnMapping.sourceInformation), context.pureModel.getClass("meta::relational::metamodel::Column"))._name(columnMapping.name)._type(new Root_meta_relational_metamodel_datatype_Varchar_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Varchar")))._owner(view));
        RichIterable<Column> pk = ListIterate.collect(srcView.primaryKey, s -> columns.select(column -> s.equals(column._name())).getFirst());
        return view._columns(columns)._primaryKey(pk)._schema(schema);
    }

    public static View processDatabaseViewSecondPass(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.View srcView, CompileContext context, Schema schema)
    {
        View view = (View) getRelation(schema, srcView.name);
        MutableList<ColumnMapping> columnMappings = ListIterate.collect(srcView.columnMappings, columnMapping ->
        {
            ColumnMapping result = new Root_meta_relational_mapping_ColumnMapping_Impl("", null, context.pureModel.getClass("meta::relational::mapping::ColumnMapping"));
            return result._columnName(columnMapping.name)._relationalOperationElement(processRelationalOperationElement(columnMapping.operation, context, Maps.mutable.empty(), Lists.mutable.empty()));
        });
        RelationalOperationElement mainTable = resolveMainTable(srcView, view, columnMappings, context);
        MutableList<RelationalOperationElement> groupByColumns = ListIterate.collect(srcView.groupBy, relationalOperationElement -> processRelationalOperationElement(relationalOperationElement, context, Maps.mutable.empty(), Lists.mutable.empty()));
        org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping filterMapping = null;
        if (srcView.filter != null)
        {
            Database database = srcView.filter.filter.db != null ? resolveDatabase(srcView.filter.filter.db, srcView.filter.sourceInformation, context) : view._schema()._database();
            filterMapping = processFilterMapping(srcView.filter, database, context);
        }
        GroupByMapping groupByMapping = groupByColumns.isEmpty() ? null : new Root_meta_relational_mapping_GroupByMapping_Impl("", null, context.pureModel.getClass("meta::relational::mapping::GroupByMapping"))._columns(groupByColumns);
        return view._mainTableAlias(new Root_meta_relational_metamodel_TableAlias_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAlias"))._relationalElement(mainTable))._distinct(srcView.distinct)._filter(filterMapping)._groupBy(groupByMapping)._columnMappings(columnMappings);
    }

    private static RelationalOperationElement resolveMainTable(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.View srcView, View view, MutableList<ColumnMapping> columnMappings, CompileContext context)
    {
        if (srcView.mainTable != null)
        {
            return getRelation(srcView.mainTable, context);
        }
        else
        {
            return findMainTable(srcView, view, columnMappings);
        }
    }

    private static RelationalOperationElement findMainTable(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.View srcView, View view, MutableList<ColumnMapping> columnMappings)
    {
        MutableSet<RelationalOperationElement> columnMappingRootTables = Sets.mutable.empty();
        for (ColumnMapping columnMapping : columnMappings)
        {
            RelationalOperationElement columnMappingRelationalElement = columnMapping._relationalOperationElement();
            RelationalOperationElement columnMappingRootTable = findMainTable(columnMappingRelationalElement);
            if (columnMappingRootTable != null)
            {
                columnMappingRootTables.add(columnMappingRootTable);
            }
        }
        return identifyMainTable(srcView, view, columnMappingRootTables);
    }

    private static RelationalOperationElement findMainTable(RelationalOperationElement relationalOperationElement)
    {
        ImmutableList<RelationalOperationElement> colMappingTablesRootFirst = findAllTablesRootFirst(relationalOperationElement);
        // TODO validateColumnReferencesOnlyReferToOneDB
        return colMappingTablesRootFirst.toList().getFirst();
    }

    private static RelationalOperationElement identifyMainTable(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.View srcView, View view, SetIterable<RelationalOperationElement> mainTables)
    {
        if (mainTables.isEmpty())
        {
            throw new EngineException("Unable to determine mainTable for View: " + srcView.name, srcView.sourceInformation, EngineErrorType.COMPILATION);
        }
        if (mainTables.size() > 1)
        {
            MutableList<String> tableNames = mainTables.collect(HelperRelationalBuilder::getNameValueWithUserPath, Lists.mutable.ofInitialCapacity(mainTables.size())).sortThis();
            throw new EngineException("View: " + getNameValueWithUserPath(view) + " contains multiple main tables: " + tableNames.makeString("[", ",", "]") + " there should be only one root Table for Views", srcView.sourceInformation, EngineErrorType.COMPILATION);
        }
        return mainTables.getAny();
    }

    private static String getNameValueWithUserPath(RelationalOperationElement mainTable)
    {
        // TODO Is this really necessary?
        if (mainTable instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement element = ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) mainTable);
            org.finos.legend.pure.m3.coreinstance.Package pkg = element._package();
            if (pkg == null)
            {
                return element._name();
            }
            return PackageableElement.writeUserPathForPackageableElement(new StringBuilder(), pkg).append(PackageableElement.DEFAULT_PATH_SEPARATOR).append(element._name()).toString();
        }
        if (mainTable instanceof NamedRelation)
        {
            return ((NamedRelation) mainTable)._name();
        }
        return "";
    }

    private static ImmutableList<RelationalOperationElement> findAllTablesRootFirst(RelationalOperationElement relationalOperationElement)
    {
        MutableList<RelationalOperationElement> allTables = Lists.mutable.empty();
        if (relationalOperationElement instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn)
        {
            allTables.add(((org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn) relationalOperationElement)._alias() == null ? null : ((org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn) relationalOperationElement)._alias()._relationalElement());
        }
        else if (relationalOperationElement instanceof RelationalOperationElementWithJoin)
        {
            RelationalOperationElement joinRelationalOperationElement = ((RelationalOperationElementWithJoin) relationalOperationElement)._relationalOperationElement();
            RelationalOperationElement targetTable = findAllTablesRootFirst(joinRelationalOperationElement).toList().getFirst();
            JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin) relationalOperationElement)._joinTreeNode();
            allTables.addAllIterable(findAllJoinTreeNodeTablesRootFirst(joinTreeNode, targetTable));
        }
        else if (relationalOperationElement instanceof DynaFunction)
        {
            RichIterable<? extends RelationalOperationElement> params = ((DynaFunction) relationalOperationElement)._parameters();
            MutableList<RelationalOperationElement> tablesForParams = params.flatCollect(HelperRelationalBuilder::findAllTablesRootFirst, Lists.mutable.empty());
            if (tablesForParams.size() == 1)
            {
                allTables.add(tablesForParams.toList().getFirst());
            }
        }
        return allTables.distinct().toImmutable();
    }

    private static ListIterable<RelationalOperationElement> findAllJoinTreeNodeTablesRootFirst(JoinTreeNode joinTreeNode, RelationalOperationElement targetTable)
    {
        ListIterable<Join> joins = getAllJoins(joinTreeNode);
        MutableList<RelationalOperationElement> allTables = Lists.mutable.with(targetTable);
        for (Join join : joins.asReversed())
        {
            ListIterable<RelationalOperationElement> others = join._aliases().collectWith((aliasPair, target) ->
            {
                RelationalOperationElement first = aliasPair._first() == null ? null : aliasPair._first()._relationalElement();
                RelationalOperationElement second = aliasPair._second() == null ? null : aliasPair._second()._relationalElement();
                return (target != first) ? first : second;
            }, targetTable, Lists.mutable.empty());
            targetTable = others.getFirst();
            allTables.add(targetTable);
        }
        return allTables.reverseThis();
    }

    private static ListIterable<Join> getAllJoins(JoinTreeNode joinTreeNode)
    {
        return collectJoins(joinTreeNode, Lists.mutable.empty());
    }

    private static MutableList<Join> collectJoins(JoinTreeNode joinTreeNode, MutableList<Join> results)
    {
        results.add(joinTreeNode._join());
        joinTreeNode._childrenData().forEach(c -> collectJoins((JoinTreeNode) c, results));
        return results;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.join.Join processDatabaseJoin(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Join srcJoin, CompileContext context, Database database)
    {
        MutableMap<String, TableAlias> aliasMap = MapAdapter.adapt(new LinkedHashMap<>());
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn> selfJoinTargets = Lists.mutable.empty();
        Operation op = (Operation) processRelationalOperationElement(srcJoin.operation, context, aliasMap, selfJoinTargets);
        MutableList<TableAlias> aliases = Lists.mutable.withAll(aliasMap.values());
        Join join = new Root_meta_relational_metamodel_join_Join_Impl(srcJoin.name, SourceInformationHelper.toM3SourceInformation(srcJoin.sourceInformation), context.pureModel.getClass(M2RelationalPaths.Join))
                ._name(srcJoin.name);
        if (aliases.size() == 2)
        {
            join._target(aliases.select(tableAlias -> tableAlias._name().equals(srcJoin.target)).getLast());
        }
        if (aliases.isEmpty())
        {
            throw new EngineException("A join must refer to at least one table", srcJoin.sourceInformation, EngineErrorType.COMPILATION);
        }
        if (aliases.size() > 2)
        {
            throw new EngineException("A join can only contain 2 tables. Please use Join chains (using '>') in your mapping in order to compose many of them.", srcJoin.sourceInformation, EngineErrorType.COMPILATION);
        }
        if (aliases.size() == 1)
        {
            // Self Join
            if (selfJoinTargets.isEmpty())
            {
                throw new EngineException("The system can only find one table in the join. Please use the '{target}' notation in order to define a directed self join.", srcJoin.sourceInformation, EngineErrorType.COMPILATION);
            }
            TableAlias existingAlias = aliases.get(0);
            String existingAliasName = existingAlias._name();
            RelationalOperationElement existingRelationalElement = existingAlias._relationalElement();

            TableAlias tableAlias = new Root_meta_relational_metamodel_TableAlias_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAlias"));
            tableAlias._name("t_" + existingAliasName);
            tableAlias._relationalElement(existingRelationalElement);
            aliases.add(tableAlias);

            join._target(tableAlias);
            for (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn selfJoinTarget : selfJoinTargets)
            {
                selfJoinTarget._alias(tableAlias);
                final String columnName = selfJoinTarget._columnName();
                Column col = null;
                if (existingRelationalElement instanceof Relation)
                {
                    col = (Column) ((Relation) existingRelationalElement)._columns().detect(c -> columnName.equals(((Column) c)._name()));
                }
                if (col == null)
                {
                    throw new EngineException("The column '" + columnName + "' can't be found in the table '" + ((NamedRelation) existingRelationalElement)._name() + "'");
                }
                selfJoinTarget._column(col);
            }
        }
        GenericType gt = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass(M3Paths.GenericType))
                                ._rawType(context.pureModel.getType(M3Paths.Pair))
                                ._typeArguments(Lists.mutable.with(context.pureModel.getGenericType(M2RelationalPaths.TableAlias),context.pureModel.getGenericType(M2RelationalPaths.TableAlias)));
        join._aliases(Lists.fixedSize.of(
                    new Root_meta_pure_functions_collection_Pair_Impl<TableAlias, TableAlias>("", null, context.pureModel.getClass(M3Paths.Pair))
                                ._classifierGenericType(gt)
                                ._first(aliases.get(0))
                                ._second(aliases.get(1)),
                    new Root_meta_pure_functions_collection_Pair_Impl<TableAlias, TableAlias>("", null, context.pureModel.getClass(M3Paths.Pair))
                                ._classifierGenericType(gt)
                                ._first(aliases.get(1))
                                ._second(aliases.get(0))
                      )
                )._database(database)
                ._operation(op);
        return join;
    }

    public static Filter processDatabaseFilter(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Filter srcFilter, CompileContext context, Database database)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation m3SourceInformation = SourceInformationHelper.toM3SourceInformation(srcFilter.sourceInformation);
        MutableMap<String, TableAlias> aliasMap = Maps.mutable.empty();
        Operation op = (Operation) processRelationalOperationElement(srcFilter.operation, context, aliasMap, Lists.mutable.empty());
        Filter filter = "multigrain".equals(srcFilter._type) ? new Root_meta_relational_metamodel_MultiGrainFilter_Impl(srcFilter.name, m3SourceInformation, null) : new Root_meta_relational_metamodel_Filter_Impl(srcFilter.name, m3SourceInformation, null);
        filter
                ._name(srcFilter.name)
                ._database(database)
                ._operation(op);
        return filter;
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.datatype.DataType transformDatabaseDataType(DataType dataType, CompileContext context)
    {
        if (dataType instanceof VarChar)
        {
            VarChar varChar = (VarChar) dataType;
            return new Root_meta_relational_metamodel_datatype_Varchar_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Varchar"))._size(varChar.size);
        }
        else if (dataType instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.datatype.Integer)
        {
            return new org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Integer_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Integer"));
        }
        else if (dataType instanceof Decimal)
        {
            Decimal decimal = (Decimal) dataType;
            return new Root_meta_relational_metamodel_datatype_Decimal_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Decimal"))._precision(decimal.precision)._scale(decimal.scale);
        }
        else if (dataType instanceof Numeric)
        {
            Numeric numeric = (Numeric) dataType;
            return new Root_meta_relational_metamodel_datatype_Numeric_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Numeric"))._precision(numeric.precision)._scale(numeric.scale);
        }
        else if (dataType instanceof BigInt)
        {
            return new org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_BigInt_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::BigInt"));
        }
        else if (dataType instanceof Bit)
        {
            return new org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Bit_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Bit"));
        }
        else if (dataType instanceof Char)
        {
            Char _char = (Char) dataType;
            return new Root_meta_relational_metamodel_datatype_Char_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Char"))._size(_char.size);
        }
        else if (dataType instanceof Date)
        {
            return new org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Date_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Date"));
        }
        else if (dataType instanceof Double)
        {
            return new org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Double_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Double"));
        }
        else if (dataType instanceof Float)
        {
            return new org.finos.legend.pure.generated.Root_meta_relational_metamodel_datatype_Float_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Float"));
        }
        else if (dataType instanceof Real)
        {
            return new Root_meta_relational_metamodel_datatype_Real_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Real"));
        }
        else if (dataType instanceof SmallInt)
        {
            return new Root_meta_relational_metamodel_datatype_SmallInt_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::SmallInt"));
        }
        else if (dataType instanceof Timestamp)
        {
            return new Root_meta_relational_metamodel_datatype_Timestamp_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Timestamp"));
        }
        else if (dataType instanceof TinyInt)
        {
            return new Root_meta_relational_metamodel_datatype_TinyInt_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::TinyInt"));
        }
        else if (dataType instanceof Varbinary)
        {
            Varbinary varbinary = (Varbinary) dataType;
            return new Root_meta_relational_metamodel_datatype_Varbinary_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Varbinary"))._size(varbinary.size);
        }
        else if (dataType instanceof Binary)
        {
            Binary binary = (Binary) dataType;
            return new Root_meta_relational_metamodel_datatype_Binary_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Binary"))._size(binary.size);
        }
        else if (dataType instanceof Other)
        {
            return new Root_meta_relational_metamodel_datatype_Other_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Other"));
        }
        else if (dataType instanceof SemiStructured)
        {
            return new Root_meta_relational_metamodel_datatype_SemiStructured_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::SemiStructured"));
        }
        else if (dataType instanceof Json)
        {
            return new Root_meta_relational_metamodel_datatype_Json_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::datatype::Json"));
        }
        throw new UnsupportedOperationException();
    }

    public static RelationalOperationElement processRelationalOperationElement(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement operationElement, CompileContext context, MutableMap<String, org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAlias> aliasMap, MutableList<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn> selfJoinTargets)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation m3SourceInformation = SourceInformationHelper.toM3SourceInformation(operationElement.sourceInformation);
        if (operationElement instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.TableAliasColumn)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.TableAliasColumn tableAliasColumn = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.TableAliasColumn) operationElement;
            // Self join
            if (tableAliasColumn.table.table.equals(SELF_JOIN_TABLE_NAME) && tableAliasColumn.tableAlias.equals(SELF_JOIN_TABLE_NAME))
            {
                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn selfJoin = new Root_meta_relational_metamodel_TableAliasColumn_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::TableAliasColumn"))
                        ._columnName(tableAliasColumn.column);
                selfJoinTargets.add(selfJoin);
                return selfJoin;
            }
            Relation relation = getRelation((Database) context.resolveStore(tableAliasColumn.table.database, tableAliasColumn.table.sourceInformation), tableAliasColumn.table.schema, tableAliasColumn.table.table, tableAliasColumn.table.sourceInformation);
            Column col = getColumn(relation, tableAliasColumn.column, tableAliasColumn.sourceInformation);
            TableAlias alias = aliasMap.getIfAbsentPut(tableAliasColumn.table.schema + "." + tableAliasColumn.tableAlias, () -> new Root_meta_relational_metamodel_TableAlias_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAlias"))
                    ._name(tableAliasColumn.tableAlias)
                    ._relationalElement(col._owner())
                    ._database(HelperRelationalBuilder.resolveDatabase(tableAliasColumn.table.getDb(), tableAliasColumn.table.sourceInformation, context)));
            return new Root_meta_relational_metamodel_TableAliasColumn_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::TableAliasColumn"))
                    ._columnName(col._name())
                    ._column(col)
                    ._alias(alias);
        }
        else if (operationElement instanceof ElementWithJoins)
        {
            ElementWithJoins elementWithJoins = (ElementWithJoins) operationElement;
            RelationalOperationElementWithJoin res = new Root_meta_relational_metamodel_RelationalOperationElementWithJoin_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::RelationalOperationElementWithJoin"))
                    ._joinTreeNode(buildElementWithJoinsJoinTreeNode(elementWithJoins.joins, context));
            return elementWithJoins.relationalElement == null ? res : res._relationalOperationElement(processRelationalOperationElement(elementWithJoins.relationalElement, context, Maps.mutable.empty(), selfJoinTargets));
        }
        else if (operationElement instanceof DynaFunc)
        {
            DynaFunc dynaFunc = (DynaFunc) operationElement;
            MutableList<RelationalOperationElement> ps = ListIterate.collect(dynaFunc.parameters, relationalOperationElement -> processRelationalOperationElement(relationalOperationElement, context, aliasMap, selfJoinTargets));
            return new Root_meta_relational_metamodel_DynaFunction_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::DynaFunction"))
                    ._name(dynaFunc.funcName)
                    ._parameters(ps);
        }
        else if (operationElement instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.Literal)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.Literal literal = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.Literal) operationElement;
            return new Root_meta_relational_metamodel_Literal_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::Literal"))._value(convertLiteral(convertLiteral(literal.value)));
        }
        else if (operationElement instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.LiteralList)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.LiteralList literalList = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.LiteralList) operationElement;
            return new Root_meta_relational_metamodel_LiteralList_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::LiteralList"))._values(ListIterate.collect(literalList.values, l -> new Root_meta_relational_metamodel_Literal_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::Literal"))._value(convertLiteral(l.value))));
        }
        throw new UnsupportedOperationException();
    }

    private static JoinTreeNode buildElementWithJoinsJoinTreeNode(List<JoinPointer> joins, CompileContext context)
    {
        MutableList<JoinWithJoinType> newJoins = ListIterate.collect(joins, joinPointer -> new JoinWithJoinType(
                getJoin(joinPointer, context), joinPointer.joinType == null
                ? null : context.pureModel.getEnumValue("meta::relational::metamodel::join::JoinType", "INNER".equals(joinPointer.joinType) ? "INNER" : "LEFT_OUTER")));
        return processElementWithJoinsJoins(newJoins, context);
    }

    private static JoinTreeNode processElementWithJoinsJoins(MutableList<JoinWithJoinType> joins, CompileContext context)
    {
        Join j = joins.getFirst().join;
        JoinTreeNode res = new Root_meta_relational_metamodel_join_JoinTreeNode_Impl(j._name(), null, context.pureModel.getClass(M2RelationalPaths.JoinTreeNode))
                ._joinName(j._name())
                ._database(j._database())
                ._join(j);
        res = joins.getFirst().joinType == null ? res : res._joinType(joins.getFirst().joinType);
        return joins.size() == 1 ? res : res._childrenData(Lists.fixedSize.of(processElementWithJoinsJoins(joins.subList(1, joins.size()), context)));
    }

    private static class JoinWithJoinType
    {
        Join join;
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum joinType;

        public JoinWithJoinType(Join join, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum joinType)
        {
            this.join = join;
            this.joinType = joinType;
        }
    }

    // NOTE: Pure does not handle some default java types, so require converting
    private static Object convertLiteral(Object o)
    {
        if (o instanceof Integer)
        {
            return Long.valueOf((Integer) o);
        }
        return o;
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning processMilestoning(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning milestoning, CompileContext context, Multimap<String, Column> columns)
    {
        List<IRelationalCompilerExtension> extensions = IRelationalCompilerExtension.getExtensions(context);

        return IRelationalCompilerExtension.process(milestoning,
                ListIterate.flatCollect(extensions, IRelationalCompilerExtension::getExtraMilestoningProcessors),
                columns, context);
    }

    public static RichIterable<Column> getMilestoneColumn(String colName, Multimap<String, Column> columns, SourceInformation sourceInformation)
    {
        RichIterable<Column> c = columns.get(colName);
        if (c == null || c.isEmpty())
        {
            throw new EngineException(String.format("Milestone column '%s' not found on table definition", colName), sourceInformation, EngineErrorType.COMPILATION);
        }
        return c;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Milestoning visitMilestoning(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning milestoning, CompileContext context, Multimap<String, Column> columns)
    {
        org.finos.legend.pure.m4.coreinstance.SourceInformation m3SourceInformation = SourceInformationHelper.toM3SourceInformation(milestoning.sourceInformation);
        if (milestoning instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessMilestoning)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessMilestoning businessMilestoning = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessMilestoning) milestoning;
            Root_meta_relational_metamodel_relation_BusinessMilestoning_Impl pureBm = new Root_meta_relational_metamodel_relation_BusinessMilestoning_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::relation::BusinessMilestoning"));
            pureBm._from(getMilestoneColumn(businessMilestoning.from, columns, milestoning.sourceInformation));
            pureBm._thru(getMilestoneColumn(businessMilestoning.thru, columns, milestoning.sourceInformation));
            if (businessMilestoning.infinityDate != null)
            {
                pureBm._infinityDate((PureDate) ((InstanceValue) businessMilestoning.infinityDate.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext("TO REMOVE!"))))._values().getFirst());
            }
            pureBm._thruIsInclusive(businessMilestoning.thruIsInclusive);
            return pureBm;
        }
        else if (milestoning instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessSnapshotMilestoning)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessSnapshotMilestoning businessSnapshotMilestoning = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessSnapshotMilestoning) milestoning;
            Root_meta_relational_metamodel_relation_BusinessSnapshotMilestoning_Impl pureBsm = new Root_meta_relational_metamodel_relation_BusinessSnapshotMilestoning_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::relation::BusinessSnapshotMilestoning"));
            pureBsm._snapshotDate(getMilestoneColumn(businessSnapshotMilestoning.snapshotDate, columns, milestoning.sourceInformation));
            return pureBsm;
        }
        else if (milestoning instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingMilestoning)
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingMilestoning processingMilestoning = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingMilestoning) milestoning;
            Root_meta_relational_metamodel_relation_ProcessingMilestoning_Impl purePm = new Root_meta_relational_metamodel_relation_ProcessingMilestoning_Impl("", m3SourceInformation, context.pureModel.getClass("meta::relational::metamodel::relation::ProcessingMilestoning"));
            purePm._in(getMilestoneColumn(processingMilestoning.in, columns, milestoning.sourceInformation));
            purePm._out(getMilestoneColumn(processingMilestoning.out, columns, milestoning.sourceInformation));
            if (processingMilestoning.infinityDate != null)
            {
                purePm._infinityDate((PureDate) ((InstanceValue) processingMilestoning.infinityDate.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext("TO REMOVE!"))))._values().getFirst());
            }
            purePm._outIsInclusive(processingMilestoning.outIsInclusive);
            return purePm;
        }
        return null;
    }

    public static PropertyMapping processAbstractRelationalPropertyMapping(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping, CompileContext context, PropertyMappingsImplementation immediateParent, InstanceSetImplementation topParent, MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings, RichIterable<EnumerationMapping<Object>> allEnumerationMappings, MutableMap<String, TableAlias> aliasMap)
    {
        if (propertyMapping instanceof RelationalPropertyMapping)
        {
            return processRelationalPropertyMapping((RelationalPropertyMapping) propertyMapping, context, immediateParent, topParent, allEnumerationMappings, aliasMap);
        }
        else if (propertyMapping instanceof EmbeddedRelationalPropertyMapping)
        {
            return processEmbeddedRelationalPropertyMapping((EmbeddedRelationalPropertyMapping) propertyMapping, context, immediateParent, topParent, embeddedRelationalPropertyMappings, allEnumerationMappings, aliasMap);
        }
        else if (propertyMapping instanceof InlineEmbeddedPropertyMapping)
        {
            return processInlineEmbeddedPropertyMapping((InlineEmbeddedPropertyMapping) propertyMapping, context, immediateParent, topParent, embeddedRelationalPropertyMappings);
        }
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PropertyMapping processRelationalPropertyMapping(RelationalPropertyMapping propertyMapping, CompileContext context, PropertyMappingsImplementation immediateParent, InstanceSetImplementation topParent, RichIterable<EnumerationMapping<Object>> allEnumerationMappings, MutableMap<String, TableAlias> aliasMap)
    {
        org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping rpm = new Root_meta_relational_mapping_RelationalPropertyMapping_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getClass("meta::relational::mapping::RelationalPropertyMapping"));
        Property property = resolvePropertyForRelationalPropertyMapping(propertyMapping, immediateParent, context);
        if (propertyMapping.bindingTransformer != null)
        {
            return buildSemiStructuredPropertyMapping(property, propertyMapping, immediateParent, (RootRelationalInstanceSetImplementation) topParent, aliasMap, context);
        }
        org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping res = rpm
                ._property(property)
                ._localMappingProperty(propertyMapping.localMappingProperty != null)
                ._relationalOperationElement(processRelationalOperationElement(propertyMapping.relationalOperation, context, aliasMap, Lists.mutable.empty()))
                ._sourceSetImplementationId(HelperRelationalBuilder.getPropertyMappingSourceId(propertyMapping, immediateParent, property, context))
                ._targetSetImplementationId(HelperRelationalBuilder.getPropertyMappingTargetId(propertyMapping, immediateParent, property, context))
                ._owner(immediateParent);

        rpm.setSourceInformation(SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation));

        if (propertyMapping.localMappingProperty != null)
        {
            res._localMappingPropertyType(context.resolveType(propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation));
            res._localMappingPropertyMultiplicity(context.pureModel.getMultiplicity(propertyMapping.localMappingProperty.multiplicity));
        }

        if (propertyMapping.enumMappingId != null)
        {
            EnumerationMapping<Object> eMap = allEnumerationMappings.detect(e -> e._name().equals(propertyMapping.enumMappingId));
            Assert.assertTrue(eMap != null, () -> "Can't find enumeration mapping '" + propertyMapping.enumMappingId + "'");
            res = res._transformer(eMap);
        }
        return res;
    }

    private static PropertyMapping processEmbeddedRelationalPropertyMapping(EmbeddedRelationalPropertyMapping propertyMapping, CompileContext context, PropertyMappingsImplementation immediateParent, InstanceSetImplementation topParent, MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings, RichIterable<EnumerationMapping<Object>> allEnumerationMappings, MutableMap<String, TableAlias> aliasMap)
    {
        if (propertyMapping instanceof OtherwiseEmbeddedRelationalPropertyMapping)
        {
            return processOtherwiseEmbeddedRelationalPropertyMapping((OtherwiseEmbeddedRelationalPropertyMapping) propertyMapping, context, immediateParent, topParent, embeddedRelationalPropertyMappings, allEnumerationMappings, aliasMap);
        }
        EmbeddedRelationalInstanceSetImplementation rpm = new Root_meta_relational_mapping_EmbeddedRelationalInstanceSetImplementation_Impl(propertyMapping.id, SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), null);
        return processEmbeddedRelationalPropertyMapping(propertyMapping, rpm, context, immediateParent, (RootRelationalInstanceSetImplementation) topParent, embeddedRelationalPropertyMappings, allEnumerationMappings, aliasMap);
    }

    private static PropertyMapping processOtherwiseEmbeddedRelationalPropertyMapping(OtherwiseEmbeddedRelationalPropertyMapping propertyMapping, CompileContext context, PropertyMappingsImplementation immediateParent, InstanceSetImplementation topParent, MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings, RichIterable<EnumerationMapping<Object>> allEnumerationMappings, MutableMap<String, TableAlias> aliasMap)
    {
        org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.OtherwiseEmbeddedRelationalInstanceSetImplementation rpm = new Root_meta_relational_mapping_OtherwiseEmbeddedRelationalInstanceSetImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getClass("meta::relational::mapping::OtherwiseEmbeddedRelationalInstanceSetImplementation"));
        rpm._otherwisePropertyMapping(processAbstractRelationalPropertyMapping(propertyMapping.otherwisePropertyMapping, context, immediateParent, topParent, embeddedRelationalPropertyMappings, allEnumerationMappings, aliasMap));
        return processEmbeddedRelationalPropertyMapping(propertyMapping, rpm, context, immediateParent, (RootRelationalInstanceSetImplementation) topParent, embeddedRelationalPropertyMappings, allEnumerationMappings, aliasMap);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static PropertyMapping processInlineEmbeddedPropertyMapping(InlineEmbeddedPropertyMapping propertyMapping, CompileContext context, PropertyMappingsImplementation immediateParent, InstanceSetImplementation topParent, MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings)
    {
        InlineEmbeddedRelationalInstanceSetImplementation rpm = new Root_meta_relational_mapping_InlineEmbeddedRelationalInstanceSetImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getClass("meta::relational::mapping::InlineEmbeddedRelationalInstanceSetImplementation"))._inlineSetImplementationId(propertyMapping.setImplementationId);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class propertyOwnerClass = extractPropertyOwner(context, propertyMapping, immediateParent);
        Property property = HelperModelBuilder.getPropertyOrResolvedEdgePointProperty(context, propertyOwnerClass, Optional.empty(), propertyMapping.property.property, propertyMapping.property.sourceInformation);
        String sourceId = HelperRelationalBuilder.getPropertyMappingSourceId(propertyMapping, immediateParent, property, context);
        rpm._property(property)
                ._sourceSetImplementationId(sourceId)
                ._targetSetImplementationId(HelperRelationalBuilder.getPropertyMappingTargetId(propertyMapping, immediateParent, property, context))
                ._owner(immediateParent)
                ._id(propertyMapping.id == null ? sourceId + "_" + propertyMapping.property.property : propertyMapping.id)
                ._class((Class<?>) property._genericType()._rawType())
                ._setMappingOwner((RootRelationalInstanceSetImplementation) topParent)
                ._parent(topParent._parent());
        embeddedRelationalPropertyMappings.add(rpm);
        return rpm;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static SetImplementation processRelationalClassMapping(RelationalClassMapping relationalClassMapping, CompileContext context, RelationalInstanceSetImplementation base, RootRelationalInstanceSetImplementation topParent, Mapping parent, MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping<Object>> enumerationMappings, MutableMap<String, TableAlias> aliasMap)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class pureClass = relationalClassMapping._class != null ? context.resolveClass(relationalClassMapping._class, relationalClassMapping.classSourceInformation) : base._class();
        RichIterable<org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement> primaryKey = ListIterate.collect(relationalClassMapping.primaryKey, p -> processRelationalOperationElement(p, context, Maps.mutable.empty(), Lists.mutable.empty()));
        MutableList<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping> localMappingProperties = ListIterate.select(relationalClassMapping.propertyMappings, p -> p.localMappingProperty != null);
        if (localMappingProperties.notEmpty())
        {
            MappingClass mappingClass = new Root_meta_pure_mapping_MappingClass_Impl<>("", SourceInformationHelper.toM3SourceInformation(relationalClassMapping.sourceInformation), null);
            mappingClass._name(pureClass._name() + "_" + parent._name() + "_" + base._id());
            GenericType gType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                    ._rawType(context.pureModel.getType("meta::pure::mapping::MappingClass"))
                    ._typeArguments(Lists.mutable.with(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(mappingClass)));
            mappingClass._classifierGenericType(gType);
            Generalization g = new Root_meta_pure_metamodel_relationship_Generalization_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::relationship::Generalization"))
                    ._specific(mappingClass)
                    ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(pureClass));
            mappingClass._generalizations(Lists.mutable.with(g));
            mappingClass._properties(localMappingProperties.collect(propertyMapping ->
            {
                GenericType returnGenericType = context.resolveGenericType(propertyMapping.localMappingProperty.type, propertyMapping.localMappingProperty.sourceInformation);
                return new Root_meta_pure_metamodel_function_property_Property_Impl<>(propertyMapping.property.property)
                        ._name(propertyMapping.property.property)
                        ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::pure::metamodel::function::property::Property"))._typeArguments(Lists.fixedSize.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(mappingClass), returnGenericType)))
                        ._genericType(returnGenericType)
                        ._multiplicity(context.pureModel.getMultiplicity(propertyMapping.localMappingProperty.multiplicity))
                        ._owner(mappingClass);
            }));
            base._mappingClass(mappingClass);
        }
        base._class(pureClass)
                ._primaryKey(primaryKey)
                ._propertyMappings(ListIterate.collect(relationalClassMapping.propertyMappings, propertyMapping -> processAbstractRelationalPropertyMapping(propertyMapping, context, base, topParent, embeddedRelationalPropertyMappings, enumerationMappings, aliasMap)))
                ._parent(parent);
        return base;
    }

    public static void processRelationalPrimaryKey(RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation, CompileContext context)
    {
        if (rootRelationalInstanceSetImplementation._groupBy() != null)
        {
            rootRelationalInstanceSetImplementation._primaryKey(rootRelationalInstanceSetImplementation._groupBy()._columns());
        }
        // TODO handle distinct
        else if (rootRelationalInstanceSetImplementation._primaryKey().isEmpty())
        {
            TableAlias mainTableAlias = rootRelationalInstanceSetImplementation._mainTableAlias();
            Relation relation = (Relation) mainTableAlias._relationalElement();
            RichIterable<? extends Column> columns;
            if (relation instanceof Table)
            {
                columns = ((Table) relation)._primaryKey();
            }
            else if (relation instanceof View)
            {
                columns = ((View) relation)._primaryKey();
            }
            else
            {
                columns = Lists.mutable.empty();
            }
            RichIterable<TableAliasColumn> primaryKey = columns.collect(column ->
            {
                TableAliasColumn tableAliasColumn = new Root_meta_relational_metamodel_TableAliasColumn_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAliasColumn"));
                tableAliasColumn._column(column);
                tableAliasColumn._alias(mainTableAlias);
                return tableAliasColumn;
            });
            rootRelationalInstanceSetImplementation._primaryKey(primaryKey);
        }
    }

    public static Relation getRelation(TablePtr tableptr, CompileContext context)
    {
        return getRelation(resolveDatabase(tableptr.database, tableptr.sourceInformation, context), tableptr.schema, tableptr.table);
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> getPropertyOwnerForRelationalPropertyMapping(CompileContext context, RelationalPropertyMapping propertyMapping, PropertyMappingsImplementation immediateParent)
    {
        if (propertyMapping.property._class != null)
        {
            PropertyOwner owner = context.resolvePropertyOwner(propertyMapping.property._class, propertyMapping.property.sourceInformation);
            return owner instanceof Class<?> ? (Class<?>) owner : HelperModelBuilder.getAssociationPropertyClass((Association) owner, propertyMapping.property.property, propertyMapping.property.sourceInformation, context);
        }
        else if (immediateParent instanceof EmbeddedRelationalInstanceSetImplementation)
        {
            return ((EmbeddedRelationalInstanceSetImplementation) immediateParent)._class();
        }
        else
        {
            throw new EngineException("Can't find property owner class for property" + propertyMapping.property.property, propertyMapping.property.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> extractPropertyOwner(CompileContext context, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping, PropertyMappingsImplementation immediateParent)
    {
        if (propertyMapping.property._class != null)
        {
            PropertyOwner owner = context.resolvePropertyOwner(propertyMapping.property._class, propertyMapping.property.sourceInformation);
            return owner instanceof Class<?> ? (Class<?>) owner : HelperModelBuilder.getAssociationPropertyClass((Association) owner, propertyMapping.property.property, propertyMapping.property.sourceInformation, context);
        }
        else if (immediateParent instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation)
        {
            return ((org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalInstanceSetImplementation) immediateParent)._class();
        }
        else
        {
            throw new EngineException("Can't find property owner class for property" + propertyMapping.property.property, propertyMapping.property.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping processEmbeddedRelationalPropertyMapping(EmbeddedRelationalPropertyMapping propertyMapping, EmbeddedRelationalInstanceSetImplementation rpm, CompileContext context, PropertyMappingsImplementation firstParent, RootRelationalInstanceSetImplementation topParent, MutableList<EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings, RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping<Object>> enumerationMappings, MutableMap<String, TableAlias> aliasMap)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> propertyOwnerClass = extractPropertyOwner(context, propertyMapping, firstParent);
        Property<?, ?> property = HelperModelBuilder.getPropertyOrResolvedEdgePointProperty(context, propertyOwnerClass, Optional.empty(), propertyMapping.property.property, propertyMapping.property.sourceInformation);

        String id = null;
        if (propertyMapping.classMapping.id != null)
        {
            id = propertyMapping.classMapping.id;
        }
        else if (firstParent._id() != null)
        {
            id = firstParent._id() + "_" + propertyMapping.property.property;
        }
        else if (propertyMapping.classMapping._class != null)
        {
            id = HelperMappingBuilder.getClassMappingId(propertyMapping.classMapping, context);
        }

        if (id == null || id.isEmpty())
        {
            throw new EngineException("Can't resolve id for '" + propertyMapping.property.property + "'", propertyMapping.property.sourceInformation, EngineErrorType.COMPILATION);
        }

        rpm._property(property)
                ._sourceSetImplementationId(HelperRelationalBuilder.getPropertyMappingSourceId(propertyMapping, firstParent, property, context))
                ._targetSetImplementationId(HelperRelationalBuilder.getPropertyMappingTargetId(propertyMapping, firstParent, property, context))
                ._owner(firstParent)
                ._id(id)
                ._setMappingOwner(topParent);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class;
        if (propertyMapping.classMapping._class != null)
        {
            _class = context.resolveClass(propertyMapping.classMapping._class, propertyMapping.property.sourceInformation);
        }
        else
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type propertyType = property._genericType()._rawType();
            Assert.assertTrue(propertyType instanceof Class, () -> "only complex classes can be the target of an embedded property mapping", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
            _class = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) propertyType;
        }
        rpm._class(_class);
        processRelationalClassMapping(propertyMapping.classMapping, context, rpm, topParent, topParent._parent(), embeddedRelationalPropertyMappings, enumerationMappings, aliasMap);
        embeddedRelationalPropertyMappings.add(rpm);
        embeddedRelationalPropertyMappings.addAll(generateMilestoningRangeEmbeddedPropertyMapping(rpm, topParent, context));
        return rpm;
    }

    private static Property<?, ?> resolvePropertyForRelationalPropertyMapping(RelationalPropertyMapping propertyMapping, PropertyMappingsImplementation immediateParent, CompileContext context)
    {
        String propertyName = propertyMapping.property.property;
        String edgePointPropertyName = MilestoningFunctions.getEdgePointPropertyName(propertyName);

        // case where local property is not null and you find property in mapping class
        if (propertyMapping.localMappingProperty != null)
        {
            return ((InstanceSetImplementation) immediateParent)._mappingClass()._properties().detect(p -> p._name().equals(propertyName));
        }
        // case were class is not defined and the parent is an association mapping. search for property inside the asssociation
        else if (immediateParent instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.AssociationImplementation && propertyMapping.property._class == null)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association association = ((AssociationImplementation) immediateParent)._association();
            Property<?, ?> property = association._properties().detect(p -> (propertyName.equals(p.getName())) || (isTypeTemporalMilestoned(p._genericType()._rawType()) && edgePointPropertyName.equals(p.getName())));
            Assert.assertTrue(property != null, () -> "Can't find property '" + propertyName + "' in association '" + (HelperModelBuilder.getElementFullPath(association, context.pureModel.getExecutionSupport())) + "'", propertyMapping.property.sourceInformation, EngineErrorType.COMPILATION);
            return property;
        }
        // look for property using the class defined
        else
        {
            Class<?> _class = getPropertyOwnerForRelationalPropertyMapping(context, propertyMapping, immediateParent);
            return HelperModelBuilder.getPropertyOrResolvedEdgePointProperty(context, _class, Optional.empty(), propertyName, true, propertyMapping.sourceInformation);
        }
    }

    private static boolean isTypeTemporalMilestoned(Type type)
    {
        return (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) &&
                (Milestoning.temporalStereotypes(((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) type)._stereotypes()) != null);
    }

    public static MutableList<EmbeddedRelationalInstanceSetImplementation> generateMilestoningRangeEmbeddedPropertyMapping(RelationalInstanceSetImplementation immediateRelationalParentSet, RootRelationalInstanceSetImplementation rootRelationalParentSet, CompileContext context)
    {
        MutableList<EmbeddedRelationalInstanceSetImplementation> generatedMilestoningRangeEmbeddedPropertyMapping = Lists.mutable.empty();
        if (shouldGenerateMilestoningPropertyMapping(immediateRelationalParentSet))
        {
            EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = createEmbeddedRelationalInstance(immediateRelationalParentSet, rootRelationalParentSet, context);
            immediateRelationalParentSet._propertyMappingsAdd(embeddedRelationalInstance);
            rootRelationalParentSet._parent()._classMappingsAdd(embeddedRelationalInstance);
            generatedMilestoningRangeEmbeddedPropertyMapping.add(embeddedRelationalInstance);
        }
        return generatedMilestoningRangeEmbeddedPropertyMapping;
    }

    private static EmbeddedRelationalInstanceSetImplementation createEmbeddedRelationalInstance(RelationalInstanceSetImplementation immediateRelationalParentSet, RootRelationalInstanceSetImplementation rootRelationalParentSet, CompileContext context)
    {
        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = new Root_meta_relational_mapping_EmbeddedRelationalInstanceSetImplementation_Impl("", null, context.pureModel.getClass("meta::relational::mapping::EmbeddedRelationalInstanceSetImplementation"));
        return embeddedRelationalInstance
                ._root(false)
                ._sourceSetImplementationId(immediateRelationalParentSet._id())
                ._id(embeddedRelationalInstance._sourceSetImplementationId() + "_" + MilestoningFunctions.MILESTONING)
                ._targetSetImplementationId(embeddedRelationalInstance._id())
                ._property((Property<?, ?>) HelperModelBuilder.getAppliedProperty(context, immediateRelationalParentSet._class(), Optional.empty(), MilestoningFunctions.MILESTONING))
                ._class((Class<?>) embeddedRelationalInstance._property()._genericType()._rawType())
                ._parent(rootRelationalParentSet._parent())
                ._owner(immediateRelationalParentSet)
                ._setMappingOwner(rootRelationalParentSet)
                ._propertyMappings(createRelationalPropertyMappings(immediateRelationalParentSet._class(), rootRelationalParentSet, embeddedRelationalInstance, context));
    }

    private static MutableList<org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping> createRelationalPropertyMappings(Class<?> immediateRelationalParentClass, RootRelationalInstanceSetImplementation rootRelationalParentSet, EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance, CompileContext context)
    {
        return getTemporalStereotype(immediateRelationalParentClass).getMilestoningPropertyNames().collect(propertyName -> createRelationalPropertyMapping(propertyName, rootRelationalParentSet, embeddedRelationalInstance, context));
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping createRelationalPropertyMapping(String propertyName, RootRelationalInstanceSetImplementation rootRelationalParentSet, EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance, CompileContext context)
    {
        return new Root_meta_relational_mapping_RelationalPropertyMapping_Impl("", null, context.pureModel.getClass("meta::relational::mapping::RelationalPropertyMapping"))
                ._localMappingProperty(false)
                ._property((Property<?, ?>) HelperModelBuilder.getAppliedProperty(context, embeddedRelationalInstance._class(), Optional.empty(), propertyName))
                ._owner(embeddedRelationalInstance)
                ._sourceSetImplementationId(embeddedRelationalInstance._id())
                ._targetSetImplementationId("")
                ._relationalOperationElement(createRelationalOperationElement(propertyName, rootRelationalParentSet, context));
    }

    private static RelationalOperationElement createRelationalOperationElement(String propertyName, RootRelationalInstanceSetImplementation rootRelationalParentSet, CompileContext context)
    {
        try
        {
            TableAlias mainTableAlias = rootRelationalParentSet._mainTableAlias();
            Column column = getColumn(mainTableAlias, propertyName);
            return createTableAliasColumn(mainTableAlias, rootRelationalParentSet, column, context);
        }
        catch (Exception e)
        {
            return createSQLNullLiteral(context);
        }
    }

    private static TableAliasColumn createTableAliasColumn(TableAlias mainTableAlias, RootRelationalInstanceSetImplementation rootRelationalParentSet, Column column, CompileContext context)
    {
        return new Root_meta_relational_metamodel_TableAliasColumn_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAliasColumn"))
                ._alias(createTableAlias(mainTableAlias, context))
                ._setMappingOwner(rootRelationalParentSet)
                ._column(column);
    }

    private static TableAlias createTableAlias(TableAlias mainTableAlias, CompileContext context)
    {
        return new Root_meta_relational_metamodel_TableAlias_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::TableAlias"))
                ._database(mainTableAlias._database())
                ._relationalElement(mainTableAlias._relationalElement())
                ._name(mainTableAlias._name());
    }

    private static Literal createSQLNullLiteral(CompileContext context)
    {
        return new Root_meta_relational_metamodel_Literal_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::Literal"))
                ._value(new Root_meta_relational_metamodel_SQLNull_Impl("", null, context.pureModel.getClass("meta::relational::metamodel::SQLNull")));
    }

    private static boolean shouldGenerateMilestoningPropertyMapping(RelationalInstanceSetImplementation immediateRelationalParentSet)
    {
        boolean isBaseMapping = immediateRelationalParentSet._superSetImplementationId() == null;
        boolean isClassTemporalStereotyped = getTemporalStereotype(immediateRelationalParentSet._class()) != null;
        return isBaseMapping && isClassTemporalStereotyped;
    }

    private static MilestoningStereotype getTemporalStereotype(Class<?> immediateRelationalParentSet)
    {
        return Milestoning.temporalStereotypes(immediateRelationalParentSet._stereotypes());
    }

    private static Column getColumn(TableAlias tableAlias, String propertyName)
    {
        Table table = (Table) tableAlias._relationalElement();
        BusinessMilestoning businessMilestoning = (BusinessMilestoning) table._milestoning().detect(m -> m instanceof BusinessMilestoning);
        ProcessingMilestoning processingMilestoning = (ProcessingMilestoning) table._milestoning().detect(m -> m instanceof ProcessingMilestoning);
        switch (propertyName)
        {
            case "from":
            {
                return businessMilestoning._from();
            }
            case "thru":
            {
                return businessMilestoning._thru();
            }
            case "in":
            {
                return processingMilestoning._in();
            }
            case "out":
            {
                return processingMilestoning._out();
            }
            default:
            {
                return null;
            }
        }
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping processFilterMapping(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.FilterMapping srcFilterMapping, Database ownerDb, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping filterMapping = new Root_meta_relational_mapping_FilterMapping_Impl("", SourceInformationHelper.toM3SourceInformation(srcFilterMapping.sourceInformation), context.pureModel.getClass("meta::relational::mapping::FilterMapping"));
        filterMapping._filter(getFilter(ownerDb, srcFilterMapping.filter.name, srcFilterMapping.sourceInformation));
        filterMapping._filterName(srcFilterMapping.filter.name);
        if (!srcFilterMapping.joins.isEmpty())
        {
            filterMapping._joinTreeNode(buildElementWithJoinsJoinTreeNode(srcFilterMapping.joins, context));
        }
        return filterMapping;
    }

    private static void collectJoinTreeNodesFromPropertyMapping(List<org.eclipse.collections.api.tuple.Pair<String, JoinTreeNode>> targetCollection, PropertyMapping propertyMapping)
    {
        if (propertyMapping instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping)
        {
            RelationalOperationElement relationalOperationElement = ((org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping) propertyMapping)._relationalOperationElement();
            HelperRelationalBuilder.collectJoinTreeNodes(targetCollection, propertyMapping._property()._name(), relationalOperationElement);
        }
        if (propertyMapping instanceof OtherwiseEmbeddedRelationalInstanceSetImplementation)
        {
            PropertyMapping relationalOperationElement = ((OtherwiseEmbeddedRelationalInstanceSetImplementation) propertyMapping)._otherwisePropertyMapping();
            collectJoinTreeNodesFromPropertyMapping(targetCollection, relationalOperationElement);
        }
        if (propertyMapping instanceof PropertyMappingsImplementation)
        {
            RichIterable<? extends PropertyMapping> propertyMappings = ((PropertyMappingsImplementation) propertyMapping)._propertyMappings();
            for (PropertyMapping subMapping : propertyMappings)
            {
                collectJoinTreeNodesFromPropertyMapping(targetCollection, subMapping);
            }
        }
    }

    private static void collectJoinTreeNodes(List<org.eclipse.collections.api.tuple.Pair<String, JoinTreeNode>> targetCollection, String propertyName, RelationalOperationElement implementation)
    {
        if (implementation instanceof RelationalOperationElementWithJoin)
        {
            JoinTreeNode joinTreeNode = ((RelationalOperationElementWithJoin) implementation)._joinTreeNode();
            if (joinTreeNode != null)
            {
                targetCollection.add(Tuples.pair(propertyName, joinTreeNode));
            }
        }
        else if (implementation instanceof DynaFunction)
        {
            ((DynaFunction) implementation)._parameters().forEach(param -> HelperRelationalBuilder.collectJoinTreeNodes(targetCollection, propertyName, param));
        }
    }

    public static void validatePropertyMappings(RootRelationalClassMapping classMapping, RootRelationalInstanceSetImplementation pureSetImp)
    {
        List<org.eclipse.collections.api.tuple.Pair<String, JoinTreeNode>> propertyAndJoinTreeNodes = Lists.mutable.empty();

        RichIterable<? extends PropertyMapping> propertyMappings = pureSetImp._propertyMappings();
        propertyMappings.forEach(propertyMapping -> HelperRelationalBuilder.collectJoinTreeNodesFromPropertyMapping(propertyAndJoinTreeNodes, propertyMapping));

        Relation mainTable = (Relation) pureSetImp._mainTableAlias()._relationalElement();
        List<org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping> legendPropertyMappings = classMapping.propertyMappings;
        ListIterate.forEach(propertyAndJoinTreeNodes, propertyAndJoinTreeNode ->
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propMapping = ListIterate.detect(legendPropertyMappings, propertyMapping -> propertyMapping.property.property.equals(propertyAndJoinTreeNode.getOne()));
            SourceInformation srcInfo = propMapping == null ? classMapping.sourceInformation : propMapping.sourceInformation;
            if (propertyAndJoinTreeNode.getTwo()._joinType() != null)
            {
                throw new EngineException("Do not support specifying join type for the first join in the classMapping.", srcInfo, EngineErrorType.COMPILATION);
            }
            HelperRelationalBuilder.validateJoinTreeNode(propertyAndJoinTreeNode.getTwo(), mainTable, srcInfo);
        });
    }

    private static TableAlias findTargetAliasFromJoinTreeNode(JoinTreeNode joinTreeNode, Relation startTable, SourceInformation sourceInformation)
    {
        Join join = joinTreeNode._join();

        Pair<? extends TableAlias, ? extends TableAlias> tableAliasPair = join._aliases().detect(aliasPair -> aliasPair._first() != null && startTable == aliasPair._first()._relationalElement());
        if (tableAliasPair == null)
        {
            throw new EngineException("Mapping error: the join " + join._name() + " does not contain the source table " + startTable.getName(), sourceInformation, EngineErrorType.COMPILATION);
        }
        return tableAliasPair._second();
    }

    private static void validateJoinTreeNode(JoinTreeNode joinTreeNode, Relation startTable, SourceInformation sourceInformation)
    {
        TableAlias alias = findTargetAliasFromJoinTreeNode(joinTreeNode, startTable, sourceInformation);
        Relation newStartTable = (Relation) alias._relationalElement();
        joinTreeNode._childrenData().forEach(child -> HelperRelationalBuilder.validateJoinTreeNode((JoinTreeNode) child, newStartTable, sourceInformation));
    }

    public static String getClassMappingId(SetImplementation cm)
    {
        if (cm._id() == null)
        {
            if (cm instanceof InlineEmbeddedRelationalInstanceSetImplementation)
            {
                InlineEmbeddedRelationalInstanceSetImplementation implementation = (InlineEmbeddedRelationalInstanceSetImplementation) cm;
                cm._id(implementation._sourceSetImplementationId() + "_" + implementation._property()._name());
            }
            else if (cm instanceof EmbeddedRelationalInstanceSetImplementation)
            {
                EmbeddedRelationalInstanceSetImplementation implementation = (EmbeddedRelationalInstanceSetImplementation) cm;
                cm._id(implementation._sourceSetImplementationId() + "_" + implementation._property()._name());
            }
        }
        return cm._id();
    }

    public static String getPropertyMappingTargetId(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping, PropertyMappingsImplementation parent, Property<?, ?> property, CompileContext context)
    {
        if (!(propertyMapping instanceof RelationalPropertyMapping || propertyMapping instanceof EmbeddedRelationalPropertyMapping || propertyMapping instanceof InlineEmbeddedPropertyMapping))
        {
            throw new UnsupportedOperationException();
        }

        if (propertyMapping.target == null && propertyMapping instanceof InlineEmbeddedPropertyMapping)
        {
            return ((InlineEmbeddedPropertyMapping) propertyMapping).id == null ? parent._id() + "_" + propertyMapping.property.property : ((InlineEmbeddedPropertyMapping) propertyMapping).id;
        }

        if (propertyMapping.target == null && propertyMapping instanceof OtherwiseEmbeddedRelationalPropertyMapping)
        {
            return ((OtherwiseEmbeddedRelationalPropertyMapping) propertyMapping).id == null ? parent._id() + "_" + propertyMapping.property.property : ((OtherwiseEmbeddedRelationalPropertyMapping) propertyMapping).id;
        }

        if (propertyMapping.target == null && propertyMapping instanceof EmbeddedRelationalPropertyMapping)
        {
            return ((EmbeddedRelationalPropertyMapping) propertyMapping).id == null ? parent._id() + "_" + propertyMapping.property.property : ((EmbeddedRelationalPropertyMapping) propertyMapping).id;
        }

        if (propertyMapping.target == null && property._genericType()._rawType() instanceof Class)
        {
            return HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) property._genericType()._rawType(), "_", context.pureModel.getExecutionSupport());
        }
        return HelperMappingBuilder.getPropertyMappingTargetId(propertyMapping);
    }

    @SuppressWarnings("unchecked")
    public static String getPropertyMappingSourceId(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping, PropertyMappingsImplementation parent, Property<?, ?> property, CompileContext context)
    {
        if (!(propertyMapping instanceof RelationalPropertyMapping || propertyMapping instanceof EmbeddedRelationalPropertyMapping || propertyMapping instanceof InlineEmbeddedPropertyMapping))
        {
            throw new UnsupportedOperationException();
        }

        if (propertyMapping.source != null)
        {
            return propertyMapping.source;
        }

        PropertyOwner owner = property._owner();
        if ((parent._id() == null) && (owner instanceof Association))
        {
            Property<?, ?> prop = ((Class<?>) property._genericType()._rawType())._propertiesFromAssociations().detect(p -> owner.equals(p._owner()));
            return HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) prop._genericType()._rawType(), "_", context.pureModel.getExecutionSupport());
        }
        return parent._id();
    }

    public static AggregationAwarePropertyMapping visitAggregationAwarePropertyMapping(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping propertyMapping, String parentId)
    {
        AggregationAwarePropertyMapping aggregationAwarePropertyMapping = new AggregationAwarePropertyMapping();

        aggregationAwarePropertyMapping.property = propertyMapping.property;
        propertyMapping.source = parentId;
        aggregationAwarePropertyMapping.source = parentId;
        if (propertyMapping instanceof EmbeddedRelationalPropertyMapping && propertyMapping.target == null)
        {
            propertyMapping.target = parentId + "_" + propertyMapping.property.property;
            aggregationAwarePropertyMapping.target = parentId + "_" + propertyMapping.property.property;
            ((EmbeddedRelationalPropertyMapping) propertyMapping).classMapping.propertyMappings.forEach(p -> p.source = propertyMapping.target);
        }
        else
        {
            aggregationAwarePropertyMapping.target = propertyMapping.target;
        }
        aggregationAwarePropertyMapping.sourceInformation = propertyMapping.sourceInformation;

        return aggregationAwarePropertyMapping;
    }

    public static void processRootRelationalClassMapping(RootRelationalInstanceSetImplementation rsi, RootRelationalClassMapping classMapping, CompileContext context)
    {
        if (rsi._mainTableAlias() == null && rsi._superSetImplementationId() != null)
        {
            PropertyMappingsImplementation currentPmi = rsi;
            boolean mainTableAliasFound = false;
            while (!mainTableAliasFound)
            {
                PropertyMappingsImplementation superMapping = Root_meta_pure_mapping_superMapping_PropertyMappingsImplementation_1__PropertyMappingsImplementation_$0_1$_(currentPmi, context.pureModel.getExecutionSupport());
                if (superMapping == null)
                {
                    throw new EngineException("Can't find the main table for class '" + classMapping.id + "'");
                }
                if (superMapping instanceof RootRelationalInstanceSetImplementation)
                {
                    RootRelationalInstanceSetImplementation superRsi = (RootRelationalInstanceSetImplementation) superMapping;
                    TableAlias mainTableAlias = superRsi._mainTableAlias();
                    if (mainTableAlias != null)
                    {
                        rsi._mainTableAlias(mainTableAlias);
                        mainTableAliasFound = true;
                    }
                }
                currentPmi = superMapping;
            }
        }

        org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.FilterMapping filterMapping = classMapping.filter == null ? null : HelperRelationalBuilder.processFilterMapping(classMapping.filter, HelperRelationalBuilder.resolveDatabase(classMapping.filter.filter.db, classMapping.filter.sourceInformation, context), context);
        rsi._filter(filterMapping);

        HelperRelationalBuilder.validatePropertyMappings(classMapping, rsi);
        if (rsi._primaryKey().isEmpty())
        {
            HelperRelationalBuilder.processRelationalPrimaryKey(rsi, context);
        }
    }

    public static void enhanceEmbeddedMappingsWithRelationalOperationElement(Iterable<? extends EmbeddedRelationalInstanceSetImplementation> embeddedRelationalPropertyMappings, RootRelationalInstanceSetImplementation res, CompileContext context)
    {
        embeddedRelationalPropertyMappings.forEach(e -> enhanceEmbeddedMappingsWithRelationalOperationElement(e, res, context));
    }

    private static void enhanceEmbeddedMappingsWithRelationalOperationElement(EmbeddedRelationalInstanceSetImplementation embedded, RootRelationalInstanceSetImplementation res, CompileContext context)
    {
        embedded._propertyMappings().forEach(propertyMapping ->
        {
            if (propertyMapping instanceof EmbeddedRelationalInstanceSetImplementation)
            {
                enhanceEmbeddedMappingsWithRelationalOperationElement((EmbeddedRelationalInstanceSetImplementation) propertyMapping, res, context);
            }
            if (propertyMapping instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping)
            {
                if (((org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping) propertyMapping)._relationalOperationElement() instanceof Literal)
                {
                    org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping relPropMapping = (org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping) propertyMapping;
                    Literal literal = ((Literal) relPropMapping._relationalOperationElement());
                    if (literal._value() instanceof SQLNull)
                    {
                        relPropMapping._relationalOperationElement(createRelationalOperationElement(relPropMapping._property().getName(), res, context));
                    }
                }
            }
        });
    }

    private static PropertyMapping buildSemiStructuredPropertyMapping(Property<?, ?> property, RelationalPropertyMapping propertyMapping, PropertyMappingsImplementation parent, RootRelationalInstanceSetImplementation topParent, MutableMap<String, TableAlias> aliasMap, CompileContext context)
    {
        Root_meta_external_format_shared_binding_Binding binding = HelperExternalFormat.getBinding(propertyMapping.bindingTransformer.binding, propertyMapping.bindingTransformer.sourceInformation, context);
        List<? extends Class<?>> bindingClasses = Lists.mutable.withAll(core_pure_model_modelUnit.Root_meta_pure_model_unit_resolve_ModelUnit_1__ResolvedModelUnit_1_(binding._modelUnit(), context.getExecutionSupport()).classes(context.getExecutionSupport()));

        Type propertyType = property._genericType()._rawType();

        if (!"Class".equals(propertyType._classifierGenericType()._rawType()._name()))
        {
            throw new EngineException("Binding transformer can be used with complex properties only. Property '" + property._name() + "' return type is '" + propertyType._name() + "'", propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        Class<?> propertyReturnType = (Class<?>) propertyType;
        if (!bindingClasses.contains(propertyReturnType))
        {
            throw new EngineException("Class: " + Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1_((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement) propertyType, context.getExecutionSupport()) + " should be included in modelUnit for binding: " + propertyMapping.bindingTransformer.binding, propertyMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        Class<?> classifier = context.pureModel.getClass("meta::external::format::shared::binding::BindingTransformer");
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(classifier)
                ._typeArguments(Lists.fixedSize.of(context.pureModel.getGenericType(propertyReturnType)));
        String setId = "semi_structured_generated_embedded_" + parent._id() + "_" + property._name();
        return new Root_meta_relational_mapping_SemiStructuredEmbeddedRelationalInstanceSetImplementation_Impl("", SourceInformationHelper.toM3SourceInformation(propertyMapping.sourceInformation), context.pureModel.getClass("meta::relational::mapping::SemiStructuredEmbeddedRelationalInstanceSetImplementation"))
                ._id(setId)
                ._root(false)
                ._class(propertyReturnType)
                ._parent(parent._parent())
                ._setMappingOwner(topParent)
                ._sourceSetImplementationId(parent._id())
                ._targetSetImplementationId(setId)
                ._property(property)
                ._owner(parent)
                ._relationalOperationElement(processRelationalOperationElement(propertyMapping.relationalOperation, context, aliasMap, Lists.mutable.empty()))
                ._transformer(
                        new Root_meta_external_format_shared_binding_BindingTransformer_Impl<>("", SourceInformationHelper.toM3SourceInformation(propertyMapping.bindingTransformer.sourceInformation), classifier)
                                ._binding(binding)
                                ._classifierGenericType(genericType)
                                ._class(propertyReturnType)
                );
    }
}
