package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePipe;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.BatchDateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.OpaqueAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.OpaqueMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.OpaqueValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.OpaqueTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;

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

    public void visit(PersistenceParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.persistencePipe().stream().map(this::visitPersistencePipe).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private PersistencePipe visitPersistencePipe(PersistenceParserGrammar.PersistencePipeContext ctx)
    {
        PersistencePipe persistencePipe = new PersistencePipe();
        persistencePipe.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        persistencePipe._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        persistencePipe.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // documentation
        PersistenceParserGrammar.DocumentationContext documentationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.documentation(), "doc", persistencePipe.sourceInformation);
        persistencePipe.documentation = PureGrammarParserUtility.fromGrammarString(documentationContext.STRING().getText(), true);

        // owners (optional)
        PersistenceParserGrammar.OwnersContext ownersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owners(), "owners", persistencePipe.sourceInformation);
        persistencePipe.owners = ownersContext != null && ownersContext.STRING() != null ? ListIterate.collect(ownersContext.STRING(), ownerCtx -> PureGrammarParserUtility.fromGrammarString(ownerCtx.getText(), true)) : Collections.emptyList();

        // trigger
        PersistenceParserGrammar.TriggerContext triggerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.trigger(), "trigger", persistencePipe.sourceInformation);
        persistencePipe.trigger = visitTrigger(triggerContext);

        // reader
        PersistenceParserGrammar.ReaderContext readerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.reader(), "reader", persistencePipe.sourceInformation);
        persistencePipe.reader = visitReader(readerContext);

        // persister
        PersistenceParserGrammar.PersisterContext persisterContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persister(), "persister", persistencePipe.sourceInformation);
        persistencePipe.persister = visitPersister(persisterContext);

        return persistencePipe;
    }

    private Trigger visitTrigger(PersistenceParserGrammar.TriggerContext ctx)
    {
        if (ctx.TRIGGER_OPAQUE() != null)
        {
            OpaqueTrigger opaqueTrigger = new OpaqueTrigger();
            opaqueTrigger.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return opaqueTrigger;
        }
        throw new UnsupportedOperationException();
    }

    private Reader visitReader(PersistenceParserGrammar.ReaderContext ctx)
    {
        if (ctx.serviceReader() != null)
        {
            return visitServiceReader(ctx.serviceReader());
        }
        throw new UnsupportedOperationException();
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

    private Persister visitPersister(PersistenceParserGrammar.PersisterContext ctx)
    {
        if (ctx.streamingPersister() != null)
        {
            return visitStreamingPersister(ctx.streamingPersister());
        }
        else if (ctx.batchPersister() != null)
        {
            return visitBatchPersister(ctx.batchPersister());
        }
        throw new UnsupportedOperationException();
    }

    private StreamingPersister visitStreamingPersister(PersistenceParserGrammar.StreamingPersisterContext ctx)
    {
        StreamingPersister streaming = new StreamingPersister();
        streaming.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        return streaming;
    }

    private BatchPersister visitBatchPersister(PersistenceParserGrammar.BatchPersisterContext ctx)
    {
        BatchPersister batch = new BatchPersister();
        batch.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // target specification
        PersistenceParserGrammar.TargetSpecificationContext targetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetSpecification(), "target", batch.sourceInformation);
        batch.targetSpecification = visitTargetSpecification(targetSpecificationContext);

        return batch;
    }

    private TargetSpecification visitTargetSpecification(PersistenceParserGrammar.TargetSpecificationContext ctx)
    {
        if (ctx.groupedTargetSpecification() != null)
        {
            return visitGroupedFlatTargetSpecification(ctx.groupedTargetSpecification());
        }
        else if (ctx.flatTargetSpecification() != null)
        {
            return visitFlatTargetSpecification(ctx.flatTargetSpecification());
        }
        else if (ctx.nestedTargetSpecification() != null)
        {
            return visitNestedTargetSpecification(ctx.nestedTargetSpecification());
        }
        throw new UnsupportedOperationException();
    }

    private GroupedFlatTargetSpecification visitGroupedFlatTargetSpecification(PersistenceParserGrammar.GroupedTargetSpecificationContext ctx)
    {
        GroupedFlatTargetSpecification targetSpecification = new GroupedFlatTargetSpecification();
        targetSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetSpecification.sourceInformation);
        targetSpecification.modelClassPath = visitModelClass(targetModelClassContext);

        // transaction scope
        PersistenceParserGrammar.TargetTransactionScopeContext targetTransactionScopeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetTransactionScope(), "transactionScope", targetSpecification.sourceInformation);
        targetSpecification.transactionScope = visitTransactionScope(targetTransactionScopeContext);

        // components
        PersistenceParserGrammar.TargetComponentsContext targetComponentsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetComponents(), "components", targetSpecification.sourceInformation);
        targetSpecification.components = ListIterate.collect(targetComponentsContext.targetComponent(), this::visitPropertyAndFlatTargetSpecification);

        return targetSpecification;
    }

    private FlatTargetSpecification visitFlatTargetSpecification(PersistenceParserGrammar.FlatTargetSpecificationContext ctx)
    {
        FlatTargetSpecification targetSpecification = createBaseFlatTargetSpecification(walkerSourceInformation.getSourceInformation(ctx), ctx.targetName(), ctx.partitionProperties(), ctx.deduplicationStrategy(), ctx.batchMode());

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetSpecification.sourceInformation);
        targetSpecification.modelClassPath = visitModelClass(targetModelClassContext);

        return targetSpecification;
    }

    private FlatTargetSpecification createBaseFlatTargetSpecification(SourceInformation sourceInformation, List<PersistenceParserGrammar.TargetNameContext> targetNameContexts, List<PersistenceParserGrammar.PartitionPropertiesContext> partitionPropertiesContexts, List<PersistenceParserGrammar.DeduplicationStrategyContext> deduplicationStrategyContexts, List<PersistenceParserGrammar.BatchModeContext> batchModeContexts)
    {
        FlatTargetSpecification targetSpecification = new FlatTargetSpecification();
        targetSpecification.sourceInformation = sourceInformation;

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(targetNameContexts, "targetName", targetSpecification.sourceInformation);
        targetSpecification.targetName = visitTargetName(targetNameContext);

        // partition properties (optional)
        PersistenceParserGrammar.PartitionPropertiesContext partitionPropertiesContext = PureGrammarParserUtility.validateAndExtractOptionalField(partitionPropertiesContexts, "partitionProperties", targetSpecification.sourceInformation);
        targetSpecification.partitionPropertyPaths = partitionPropertiesContext != null ? visitPartitionProperties(partitionPropertiesContext) : Collections.emptyList();

        // deduplication strategy (optional)
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractOptionalField(deduplicationStrategyContexts, "deduplicationStrategy", targetSpecification.sourceInformation);
        targetSpecification.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        // batch mode
        PersistenceParserGrammar.BatchModeContext batchModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(batchModeContexts, "batchMode", targetSpecification.sourceInformation);
        targetSpecification.milestoningMode = visitBatchMilestoningMode(batchModeContext);

        return targetSpecification;
    }

    private TargetSpecification visitNestedTargetSpecification(PersistenceParserGrammar.NestedTargetSpecificationContext ctx)
    {
        NestedTargetSpecification targetSpecification = new NestedTargetSpecification();
        targetSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetName(), "targetName", targetSpecification.sourceInformation);
        targetSpecification.targetName = visitTargetName(targetNameContext);

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetSpecification.sourceInformation);
        targetSpecification.modelClassPath = visitModelClass(targetModelClassContext);

        return targetSpecification;
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

    private TransactionScope visitTransactionScope(PersistenceParserGrammar.TargetTransactionScopeContext ctx)
    {
        if (ctx.TXN_SCOPE_SINGLE() != null)
        {
            return TransactionScope.SINGLE_TARGET;
        }
        else if (ctx.TXN_SCOPE_ALL() != null)
        {
            return TransactionScope.ALL_TARGETS;
        }
        throw new UnsupportedOperationException();
    }

    private PropertyAndFlatTargetSpecification visitPropertyAndFlatTargetSpecification(PersistenceParserGrammar.TargetComponentContext ctx)
    {
        PropertyAndFlatTargetSpecification propertyAndFlatTargetSpecification = new PropertyAndFlatTargetSpecification();
        propertyAndFlatTargetSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // property
        PersistenceParserGrammar.TargetComponentPropertyContext targetComponentPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetComponentProperty(), "property", propertyAndFlatTargetSpecification.sourceInformation);
        propertyAndFlatTargetSpecification.propertyPath = PureGrammarParserUtility.fromIdentifier(targetComponentPropertyContext.identifier());

        // target specification (note: not expecting a model class in this context; compiler will populate based on target type of property above)
        PersistenceParserGrammar.TargetComponentTargetSpecificationContext targetComponentTargetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetComponentTargetSpecification(), "targetSpecification", propertyAndFlatTargetSpecification.sourceInformation);
        propertyAndFlatTargetSpecification.targetSpecification = visitComponentFlatTargetSpecification(targetComponentTargetSpecificationContext);

        return propertyAndFlatTargetSpecification;
    }

    private FlatTargetSpecification visitComponentFlatTargetSpecification(PersistenceParserGrammar.TargetComponentTargetSpecificationContext ctx)
    {
        return createBaseFlatTargetSpecification(walkerSourceInformation.getSourceInformation(ctx), ctx.targetName(), ctx.partitionProperties(), ctx.deduplicationStrategy(), ctx.batchMode());
    }

    private List<String> visitPartitionProperties(PersistenceParserGrammar.PartitionPropertiesContext ctx)
    {
        List<PersistenceParserGrammar.IdentifierContext> identifierContexts = ctx.identifier();
        return Lists.immutable.ofAll(identifierContexts).collect(PureGrammarParserUtility::fromIdentifier).castToList();
    }

    private DeduplicationStrategy visitDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyContext ctx)
    {
        // default to none
        if (ctx == null)
        {
            return new NoDeduplicationStrategy();
        }

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
        throw new UnsupportedOperationException();
    }

    private MaxVersionDeduplicationStrategy visitMaxVersionDeduplicationStrategy(PersistenceParserGrammar.MaxVersionDeduplicationStrategyContext ctx)
    {
        MaxVersionDeduplicationStrategy deduplicationStrategy = new MaxVersionDeduplicationStrategy();
        deduplicationStrategy.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // version property
        PersistenceParserGrammar.DeduplicationVersionPropertyContext versionPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.deduplicationVersionProperty()), "versionProperty", deduplicationStrategy.sourceInformation);
        deduplicationStrategy.versionProperty = PureGrammarParserUtility.fromIdentifier(versionPropertyContext.identifier());

        return deduplicationStrategy;
    }

    private BatchMilestoningMode visitBatchMilestoningMode(PersistenceParserGrammar.BatchModeContext ctx)
    {
        if (ctx.nonMilestonedSnapshot() != null)
        {
            return visitNonMilestonedSnapshot(ctx.nonMilestonedSnapshot());
        }
        else if (ctx.unitemporalSnapshot() != null)
        {
            return visitUnitemporalSnapshot(ctx.unitemporalSnapshot());
        }
        else if (ctx.bitemporalSnapshot() != null)
        {
            return visitBitemporalSnapshot(ctx.bitemporalSnapshot());
        }
        else if (ctx.nonMilestonedDelta() != null)
        {
            return visitNonMilestonedDelta(ctx.nonMilestonedDelta());
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
        throw new UnsupportedOperationException();
    }

    private NonMilestonedDelta visitNonMilestonedDelta(PersistenceParserGrammar.NonMilestonedDeltaContext ctx)
    {
        NonMilestonedDelta nonMilestonedDelta = new NonMilestonedDelta();
        nonMilestonedDelta.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // auditing
        PersistenceParserGrammar.AuditingContext auditingContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.auditing()), "auditing", nonMilestonedDelta.sourceInformation);
        nonMilestonedDelta.auditing = visitAuditing(auditingContext);

        return nonMilestonedDelta;
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

        // validity derivation
        PersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "validityDerivation", bitemporalDelta.sourceInformation);
        bitemporalDelta.validityDerivation = visitValidityDerivation(validityDerivationContext);

        return bitemporalDelta;
    }

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

    private NonMilestonedSnapshot visitNonMilestonedSnapshot(PersistenceParserGrammar.NonMilestonedSnapshotContext ctx)
    {
        NonMilestonedSnapshot nonMilestonedSnapshot = new NonMilestonedSnapshot();
        nonMilestonedSnapshot.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // auditing
        PersistenceParserGrammar.AuditingContext auditingContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.auditing()), "auditing", nonMilestonedSnapshot.sourceInformation);
        nonMilestonedSnapshot.auditing = visitAuditing(auditingContext);

        return nonMilestonedSnapshot;
    }

    private UnitemporalSnapshot visitUnitemporalSnapshot(PersistenceParserGrammar.UnitemporalSnapshotContext ctx)
    {
        UnitemporalSnapshot unitemporalSnapshot = new UnitemporalSnapshot();
        unitemporalSnapshot.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // transaction milestoning
        PersistenceParserGrammar.TransactionMilestoningContext transactionMilestoningContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.transactionMilestoning()), "transactionMilestoning", unitemporalSnapshot.sourceInformation);
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

        // validity derivation
        PersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "validityDerivation", bitemporalSnapshot.sourceInformation);
        bitemporalSnapshot.validityDerivation = visitValidityDerivation(validityDerivationContext);

        return bitemporalSnapshot;
    }

    private Auditing visitAuditing(PersistenceParserGrammar.AuditingContext ctx)
    {
        if (ctx.noAuditing() != null)
        {
            return new NoAuditing();
        }
        if (ctx.batchDateTimeAuditing() != null)
        {
            return visitBatchDateTimeAuditScheme(ctx.batchDateTimeAuditing());
        }
        if (ctx.opaqueAuditing() != null)
        {
            return new OpaqueAuditing();
        }
        throw new UnsupportedOperationException();
    }

    private BatchDateTimeAuditing visitBatchDateTimeAuditScheme(PersistenceParserGrammar.BatchDateTimeAuditingContext ctx)
    {
        BatchDateTimeAuditing auditing = new BatchDateTimeAuditing();
        auditing.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batch date time field name
        PersistenceParserGrammar.BatchDateTimeFieldNameContext transactionDateTimePropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.batchDateTimeFieldName()), "batchDateTimeFieldName", auditing.sourceInformation);
        auditing.dateTimeFieldName = PureGrammarParserUtility.fromGrammarString(transactionDateTimePropertyContext.STRING().getText(), true);

        return auditing;
    }

    private TransactionMilestoning visitTransactionMilestoning(PersistenceParserGrammar.TransactionMilestoningContext ctx)
    {
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
        throw new UnsupportedOperationException();
    }

    private BatchIdTransactionMilestoning visitBatchIdTransactionMilestoning(PersistenceParserGrammar.BatchIdTransactionMilestoningContext ctx)
    {
        BatchIdTransactionMilestoning milestoning = new BatchIdTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in field name
        PersistenceParserGrammar.BatchIdInFieldNameContext batchIdInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInFieldName(), "batchIdInFieldName", milestoning.sourceInformation);
        milestoning.batchIdInName = PureGrammarParserUtility.fromGrammarString(batchIdInFieldNameContext.STRING().getText(), true);

        // batchId out field name
        PersistenceParserGrammar.BatchIdOutFieldNameContext batchIdOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutFieldName(), "batchIdOutFieldName", milestoning.sourceInformation);
        milestoning.batchIdOutName = PureGrammarParserUtility.fromGrammarString(batchIdOutFieldNameContext.STRING().getText(), true);

        return milestoning;
    }

    private DateTimeTransactionMilestoning visitDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.DateTimeTransactionMilestoningContext ctx)
    {
        DateTimeTransactionMilestoning milestoning = new DateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime in field name
        PersistenceParserGrammar.DateTimeInFieldNameContext dateTimeInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInFieldName(), "dateTimeInFieldName", milestoning.sourceInformation);
        milestoning.dateTimeInName = PureGrammarParserUtility.fromGrammarString(dateTimeInFieldNameContext.STRING().getText(), true);

        // datetime out field name
        PersistenceParserGrammar.DateTimeOutFieldNameContext dateTimeOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutFieldName(), "dateTimeOutFieldName", milestoning.sourceInformation);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromGrammarString(dateTimeOutFieldNameContext.STRING().getText(), true);

        return milestoning;
    }

    private BatchIdAndDateTimeTransactionMilestoning visitBatchIdAndDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.BothTransactionMilestoningContext ctx)
    {
        BatchIdAndDateTimeTransactionMilestoning milestoning = new BatchIdAndDateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in field name
        PersistenceParserGrammar.BatchIdInFieldNameContext batchIdInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInFieldName(), "batchIdInFieldName", milestoning.sourceInformation);
        milestoning.batchIdInName = PureGrammarParserUtility.fromGrammarString(batchIdInFieldNameContext.STRING().getText(), true);

        // batchId out field name
        PersistenceParserGrammar.BatchIdOutFieldNameContext batchIdOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutFieldName(), "batchIdOutFieldName", milestoning.sourceInformation);
        milestoning.batchIdOutName = PureGrammarParserUtility.fromGrammarString(batchIdOutFieldNameContext.STRING().getText(), true);

        // datetime in field name
        PersistenceParserGrammar.DateTimeInFieldNameContext dateTimeInFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInFieldName(), "dateTimeInFieldName", milestoning.sourceInformation);
        milestoning.dateTimeInName = PureGrammarParserUtility.fromGrammarString(dateTimeInFieldNameContext.STRING().getText(), true);

        // datetime out field name
        PersistenceParserGrammar.DateTimeOutFieldNameContext dateTimeOutFieldNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutFieldName(), "dateTimeOutFieldName", milestoning.sourceInformation);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromGrammarString(dateTimeOutFieldNameContext.STRING().getText(), true);

        return milestoning;
    }

    private ValidityMilestoning visitValidityMilestoning(PersistenceParserGrammar.ValidityMilestoningContext ctx)
    {
        if (ctx.dateTimeValidityMilestoning() != null)
        {
            return visitDateTimeValidityMilestoningScheme(ctx.dateTimeValidityMilestoning());
        }
        else if (ctx.opaqueValidityMilestoning() != null)
        {
            return new OpaqueValidityMilestoning();
        }
        throw new UnsupportedOperationException();
    }

    private DateTimeValidityMilestoning visitDateTimeValidityMilestoningScheme(PersistenceParserGrammar.DateTimeValidityMilestoningContext ctx)
    {
        DateTimeValidityMilestoning milestoning = new DateTimeValidityMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime from field name
        PersistenceParserGrammar.DateTimeFromFieldNameContext validityDateTimeFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeFromFieldName(), "dateTimeFromName", milestoning.sourceInformation);
        milestoning.dateTimeFromName = PureGrammarParserUtility.fromGrammarString(validityDateTimeFromPropertyContext.STRING().getText(), true);

        // datetime thru field name
        PersistenceParserGrammar.DateTimeThruFieldNameContext validityDateTimeThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeThruFieldName(), "dateTimeThruName", milestoning.sourceInformation);
        milestoning.dateTimeThruName = PureGrammarParserUtility.fromGrammarString(validityDateTimeThruPropertyContext.STRING().getText(), true);

        return milestoning;
    }

    private ValidityDerivation visitValidityDerivation(PersistenceParserGrammar.ValidityDerivationContext ctx)
    {
        if (ctx.sourceSpecifiesFromValidityDerivation() != null)
        {
            return visitSourceSpecifiesFromDate(ctx.sourceSpecifiesFromValidityDerivation());
        }
        else if (ctx.sourceSpecifiesFromThruValidityDerivation() != null)
        {
            return visitSourceSpecifiesFromThruDate(ctx.sourceSpecifiesFromThruValidityDerivation());
        }
        throw new UnsupportedOperationException();
    }

    private ValidityDerivation visitSourceSpecifiesFromDate(PersistenceParserGrammar.SourceSpecifiesFromValidityDerivationContext ctx)
    {
        SourceSpecifiesFromDateTime validityDerivation = new SourceSpecifiesFromDateTime();
        validityDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time from property
        PersistenceParserGrammar.ValidityDerivationFromPropertyContext validityDerivationFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.validityDerivationFromProperty()), "sourceDateTimeFromProperty", validityDerivation.sourceInformation);
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

    private MergeStrategy visitMergeStrategy(PersistenceParserGrammar.MergeStrategyContext ctx)
    {
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
        throw new UnsupportedOperationException();
    }

    private DeleteIndicatorMergeStrategy visitDeleteIndicatorMergeScheme(PersistenceParserGrammar.DeleteIndicatorMergeStrategyContext ctx)
    {
        DeleteIndicatorMergeStrategy mergeStrategy = new DeleteIndicatorMergeStrategy();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // delete property
        PersistenceParserGrammar.MergeStrategyDeletePropertyContext deletePropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteProperty(), "deleteIndicatorProperty", sourceInformation);
        mergeStrategy.deleteProperty = PureGrammarParserUtility.fromIdentifier(deletePropertyContext.identifier());

        // delete values
        PersistenceParserGrammar.MergeStrategyDeleteValuesContext deleteValuesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteValues(), "deleteValues", sourceInformation);
        mergeStrategy.deleteValues = deleteValuesContext != null && deleteValuesContext.STRING() != null ? ListIterate.collect(deleteValuesContext.STRING(), deleteValueContext -> PureGrammarParserUtility.fromGrammarString(deleteValueContext.getText(), true)) : Collections.emptyList();

        return mergeStrategy;
    }
}
