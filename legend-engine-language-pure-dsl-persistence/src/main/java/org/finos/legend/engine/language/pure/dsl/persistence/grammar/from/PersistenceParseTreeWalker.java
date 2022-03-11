package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.OpaqueAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.OpaqueMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.OpaqueValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.OpaqueTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PersistenceParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    public PersistenceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    /**********
     * persistence
     **********/

    public void visit(PersistenceParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.persistence().stream().map(this::visitPersistence).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private Persistence visitPersistence(PersistenceParserGrammar.PersistenceContext ctx)
    {
        Persistence persistence = new Persistence();
        persistence.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        persistence._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        persistence.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // documentation
        PersistenceParserGrammar.DocumentationContext documentationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.documentation(), "doc", persistence.sourceInformation);
        persistence.documentation = PureGrammarParserUtility.fromGrammarString(documentationContext.STRING().getText(), true);

        // trigger
        PersistenceParserGrammar.TriggerContext triggerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.trigger(), "trigger", persistence.sourceInformation);
        persistence.trigger = visitTrigger(triggerContext);

        // reader
        PersistenceParserGrammar.ReaderContext readerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.reader(), "reader", persistence.sourceInformation);
        persistence.reader = visitReader(readerContext);

        // persister
        PersistenceParserGrammar.PersisterContext persisterContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persister(), "persister", persistence.sourceInformation);
        persistence.persister = visitPersister(persisterContext);

        return persistence;
    }

    /**********
     * trigger
     **********/

    private Trigger visitTrigger(PersistenceParserGrammar.TriggerContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.TRIGGER_OPAQUE() != null)
        {
            OpaqueTrigger opaqueTrigger = new OpaqueTrigger();
            opaqueTrigger.sourceInformation = sourceInformation;

            return opaqueTrigger;
        }
        throw new EngineException("Unrecognized trigger", sourceInformation, EngineErrorType.PARSER);
    }

    /**********
     * reader
     **********/

    private Reader visitReader(PersistenceParserGrammar.ReaderContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.serviceReader() != null)
        {
            return visitServiceReader(ctx.serviceReader());
        }
        throw new EngineException("Unrecognized reader", sourceInformation, EngineErrorType.PARSER);
    }

    private Reader visitServiceReader(PersistenceParserGrammar.ServiceReaderContext ctx)
    {
        ServiceReader reader = new ServiceReader();
        reader.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // service
        PersistenceParserGrammar.ServiceContext serviceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.service(), "service", reader.sourceInformation);
        reader.service = PureGrammarParserUtility.fromQualifiedName(serviceContext.qualifiedName().packagePath() == null ? Collections.emptyList() : serviceContext.qualifiedName().packagePath().identifier(), serviceContext.qualifiedName().identifier());

        return reader;
    }

    /**********
     * persister
     **********/

    private Persister visitPersister(PersistenceParserGrammar.PersisterContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.streamingPersister() != null)
        {
            return visitStreamingPersister(ctx.streamingPersister());
        }
        else if (ctx.batchPersister() != null)
        {
            return visitBatchPersister(ctx.batchPersister());
        }
        throw new EngineException("Unrecognized persister", sourceInformation, EngineErrorType.PARSER);
    }

    private StreamingPersister visitStreamingPersister(PersistenceParserGrammar.StreamingPersisterContext ctx)
    {
        StreamingPersister streaming = new StreamingPersister();
        streaming.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // target shape
        PersistenceParserGrammar.TargetShapeContext targetShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetShape(), "target", streaming.sourceInformation);
        streaming.targetShape = visitTargetShape(targetShapeContext);

        return streaming;
    }

    private BatchPersister visitBatchPersister(PersistenceParserGrammar.BatchPersisterContext ctx)
    {
        BatchPersister batch = new BatchPersister();
        batch.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // target shape
        PersistenceParserGrammar.TargetShapeContext targetShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetShape(), "target", batch.sourceInformation);
        batch.targetShape = visitTargetShape(targetShapeContext);

        return batch;
    }

    /**********
     * target shape
     **********/

    private TargetShape visitTargetShape(PersistenceParserGrammar.TargetShapeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.flatTargetShape() != null)
        {
            return visitFlatTarget(ctx.flatTargetShape());
        }
        else if (ctx.multiTargetShape() != null)
        {
            return visitMultiFlatTarget(ctx.multiTargetShape());
        }
        else if (ctx.opaqueTargetShape() != null)
        {
            return visitOpaqueTarget(ctx.opaqueTargetShape());
        }
        throw new EngineException("Unrecognized target", sourceInformation, EngineErrorType.PARSER);
    }

    private MultiFlatTarget visitMultiFlatTarget(PersistenceParserGrammar.MultiTargetShapeContext ctx)
    {
        MultiFlatTarget targetShape = new MultiFlatTarget();
        targetShape.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetShape.sourceInformation);
        targetShape.modelClass = visitModelClass(targetModelClassContext);

        // transaction scope
        PersistenceParserGrammar.TargetTransactionScopeContext targetTransactionScopeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetTransactionScope(), "transactionScope", targetShape.sourceInformation);
        targetShape.transactionScope = visitTransactionScope(targetTransactionScopeContext);

        // parts
        PersistenceParserGrammar.TargetChildrenContext targetChildrenContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetChildren(), "parts", targetShape.sourceInformation);
        targetShape.parts = ListIterate.collect(targetChildrenContext.targetChild(), this::visitPropertyAndFlatTarget);

        return targetShape;
    }

    private FlatTarget visitFlatTarget(PersistenceParserGrammar.FlatTargetShapeContext ctx)
    {
        FlatTarget targetShape = createBaseFlatTarget(walkerSourceInformation.getSourceInformation(ctx), ctx.targetName(), ctx.partitionProperties(), ctx.deduplicationStrategy(), ctx.ingestMode());

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetShape.sourceInformation);
        targetShape.modelClass = visitModelClass(targetModelClassContext);

        return targetShape;
    }

    private FlatTarget createBaseFlatTarget(SourceInformation sourceInformation, List<PersistenceParserGrammar.TargetNameContext> targetNameContexts, List<PersistenceParserGrammar.PartitionPropertiesContext> partitionPropertiesContexts, List<PersistenceParserGrammar.DeduplicationStrategyContext> deduplicationStrategyContexts, List<PersistenceParserGrammar.IngestModeContext> ingestModeContexts)
    {
        FlatTarget targetShape = new FlatTarget();
        targetShape.sourceInformation = sourceInformation;

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(targetNameContexts, "targetName", targetShape.sourceInformation);
        targetShape.targetName = visitTargetName(targetNameContext);

        // partition properties (optional)
        PersistenceParserGrammar.PartitionPropertiesContext partitionPropertiesContext = PureGrammarParserUtility.validateAndExtractOptionalField(partitionPropertiesContexts, "partitionProperties", targetShape.sourceInformation);
        targetShape.partitionProperties = partitionPropertiesContext != null ? visitPartitionProperties(partitionPropertiesContext) : Collections.emptyList();

        // deduplication strategy (optional)
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractOptionalField(deduplicationStrategyContexts, "deduplicationStrategy", targetShape.sourceInformation);
        targetShape.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        // ingest mode
        PersistenceParserGrammar.IngestModeContext ingestModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ingestModeContexts, "ingestMode", targetShape.sourceInformation);
        targetShape.ingestMode = visitIngestMode(ingestModeContext);

        return targetShape;
    }

    private OpaqueTarget visitOpaqueTarget(PersistenceParserGrammar.OpaqueTargetShapeContext ctx)
    {
        OpaqueTarget targetShape = new OpaqueTarget();
        targetShape.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetName(), "targetName", targetShape.sourceInformation);
        targetShape.targetName = visitTargetName(targetNameContext);

        return targetShape;
    }

    private PropertyAndFlatTarget visitPropertyAndFlatTarget(PersistenceParserGrammar.TargetChildContext ctx)
    {
        PropertyAndFlatTarget propertyAndFlatTarget = new PropertyAndFlatTarget();
        propertyAndFlatTarget.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // property
        PersistenceParserGrammar.TargetChildPropertyContext targetChildPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetChildProperty(), "property", propertyAndFlatTarget.sourceInformation);
        propertyAndFlatTarget.property = PureGrammarParserUtility.fromIdentifier(targetChildPropertyContext.identifier());

        // flat target (note: not expecting a model class in this context; compiler will populate based on target type of property above)
        PersistenceParserGrammar.TargetChildTargetShapeContext targetChildTargetShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetChildTargetShape(), "flatTarget", propertyAndFlatTarget.sourceInformation);
        propertyAndFlatTarget.flatTarget = visitPartFlatTarget(targetChildTargetShapeContext);

        return propertyAndFlatTarget;
    }

    private FlatTarget visitPartFlatTarget(PersistenceParserGrammar.TargetChildTargetShapeContext ctx)
    {
        return createBaseFlatTarget(walkerSourceInformation.getSourceInformation(ctx), ctx.targetName(), ctx.partitionProperties(), ctx.deduplicationStrategy(), ctx.ingestMode());
    }

    private String visitTargetName(PersistenceParserGrammar.TargetNameContext ctx)
    {
        return ctx != null ? PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true) : null;
    }

    private String visitModelClass(PersistenceParserGrammar.TargetModelClassContext ctx)
    {
        PersistenceParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        return PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
    }

    private List<String> visitPartitionProperties(PersistenceParserGrammar.PartitionPropertiesContext ctx)
    {
        List<PersistenceParserGrammar.IdentifierContext> identifierContexts = ctx.identifier();
        return Lists.immutable.ofAll(identifierContexts).collect(PureGrammarParserUtility::fromIdentifier).castToList();
    }

    private TransactionScope visitTransactionScope(PersistenceParserGrammar.TargetTransactionScopeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.TXN_SCOPE_SINGLE() != null)
        {
            return TransactionScope.SINGLE_TARGET;
        }
        else if (ctx.TXN_SCOPE_ALL() != null)
        {
            return TransactionScope.ALL_TARGETS;
        }
        throw new EngineException("Unrecognized transaction scope", sourceInformation, EngineErrorType.PARSER);
    }

    /**********
     * deduplication strategy
     **********/

    private DeduplicationStrategy visitDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyContext ctx)
    {
        // default to none
        if (ctx == null)
        {
            return new NoDeduplicationStrategy();
        }

        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        if (ctx.noDeduplicationStrategy() != null)
        {
            return new NoDeduplicationStrategy();
        }
        else if (ctx.anyVersionDeduplicationStrategy() != null)
        {
            return new AnyVersionDeduplicationStrategy();
        }
        else if (ctx.maxVersionDeduplicationStrategy() != null)
        {
            return visitMaxVersionDeduplicationStrategy(ctx.maxVersionDeduplicationStrategy());
        }
        else if (ctx.opaqueDeduplicationStrategy() != null)
        {
            return new OpaqueDeduplicationStrategy();
        }
        throw new EngineException("Unrecognized deduplication strategy", sourceInformation, EngineErrorType.PARSER);
    }

    private MaxVersionDeduplicationStrategy visitMaxVersionDeduplicationStrategy(PersistenceParserGrammar.MaxVersionDeduplicationStrategyContext ctx)
    {
        MaxVersionDeduplicationStrategy deduplicationStrategy = new MaxVersionDeduplicationStrategy();
        deduplicationStrategy.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // version property
        PersistenceParserGrammar.DeduplicationVersionPropertyContext versionPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationVersionProperty(), "versionProperty", deduplicationStrategy.sourceInformation);
        deduplicationStrategy.versionProperty = PureGrammarParserUtility.fromIdentifier(versionPropertyContext.identifier());

        return deduplicationStrategy;
    }

    /**********
     * ingest mode
     **********/

    private IngestMode visitIngestMode(PersistenceParserGrammar.IngestModeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.nontemporalSnapshot() != null)
        {
            return visitNontemporalSnapshot(ctx.nontemporalSnapshot());
        }
        else if (ctx.unitemporalSnapshot() != null)
        {
            return visitUnitemporalSnapshot(ctx.unitemporalSnapshot());
        }
        else if (ctx.bitemporalSnapshot() != null)
        {
            return visitBitemporalSnapshot(ctx.bitemporalSnapshot());
        }
        else if (ctx.nontemporalDelta() != null)
        {
            return visitNontemporalDelta(ctx.nontemporalDelta());
        }
        else if (ctx.unitemporalDelta() != null)
        {
            return visitUnitemporalDelta(ctx.unitemporalDelta());
        }
        else if (ctx.bitemporalDelta() != null)
        {
            return visitBitemporalDelta(ctx.bitemporalDelta());
        }
        else if (ctx.appendOnly() != null)
        {
            return visitAppendOnly(ctx.appendOnly());
        }
        throw new EngineException("Unrecognized ingest mode", sourceInformation, EngineErrorType.PARSER);
    }

    /**********
     * ingest mode - snapshot
     **********/

    private NontemporalSnapshot visitNontemporalSnapshot(PersistenceParserGrammar.NontemporalSnapshotContext ctx)
    {
        NontemporalSnapshot nontemporalSnapshot = new NontemporalSnapshot();
        nontemporalSnapshot.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // auditing
        PersistenceParserGrammar.AuditingContext auditingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditing(), "auditing", nontemporalSnapshot.sourceInformation);
        nontemporalSnapshot.auditing = visitAuditing(auditingContext);

        return nontemporalSnapshot;
    }

    private UnitemporalSnapshot visitUnitemporalSnapshot(PersistenceParserGrammar.UnitemporalSnapshotContext ctx)
    {
        UnitemporalSnapshot unitemporalSnapshot = new UnitemporalSnapshot();
        unitemporalSnapshot.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning
        PersistenceParserGrammar.TransactionMilestoningContext transactionMilestoningContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoning(), "transactionMilestoning", unitemporalSnapshot.sourceInformation);
        unitemporalSnapshot.transactionMilestoning = visitTransactionMilestoning(transactionMilestoningContext);

        return unitemporalSnapshot;
    }

    private BitemporalSnapshot visitBitemporalSnapshot(PersistenceParserGrammar.BitemporalSnapshotContext ctx)
    {
        BitemporalSnapshot bitemporalSnapshot = new BitemporalSnapshot();
        bitemporalSnapshot.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning
        PersistenceParserGrammar.TransactionMilestoningContext transactionMilestoningContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoning(), "transactionMilestoning", bitemporalSnapshot.sourceInformation);
        bitemporalSnapshot.transactionMilestoning = visitTransactionMilestoning(transactionMilestoningContext);

        // validity milestoning
        PersistenceParserGrammar.ValidityMilestoningContext validityMilestoningContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityMilestoning(), "validityMilestoning", bitemporalSnapshot.sourceInformation);
        bitemporalSnapshot.validityMilestoning = visitValidityMilestoning(validityMilestoningContext);

        return bitemporalSnapshot;
    }

    /**********
     * ingest mode - delta
     **********/

    private NontemporalDelta visitNontemporalDelta(PersistenceParserGrammar.NontemporalDeltaContext ctx)
    {
        NontemporalDelta nontemporalDelta = new NontemporalDelta();
        nontemporalDelta.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge strategy
        PersistenceParserGrammar.MergeStrategyContext mergeStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategy(), "mergeStrategy", nontemporalDelta.sourceInformation);
        nontemporalDelta.mergeStrategy = visitMergeStrategy(mergeStrategyContext);

        // auditing
        PersistenceParserGrammar.AuditingContext auditingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditing(), "auditing", nontemporalDelta.sourceInformation);
        nontemporalDelta.auditing = visitAuditing(auditingContext);

        return nontemporalDelta;
    }

    private UnitemporalDelta visitUnitemporalDelta(PersistenceParserGrammar.UnitemporalDeltaContext ctx)
    {
        UnitemporalDelta unitemporalDelta = new UnitemporalDelta();
        unitemporalDelta.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge strategy
        PersistenceParserGrammar.MergeStrategyContext mergeStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategy(), "mergeStrategy", unitemporalDelta.sourceInformation);
        unitemporalDelta.mergeStrategy = visitMergeStrategy(mergeStrategyContext);

        // transaction milestoning
        PersistenceParserGrammar.TransactionMilestoningContext transactionMilestoning = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoning(), "transactionMilestoning", unitemporalDelta.sourceInformation);
        unitemporalDelta.transactionMilestoning = visitTransactionMilestoning(transactionMilestoning);

        return unitemporalDelta;
    }

    private BitemporalDelta visitBitemporalDelta(PersistenceParserGrammar.BitemporalDeltaContext ctx)
    {
        BitemporalDelta bitemporalDelta = new BitemporalDelta();
        bitemporalDelta.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge strategy
        PersistenceParserGrammar.MergeStrategyContext mergeStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategy(), "mergeStrategy", bitemporalDelta.sourceInformation);
        bitemporalDelta.mergeStrategy = visitMergeStrategy(mergeStrategyContext);

        // transaction milestoning
        PersistenceParserGrammar.TransactionMilestoningContext transactionMilestoningContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoning(), "transactionMilestoning", bitemporalDelta.sourceInformation);
        bitemporalDelta.transactionMilestoning = visitTransactionMilestoning(transactionMilestoningContext);

        // validity milestoning
        PersistenceParserGrammar.ValidityMilestoningContext validityMilestoningContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityMilestoning(), "validityMilestoning", bitemporalDelta.sourceInformation);
        bitemporalDelta.validityMilestoning = visitValidityMilestoning(validityMilestoningContext);

        return bitemporalDelta;
    }

    /**********
     * ingest mode - append only
     **********/

    private AppendOnly visitAppendOnly(PersistenceParserGrammar.AppendOnlyContext ctx)
    {
        AppendOnly appendOnly = new AppendOnly();
        appendOnly.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // auditing
        PersistenceParserGrammar.AuditingContext auditingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditing(), "auditing", appendOnly.sourceInformation);
        appendOnly.auditing = visitAuditing(auditingContext);

        // filter duplicates
        PersistenceParserGrammar.FilterDuplicatesContext filterDuplicatesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.filterDuplicates(), "filterDuplicates", appendOnly.sourceInformation);
        appendOnly.filterDuplicates = Boolean.parseBoolean(filterDuplicatesContext.FILTER_DUPLICATES().getText());

        return appendOnly;
    }

    // merge strategy

    private MergeStrategy visitMergeStrategy(PersistenceParserGrammar.MergeStrategyContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.noDeletesMergeStrategy() != null)
        {
            return new NoDeletesMergeStrategy();
        }
        else if (ctx.deleteIndicatorMergeStrategy() != null)
        {
            return visitDeleteIndicatorMergeScheme(ctx.deleteIndicatorMergeStrategy());
        }
        else if (ctx.opaqueMergeStrategy() != null)
        {
            return new OpaqueMergeStrategy();
        }
        throw new EngineException("Unrecognized merge strategy", sourceInformation, EngineErrorType.PARSER);
    }

    private DeleteIndicatorMergeStrategy visitDeleteIndicatorMergeScheme(PersistenceParserGrammar.DeleteIndicatorMergeStrategyContext ctx)
    {
        DeleteIndicatorMergeStrategy mergeStrategy = new DeleteIndicatorMergeStrategy();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // delete property
        PersistenceParserGrammar.MergeStrategyDeletePropertyContext deletePropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteProperty(), "deleteProperty", sourceInformation);
        mergeStrategy.deleteProperty = PureGrammarParserUtility.fromIdentifier(deletePropertyContext.identifier());

        // delete values
        PersistenceParserGrammar.MergeStrategyDeleteValuesContext deleteValuesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteValues(), "deleteValues", sourceInformation);
        mergeStrategy.deleteValues = deleteValuesContext != null && deleteValuesContext.STRING() != null ? ListIterate.collect(deleteValuesContext.STRING(), deleteValueContext -> PureGrammarParserUtility.fromGrammarString(deleteValueContext.getText(), true)) : Collections.emptyList();

        return mergeStrategy;
    }

    /**********
     * auditing
     **********/

    private Auditing visitAuditing(PersistenceParserGrammar.AuditingContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.noAuditing() != null)
        {
            return new NoAuditing();
        }
        if (ctx.dateTimeAuditing() != null)
        {
            return visitBatchDateTimeAuditScheme(ctx.dateTimeAuditing());
        }
        if (ctx.opaqueAuditing() != null)
        {
            return new OpaqueAuditing();
        }
        throw new EngineException("Unrecognized auditing", sourceInformation, EngineErrorType.PARSER);
    }

    private DateTimeAuditing visitBatchDateTimeAuditScheme(PersistenceParserGrammar.DateTimeAuditingContext ctx)
    {
        DateTimeAuditing auditing = new DateTimeAuditing();
        auditing.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // date time field name
        PersistenceParserGrammar.DateTimeFieldNameContext transactionDateTimePropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeFieldName(), "dateTimeFieldName", auditing.sourceInformation);
        auditing.dateTimeFieldName = PureGrammarParserUtility.fromGrammarString(transactionDateTimePropertyContext.STRING().getText(), true);

        return auditing;
    }

    /**********
     * transaction milestoning
     **********/

    private TransactionMilestoning visitTransactionMilestoning(PersistenceParserGrammar.TransactionMilestoningContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.batchIdTransactionMilestoning() != null)
        {
            return visitBatchIdTransactionMilestoning(ctx.batchIdTransactionMilestoning());
        }
        else if (ctx.dateTimeTransactionMilestoning() != null)
        {
            return visitDateTimeTransactionMilestoningScheme(ctx.dateTimeTransactionMilestoning());
        }
        else if (ctx.bothTransactionMilestoning() != null)
        {
            return visitBatchIdAndDateTimeTransactionMilestoningScheme(ctx.bothTransactionMilestoning());
        }
        else if (ctx.opaqueTransactionMilestoning() != null)
        {
            return new OpaqueTransactionMilestoning();
        }
        throw new EngineException("Unrecognized transaction milestoning", sourceInformation, EngineErrorType.PARSER);
    }

    private BatchIdTransactionMilestoning visitBatchIdTransactionMilestoning(PersistenceParserGrammar.BatchIdTransactionMilestoningContext ctx)
    {
        BatchIdTransactionMilestoning milestoning = new BatchIdTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in field name
        PersistenceParserGrammar.BatchIdInFieldNameContext batchIdInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInFieldName(), "batchIdInFieldName", milestoning.sourceInformation);
        milestoning.batchIdInFieldName = PureGrammarParserUtility.fromGrammarString(batchIdInFieldNameContext.STRING().getText(), true);

        // batchId out field name
        PersistenceParserGrammar.BatchIdOutFieldNameContext batchIdOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutFieldName(), "batchIdOutFieldName", milestoning.sourceInformation);
        milestoning.batchIdOutFieldName = PureGrammarParserUtility.fromGrammarString(batchIdOutFieldNameContext.STRING().getText(), true);

        return milestoning;
    }

    private DateTimeTransactionMilestoning visitDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.DateTimeTransactionMilestoningContext ctx)
    {
        DateTimeTransactionMilestoning milestoning = new DateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime in field name
        PersistenceParserGrammar.DateTimeInFieldNameContext dateTimeInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInFieldName(), "dateTimeInFieldName", milestoning.sourceInformation);
        milestoning.dateTimeInFieldName = PureGrammarParserUtility.fromGrammarString(dateTimeInFieldNameContext.STRING().getText(), true);

        // datetime out field name
        PersistenceParserGrammar.DateTimeOutFieldNameContext dateTimeOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutFieldName(), "dateTimeOutFieldName", milestoning.sourceInformation);
        milestoning.dateTimeOutFieldName = PureGrammarParserUtility.fromGrammarString(dateTimeOutFieldNameContext.STRING().getText(), true);

        return milestoning;
    }

    private BatchIdAndDateTimeTransactionMilestoning visitBatchIdAndDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.BothTransactionMilestoningContext ctx)
    {
        BatchIdAndDateTimeTransactionMilestoning milestoning = new BatchIdAndDateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in field name
        PersistenceParserGrammar.BatchIdInFieldNameContext batchIdInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInFieldName(), "batchIdInFieldName", milestoning.sourceInformation);
        milestoning.batchIdInFieldName = PureGrammarParserUtility.fromGrammarString(batchIdInFieldNameContext.STRING().getText(), true);

        // batchId out field name
        PersistenceParserGrammar.BatchIdOutFieldNameContext batchIdOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutFieldName(), "batchIdOutFieldName", milestoning.sourceInformation);
        milestoning.batchIdOutFieldName = PureGrammarParserUtility.fromGrammarString(batchIdOutFieldNameContext.STRING().getText(), true);

        // datetime in field name
        PersistenceParserGrammar.DateTimeInFieldNameContext dateTimeInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInFieldName(), "dateTimeInFieldName", milestoning.sourceInformation);
        milestoning.dateTimeInFieldName = PureGrammarParserUtility.fromGrammarString(dateTimeInFieldNameContext.STRING().getText(), true);

        // datetime out field name
        PersistenceParserGrammar.DateTimeOutFieldNameContext dateTimeOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutFieldName(), "dateTimeOutFieldName", milestoning.sourceInformation);
        milestoning.dateTimeOutFieldName = PureGrammarParserUtility.fromGrammarString(dateTimeOutFieldNameContext.STRING().getText(), true);

        return milestoning;
    }

    /**********
     * validity milestoning
     **********/

    private ValidityMilestoning visitValidityMilestoning(PersistenceParserGrammar.ValidityMilestoningContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.dateTimeValidityMilestoning() != null)
        {
            return visitDateTimeValidityMilestoningScheme(ctx.dateTimeValidityMilestoning());
        }
        else if (ctx.opaqueValidityMilestoning() != null)
        {
            return new OpaqueValidityMilestoning();
        }
        throw new EngineException("Unrecognized validity milestoning", sourceInformation, EngineErrorType.PARSER);
    }

    private DateTimeValidityMilestoning visitDateTimeValidityMilestoningScheme(PersistenceParserGrammar.DateTimeValidityMilestoningContext ctx)
    {
        DateTimeValidityMilestoning milestoning = new DateTimeValidityMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime from field name
        PersistenceParserGrammar.DateTimeFromFieldNameContext validityDateTimeFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeFromFieldName(), "dateTimeFromName", milestoning.sourceInformation);
        milestoning.dateTimeFromFieldName = PureGrammarParserUtility.fromGrammarString(validityDateTimeFromPropertyContext.STRING().getText(), true);

        // datetime thru field name
        PersistenceParserGrammar.DateTimeThruFieldNameContext validityDateTimeThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeThruFieldName(), "dateTimeThruName", milestoning.sourceInformation);
        milestoning.dateTimeThruFieldName = PureGrammarParserUtility.fromGrammarString(validityDateTimeThruPropertyContext.STRING().getText(), true);

        // validity derivation
        PersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "derivation", milestoning.sourceInformation);
        milestoning.derivation = visitValidityDerivation(validityDerivationContext);

        return milestoning;
    }

    /**********
     * validity derivation
     **********/

    private ValidityDerivation visitValidityDerivation(PersistenceParserGrammar.ValidityDerivationContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.sourceSpecifiesFromValidityDerivation() != null)
        {
            return visitSourceSpecifiesFromDate(ctx.sourceSpecifiesFromValidityDerivation());
        }
        else if (ctx.sourceSpecifiesFromThruValidityDerivation() != null)
        {
            return visitSourceSpecifiesFromThruDate(ctx.sourceSpecifiesFromThruValidityDerivation());
        }
        throw new EngineException("Unrecognized validity derivation", sourceInformation, EngineErrorType.PARSER);
    }

    private ValidityDerivation visitSourceSpecifiesFromDate(PersistenceParserGrammar.SourceSpecifiesFromValidityDerivationContext ctx)
    {
        SourceSpecifiesFromDateTime validityDerivation = new SourceSpecifiesFromDateTime();
        validityDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time from property
        PersistenceParserGrammar.ValidityDerivationFromPropertyContext validityDerivationFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationFromProperty(), "sourceDateTimeFromProperty", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromProperty = PureGrammarParserUtility.fromIdentifier(validityDerivationFromPropertyContext.identifier());

        return validityDerivation;
    }

    private ValidityDerivation visitSourceSpecifiesFromThruDate(PersistenceParserGrammar.SourceSpecifiesFromThruValidityDerivationContext ctx)
    {
        SourceSpecifiesFromAndThruDateTime validityDerivation = new SourceSpecifiesFromAndThruDateTime();
        validityDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time from property
        PersistenceParserGrammar.ValidityDerivationFromPropertyContext validityDerivationFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationFromProperty(), "sourceDateTimeFromProperty", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromProperty = PureGrammarParserUtility.fromIdentifier(validityDerivationFromPropertyContext.identifier());

        // source date time thru property
        PersistenceParserGrammar.ValidityDerivationThruPropertyContext validityDerivationThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationThruProperty(), "sourceDateTimeThruProperty", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeThruProperty = PureGrammarParserUtility.fromIdentifier(validityDerivationThruPropertyContext.identifier());

        return validityDerivation;
    }
}
