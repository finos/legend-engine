package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromAndThruDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.EventType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.OpaqueEventType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.ScheduleFired;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersister;
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

        // owners
        PersistenceParserGrammar.OwnersContext ownersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owners(), "owners", persistencePipe.sourceInformation);
        persistencePipe.owners = ownersContext != null && ownersContext.STRING() != null ? ListIterate.collect(ownersContext.STRING(), ownerCtx -> PureGrammarParserUtility.fromGrammarString(ownerCtx.getText(), true)) : Collections.emptyList();

        // trigger
        PersistenceParserGrammar.TriggerContext triggerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.trigger(), "trigger", persistencePipe.sourceInformation);
        persistencePipe.trigger = visitEventType(triggerContext);

        // input source
        PersistenceParserGrammar.ReaderContext readerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.reader(), "reader", persistencePipe.sourceInformation);
        persistencePipe.reader = visitReader(readerContext);

        // persistence
        PersistenceParserGrammar.PersisterContext persisterContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persister(), "persister", persistencePipe.sourceInformation);
        persistencePipe.persister = visitPersister(persisterContext);

        return persistencePipe;
    }

    private EventType visitEventType(PersistenceParserGrammar.TriggerContext ctx)
    {
        if (ctx.EVENT_SCHEDULE_FIRED() != null)
        {
            ScheduleFired scheduleFired = new ScheduleFired();
            scheduleFired.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return scheduleFired;
        }
        else if (ctx.EVENT_OPAQUE() != null)
        {
            OpaqueEventType opaqueEventType = new OpaqueEventType();
            opaqueEventType.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return opaqueEventType;
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
        reader.service = new PackageableElementPointer(
                PackageableElementType.SERVICE,
                PureGrammarParserUtility.fromQualifiedName(ctx.service().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.service().qualifiedName().packagePath().identifier(), ctx.service().qualifiedName().identifier()));

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
        PersistenceParserGrammar.TargetSpecificationContext targetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.targetSpecification()), "target", batch.sourceInformation);
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
            return visitFlatTargetSpecification(ctx.flatTargetSpecification().flatTargetSpecificationProperties());
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

        // datasets
        PersistenceParserGrammar.TargetComponentsContext targetComponentsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetComponents(), "components", targetSpecification.sourceInformation);
        targetSpecification.components = ListIterate.collect(targetComponentsContext.targetComponent(), this::visitPropertyAndFlatTargetSpecification);

        return targetSpecification;
    }

    private FlatTargetSpecification visitFlatTargetSpecification(PersistenceParserGrammar.FlatTargetSpecificationPropertiesContext ctx)
    {
        FlatTargetSpecification targetSpecification = new FlatTargetSpecification();
        targetSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetName(), "targetName", targetSpecification.sourceInformation);
        targetSpecification.targetName = visitTargetName(targetNameContext);

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetSpecification.sourceInformation);
        targetSpecification.modelClassPath = visitModelClass(targetModelClassContext);

        // partition properties -- currently parsing as a list of strings, to change to Property
        PersistenceParserGrammar.PartitionPropertiesContext partitionPropertiesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.partitionProperties(), "partitionProperties", targetSpecification.sourceInformation);
        targetSpecification.partitionPropertyPaths = visitPartitionProperties(partitionPropertiesContext);

        // deduplication strategy
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationStrategy(), "deduplicationStrategy", targetSpecification.sourceInformation);
        targetSpecification.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        // batch mode
        PersistenceParserGrammar.BatchModeContext batchModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchMode(), "batchMode", targetSpecification.sourceInformation);
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
        propertyAndFlatTargetSpecification.propertyPath = visitTargetComponentProperty(targetComponentPropertyContext);

        // target specification
        PersistenceParserGrammar.TargetComponentTargetSpecificationContext targetComponentTargetSpecificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetComponentTargetSpecification(), "targetSpecification", propertyAndFlatTargetSpecification.sourceInformation);
        propertyAndFlatTargetSpecification.targetSpecification = visitFlatTargetSpecification(targetComponentTargetSpecificationContext.flatTargetSpecificationProperties());

        return propertyAndFlatTargetSpecification;
    }

    private String visitTargetComponentProperty(PersistenceParserGrammar.TargetComponentPropertyContext ctx)
    {
        PersistenceParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        String modelClass = PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
        String modelProperty = ctx.identifier().getText();

        return modelClass + "->" + modelProperty;
    }

    private List<String> visitPartitionProperties(PersistenceParserGrammar.PartitionPropertiesContext ctx)
    {
        //TODO: ledav -- reconstruct property path correctly
        List<PersistenceParserGrammar.QualifiedNameContext> qualifiedNameContexts = ctx.qualifiedName();
        return Lists.immutable.ofAll(qualifiedNameContexts).collect(context -> PureGrammarParserUtility.fromQualifiedName(context.packagePath() == null ? Collections.emptyList() : context.packagePath().identifier(), context.identifier())).castToList();
    }

    private DeduplicationStrategy visitDeduplicationStrategy(PersistenceParserGrammar.DeduplicationStrategyContext ctx)
    {
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
        PersistenceParserGrammar.DeduplicationVersionPropertyNameContext versionPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.deduplicationVersionPropertyName()), "versionProperty", deduplicationStrategy.sourceInformation);
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

        // batch date time property
        PersistenceParserGrammar.BatchDateTimePropertyNameContext transactionDateTimePropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.batchDateTimePropertyName()), "batchDateTimePropertyName", auditing.sourceInformation);
        auditing.dateTimePropertyName = PureGrammarParserUtility.fromIdentifier(transactionDateTimePropertyContext.identifier());

        return auditing;
    }

    private TransactionMilestoning visitTransactionMilestoning(PersistenceParserGrammar.TransactionMilestoningContext ctx)
    {
        if (ctx.transactionSchemeBatchId() != null)
        {
            return visitBatchIdTransactionMilestoningScheme(ctx.transactionSchemeBatchId());
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

    private BatchIdTransactionMilestoning visitBatchIdTransactionMilestoningScheme(PersistenceParserGrammar.TransactionSchemeBatchIdContext ctx)
    {
        BatchIdTransactionMilestoning milestoning = new BatchIdTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in property
        PersistenceParserGrammar.BatchIdInPropertyContext batchIdInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInProperty(), "batchIdInProperty", milestoning.sourceInformation);
        milestoning.batchIdInName = PureGrammarParserUtility.fromIdentifier(batchIdInPropertyContext.identifier());

        // batchId out property
        PersistenceParserGrammar.BatchIdOutPropertyContext batchIdOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutProperty(), "batchIdOutProperty", milestoning.sourceInformation);
        milestoning.batchIdOutName = PureGrammarParserUtility.fromIdentifier(batchIdOutPropertyContext.identifier());

        return milestoning;
    }

    private DateTimeTransactionMilestoning visitDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.DateTimeTransactionMilestoningContext ctx)
    {
        DateTimeTransactionMilestoning milestoning = new DateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime in property
        PersistenceParserGrammar.DateTimeInPropertyContext transactionDateTimeInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInProperty(), "dateTimeInName", milestoning.sourceInformation);
        milestoning.dateTimeInName = PureGrammarParserUtility.fromIdentifier(transactionDateTimeInPropertyContext.identifier());

        // datetime out property
        PersistenceParserGrammar.DateTimeOutPropertyContext transactionDateTimeOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutProperty(), "dateTimeOutName", milestoning.sourceInformation);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromIdentifier(transactionDateTimeOutPropertyContext.identifier());

        return milestoning;
    }

    private BatchIdAndDateTimeTransactionMilestoning visitBatchIdAndDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.BothTransactionMilestoningContext ctx)
    {
        BatchIdAndDateTimeTransactionMilestoning milestoning = new BatchIdAndDateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in property
        PersistenceParserGrammar.BatchIdInPropertyContext batchIdInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInProperty(), "batchIdInProperty", milestoning.sourceInformation);
        milestoning.batchIdInName = PureGrammarParserUtility.fromIdentifier(batchIdInPropertyContext.identifier());

        // batchId out property
        PersistenceParserGrammar.BatchIdOutPropertyContext batchIdOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutProperty(), "batchIdOutProperty", milestoning.sourceInformation);
        milestoning.batchIdOutName = PureGrammarParserUtility.fromIdentifier(batchIdOutPropertyContext.identifier());

        // datetime in property
        PersistenceParserGrammar.DateTimeInPropertyContext dateTimeInPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInProperty(), "dateTimeInName", milestoning.sourceInformation);
        milestoning.dateTimeInName = PureGrammarParserUtility.fromIdentifier(dateTimeInPropertyContext.identifier());

        // datetime out property
        PersistenceParserGrammar.DateTimeOutPropertyContext dateTimeOutPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutProperty(), "dateTimeOutName", milestoning.sourceInformation);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromIdentifier(dateTimeOutPropertyContext.identifier());

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

        // datetime from property
        PersistenceParserGrammar.DateTimeFromPropertyContext validityDateTimeFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeFromProperty(), "dateTimeFromName", milestoning.sourceInformation);
        milestoning.dateTimeFromName = PureGrammarParserUtility.fromIdentifier(validityDateTimeFromPropertyContext.identifier());

        // datetime thru property
        PersistenceParserGrammar.DateTimeThruPropertyContext validityDateTimeThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeThruProperty(), "dateTimeThruName", milestoning.sourceInformation);
        milestoning.dateTimeThruName = PureGrammarParserUtility.fromIdentifier(validityDateTimeThruPropertyContext.identifier());

        return milestoning;
    }

    //TODO: ledav -- populate properties
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
        SourceSpecifiesFromDate validityDerivation = new SourceSpecifiesFromDate();
        validityDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time from property
        PersistenceParserGrammar.ValidityDerivationFromPropertyContext validityDerivationFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(Lists.fixedSize.of(ctx.validityDerivationFromProperty()), "sourceDateTimeFromProperty", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromProperty = visitValidityDerivationFromProperty(validityDerivationFromPropertyContext);

        return validityDerivation;
    }

    private ValidityDerivation visitSourceSpecifiesFromThruDate(PersistenceParserGrammar.SourceSpecifiesFromThruValidityDerivationContext ctx)
    {
        SourceSpecifiesFromAndThruDate validityDerivation = new SourceSpecifiesFromAndThruDate();
        validityDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time from property
        PersistenceParserGrammar.ValidityDerivationFromPropertyContext validityDerivationFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationFromProperty(), "sourceDateTimeFromProperty", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromProperty = visitValidityDerivationFromProperty(validityDerivationFromPropertyContext);

        // source date time thru property
        PersistenceParserGrammar.ValidityDerivationThruPropertyContext validityDerivationThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationThruProperty(), "sourceDateTimeThruProperty", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromProperty = visitValidityDerivationThruProperty(validityDerivationThruPropertyContext);

        return validityDerivation;
    }

    private String visitValidityDerivationFromProperty(PersistenceParserGrammar.ValidityDerivationFromPropertyContext ctx)
    {
        PersistenceParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        String modelClass = PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
        String modelProperty = ctx.identifier().getText();

        return modelClass + "->" + modelProperty;
    }

    private String visitValidityDerivationThruProperty(PersistenceParserGrammar.ValidityDerivationThruPropertyContext ctx)
    {
        PersistenceParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        String modelClass = PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
        String modelProperty = ctx.identifier().getText();

        return modelClass + "->" + modelProperty;
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
        PersistenceParserGrammar.MergeStrategyDeleteIndicatorPropertyContext deleteIndicatorPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteIndicatorProperty(), "deleteIndicatorProperty", sourceInformation);
        mergeStrategy.deleteProperty = PureGrammarParserUtility.fromIdentifier(deleteIndicatorPropertyContext.identifier());

        // delete values
        PersistenceParserGrammar.MergeStrategyDeleteIndicatorValuesContext deleteIndicatorValuesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteIndicatorValues(), "deleteValues", sourceInformation);
        //TODO: ledav -- handle non-strings
        mergeStrategy.deleteValues = deleteIndicatorValuesContext != null && deleteIndicatorValuesContext.STRING() != null ? ListIterate.collect(deleteIndicatorValuesContext.STRING(), deleteIndicatorValueContext -> PureGrammarParserUtility.fromGrammarString(deleteIndicatorValueContext.getText(), true)) : Collections.emptyList();

        return mergeStrategy;
    }
}
