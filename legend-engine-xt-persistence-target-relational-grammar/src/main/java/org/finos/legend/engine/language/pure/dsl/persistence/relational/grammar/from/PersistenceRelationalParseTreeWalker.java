// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Temporality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedDimension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeFields;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStartAndEnd;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.sink.PersistenceTarget;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.grammar.from.antlr4.PersistenceRelationalParserGrammar;

public class PersistenceRelationalParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public PersistenceRelationalParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    /**********
     * persistence target
     **********/

    public PersistenceTarget visitPersistenceTarget(PersistenceRelationalParserGrammar.DefinitionContext ctx)
    {
        RelationalPersistenceTarget persistenceTarget = new RelationalPersistenceTarget();
        persistenceTarget.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // table
        PersistenceRelationalParserGrammar.TableContext tableContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.table(), "table", persistenceTarget.sourceInformation);
        persistenceTarget.table = PureGrammarParserUtility.fromIdentifier(tableContext.identifier());

        // temporality
        PersistenceRelationalParserGrammar.TemporalityContext temporalityContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.temporality(), "temporality", persistenceTarget.sourceInformation);
        persistenceTarget.temporality = temporalityContext == null ? new Nontemporal() : visitTemporality(temporalityContext);

        return persistenceTarget;
    }

    private Temporality visitTemporality(PersistenceRelationalParserGrammar.TemporalityContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        if (ctx.nontemporal() != null)
        {
            return visitNontemporal(ctx.nontemporal());
        }
        else if (ctx.unitemporal() != null)
        {
            return visitUnitemporal(ctx.unitemporal());
        }
        else if (ctx.bitemporal() != null)
        {
            return visitBitemporal(ctx.bitemporal());
        }
        throw new EngineException("Unrecognized temporality", sourceInformation, EngineErrorType.PARSER);
    }

    private Temporality visitNontemporal(PersistenceRelationalParserGrammar.NontemporalContext ctx)
    {
        Nontemporal temporality = new Nontemporal();
        temporality.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return temporality;
    }

    private Temporality visitUnitemporal(PersistenceRelationalParserGrammar.UnitemporalContext ctx)
    {
        Unitemporal temporality = new Unitemporal();
        temporality.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // processing dimension
        PersistenceRelationalParserGrammar.ProcessingDimensionContext processingDimensionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.processingDimension(), "processingDimension", temporality.sourceInformation);
        temporality.processingDimension = visitProcessingDimension(processingDimensionContext);

        return temporality;
    }

    private Temporality visitBitemporal(PersistenceRelationalParserGrammar.BitemporalContext ctx)
    {
        Bitemporal temporality = new Bitemporal();
        temporality.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // processing dimension
        PersistenceRelationalParserGrammar.ProcessingDimensionContext processingDimensionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.processingDimension(), "processingDimension", temporality.sourceInformation);
        temporality.processingDimension = visitProcessingDimension(processingDimensionContext);

        // source derived dimension
        PersistenceRelationalParserGrammar.SourceDerivedDimensionContext sourceDerivedDimensionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourceDerivedDimension(), "sourceDerivedDimension", temporality.sourceInformation);
        temporality.sourceDerivedDimension = visitSourceDerivedDimension(sourceDerivedDimensionContext);

        return temporality;
    }

    private ProcessingDimension visitProcessingDimension(PersistenceRelationalParserGrammar.ProcessingDimensionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.processingBatchId() != null)
        {
            return visitProcessingBatchId(ctx.processingBatchId());
        }
        else if (ctx.processingDateTime() != null)
        {
            return visitProcessingDateTime(ctx.processingDateTime());
        }
        else if (ctx.processingBatchIdAndDateTime() != null)
        {
            return visitProcessingBatchIdAndDateTime(ctx.processingBatchIdAndDateTime());
        }
        throw new EngineException("Unrecognized processing dimension", sourceInformation, EngineErrorType.PARSER);
    }

    private ProcessingDimension visitProcessingBatchId(PersistenceRelationalParserGrammar.ProcessingBatchIdContext ctx)
    {
        BatchId processingDimension = new BatchId();
        processingDimension.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batch id in
        PersistenceRelationalParserGrammar.BatchIdInContext batchIdInContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdIn(), "batchIdIn", processingDimension.sourceInformation);
        processingDimension.batchIdIn = PureGrammarParserUtility.fromIdentifier(batchIdInContext.identifier());

        // batch id out
        PersistenceRelationalParserGrammar.BatchIdOutContext batchIdOutContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOut(), "batchIdOut", processingDimension.sourceInformation);
        processingDimension.batchIdOut = PureGrammarParserUtility.fromIdentifier(batchIdOutContext.identifier());

        return processingDimension;
    }

    private ProcessingDimension visitProcessingDateTime(PersistenceRelationalParserGrammar.ProcessingDateTimeContext ctx)
    {
        ProcessingDateTime processingDimension = new ProcessingDateTime();
        processingDimension.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // date time in
        PersistenceRelationalParserGrammar.DateTimeInContext dateTimeInContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeIn(), "dateTimeIn", processingDimension.sourceInformation);
        processingDimension.timeIn = PureGrammarParserUtility.fromIdentifier(dateTimeInContext.identifier());

        // date time out
        PersistenceRelationalParserGrammar.DateTimeOutContext dateTimeOutContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOut(), "dateTimeOut", processingDimension.sourceInformation);
        processingDimension.timeOut = PureGrammarParserUtility.fromIdentifier(dateTimeOutContext.identifier());

        return processingDimension;
    }

    private ProcessingDimension visitProcessingBatchIdAndDateTime(PersistenceRelationalParserGrammar.ProcessingBatchIdAndDateTimeContext ctx)
    {
        BatchIdAndDateTime processingDimension = new BatchIdAndDateTime();
        processingDimension.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batch id in
        PersistenceRelationalParserGrammar.BatchIdInContext batchIdInContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdIn(), "batchIdIn", processingDimension.sourceInformation);
        processingDimension.batchIdIn = PureGrammarParserUtility.fromIdentifier(batchIdInContext.identifier());

        // batch id out
        PersistenceRelationalParserGrammar.BatchIdOutContext batchIdOutContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOut(), "batchIdOut", processingDimension.sourceInformation);
        processingDimension.batchIdOut = PureGrammarParserUtility.fromIdentifier(batchIdOutContext.identifier());

        // date time in
        PersistenceRelationalParserGrammar.DateTimeInContext dateTimeInContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeIn(), "dateTimeIn", processingDimension.sourceInformation);
        processingDimension.timeIn = PureGrammarParserUtility.fromIdentifier(dateTimeInContext.identifier());

        // date time out
        PersistenceRelationalParserGrammar.DateTimeOutContext dateTimeOutContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOut(), "dateTimeOut", processingDimension.sourceInformation);
        processingDimension.timeOut = PureGrammarParserUtility.fromIdentifier(dateTimeOutContext.identifier());

        return processingDimension;
    }

    private SourceDerivedDimension visitSourceDerivedDimension(PersistenceRelationalParserGrammar.SourceDerivedDimensionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.sourceDerivedDateTime() != null)
        {
            return visitSourceDerivedDateTime(ctx.sourceDerivedDateTime());
        }
        throw new EngineException("Unrecognized source derived dimension", sourceInformation, EngineErrorType.PARSER);
    }

    private SourceDerivedDimension visitSourceDerivedDateTime(PersistenceRelationalParserGrammar.SourceDerivedDateTimeContext ctx)
    {
        SourceDerivedTime sourceDerivedDimension = new SourceDerivedTime();
        sourceDerivedDimension.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // date time start
        PersistenceRelationalParserGrammar.DateTimeStartContext dateTimeStartContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeStart(), "dateTimeStart", sourceDerivedDimension.sourceInformation);
        sourceDerivedDimension.timeStart = PureGrammarParserUtility.fromIdentifier(dateTimeStartContext.identifier());

        // date time end
        PersistenceRelationalParserGrammar.DateTimeEndContext dateTimeEndContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeEnd(), "dateTimeEnd", sourceDerivedDimension.sourceInformation);
        sourceDerivedDimension.timeEnd = PureGrammarParserUtility.fromIdentifier(dateTimeEndContext.identifier());

        // source fields
        PersistenceRelationalParserGrammar.SourceFieldsContext sourceFieldsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourceFields(), "sourceFields", sourceDerivedDimension.sourceInformation);
        sourceDerivedDimension.sourceTimeFields = visitSourceFields(sourceFieldsContext);

        return sourceDerivedDimension;
    }

    private SourceTimeFields visitSourceFields(PersistenceRelationalParserGrammar.SourceFieldsContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.sourceFieldsStart() != null)
        {
            return visitSourceFieldsStart(ctx.sourceFieldsStart());
        }
        else if (ctx.sourceFieldsStartAndEnd() != null)
        {
            return visitSourceFieldsStartAndEnd(ctx.sourceFieldsStartAndEnd());
        }
        throw new EngineException("Unrecognized source fields", sourceInformation, EngineErrorType.PARSER);
    }

    private SourceTimeFields visitSourceFieldsStart(PersistenceRelationalParserGrammar.SourceFieldsStartContext ctx)
    {
        SourceTimeStart sourceTimeFields = new SourceTimeStart();
        sourceTimeFields.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // start field
        PersistenceRelationalParserGrammar.SourceFieldStartContext startFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourceFieldStart(), "startField", sourceTimeFields.sourceInformation);
        sourceTimeFields.startField = PureGrammarParserUtility.fromIdentifier(startFieldContext.identifier());

        return sourceTimeFields;
    }

    private SourceTimeFields visitSourceFieldsStartAndEnd(PersistenceRelationalParserGrammar.SourceFieldsStartAndEndContext ctx)
    {
        SourceTimeStartAndEnd sourceTimeFields = new SourceTimeStartAndEnd();
        sourceTimeFields.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // start field
        PersistenceRelationalParserGrammar.SourceFieldStartContext startFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourceFieldStart(), "startField", sourceTimeFields.sourceInformation);
        sourceTimeFields.startField = PureGrammarParserUtility.fromIdentifier(startFieldContext.identifier());

        // end field
        PersistenceRelationalParserGrammar.SourceFieldEndContext endFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourceFieldEnd(), "endField", sourceTimeFields.sourceInformation);
        sourceTimeFields.endField = PureGrammarParserUtility.fromIdentifier(endFieldContext.identifier());

        return sourceTimeFields;
    }
}
