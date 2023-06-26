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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.to;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Temporality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.TemporalityVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedDimension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeFields;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeFieldsVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStartAndEnd;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.Overwrite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.UpdatesHandling;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.UpdatesHandlingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.AllowDuplicates;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.AppendStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.AppendStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.FailOnDuplicates;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.FilterDuplicates;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperPersistenceRelationalComposer
{
    private HelperPersistenceRelationalComposer()
    {
    }

    public static String renderRelationalPersistenceTarget(RelationalPersistenceTarget persistenceTarget, int indentLevel, PureGrammarComposerContext context)
    {
        return getTabString(indentLevel) + "Relational\n" +
            getTabString(indentLevel) + "#{\n" +
            renderTable(persistenceTarget.table, indentLevel + 1) +
            renderDatabasePointer(persistenceTarget.database, indentLevel + 1) +
            renderTemporality(persistenceTarget.temporality, indentLevel + 1) +
            getTabString(indentLevel) + "}#";
    }

    public static String renderTable(String table, int indentLevel)
    {
        return getTabString(indentLevel) + "table: " + table + ";\n";
    }

    public static String renderDatabasePointer(String databasePath, int indentLevel)
    {
        return getTabString(indentLevel) + "database: " + databasePath + ";\n";
    }

    public static String renderTemporality(Temporality temporality, int indentLevel)
    {
        return getTabString(indentLevel) + "temporality: " + temporality.accept(new TemporalityComposer(indentLevel));
    }

    private static class TemporalityComposer implements TemporalityVisitor<String>
    {
        private final int indentLevel;

        private TemporalityComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitNontemporal(Nontemporal val)
        {
            return "None\n" +
                getTabString(indentLevel) + "{\n" +
                renderAuditing(val.auditing, indentLevel + 1) +
                renderUpdatesHandling(val.updatesHandling, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitUnitemporal(Unitemporal val)
        {
            return "Unitemporal\n" +
                getTabString(indentLevel) + "{\n" +
                renderProcessingDimension(val.processingDimension, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitBitemporal(Bitemporal val)
        {
            return "Bitemporal\n" +
                getTabString(indentLevel) + "{\n" +
                renderProcessingDimension(val.processingDimension, indentLevel + 1) +
                renderSourceDerivedDimension(val.sourceDerivedDimension, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
        }

        public static String renderAuditing(Auditing auditing, int indentLevel)
        {
            return auditing == null ? "" : getTabString(indentLevel) + "auditing: " + auditing.accept(new AuditingComposer(indentLevel));
        }

        public static String renderUpdatesHandling(UpdatesHandling updatesHandling, int indentLevel)
        {
            return getTabString(indentLevel) + "updatesHandling: " + updatesHandling.accept(new UpdatesHandlingComposer(indentLevel));
        }

        public static String renderProcessingDimension(ProcessingDimension processingDimension, int indentLevel)
        {
            return getTabString(indentLevel) + "processingDimension: " + processingDimension.accept(new ProcessingDimensionComposer(indentLevel));
        }

        public static String renderSourceDerivedDimension(SourceDerivedDimension sourceDerivedDimension, int indentLevel)
        {
            return getTabString(indentLevel) + "sourceDerivedDimension: " + sourceDerivedDimension.accept(new SourceDerivedDimensionComposer(indentLevel));
        }
    }

    private static class AuditingComposer implements AuditingVisitor<String>
    {
        private final int indentLevel;

        private AuditingComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitAuditingDateTime(AuditingDateTime val)
        {
            return "DateTime\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "dateTimeName: " + val.auditingDateTimeName + ";\n" +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitNoAuditing(NoAuditing val)
        {
            return "None;\n";
        }
    }

    private static class UpdatesHandlingComposer implements UpdatesHandlingVisitor<String>
    {
        private final int indentLevel;

        private UpdatesHandlingComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitAppendOnly(AppendOnly val)
        {
            return "AppendOnly\n" +
                getTabString(indentLevel) + "{\n" +
                renderAppendStrategy(val.appendStrategy, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitOverwrite(Overwrite val)
        {
            return "Overwrite;\n";
        }

        public static String renderAppendStrategy(AppendStrategy appendStrategy, int indentLevel)
        {
            return getTabString(indentLevel) + "appendStrategy: " + appendStrategy.accept(new AppendStrategyComposer(indentLevel));
        }
    }

    private static class AppendStrategyComposer implements AppendStrategyVisitor<String>
    {
        private final int indentLevel;

        private AppendStrategyComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitAllowDuplicates(AllowDuplicates val)
        {
            return "AllowDuplicates;\n";
        }

        @Override
        public String visitFailOnDuplicates(FailOnDuplicates val)
        {
            return "FailOnDuplicates;\n";
        }

        @Override
        public String visitFilterDuplicates(FilterDuplicates val)
        {
            return "FilterDuplicates;\n";
        }
    }

    private static class ProcessingDimensionComposer implements ProcessingDimensionVisitor<String>
    {
        private final int indentLevel;

        private ProcessingDimensionComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitBatchId(BatchId val)
        {
            return "BatchId\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "batchIdIn: " + val.batchIdIn + ";\n" +
                getTabString(indentLevel + 1) + "batchIdOut: " + val.batchIdOut + ";\n" +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitDateTime(ProcessingDateTime val)
        {
            return "DateTime\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "dateTimeIn: " + val.timeIn + ";\n" +
                getTabString(indentLevel + 1) + "dateTimeOut: " + val.timeOut + ";\n" +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visitBatchIdAndDateTime(BatchIdAndDateTime val)
        {
            return "BatchIdAndDateTime\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "batchIdIn: " + val.batchIdIn + ";\n" +
                getTabString(indentLevel + 1) + "batchIdOut: " + val.batchIdOut + ";\n" +
                getTabString(indentLevel + 1) + "dateTimeIn: " + val.timeIn + ";\n" +
                getTabString(indentLevel + 1) + "dateTimeOut: " + val.timeOut + ";\n" +
                getTabString(indentLevel) + "}\n";
        }
    }

    private static class SourceDerivedDimensionComposer implements SourceDerivedDimensionVisitor<String>
    {
        private final int indentLevel;

        private SourceDerivedDimensionComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visitSourceDerivedTime(SourceDerivedTime val)
        {
            return "DateTime\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "dateTimeStart: " + val.timeStart + ";\n" +
                getTabString(indentLevel + 1) + "dateTimeEnd: " + val.timeEnd + ";\n" +
                renderSourceTimeFields(val.sourceTimeFields, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
        }

        public static String renderSourceTimeFields(SourceTimeFields sourceTimeFields, int indentLevel)
        {
            return getTabString(indentLevel) + "sourceFields: " + sourceTimeFields.accept(new SourceTimeFieldsComposer(indentLevel));
        }
    }

    private static class SourceTimeFieldsComposer implements SourceTimeFieldsVisitor<String>
    {
        private int indentLevel;

        private SourceTimeFieldsComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(SourceTimeStart val)
        {
            return "Start\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "startField: " + val.startField + ";\n" +
                getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(SourceTimeStartAndEnd val)
        {
            return "StartAndEnd\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "startField: " + val.startField + ";\n" +
                getTabString(indentLevel + 1) + "endField: " + val.endField + ";\n" +
                getTabString(indentLevel) + "}\n";
        }
    }
}
