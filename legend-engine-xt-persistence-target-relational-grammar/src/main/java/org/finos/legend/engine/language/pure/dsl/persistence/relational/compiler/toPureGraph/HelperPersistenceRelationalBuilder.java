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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Temporality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.TemporalityVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeFieldsVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStartAndEnd;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_target_PersistenceTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_BatchIdAndTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_BatchId_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_BitemporalMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_Milestoning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_NoMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_ProcessingDimension;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_ProcessingTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceDerivedDimension;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceDerivedTime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceTimeFields;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceTimeStartAndEnd_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_SourceTimeStart_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_relational_metamodel_UnitemporalMilestoning_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;

public class HelperPersistenceRelationalBuilder
{
    private HelperPersistenceRelationalBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_target_PersistenceTarget buildRelationalPersistenceTarget(RelationalPersistenceTarget persistenceTarget, CompileContext context)
    {
        return new Root_meta_pure_persistence_relational_metamodel_RelationalPersistenceTarget_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::RelationalPersistenceTarget"))
            ._table(buildTable(persistenceTarget.table, persistenceTarget.sourceInformation, context))
            ._milestoning(buildMilestoning(persistenceTarget.temporality, context));
    }

    // todo: ??
    public static Table buildTable(String table, SourceInformation sourceInformation, CompileContext context)
    {
        String tablePath = table.substring(0, table.lastIndexOf("::"));
        String tableName = table.substring(table.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(tablePath)._children().detect(c -> tableName.equals(c._name()));
        if (packageableElement instanceof Table)
        {
            return (Table) packageableElement;
        }

        throw new EngineException(String.format("Table '%s' is not defined", table), sourceInformation, EngineErrorType.COMPILATION);
    }

    // todo: ??
    public static Column buildColumn(String column, SourceInformation sourceInformation, CompileContext context)
    {
        String columnPath = column.substring(0, column.lastIndexOf("::"));
        String columnName = column.substring(column.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(columnPath)._children().detect(c -> columnName.equals(c._name()));
        if (packageableElement instanceof Column)
        {
            return (Column) packageableElement;
        }

        throw new EngineException(String.format("Column '%s' is not defined", column), sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_pure_persistence_relational_metamodel_Milestoning buildMilestoning(Temporality temporality, CompileContext context)
    {
        return temporality.accept(new TemporalityBuilder(context));
    }

    private static class TemporalityBuilder implements TemporalityVisitor<Root_meta_pure_persistence_relational_metamodel_Milestoning>
    {
        private final CompileContext context;

        private TemporalityBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Milestoning visitNontemporal(Nontemporal val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_NoMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::NoMilestoning"));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Milestoning visitUnitemporal(Unitemporal val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_UnitemporalMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::UnitemporalMilestoning"))
                ._processingDimension(val.processingDimension.accept(new ProcessingDimensionBuilder(context)));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_Milestoning visitBitemporal(Bitemporal val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_BitemporalMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::BitemporalMilestoning"))
                ._processingDimension(val.processingDimension.accept(new ProcessingDimensionBuilder(context)))
                ._sourceDerivedDimension(val.sourceDerivedDimension.accept(new SourceDerivedDimensionBuilder(context)));
        }
    }

    private static class ProcessingDimensionBuilder implements ProcessingDimensionVisitor<Root_meta_pure_persistence_relational_metamodel_ProcessingDimension>
    {
        private final CompileContext context;

        private ProcessingDimensionBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_ProcessingDimension visitBatchId(BatchId val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_BatchId_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::BatchId"))
                ._batchIdIn(buildColumn(val.batchIdIn, val.sourceInformation, context))
                ._batchIdOut(buildColumn(val.batchIdOut, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_ProcessingDimension visitDateTime(ProcessingDateTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_ProcessingTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::ProcessingTime"))
                ._timeIn(buildColumn(val.timeIn, val.sourceInformation, context))
                ._timeOut(buildColumn(val.timeOut, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_ProcessingDimension visitBatchIdAndDateTime(BatchIdAndDateTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_BatchIdAndTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::BatchIdAndTime"))
                ._batchIdIn(buildColumn(val.batchIdIn, val.sourceInformation, context))
                ._batchIdOut(buildColumn(val.batchIdOut, val.sourceInformation, context))
                ._timeIn(buildColumn(val.timeIn, val.sourceInformation, context))
                ._timeOut(buildColumn(val.timeOut, val.sourceInformation, context));
        }
    }

    private static class SourceDerivedDimensionBuilder implements SourceDerivedDimensionVisitor<Root_meta_pure_persistence_relational_metamodel_SourceDerivedDimension>
    {
        private final CompileContext context;

        private SourceDerivedDimensionBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_relational_metamodel_SourceDerivedDimension visitSourceDerivedTime(SourceDerivedTime val)
        {
            return new Root_meta_pure_persistence_relational_metamodel_SourceDerivedTime_Impl("", null, context.pureModel.getClass("meta::pure::persistence::relational::metamodel::SourceDerivedTime"))
                ._timeStart(buildColumn(val.timeStart, val.sourceInformation, context))
                ._timeEnd(buildColumn(val.timeEnd, val.sourceInformation, context))
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
