package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.DataShape;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.ServicePersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchDatasetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchDatastoreSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchTransactionMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.audit.AuditScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.audit.BatchDateTimeAuditScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.audit.NoAuditScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.audit.OpaqueAuditScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.DeleteIndicatorMergeScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.MergeScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.NoDeletesMergeScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.DateTimeValidityMilestoningScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.OpaqueValidityMilestoningScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.ValidityMilestoningScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation.SourceSpecifiesValidFromAndThruDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation.SourceSpecifiesValidFromDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.EventType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.RegistryDatasetAvailable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.ScheduleTriggered;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;

import java.util.Collections;
import java.util.function.Consumer;

public class PersistenceParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserContext context;

    public PersistenceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, PureGrammarParserContext context)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.context = context;
    }

    public void visit(PersistenceParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.servicePersistence().stream().map(this::visitServicePersistence).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private ServicePersistence visitServicePersistence(PersistenceParserGrammar.ServicePersistenceContext ctx)
    {
        ServicePersistence servicePersistence = new ServicePersistence();
        servicePersistence.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        servicePersistence._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        servicePersistence.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // documentation
        PersistenceParserGrammar.DocumentationContext documentationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.documentation(), "doc", servicePersistence.sourceInformation);
        servicePersistence.documentation = PureGrammarParserUtility.fromGrammarString(documentationContext.STRING().getText(), true);

        // owners
        PersistenceParserGrammar.OwnersContext ownersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owners(), "owners", servicePersistence.sourceInformation);
        servicePersistence.owners = ownersContext != null && ownersContext.STRING() != null ? ListIterate.collect(ownersContext.STRING(), ownerCtx -> PureGrammarParserUtility.fromGrammarString(ownerCtx.getText(), true)) : Collections.emptyList();

        // trigger
        PersistenceParserGrammar.TriggerContext triggerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.trigger(), "trigger", servicePersistence.sourceInformation);
        servicePersistence.trigger = visitEventType(triggerContext);

        // service
        PersistenceParserGrammar.ServiceContext serviceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.service(), "service", servicePersistence.sourceInformation);
        servicePersistence.service = new PackageableElementPointer(
                PackageableElementType.SERVICE_PERSISTENCE,
                PureGrammarParserUtility.fromQualifiedName(serviceContext.qualifiedName().packagePath() == null ? Collections.emptyList() : serviceContext.qualifiedName().packagePath().identifier(), serviceContext.qualifiedName().identifier()));

        // persistence
        PersistenceParserGrammar.PersistenceContext persistenceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persistence(), "persistence", servicePersistence.sourceInformation);
        servicePersistence.persistence = visitPersistence(persistenceContext);

        return servicePersistence;
    }

    private EventType visitEventType(PersistenceParserGrammar.TriggerContext ctx)
    {
        if (ctx.EVENT_TYPE_SCHEDULE_TRIGGERED() != null) {
            return new ScheduleTriggered();
        }
        else if (ctx.EVENT_TYPE_REGISTRY_DATASET_AVAILABLE() != null)
        {
            return new RegistryDatasetAvailable();
        }
        throw new UnsupportedOperationException();
    }

    private Persistence visitPersistence(PersistenceParserGrammar.PersistenceContext ctx)
    {
        if (ctx.batchPersistence() != null)
        {
            return visitBatchPersistence(ctx.batchPersistence());
        }
        else if (ctx.streamingPersistence() != null)
        {
            return visitStreamingPersistence(ctx.streamingPersistence());
        }
        throw new UnsupportedOperationException();
    }

    private BatchPersistence visitBatchPersistence(PersistenceParserGrammar.BatchPersistenceContext ctx)
    {
        BatchPersistence batch = new BatchPersistence();
        batch.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // input shape
        PersistenceParserGrammar.InputShapeContext inputShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputShape(), "inputShape", batch.sourceInformation);
        batch.inputShape = visitInputShape(inputShapeContext);

        // input class
        PersistenceParserGrammar.InputClassContext inputClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputClass(), "inputClass", batch.sourceInformation);
        batch.inputClassPath = PureGrammarParserUtility.fromQualifiedName(inputClassContext.qualifiedName().packagePath() == null ? Collections.emptyList() : inputClassContext.qualifiedName().packagePath().identifier(), inputClassContext.qualifiedName().identifier());

        // transaction mode
        PersistenceParserGrammar.TransactionModeContext transactionModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMode(), "transactionMode", batch.sourceInformation);
        batch.transactionMode = visitTransactionMode(transactionModeContext);

        // target specification -- check if names match with corresponding java classes
        PersistenceParserGrammar.TargetSpecificationContext targetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetSpecification(), "target", batch.sourceInformation);
        batch.targetSpecification = visitTargetSpecification(targetSpecificationContext);

        return batch;
    }

    private DataShape visitInputShape(PersistenceParserGrammar.InputShapeContext ctx)
    {
        if (ctx.INPUT_SHAPE_FLAT() != null)
        {
            return DataShape.FLAT;
        }
        else if (ctx.INPUT_SHAPE_GROUPED_FLAT() != null)
        {
            return DataShape.GROUPED_FLAT;
        }
        else if (ctx.INPUT_SHAPE_NESTED() != null)
        {
            return DataShape.NESTED;
        }
        throw new UnsupportedOperationException();
    }

    private BatchTransactionMode visitTransactionMode(PersistenceParserGrammar.TransactionModeContext ctx)
    {
        if (ctx.TRANSACTION_MODE_SINGLE_DATASET() != null)
        {
            return BatchTransactionMode.SINGLE_DATASET;
        }
        if (ctx.TRANSACTION_MODE_ALL_DATASETS() != null)
        {
            return BatchTransactionMode.ALL_DATASETS;
        }
        throw new UnsupportedOperationException();
    }

    //TODO: ledav -- generalize to TargetSpecification
    private BatchDatastoreSpecification visitTargetSpecification(PersistenceParserGrammar.TargetSpecificationContext ctx)
    {
        if (ctx.datastore() != null)
        {
            return visitBatchDatastoreSpecification(ctx.datastore());
        }
        /* else if (others)
        {
            return visitOthers());
        }
        */
        throw new UnsupportedOperationException();
    }

    private BatchDatastoreSpecification visitBatchDatastoreSpecification(PersistenceParserGrammar.DatastoreContext ctx)
    {
        BatchDatastoreSpecification batchDatastoreSpecification = new BatchDatastoreSpecification();
        batchDatastoreSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datastore name
        PersistenceParserGrammar.DatastoreNameContext datastoreNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datastoreName(), "datastoreName", batchDatastoreSpecification.sourceInformation);
        batchDatastoreSpecification.datastoreName = datastoreNameContext != null ? PureGrammarParserUtility.fromIdentifier(datastoreNameContext.identifier()) : null;

        // datasets
        PersistenceParserGrammar.DatasetsContext datasetsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datasets(), "datasets", batchDatastoreSpecification.sourceInformation);
        batchDatastoreSpecification.datasets = ListIterate.collect(datasetsContext.dataset(), this::visitBatchDatasetSpecification);

        return batchDatastoreSpecification;
    }

    private BatchDatasetSpecification visitBatchDatasetSpecification(PersistenceParserGrammar.DatasetContext ctx)
    {
        BatchDatasetSpecification dataset = new BatchDatasetSpecification();
        dataset.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // dataset name
        PersistenceParserGrammar.DatasetNameContext datasetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datasetName(), "datasetName", dataset.sourceInformation);
        dataset.datasetName = datasetNameContext != null ? PureGrammarParserUtility.fromIdentifier(datasetNameContext.identifier()) : null;

        // partition properties -- currently parsing as a list of strings, to change to Property
        PersistenceParserGrammar.PartitionPropertiesContext partitionPropertiesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.partitionProperties(), "partitionProperties", dataset.sourceInformation);
//        dataset.partitionProperties = partitionPropertiesContext != null && partitionPropertiesContext.STRING() != null ? ListIterate.collect(partitionPropertiesContext.STRING(), partitionPropertyCtx -> PureGrammarParserUtility.fromGrammarString(partitionPropertyCtx.getText(), true)) : Collections.emptyList();

        // deduplication strategy
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationStrategy(), "deduplicationStrategy", dataset.sourceInformation);
        dataset.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        // batch mode
        PersistenceParserGrammar.BatchModeContext batchModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchMode(), "batchMode", dataset.sourceInformation);
        dataset.milestoningMode = visitBatchMilestoningMode(batchModeContext);

        return dataset;
    }

    private DeduplicationStrategy visitDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyContext ctx)
    {
        if (ctx.DEDUPLICATION_STRATEGY_NONE() != null)
        {
            return new NoDeduplicationStrategy();
        }
        else if (ctx.DEDUPLICATION_STRATEGY_ANY() != null)
        {
            return new AnyDeduplicationStrategy();
        }
        else if (ctx.DEDUPLICATION_STRATEGY_COUNT() != null)
        {
            return new CountDeduplicationStrategy();
        }
        else if (ctx.DEDUPLICATION_STRATEGY_MAX_VERSION() != null)
        {
            return new MaxVersionDeduplicationStrategy();
        }
        throw new UnsupportedOperationException();
    }

    private BatchMilestoningMode visitBatchMilestoningMode(PersistenceParserGrammar.BatchModeContext ctx)
    {
        if (ctx.appendOnly() != null)
        {
            return visitAppendOnly(ctx.appendOnly());
        }
        else if (ctx.deltaUnitemporal() != null)
        {
            return visitDeltaUnitemporal(ctx.deltaUnitemporal());
        }
        else if (ctx.snapshotUnitemporal() != null)
        {
            return visitSnapshotUnitemporal(ctx.snapshotUnitemporal());
        }
        else if (ctx.deltaBitemporal() != null)
        {
            return visitDeltaBitemporal(ctx.deltaBitemporal());
        }
        else if (ctx.snapshotBitemporal() != null)
        {
            return visitSnapshotBitemporal(ctx.snapshotBitemporal());
        }
        else if (ctx.deltaNonMilestoned() != null)
        {
            return visitDeltaNonMilestoned(ctx.deltaNonMilestoned());
        }
        else if (ctx.snapshotNonMilestoned() != null)
        {
            return visitSnapshotNonMilestoned(ctx.snapshotNonMilestoned());
        }
        throw new UnsupportedOperationException();
    }

    private AppendOnly visitAppendOnly(PersistenceParserGrammar.AppendOnlyContext ctx)
    {
        AppendOnly appendOnly = new AppendOnly();
        appendOnly.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // audit scheme
        PersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.auditScheme()), "auditScheme", appendOnly.sourceInformation);
        appendOnly.auditScheme = visitAuditScheme(auditSchemeContext);

        // filter duplicates
        PersistenceParserGrammar.FilterDuplicatesContext filterDuplicatesContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.filterDuplicates()), "filterDuplicates", appendOnly.sourceInformation);
        appendOnly.filterDuplicates = Boolean.parseBoolean(filterDuplicatesContext.FILTER_DUPLICATES().getText());

        return appendOnly;
    }

    private UnitemporalDelta visitDeltaUnitemporal(PersistenceParserGrammar.DeltaUnitemporalContext ctx)
    {
        UnitemporalDelta deltaUnitemporal = new UnitemporalDelta();
        deltaUnitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge scheme
        PersistenceParserGrammar.MergeSchemeContext mergeSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeScheme(), "mergeScheme", deltaUnitemporal.sourceInformation);
        deltaUnitemporal.mergeScheme = visitMergeScheme(mergeSchemeContext);

        // transaction milestoning scheme
        PersistenceParserGrammar.TransactionSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionScheme(), "transactionScheme", deltaUnitemporal.sourceInformation);
        deltaUnitemporal.transactionMilestoningScheme = visitTransactionMilestoningScheme(transactionSchemeContext);

        return deltaUnitemporal;
    }

    private BitemporalDelta visitDeltaBitemporal(PersistenceParserGrammar.DeltaBitemporalContext ctx)
    {
        BitemporalDelta deltaBitemporal = new BitemporalDelta();
        deltaBitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge scheme -- is this an abstract class, or contains enums? need to modify grammar file if former
        PersistenceParserGrammar.MergeSchemeContext mergeSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeScheme(), "mergeScheme", deltaBitemporal.sourceInformation);
        deltaBitemporal.mergeScheme = visitMergeScheme(mergeSchemeContext);

        // transaction milestoning scheme
        PersistenceParserGrammar.TransactionSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionScheme(), "transactionScheme", deltaBitemporal.sourceInformation);
        deltaBitemporal.transactionMilestoningScheme = visitTransactionMilestoningScheme(transactionSchemeContext);

        // validity milestoning scheme -- could perhaps make the names more uniform, referred to as validityScheme, validityMilestoningScheme, and validityMilestoning in different files/uses
        PersistenceParserGrammar.ValiditySchemeContext validitySchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityScheme(), "validityMilestoning", deltaBitemporal.sourceInformation);
        deltaBitemporal.validityMilestoningScheme = visitValidityMilestoningScheme(validitySchemeContext);

        // validity derivation
        PersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "validityDerivation", deltaBitemporal.sourceInformation);
        deltaBitemporal.validityDerivation = visitValidityDerivation(validityDerivationContext);

        return deltaBitemporal;
    }

    private NonMilestonedDelta visitDeltaNonMilestoned(PersistenceParserGrammar.DeltaNonMilestonedContext ctx)
    {
        NonMilestonedDelta deltaNonMilestoned = new NonMilestonedDelta();
        deltaNonMilestoned.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // audit scheme
        PersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.auditScheme()), "auditScheme", deltaNonMilestoned.sourceInformation);
        deltaNonMilestoned.auditScheme = visitAuditScheme(auditSchemeContext);

        return deltaNonMilestoned;
    }

    private UnitemporalSnapshot visitSnapshotUnitemporal(PersistenceParserGrammar.SnapshotUnitemporalContext ctx)
    {
        UnitemporalSnapshot snapshotUnitemporal = new UnitemporalSnapshot();
        snapshotUnitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning scheme
        PersistenceParserGrammar.TransactionSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.transactionScheme()), "transactionScheme", snapshotUnitemporal.sourceInformation);
        snapshotUnitemporal.transactionMilestoningScheme = visitTransactionMilestoningScheme(transactionSchemeContext);

        return snapshotUnitemporal;
    }

    private BitemporalSnapshot visitSnapshotBitemporal(PersistenceParserGrammar.SnapshotBitemporalContext ctx)
    {
        BitemporalSnapshot snapshotBitemporal = new BitemporalSnapshot();
        snapshotBitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning scheme
        PersistenceParserGrammar.TransactionSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionScheme(), "transactionScheme", snapshotBitemporal.sourceInformation);
        snapshotBitemporal.transactionMilestoningScheme = visitTransactionMilestoningScheme(transactionSchemeContext);

        // validity milestoning scheme -- could perhaps make the names more uniform, referred to as validityScheme, validityMilestoningScheme, and validityMilestoning in different files/uses
        PersistenceParserGrammar.ValiditySchemeContext validitySchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityScheme(), "validityMilestoning", snapshotBitemporal.sourceInformation);
        snapshotBitemporal.validityMilestoningScheme = visitValidityMilestoningScheme(validitySchemeContext);

        // validity derivation
        PersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "validityDerivation", snapshotBitemporal.sourceInformation);
        snapshotBitemporal.validityDerivation = visitValidityDerivation(validityDerivationContext);

        return snapshotBitemporal;
    }

    private NonMilestonedSnapshot visitSnapshotNonMilestoned(PersistenceParserGrammar.SnapshotNonMilestonedContext ctx)
    {
        NonMilestonedSnapshot snapshotNonMilestoned = new NonMilestonedSnapshot();
        snapshotNonMilestoned.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // audit scheme
        PersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.auditScheme()), "auditScheme", snapshotNonMilestoned.sourceInformation);
        snapshotNonMilestoned.auditScheme = visitAuditScheme(auditSchemeContext);

        return snapshotNonMilestoned;
    }

    private StreamingPersistence visitStreamingPersistence(PersistenceParserGrammar.StreamingPersistenceContext ctx)
    {
        StreamingPersistence streaming = new StreamingPersistence();
        streaming.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // input shape
        PersistenceParserGrammar.InputShapeContext inputShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputShape(), "inputShape", streaming.sourceInformation);
        streaming.inputShape = visitInputShape(inputShapeContext);

        // input class
        PersistenceParserGrammar.InputClassContext inputClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputClass(), "inputClass", streaming.sourceInformation);
        streaming.inputClassPath = PureGrammarParserUtility.fromQualifiedName(inputClassContext.qualifiedName().packagePath() == null ? Collections.emptyList() : inputClassContext.qualifiedName().packagePath().identifier(), inputClassContext.qualifiedName().identifier());

        return streaming;
    }

    private AuditScheme visitAuditScheme(PersistenceParserGrammar.AuditSchemeContext ctx)
    {
        if (ctx.AUDIT_SCHEME_NONE() != null)
        {
            return new NoAuditScheme();
        }
        if (ctx.AUDIT_SCHEME_BATCH_DATE_TIME() != null)
        {
            //TODO: ledav -- populate properties
            return new BatchDateTimeAuditScheme();
        }
        if (ctx.AUDIT_SCHEME_OPAQUE() != null)
        {
            return new OpaqueAuditScheme();
        }
        throw new UnsupportedOperationException();
    }

    //TODO: ledav -- populate properties
    private TransactionMilestoningScheme visitTransactionMilestoningScheme(PersistenceParserGrammar.TransactionSchemeContext ctx)
    {
        if (ctx.TRANSACTION_SCHEME_BATCH_ID() != null)
        {
            return new BatchIdTransactionMilestoningScheme();
        }
        else if (ctx.TRANSACTION_SCHEME_DATE_TIME() != null)
        {
            return new DateTimeTransactionMilestoningScheme();
        }
        else if (ctx.TRANSACTION_SCHEME_BOTH() != null)
        {
            return new BatchIdAndDateTimeTransactionMilestoningScheme();
        }
        else if (ctx.TRANSACTION_SCHEME_OPAQUE() != null)
        {
            return new OpaqueTransactionMilestoningScheme();
        }
        throw new UnsupportedOperationException();
    }

    //TODO: ledav -- populate properties
    private ValidityMilestoningScheme visitValidityMilestoningScheme(PersistenceParserGrammar.ValiditySchemeContext ctx)
    {
        if (ctx.VALIDITY_SCHEME_DATE_TIME() != null)
        {
            return new DateTimeValidityMilestoningScheme();
        }
        else if (ctx.VALIDITY_SCHEME_OPAQUE() != null)
        {
            return new OpaqueValidityMilestoningScheme();
        }
        throw new UnsupportedOperationException();
    }

    //TODO: ledav -- populate properties
    private ValidityDerivation visitValidityDerivation(PersistenceParserGrammar.ValidityDerivationContext ctx)
    {
        if (ctx.VALIDITY_DERIVATION_SOURCE_FROM() != null)
        {
            return new SourceSpecifiesValidFromDate();
        }
        else if (ctx.VALIDITY_DERIVATION_SOURCE_FROM_THRU() != null)
        {
            return new SourceSpecifiesValidFromAndThruDate();
        }
        throw new UnsupportedOperationException();
    }

    private MergeScheme visitMergeScheme(PersistenceParserGrammar.MergeSchemeContext ctx)
    {
        if (ctx.MERGE_SCHEME_NO_DELETES() != null)
        {
            return new NoDeletesMergeScheme();
        }
        if (ctx.MERGE_SCHEME_DELETE_INDICATOR() != null)
        {
            //TODO: ledav -- populate delete property and values
            return new DeleteIndicatorMergeScheme();
        }
        throw new UnsupportedOperationException();
    }
}
