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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Temporality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.TemporalityVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeFieldsVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStartAndEnd;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.Overwrite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.UpdatesHandlingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.AllowDuplicates;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.AppendStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.FailOnDuplicates;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.FilterDuplicates;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_target_PersistenceTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_AllowDuplicates_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_AppendOnly_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_AppendStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_Auditing;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_AuditingDateTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_BatchIdAndTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_BatchId_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_BitemporalMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_FailOnDuplicates_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_FilterDuplicates_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_Milestoning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_NoAuditing_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_NoMilestoning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_NoMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_Overwrite_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_ProcessingDimension;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_ProcessingTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceDerivedDimension;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceDerivedTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceTimeFields;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceTimeStartAndEnd_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceTimeStart_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_UnitemporalMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_UpdatesHandling;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class HelperPersistenceRelationalBuilder
{
    private HelperPersistenceRelationalBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_target_PersistenceTarget buildRelationalPersistenceTarget(RelationalPersistenceTarget persistenceTarget, CompileContext context)
    {
        Database database = buildDatabase(persistenceTarget.database, persistenceTarget.sourceInformation, context);
        Table table = buildTable(persistenceTarget.table, persistenceTarget.sourceInformation, database);
        return new Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::RelationalPersistenceTarget"))
            ._table(table)
            ._database(database)
            ._milestoning(buildMilestoning(persistenceTarget.temporality, context, table));
    }

    public static Database buildDatabase(PackageableElementPointer database, SourceInformation sourceInformation, CompileContext context)
    {
        PackageableElement packageableElement = context.resolvePackageableElement(database);
        if (packageableElement instanceof Database)
        {
            return (Database) packageableElement;
        }

        throw new EngineException(String.format("Database '%s' is not defined", database), sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Column buildColumn(String columnName, SourceInformation sourceInformation, Table table)
    {
        Column column = (Column) table._columns().detect(col -> columnName.equals(col.getName()));
        if (column != null)
        {
            return column;
        }

        throw new EngineException(String.format("Column '%s' is not defined", columnName), sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Table buildTable(String tableName, SourceInformation sourceInformation, Database database)
    {
        String[] nameParts = tableName.split("\\.");
        if (nameParts.length == 2)
        {
            return buildTableFromSpecifiedSchema(nameParts[1], nameParts[0], sourceInformation, database);
        }
        else
        {
            return buildTableFromAllSchemas(tableName, sourceInformation, database);
        }
    }

    public static Table buildTableFromSpecifiedSchema(String tableName, String schemaName, SourceInformation sourceInformation, Database database)
    {
        Schema schema = database._schemas().detect(x -> x._name().equals(schemaName));
        if (schema == null)
        {
            throw new EngineException(String.format("Schema '%s' is not defined", schemaName), sourceInformation, EngineErrorType.COMPILATION);
        }

        Table table = schema._tables().detect(t -> t._name().equals(tableName));
        if (table == null)
        {
            throw new EngineException(String.format("Table '%s' is not defined", tableName), sourceInformation, EngineErrorType.COMPILATION);
        }

        return table;
    }

    public static Table buildTableFromAllSchemas(String tableName, SourceInformation sourceInformation, Database database)
    {
        SetIterable<Table> tables = getAllTables(database);
        Table table = tables.detect(t -> tableName.equals(t._name()));
        if (table != null)
        {
            return table;
        }

        throw new EngineException(String.format("Table '%s' is not defined", tableName), sourceInformation, EngineErrorType.COMPILATION);
    }

    private static SetIterable<Table> getAllTables(Database db)
    {
        MutableSet<Table> tables = Sets.mutable.empty();
        for (Database _db : getAllIncludedDBs(db))
        {
            _db._schemas().asLazy().forEach(x -> x._tables().forEach(tables::add));
        }
        return tables;
    }

    private static SetIterable<Database> getAllIncludedDBs(Database database)
    {
        RichIterable<? extends Store> includes = database._includes();
        if (includes.isEmpty())
        {
            return Sets.immutable.with(database);
        }
        MutableSet<Database> results = UnifiedSet.<Database>newSet(includes.size() + 1).with(database);
        collectIncludedDBs(results, includes);
        return results;
    }

    private static void collectIncludedDBs(MutableSet<Database> results, RichIterable<? extends CoreInstance> databases)
    {
        databases.forEach(db ->
        {
            Database database = (Database) db;
            if (results.add(database))
            {
                collectIncludedDBs(results, database._includes());
            }
        });
    }

    public static Root_meta_pure_persistence_relational_metamodel_Milestoning buildMilestoning(Temporality temporality, CompileContext context, Table table)
    {
        return temporality.accept(new TemporalityBuilder(context, table));
    }

    private static class TemporalityBuilder implements TemporalityVisitor<Root_meta_pure_persistence_relational_metamodel_Milestoning>
    {
        private final CompileContext context;
        private final Table table;

        private TemporalityBuilder(CompileContext context, Table table)
        {
            this.context = context;
            this.table = table;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Milestoning visitNontemporal(Nontemporal val)
        {
            Root_meta_pure_persistence_relational_metamodel_NoMilestoning noMilestoning = new Root_meta_pure_persistence_relational_metamodel_NoMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::NoMilestoning"))
                ._updatesHandling(val.updatesHandling.accept(new UpdatesHandlingBuilder(context)));

            if (val.auditing != null)
            {
                noMilestoning._auditing(val.auditing.accept(new AuditingBuilder(context, table)));
            }

            return noMilestoning;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Milestoning visitUnitemporal(Unitemporal val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_UnitemporalMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::UnitemporalMilestoning"))
                ._processingDimension(val.processingDimension.accept(new ProcessingDimensionBuilder(context, table)));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Milestoning visitBitemporal(Bitemporal val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_BitemporalMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::BitemporalMilestoning"))
                ._processingDimension(val.processingDimension.accept(new ProcessingDimensionBuilder(context, table)))
                ._sourceDerivedDimension(val.sourceDerivedDimension.accept(new SourceDerivedDimensionBuilder(context, table)));
        }
    }

    private static class UpdatesHandlingBuilder implements UpdatesHandlingVisitor<Root_meta_pure_persistence_relational_metamodel_UpdatesHandling>
    {
        private final CompileContext context;

        private UpdatesHandlingBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_UpdatesHandling visitAppendOnly(AppendOnly val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_AppendOnly_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::AppendOnly"))
                ._appendStrategy(val.appendStrategy.accept(new AppendStrategyBuilder(context)));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_UpdatesHandling visitOverwrite(Overwrite val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_Overwrite_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::Overwrite"));
        }
    }

    private static class AppendStrategyBuilder implements AppendStrategyVisitor<Root_meta_pure_persistence_relational_metamodel_AppendStrategy>
    {
        private final CompileContext context;

        private AppendStrategyBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_AppendStrategy visitAllowDuplicates(AllowDuplicates val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_AllowDuplicates_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::AllowDuplicates"));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_AppendStrategy visitFailOnDuplicates(FailOnDuplicates val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_FailOnDuplicates_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::FailOnDuplicates"));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_AppendStrategy visitFilterDuplicates(FilterDuplicates val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_FilterDuplicates_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::FilterDuplicates"));
        }
    }

    private static class AuditingBuilder implements AuditingVisitor<Root_meta_pure_persistence_relational_metamodel_Auditing>
    {
        private final CompileContext context;
        private final Table table;

        private AuditingBuilder(CompileContext context, Table table)
        {
            this.context = context;
            this.table = table;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Auditing visitAuditingDateTime(AuditingDateTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_AuditingDateTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::AuditingDateTime"))
                ._auditingDateTimeName(buildColumn(val.auditingDateTimeName, val.sourceInformation, table));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Auditing visitNoAuditing(NoAuditing val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_NoAuditing_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::NoAuditing"));
        }
    }

    private static class ProcessingDimensionBuilder implements ProcessingDimensionVisitor<Root_meta_pure_persistence_relational_metamodel_ProcessingDimension>
    {
        private final CompileContext context;
        private final Table table;

        private ProcessingDimensionBuilder(CompileContext context, Table table)
        {
            this.context = context;
            this.table = table;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_ProcessingDimension visitBatchId(BatchId val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_BatchId_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::BatchId"))
                ._batchIdIn(buildColumn(val.batchIdIn, val.sourceInformation, table))
                ._batchIdOut(buildColumn(val.batchIdOut, val.sourceInformation, table));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_ProcessingDimension visitDateTime(ProcessingDateTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_ProcessingTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::ProcessingTime"))
                ._timeIn(buildColumn(val.timeIn, val.sourceInformation, table))
                ._timeOut(buildColumn(val.timeOut, val.sourceInformation, table));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_ProcessingDimension visitBatchIdAndDateTime(BatchIdAndDateTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_BatchIdAndTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::BatchIdAndTime"))
                ._batchIdIn(buildColumn(val.batchIdIn, val.sourceInformation, table))
                ._batchIdOut(buildColumn(val.batchIdOut, val.sourceInformation, table))
                ._timeIn(buildColumn(val.timeIn, val.sourceInformation, table))
                ._timeOut(buildColumn(val.timeOut, val.sourceInformation, table));
        }
    }

    private static class SourceDerivedDimensionBuilder implements SourceDerivedDimensionVisitor<Root_meta_pure_persistence_relational_metamodel_SourceDerivedDimension>
    {
        private final CompileContext context;
        private final Table table;

        private SourceDerivedDimensionBuilder(CompileContext context, Table table)
        {
            this.context = context;
            this.table = table;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_SourceDerivedDimension visitSourceDerivedTime(SourceDerivedTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_SourceDerivedTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::SourceDerivedTime"))
                ._timeStart(buildColumn(val.timeStart, val.sourceInformation, table))
                ._timeEnd(buildColumn(val.timeEnd, val.sourceInformation, table))
                ._sourceTimeFields(val.sourceTimeFields.accept(new SourceTimeFieldsBuilder(context)));
        }
    }

    private static class SourceTimeFieldsBuilder implements SourceTimeFieldsVisitor<Root_meta_pure_persistence_relational_metamodel_SourceTimeFields>
    {
        private final CompileContext context;

        private SourceTimeFieldsBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_SourceTimeFields visit(SourceTimeStart val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_SourceTimeStart_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::SourceTimeStart"))
                ._startField(val.startField);
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_SourceTimeFields visit(SourceTimeStartAndEnd val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_SourceTimeStartAndEnd_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::SourceTimeStartAndEnd"))
                ._startField(val.startField)
                ._endField(val.endField);
        }
    }
}
