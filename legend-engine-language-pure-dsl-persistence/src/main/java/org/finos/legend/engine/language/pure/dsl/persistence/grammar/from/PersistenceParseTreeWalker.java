package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.ServicePersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.function.Consumer;

public class PersistenceParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section; //Default vs ImportAwareCodeSection?
    private final PureGrammarParserContext context;

    public PersistenceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
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
        ServicePersistenceParserGrammar.DocumentationContext documentationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.documentation(), "doc", servicePersistence.sourceInformation);
        servicePersistence.documentation = PureGrammarParserUtility.fromGrammarString(documentationContext.STRING().getText(), true);

        // owners
        ServicePersistenceParserGrammar.OwnersContext ownersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owners(), "owners", servicePersistence.sourceInformation);
        servicePersistence.owners = ownersContext != null && ownersContext.STRING() != null ? ListIterate.collect(ownersContext.STRING(), ownerCtx -> PureGrammarParserUtility.fromGrammarString(ownerCtx.getText(), true)) : Collections.emptyList();

        // trigger -- todo: parse as abstract class event type
        ServicePersistenceParserGrammar.TriggerContext triggerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.trigger(), "trigger", servicePersistence.sourceInformation);
        servicePersistence.trigger = triggerContext != null ? PureGrammarParserUtility.fromIdentifier(triggerContext.identifier()) : null;

        // service
        ServicePersistenceParserGrammar.ServiceContext serviceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.service(), "service", servicePersistence.sourceInformation);
        servicePersistence.service = PureGrammarParserUtility.fromQualifiedName(serviceContext.qualifiedName().packagePath() == null ? Collections.emptyList() : serviceContext.qualifiedName().packagePath().identifier(), serviceContext.qualifiedName().identifier());

        // persistence
        ServicePersistenceParserGrammar.PersistenceContext persistenceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persistence(), "persistence", servicePersistence.sourceInformation);
        servicePersistence.persistence = visitPersistence(persistenceContext);

        return servicePersistence;
    }

    private Persistence visitPersistence(ServicePersistenceParserGrammar.PersistenceContext ctx)
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

    private BatchPersistence visitBatchPersistence(ServicePersistenceParserGrammar.BatchPersistenceContext ctx)
    {
        BatchPersistence batch = new BatchPersistence();
        batch.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // input shape
        ServicePersistenceParserGrammar.InputShapeContext inputShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputShape(), "inputShape", batch.sourceInformation);
        batch.inputShape = inputShapeContext != null ? PureGrammarParserUtility.fromIdentifier(inputShapeContext.identifier()) : null;

        // input Class
        ServicePersistenceParserGrammar.InputClassContext inputClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputClass(), "inputClass", batch.sourceInformation);
        batch.inputClass = PureGrammarParserUtility.fromQualifiedName(inputClassContext.qualifiedName().packagePath() == null ? Collections.emptyList() : inputClassContext.qualifiedName().packagePath().identifier(), inputClassContext.qualifiedName().identifier());

        // transaction mode
        ServicePersistenceParserGrammar.BatchTransactionModeContext batchTransactionModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchTransactionMode(), "transactionMode", batch.sourceInformation);
        batch.batchTransactionMode = batchTransactionModeContext != null ? PureGrammarParserUtility.fromIdentifier(batchTransactionModeContext.identifier()) : null;

        // target specification -- check if names match with corresponding java classes
        ServicePersistenceParserGrammar.TargetSpecificationContext targetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetSpecification(), "target", batch.sourceInformation);
        batch.targetSpecification = visitTargetSpecification(targetSpecificationContext);

        return batch;
    }

    private TargetSpecification visitTargetSpecification(ServicePersistenceParserGrammar.TargetSpecificationContext ctx)
    {
        if (ctx.batchDatastoreSpecification() != null)
        {
            return visitBatchDatastoreSpecification(ctx.batchDatastoreSpecification());
        }
        /* else if (others)
        {
            return visitOthers());
        }
        */
        throw new UnsupportedOperationException();
    }

    private BatchDatastoreSpecification visitBatchDatastoreSpecification(ServicePersistenceParserGrammar.DatastoreContext ctx)
    {
        BatchDatastoreSpecification batchDatastoreSpecification = new BatchDatastoreSpecification();
        batchDatastoreSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datastore name
        ServicePersistenceParserGrammar.DatastoreNameContext datastoreNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datastoreName(), "datastoreName", batchDatastoreSpecification.sourceInformation);
        batchDatastoreSpecification.datastoreName = datastoreNameContext != null ? PureGrammarParserUtility.fromIdentifier(datastoreNameContext.identifier()) : null;

        // datasets
        ServicePersistenceParserGrammar.DatasetsContext datasetsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datasets(), "datasets", batchDatastoreSpecification.sourceInformation);
        batchDatastoreSpecification.datasets = ListIterate.collect(datasetsContext.dataset(), this::visitBatchDatasetSpecification);

        return batchDatastoreSpecification;
    }

    private BatchDatasetSpecification(ServicePersistenceParserGrammar.BatchDatasetSpecificationContext ctx)
    {
        BatchDatasetSpecification dataset = new BatchDatasetSpecification();
        dataset.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // dataset name
        ServicePersistenceParserGrammar.DatasetNameContext datasetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.datasetName(), "datasetName", dataset.sourceInformation);
        dataset.datasetName = datasetNameContext != null ? PureGrammarParserUtility.fromIdentifier(datasetNameContext.identifier()) : null;

        // partition properties -- currently parsing as a list of strings, to change to Property
        ServicePersistenceParserGrammar.PartitionPropertiesContext partitionPropertiesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.partitionProperties(), "partitionProperties", dataset.sourceInformation);
        dataset.partitionProperties = partitionPropertiesContext != null && partitionPropertiesContext.STRING() != null ? ListIterate.collect(partitionPropertiesContext.STRING(), partitionPropertyCtx -> PureGrammarParserUtility.fromGrammarString(partitionPropertyCtx.getText(), true)) : Collections.emptyList();

        // deduplication strategy
        ServicePersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationStrategy(), "deduplicationStrategy", dataset.sourceInformation);
        dataset.deduplicationStrategy = deduplicationStrategyContext != null ? PureGrammarParserUtility.fromIdentifier(deduplicationStrategyContext.identifier()) : null;

        // batch mode
        ServicePersistenceParserGrammar.BatchMilestoningModeContext batchModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchMilestoningMode(), "batchMode", dataset.sourceInformation);
        dataset.batchMode = visitBatchMilestoningMode(batchModeContext);

        return dataset;
    }

    private BatchMilestoningMode visitBatchMilestoningMode(ServicePersistenceParserGrammar.BatchMilestoningModeContext ctx)
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

    private AppendOnly visitAppendOnly(ServicePersistenceParserGrammar.AppendOnlyContext ctx)
    {
        AppendOnly appendOnly = new AppendOnly();
        appendOnly.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // audit scheme
        ServicePersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditScheme(), "auditScheme", appendOnly.sourceInformation);
        appendOnly.auditScheme = auditSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(auditSchemeContext.identifier()) : null;

        // filter duplicates
        ServicePersistenceParserGrammar.FilterDuplicatesContext filterDuplicatesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.filterDuplicates(), "filterDuplicates", appendOnly.sourceInformation);
        appendOnly.filterDuplicates = filterDuplicatesContext != null ? PureGrammarParserUtility.fromIdentifier(filterDuplicatesContext.identifier()) : null;

        return appendOnly;
    }

    private UnitemporalDelta visitDeltaUnitemporal(ServicePersistenceParserGrammar.UnitemporalDeltaContext ctx)
    {
        UnitemporalDelta deltaUnitemporal = new UnitemporalDelta();
        deltaUnitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge scheme
        ServicePersistenceParserGrammar.MergeSchemeContext mergeSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeScheme(), "mergeScheme", deltaUnitemporal.sourceInformation);
        deltaUnitemporal.mergeScheme = mergeSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(mergeSchemeContext.identifier()) : null;

        // transaction milestoning scheme
        ServicePersistenceParserGrammar.TransactionMilestoningSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoningScheme(), "transactionScheme", deltaUnitemporal.sourceInformation);
        deltaUnitemporal.transactionScheme = transactionSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(transactionSchemeContext.identifier()) : null;

        return deltaUnitemporal;
    }

    private BitemporalDelta visitDeltaBitemporal(ServicePersistenceParserGrammar.BitemporalDeltaContext ctx)
    {
        BitemporalDelta deltaBitemporal = new BitemporalDelta();
        deltaBitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // merge scheme -- is this an abstract class, or contains enums? need to modify grammar file if former
        ServicePersistenceParserGrammar.MergeSchemeContext mergeSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeScheme(), "mergeScheme", deltaBitemporal.sourceInformation);
        deltaBitemporal.mergeScheme = mergeSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(mergeSchemeContext.identifier()) : null;

        // transaction milestoning scheme
        ServicePersistenceParserGrammar.TransactionMilestoningSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoningScheme(), "transactionScheme", deltaBitemporal.sourceInformation);
        deltaBitemporal.transactionScheme = transactionSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(transactionSchemeContext.identifier()) : null;

        // validity milestoning scheme -- could perhaps make the names more uniform, referred to as validityScheme, validityMilestoningScheme, and validityMilestoning in different files/uses
        ServicePersistenceParserGrammar.ValidityMilestoningSchemeContext validitySchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityMilestoningScheme(), "validityMilestoning", deltaBitemporal.sourceInformation);
        deltaBitemporal.validityMilestoningScheme = validitySchemeContext != null ? PureGrammarParserUtility.fromIdentifier(validitySchemeContext.identifier()) : null;

        // validity derivation
        ServicePersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "validityDerivation", deltaBitemporal.sourceInformation);
        deltaBitemporal.validityDerivation = validityDerivationContext != null ? PureGrammarParserUtility.fromIdentifier(validityDerivationContext.identifier()) : null;

        return deltaBitemporal;
    }

    private NonMilestonedDelta visitDeltaNonMilestoned(ServicePersistenceParserGrammar.NonMilestonedDeltaContext ctx)
    {
        NonMilestonedDelta deltaNonMilestoned = new NonMilestonedDelta();
        deltaNonMilestoned.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // audit scheme
        ServicePersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditScheme(), "auditScheme", deltaNonMilestoned.sourceInformation);
        deltaNonMilestoned.auditScheme = auditSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(auditSchemeContext.identifier()) : null;

        return deltaNonMilestoned;
    }

    private UnitemporalSnapshot visitSnapshotUnitemporal(ServicePersistenceParserGrammar.UnitemporalSnapshotContext ctx)
    {
        UnitemporalSnapshot snapshotUnitemporal = new UnitemporalSnapshot();
        snapshotUnitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning scheme
        ServicePersistenceParserGrammar.TransactionMilestoningSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoningScheme(), "transactionScheme", snapshotUnitemporal.sourceInformation);
        snapshotUnitemporal.transactionScheme = transactionSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(transactionSchemeContext.identifier()) : null;

        return snapshotUnitemporal;
    }

    private BitemporalSnapshot visitSnapshotBitemporal(ServicePersistenceParserGrammar.BitemporalSnapshotContext ctx)
    {
        BitemporalSnapshot snapshotBitemporal = new BitemporalSnapshot();
        snapshotBitemporal.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning scheme
        ServicePersistenceParserGrammar.TransactionMilestoningSchemeContext transactionSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionMilestoningScheme(), "transactionScheme", snapshotBitemporal.sourceInformation);
        snapshotBitemporal.transactionScheme = transactionSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(transactionSchemeContext.identifier()) : null;

        // validity milestoning scheme -- could perhaps make the names more uniform, referred to as validityScheme, validityMilestoningScheme, and validityMilestoning in different files/uses
        ServicePersistenceParserGrammar.ValidityMilestoningSchemeContext validitySchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityMilestoningScheme(), "validityMilestoning", snapshotBitemporal.sourceInformation);
        snapshotBitemporal.validityMilestoningScheme = validitySchemeContext != null ? PureGrammarParserUtility.fromIdentifier(validitySchemeContext.identifier()) : null;

        // validity derivation
        ServicePersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "validityDerivation", snapshotBitemporal.sourceInformation);
        snapshotBitemporal.validityDerivation = validityDerivationContext != null ? PureGrammarParserUtility.fromIdentifier(validityDerivationContext.identifier()) : null;

        return snapshotBitemporal;
    }

    private NonMilestonedSnapshot visitSnapshotNonMilestoned(ServicePersistenceParserGrammar.NonMilestonedSnapshotContext ctx)
    {
        NonMilestonedSnapshot snapshotNonMilestoned = new NonMilestonedSnapshot();
        snapshotNonMilestoned.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // audit scheme
        ServicePersistenceParserGrammar.AuditSchemeContext auditSchemeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.auditScheme(), "auditScheme", snapshotNonMilestoned.sourceInformation);
        snapshotNonMilestoned.auditScheme = auditSchemeContext != null ? PureGrammarParserUtility.fromIdentifier(auditSchemeContext.identifier()) : null;

        return snapshotNonMilestoned;
    }


    private StreamingPersistence visitStreamingPersistence(ServicePersistenceParserGrammar.StreamingPersistenceContext ctx)
    {
        StreamingPersistence streaming = new StreamingPersistence();
        streaming.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // input shape
        ServicePersistenceParserGrammar.InputShapeContext inputShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputShape(), "inputShape", streaming.sourceInformation);
        streaming.inputShape = inputShapeContext != null ? PureGrammarParserUtility.fromIdentifier(inputShapeContext.identifier()) : null;

        // input Class
        ServicePersistenceParserGrammar.InputClassContext inputClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.inputClass(), "inputClass", streaming.sourceInformation);
        streaming.inputClass = PureGrammarParserUtility.fromQualifiedName(inputClassContext.qualifiedName().packagePath() == null ? Collections.emptyList() : inputClassContext.qualifiedName().packagePath().identifier(), inputClassContext.qualifiedName().identifier());

        return streaming;
    }

}
