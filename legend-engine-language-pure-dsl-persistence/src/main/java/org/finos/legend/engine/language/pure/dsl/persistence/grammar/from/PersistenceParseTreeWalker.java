package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
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
import java.util.List;
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
        dataset.partitionProperties = visitPartitionProperties(partitionPropertiesContext);

        // deduplication strategy
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationStrategy(), "deduplicationStrategy", dataset.sourceInformation);
        dataset.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        // batch mode
        PersistenceParserGrammar.BatchModeContext batchModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchMode(), "batchMode", dataset.sourceInformation);
        dataset.milestoningMode = visitBatchMilestoningMode(batchModeContext);

        return dataset;
    }

    private List<String> visitPartitionProperties(PersistenceParserGrammar.PartitionPropertiesContext ctx)
    {
        List<PersistenceParserGrammar.QualifiedNameContext> qualifiedNameContexts = ctx.qualifiedName();
        return Lists.immutable.ofAll(qualifiedNameContexts).collect(context -> PureGrammarParserUtility.fromQualifiedName(context.packagePath() == null ? Collections.emptyList() : context.packagePath().identifier(), context.identifier())).castToList();
    }

    private DeduplicationStrategy visitDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyContext ctx)
    {
        if (ctx.deduplicationStrategyNone() != null)
        {
            return new NoDeduplicationStrategy();
        }
        else if (ctx.deduplicationStrategyAny() != null)
        {
            return new AnyDeduplicationStrategy();
        }
        else if (ctx.deduplicationStrategyCount() != null)
        {
            return visitCountDeduplicationStrategy(ctx.deduplicationStrategyCount());
        }
        else if (ctx.deduplicationStrategyMaxVersion() != null)
        {
            return visitMaxVersionDeduplicationStrategy(ctx.deduplicationStrategyMaxVersion());
        }
        throw new UnsupportedOperationException();
    }

    private CountDeduplicationStrategy visitCountDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyCountContext ctx)
    {
        CountDeduplicationStrategy deduplicationStrategy = new CountDeduplicationStrategy();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // duplicate count property
        PersistenceParserGrammar.DeduplicationCountPropertyNameContext duplicateCountPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.deduplicationCountPropertyName()), "duplicateCountProperty", sourceInformation);
        deduplicationStrategy.duplicateCountPropertyName = PureGrammarParserUtility.fromIdentifier(duplicateCountPropertyContext.identifier());

        return deduplicationStrategy;
    }

    private MaxVersionDeduplicationStrategy visitMaxVersionDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyMaxVersionContext ctx)
    {
        MaxVersionDeduplicationStrategy deduplicationStrategy = new MaxVersionDeduplicationStrategy();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // duplicate count property
        PersistenceParserGrammar.DeduplicationVersionPropertyNameContext versionPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.deduplicationVersionPropertyName()), "versionProperty", sourceInformation);
        deduplicationStrategy.versionProperty = PureGrammarParserUtility.fromIdentifier(versionPropertyContext.identifier());

        return deduplicationStrategy;
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
        PersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditScheme(), "auditScheme", appendOnly.sourceInformation);
        appendOnly.auditScheme = visitAuditScheme(auditSchemeContext);

        // filter duplicates
        PersistenceParserGrammar.FilterDuplicatesContext filterDuplicatesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.filterDuplicates(), "filterDuplicates", appendOnly.sourceInformation);
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
        if (ctx.auditSchemeNone() != null)
        {
            return new NoAuditScheme();
        }
        if (ctx.auditSchemeBatchDateTime() != null)
        {
            return visitBatchDateTimeAuditScheme(ctx.auditSchemeBatchDateTime());
        }
        if (ctx.auditSchemeOpaque() != null)
        {
            return new OpaqueAuditScheme();
        }
        throw new UnsupportedOperationException();
    }

    private BatchDateTimeAuditScheme visitBatchDateTimeAuditScheme(PersistenceParserGrammar.AuditSchemeBatchDateTimeContext ctx)
    {
        BatchDateTimeAuditScheme auditScheme = new BatchDateTimeAuditScheme();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction date time property
        PersistenceParserGrammar.TransactionDateTimePropertyNameContext transactionDateTimePropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.transactionDateTimePropertyName()), "transactionDateTimePropertyName", sourceInformation);
        auditScheme.transactionDateTimePropertyName = PureGrammarParserUtility.fromIdentifier(transactionDateTimePropertyContext.identifier());

        return auditScheme;
    }

    private TransactionMilestoningScheme visitTransactionMilestoningScheme(PersistenceParserGrammar.TransactionSchemeContext ctx)
    {
        if (ctx.transactionSchemeBatchId() != null)
        {
            return visitBatchIdTransactionMilestoningScheme(ctx.transactionSchemeBatchId());
        }
        else if (ctx.transactionSchemeDateTime() != null)
        {
            return visitDateTimeTransactionMilestoningScheme(ctx.transactionSchemeDateTime());
        }
        else if (ctx.transactionSchemeBoth() != null)
        {
            return visitBatchIdAndDateTimeTransactionMilestoningScheme(ctx.transactionSchemeBoth());
        }
        else if (ctx.transactionSchemeOpaque() != null)
        {
            return new OpaqueTransactionMilestoningScheme();
        }
        throw new UnsupportedOperationException();
    }

    private BatchIdTransactionMilestoningScheme visitBatchIdTransactionMilestoningScheme(PersistenceParserGrammar.TransactionSchemeBatchIdContext ctx)
    {
        BatchIdTransactionMilestoningScheme milestoningScheme = new BatchIdTransactionMilestoningScheme();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in property
        PersistenceParserGrammar.BatchIdInPropertyContext batchIdInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInProperty(), "batchIdInProperty", sourceInformation);
        milestoningScheme.batchIdInName = PureGrammarParserUtility.fromIdentifier(batchIdInPropertyContext.identifier());

        // batchId out property
        PersistenceParserGrammar.BatchIdOutPropertyContext batchIdOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutProperty(), "batchIdOutProperty", sourceInformation);
        milestoningScheme.batchIdOutName = PureGrammarParserUtility.fromIdentifier(batchIdOutPropertyContext.identifier());

        return milestoningScheme;
    }

    private DateTimeTransactionMilestoningScheme visitDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.TransactionSchemeDateTimeContext ctx)
    {
        DateTimeTransactionMilestoningScheme milestoningScheme = new DateTimeTransactionMilestoningScheme();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime in property
        PersistenceParserGrammar.TransactionDateTimeInPropertyContext transactionDateTimeInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDateTimeInProperty(), "transactionDateTimeInName", sourceInformation);
        milestoningScheme.transactionDateTimeInName = PureGrammarParserUtility.fromIdentifier(transactionDateTimeInPropertyContext.identifier());

        // datetime out property
        PersistenceParserGrammar.TransactionDateTimeOutPropertyContext transactionDateTimeOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDateTimeOutProperty(), "transactionDateTimeOutName", sourceInformation);
        milestoningScheme.transactionDateTimeOutName = PureGrammarParserUtility.fromIdentifier(transactionDateTimeOutPropertyContext.identifier());

        return milestoningScheme;
    }

    private BatchIdAndDateTimeTransactionMilestoningScheme visitBatchIdAndDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.TransactionSchemeBothContext ctx)
    {
        BatchIdAndDateTimeTransactionMilestoningScheme milestoningScheme = new BatchIdAndDateTimeTransactionMilestoningScheme();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in property
        PersistenceParserGrammar.BatchIdInPropertyContext batchIdInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInProperty(), "batchIdInProperty", sourceInformation);
        milestoningScheme.batchIdInName = PureGrammarParserUtility.fromIdentifier(batchIdInPropertyContext.identifier());

        // batchId out property
        PersistenceParserGrammar.BatchIdOutPropertyContext batchIdOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutProperty(), "batchIdOutProperty", sourceInformation);
        milestoningScheme.batchIdOutName = PureGrammarParserUtility.fromIdentifier(batchIdOutPropertyContext.identifier());

        // datetime in property
        PersistenceParserGrammar.TransactionDateTimeInPropertyContext transactionDateTimeInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDateTimeInProperty(), "transactionDateTimeInName", sourceInformation);
        milestoningScheme.transactionDateTimeInName = PureGrammarParserUtility.fromIdentifier(transactionDateTimeInPropertyContext.identifier());

        // datetime out property
        PersistenceParserGrammar.TransactionDateTimeOutPropertyContext transactionDateTimeOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDateTimeOutProperty(), "transactionDateTimeOutName", sourceInformation);
        milestoningScheme.transactionDateTimeOutName = PureGrammarParserUtility.fromIdentifier(transactionDateTimeOutPropertyContext.identifier());

        return milestoningScheme;
    }

    //TODO: ledav -- populate properties
    private ValidityMilestoningScheme visitValidityMilestoningScheme(PersistenceParserGrammar.ValiditySchemeContext ctx)
    {
        if (ctx.validitySchemeDateTime() != null)
        {
            return visitDateTimeValidityMilestoningScheme(ctx.validitySchemeDateTime());
        }
        else if (ctx.validitySchemeOpaque() != null)
        {
            return new OpaqueValidityMilestoningScheme();
        }
        throw new UnsupportedOperationException();
    }

    private DateTimeValidityMilestoningScheme visitDateTimeValidityMilestoningScheme(PersistenceParserGrammar.ValiditySchemeDateTimeContext ctx)
    {
        DateTimeValidityMilestoningScheme milestoningScheme = new DateTimeValidityMilestoningScheme();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime in property
        PersistenceParserGrammar.ValidityDateTimeFromPropertyContext validityDateTimeFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDateTimeFromProperty(), "validityDateTimeFromName", sourceInformation);
        milestoningScheme.validDateTimeFromName = PureGrammarParserUtility.fromIdentifier(validityDateTimeFromPropertyContext.identifier());

        // datetime out property
        PersistenceParserGrammar.ValidityDateTimeThruPropertyContext validityDateTimeThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDateTimeThruProperty(), "validityDateTimeThruName", sourceInformation);
        milestoningScheme.validDateTimeThruName = PureGrammarParserUtility.fromIdentifier(validityDateTimeThruPropertyContext.identifier());

        return milestoningScheme;
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
        if (ctx.mergeSchemeNoDeletes() != null)
        {
            return new NoDeletesMergeScheme();
        }
        if (ctx.mergeSchemeDeleteIndicator() != null)
        {
            return visitDeleteIndicatorMergeScheme(ctx.mergeSchemeDeleteIndicator());
        }
        throw new UnsupportedOperationException();
    }

    private DeleteIndicatorMergeScheme visitDeleteIndicatorMergeScheme(PersistenceParserGrammar.MergeSchemeDeleteIndicatorContext ctx)
    {
        DeleteIndicatorMergeScheme mergeScheme = new DeleteIndicatorMergeScheme();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // delete property
        PersistenceParserGrammar.MergeSchemeDeleteIndicatorPropertyContext deleteIndicatorPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeSchemeDeleteIndicatorProperty(), "deleteIndicatorProperty", sourceInformation);
        mergeScheme.deleteProperty = PureGrammarParserUtility.fromIdentifier(deleteIndicatorPropertyContext.identifier());

        // delete values
        PersistenceParserGrammar.MergeSchemeDeleteIndicatorValuesContext deleteIndicatorValuesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeSchemeDeleteIndicatorValues(), "deleteValues", sourceInformation);
        //TODO: ledav -- handle non-strings
        mergeScheme.deleteValues = deleteIndicatorValuesContext != null && deleteIndicatorValuesContext.STRING() != null ? ListIterate.collect(deleteIndicatorValuesContext.STRING(), deleteIndicatorValueContext -> PureGrammarParserUtility.fromGrammarString(deleteIndicatorValueContext.getText(), true)) : Collections.emptyList();
        return mergeScheme;
    }
}
