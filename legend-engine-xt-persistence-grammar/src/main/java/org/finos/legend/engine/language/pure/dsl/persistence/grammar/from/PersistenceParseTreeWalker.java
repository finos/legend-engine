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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import java.util.ArrayList;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.context.PersistenceContextParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.HelperTestAssertionGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.EmailNotifyee;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.Notifier;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.Notifyee;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.PagerDutyNotifyee;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.ConnectionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTestBatch;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.AnyVersionDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DuplicateCountDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.MaxVersionDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.NoDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.ObjectStorageSink;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.RelationalSink;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.Sink;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.FlatTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.MultiFlatTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.MultiFlatTargetPart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.TargetShape;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.TransactionScope;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdAndDateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.DateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.SourceSpecifiesInAndOutDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.SourceSpecifiesInDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.TransactionDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PersistenceParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final List<Function<TriggerSourceCode, Trigger>> triggerProcessors;
    private final PureGrammarParserContext context;

    private final PersistenceContextParseTreeWalker persistenceContextWalker;

    public PersistenceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, List<Function<TriggerSourceCode, Trigger>> triggerProcessors, PersistenceContextParseTreeWalker persistenceContextWalker, PureGrammarParserContext context)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.triggerProcessors = triggerProcessors;
        this.context = context;
        this.persistenceContextWalker = persistenceContextWalker;
    }

    public void visit(PersistenceParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.elementDefinition().stream().map(this::visitElement).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private PackageableElement visitElement(PersistenceParserGrammar.ElementDefinitionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        if (ctx.persistence() != null)
        {
            return visitPersistence(ctx.persistence());
        }
        else if (ctx.context() != null)
        {
            return this.persistenceContextWalker.visitPersistenceContext(ctx.context());
        }
        throw new EngineException("Unrecognized element", sourceInformation, EngineErrorType.PARSER);
    }

    /**********
     * persistence
     **********/

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
        persistence.trigger = visitTriggerSpecification(triggerContext.triggerSpecification());

        // service
        PersistenceParserGrammar.ServiceContext serviceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.service(), "service", persistence.sourceInformation);
        persistence.service = PureGrammarParserUtility.fromQualifiedName(serviceContext.qualifiedName().packagePath() == null ? Collections.emptyList() : serviceContext.qualifiedName().packagePath().identifier(), serviceContext.qualifiedName().identifier());

        // persister
        PersistenceParserGrammar.PersisterContext persisterContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persister(), "persister", persistence.sourceInformation);
        persistence.persister = visitPersister(persisterContext);

        // notifier
        PersistenceParserGrammar.NotifierContext notifierContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.notifier(), "notifier", persistence.sourceInformation);
        persistence.notifier = notifierContext == null ? new Notifier() : visitNotifier(notifierContext);

        // test
        PersistenceParserGrammar.TestsContext persistenceTestsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.tests(), "tests", persistence.sourceInformation);
        persistence.tests = persistenceTestsContext == null ? null : ListIterate.collect(persistenceTestsContext.test(), this::visitPersistenceTest);

        return persistence;
    }

    /**********
     * trigger
     **********/

    private Trigger visitTriggerSpecification(PersistenceParserGrammar.TriggerSpecificationContext ctx)
    {
        StringBuilder text = new StringBuilder();
        PersistenceParserGrammar.TriggerValueContext triggerValueContext = ctx.triggerValue();
        if (triggerValueContext != null)
        {
            for (PersistenceParserGrammar.TriggerValueContentContext fragment : triggerValueContext.triggerValueContent())
            {
                text.append(fragment.getText());
            }
            String textToParse = text.length() > 0 ? text.substring(0, text.length() - 2) : text.toString();

            // prepare island grammar walker source information
            int startLine = triggerValueContext.ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + triggerValueContext.ISLAND_OPEN().getSymbol().getCharPositionInLine() + triggerValueContext.ISLAND_OPEN().getSymbol().getText().length();
            ParseTreeWalkerSourceInformation triggerValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation triggerValueSourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            TriggerSourceCode sourceCode = new TriggerSourceCode(textToParse, ctx.triggerType().getText(), triggerValueSourceInformation, triggerValueWalkerSourceInformation);
            return IPersistenceParserExtension.process(sourceCode, triggerProcessors);
        }
        else
        {
            SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            TriggerSourceCode sourceCode = new TriggerSourceCode(text.toString(), ctx.triggerType().getText(), sourceInformation, walkerSourceInformation);
            return IPersistenceParserExtension.process(sourceCode, triggerProcessors);
        }
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
        StreamingPersister persister = new StreamingPersister();
        persister.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // sink
        PersistenceParserGrammar.PersisterSinkContext persisterSinkContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persisterSink(), "sink", persister.sourceInformation);
        persister.sink = visitSink(persisterSinkContext, persister.sourceInformation);

        return persister;
    }

    private BatchPersister visitBatchPersister(PersistenceParserGrammar.BatchPersisterContext ctx)
    {
        BatchPersister persister = new BatchPersister();
        persister.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // sink
        PersistenceParserGrammar.PersisterSinkContext persisterSinkContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persisterSink(), "sink", persister.sourceInformation);
        persister.sink = visitSink(persisterSinkContext, persister.sourceInformation);

        // target shape
        PersistenceParserGrammar.TargetShapeContext targetShapeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetShape(), "targetShape", persister.sourceInformation);
        persister.targetShape = visitTargetShape(targetShapeContext);

        // ingest mode
        PersistenceParserGrammar.IngestModeContext ingestModeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.ingestMode(), "ingestMode", persister.sourceInformation);
        persister.ingestMode = visitIngestMode(ingestModeContext);

        return persister;
    }

    /**********
     * notifier
     **********/

    private Notifier visitNotifier(PersistenceParserGrammar.NotifierContext ctx)
    {
        Notifier notifier = new Notifier();
        notifier.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // notifyees
        PersistenceParserGrammar.NotifyeesContext notifyeesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.notifyees(), "notifyees", notifier.sourceInformation);
        notifier.notifyees = ListIterate.collect(notifyeesContext.notifyee(), this::visitNotifyee);

        return notifier;
    }

    private Notifyee visitNotifyee(PersistenceParserGrammar.NotifyeeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.emailNotifyee() != null)
        {
            return visitEmailNotifyee(ctx.emailNotifyee());
        }
        else if (ctx.pagerDutyNotifyee() != null)
        {
            return visitPagerDutyNotifyee(ctx.pagerDutyNotifyee());
        }
        throw new EngineException("Unrecognized notifyee", sourceInformation, EngineErrorType.PARSER);
    }

    private EmailNotifyee visitEmailNotifyee(PersistenceParserGrammar.EmailNotifyeeContext ctx)
    {
        EmailNotifyee notifyee = new EmailNotifyee();
        notifyee.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // address
        PersistenceParserGrammar.EmailAddressContext emailAddressContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.emailAddress(), "address", notifyee.sourceInformation);
        notifyee.address = visitEmailAddress(emailAddressContext);

        return notifyee;
    }

    private PagerDutyNotifyee visitPagerDutyNotifyee(PersistenceParserGrammar.PagerDutyNotifyeeContext ctx)
    {
        PagerDutyNotifyee notifyee = new PagerDutyNotifyee();
        notifyee.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // url
        PersistenceParserGrammar.PagerDutyUrlContext pagerDutyUrlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.pagerDutyUrl(), "url", notifyee.sourceInformation);
        notifyee.url = visitPagerDutyUrl(pagerDutyUrlContext);

        return notifyee;
    }

    private String visitEmailAddress(PersistenceParserGrammar.EmailAddressContext ctx)
    {
        return ctx != null ? PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true) : null;
    }

    private String visitPagerDutyUrl(PersistenceParserGrammar.PagerDutyUrlContext ctx)
    {
        return ctx != null ? PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true) : null;
    }

    /**********
     * test
     **********/

    private PersistenceTest visitPersistenceTest(PersistenceParserGrammar.TestContext ctx)
    {
        PersistenceTest persistenceTest = new PersistenceTest();
        persistenceTest.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        persistenceTest.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // testBatches
        PersistenceParserGrammar.PersistenceTestBatchesContext testBatchesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persistenceTestBatches(), "testBatches", persistenceTest.sourceInformation);
        if (testBatchesContext != null)
        {
            persistenceTest.testBatches = testBatchesContext.persistenceTestBatch().stream().map(testBatch -> visitTestBatch(testBatch, testBatchesContext.persistenceTestBatch().indexOf(testBatch))).collect(Collectors.toCollection(ArrayList::new));
        }

        // isTestDataFromServiceOutput
        PersistenceParserGrammar.IsTestDataFromServiceOutputContext isTestDataFromServiceOutputContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.isTestDataFromServiceOutput(), "isTestDataFromServiceOutput", persistenceTest.sourceInformation);
        if (isTestDataFromServiceOutputContext != null)
        {
            persistenceTest.isTestDataFromServiceOutput = Boolean.parseBoolean(PureGrammarParserUtility.fromIdentifier(isTestDataFromServiceOutputContext.identifier()));
        }

        return persistenceTest;
    }

    private PersistenceTestBatch visitTestBatch(PersistenceParserGrammar.PersistenceTestBatchContext ctx, int index)
    {
        PersistenceTestBatch testBatch = new PersistenceTestBatch();
        testBatch.batchId = index;
        testBatch.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        testBatch.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // testData
        PersistenceParserGrammar.PersistenceTestDataContext testDataContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.persistenceTestData(), "data", testBatch.sourceInformation);
        if (testDataContext == null)
        {
            throw new EngineException("TestData cannot be empty or null within Persistence TestBatch", testBatch.sourceInformation, EngineErrorType.PARSER);
        }
        testBatch.testData = visitPersistenceTestData(testDataContext);

        // assert
        if (ctx.persistenceTestBatchAssert() == null)
        {
            throw new EngineException("Assert cannot be null within Persistence TestBatch", testBatch.sourceInformation, EngineErrorType.PARSER);
        }
        PersistenceParserGrammar.PersistenceTestDataContext testAssertsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.persistenceTestBatchAssert(), "asserts", testBatch.sourceInformation);
        testBatch.assertions = ListIterate.collect(testAssertsContext.persistenceTestAssert(), this::visitPersistenceTestAssert);

        return testBatch;
    }

    private TestData visitPersistenceTestData(PersistenceParserGrammar.PersistenceTestDataContext ctx)
    {
        TestData testData = new TestData();

        testData.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        PersistenceParserGrammar.PersistenceTestConnectionDataContext testConnectionDataContext = ctx.persistenceTestConnectionData();
        if (testConnectionDataContext == null)
        {
            throw new EngineException("TestConnectionData cannot be empty or null within Persistence TestBatch TestData", testData.sourceInformation, EngineErrorType.PARSER);
        }
        testData.connection = visitPersistenceTestConnectionData(testConnectionDataContext);

        return testData;
    }

    private ConnectionTestData visitPersistenceTestConnectionData(PersistenceParserGrammar.PersistenceTestConnectionDataContext ctx)
    {
        ConnectionTestData connectionData = new ConnectionTestData();

        connectionData.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        connectionData.data = HelperEmbeddedDataGrammarParser.parseEmbeddedData(ctx.embeddedData(), this.walkerSourceInformation, this.context.getPureGrammarParserExtensions());

        return connectionData;
    }

    private TestAssertion visitPersistenceTestAssert(PersistenceParserGrammar.PersistenceTestAssertContext ctx)
    {
        TestAssertion testAssertion = HelperTestAssertionGrammarParser.parseTestAssertion(ctx.testAssertion(), this.walkerSourceInformation, this.context.getPureGrammarParserExtensions());
        testAssertion.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        return testAssertion;
    }

    /**********
     * sink
     **********/

    private Sink visitSink(PersistenceParserGrammar.PersisterSinkContext ctx, SourceInformation sourceInformation)
    {
        if (ctx.relationalSink() != null)
        {
            return visitRelationalSink(ctx.relationalSink());
        }
        else if (ctx.objectStorageSink() != null)
        {
            return visitObjectStorageSink(ctx.objectStorageSink());
        }
        throw new EngineException("Unrecognized sink", sourceInformation, EngineErrorType.PARSER);
    }

    private RelationalSink visitRelationalSink(PersistenceParserGrammar.RelationalSinkContext ctx)
    {
        RelationalSink sink = new RelationalSink();
        sink.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // database
        PersistenceParserGrammar.SinkDatabaseContext sinkDatabaseContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sinkDatabase(), "database", sink.sourceInformation);
        sink.database = visitDatabasePointer(sinkDatabaseContext, sink.sourceInformation);

        return sink;
    }

    private ObjectStorageSink visitObjectStorageSink(PersistenceParserGrammar.ObjectStorageSinkContext ctx)
    {
        ObjectStorageSink sink = new ObjectStorageSink();
        sink.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // binding
        PersistenceParserGrammar.SinkBindingContext sinkBindingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sinkBinding(), "binding", sink.sourceInformation);
        sink.binding = visitBindingPointer(sinkBindingContext, sink.sourceInformation);

        return sink;
    }

    /**********
     * database
     **********/

    private String visitDatabasePointer(PersistenceParserGrammar.SinkDatabaseContext ctx, SourceInformation sourceInformation)
    {
        return PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
    }

    /**********
     * binding
     **********/

    private String visitBindingPointer(PersistenceParserGrammar.SinkBindingContext ctx, SourceInformation sourceInformation)
    {
        return PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
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
        throw new EngineException("Unrecognized target", sourceInformation, EngineErrorType.PARSER);
    }

    private FlatTarget visitFlatTarget(PersistenceParserGrammar.FlatTargetShapeContext ctx)
    {
        FlatTarget targetShape = new FlatTarget();
        targetShape.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // model class
        PersistenceParserGrammar.TargetModelClassContext targetModelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelClass(), "modelClass", targetShape.sourceInformation);
        targetShape.modelClass = visitModelClass(targetModelClassContext);

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetName(), "targetName", targetShape.sourceInformation);
        targetShape.targetName = visitTargetName(targetNameContext);

        // partition fields (optional)
        PersistenceParserGrammar.PartitionFieldsContext partitionFieldsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.partitionFields(), "partitionFields", targetShape.sourceInformation);
        targetShape.partitionFields = partitionFieldsContext != null ? visitPartitionFields(partitionFieldsContext) : Collections.emptyList();

        // deduplication strategy (optional)
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.deduplicationStrategy(), "deduplicationStrategy", targetShape.sourceInformation);
        targetShape.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        return targetShape;
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
        PersistenceParserGrammar.TargetPartsContext targetPartsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetParts(), "parts", targetShape.sourceInformation);
        targetShape.parts = ListIterate.collect(targetPartsContext.targetPart(), this::visitMultiFlatTargetPart);

        return targetShape;
    }

    private MultiFlatTargetPart visitMultiFlatTargetPart(PersistenceParserGrammar.TargetPartContext ctx)
    {
        MultiFlatTargetPart multiFlatTargetPart = new MultiFlatTargetPart();
        multiFlatTargetPart.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // model property
        PersistenceParserGrammar.TargetModelPropertyContext targetModelPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetModelProperty(), "modelProperty", multiFlatTargetPart.sourceInformation);
        multiFlatTargetPart.modelProperty = PureGrammarParserUtility.fromIdentifier(targetModelPropertyContext.identifier());

        // target name
        PersistenceParserGrammar.TargetNameContext targetNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.targetName(), "targetName", multiFlatTargetPart.sourceInformation);
        multiFlatTargetPart.targetName = visitTargetName(targetNameContext);

        // partition fields (optional)
        PersistenceParserGrammar.PartitionFieldsContext partitionFieldsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.partitionFields(), "partitionFields", multiFlatTargetPart.sourceInformation);
        multiFlatTargetPart.partitionFields = partitionFieldsContext != null ? visitPartitionFields(partitionFieldsContext) : Collections.emptyList();

        // deduplication strategy (optional)
        PersistenceParserGrammar.DeduplicationStrategyContext deduplicationStrategyContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.deduplicationStrategy(), "deduplicationStrategy", multiFlatTargetPart.sourceInformation);
        multiFlatTargetPart.deduplicationStrategy = visitDeduplicationStrategy(deduplicationStrategyContext);

        return multiFlatTargetPart;
    }

    private String visitTargetName(PersistenceParserGrammar.TargetNameContext ctx)
    {
        return ctx != null ? PureGrammarParserUtility.fromGrammarString(ctx.STRING().getText(), true) : null;
    }

    private List<String> visitPartitionFields(PersistenceParserGrammar.PartitionFieldsContext ctx)
    {
        List<PersistenceParserGrammar.IdentifierContext> identifierContexts = ctx.identifier();
        return Lists.immutable.ofAll(identifierContexts).collect(PureGrammarParserUtility::fromIdentifier).castToList();
    }

    private String visitModelClass(PersistenceParserGrammar.TargetModelClassContext ctx)
    {
        PersistenceParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        return PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
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
        else if (ctx.duplicateCountDeduplicationStrategy() != null)
        {
            return visitDuplicateCountDeduplicationStrategy(ctx.duplicateCountDeduplicationStrategy());
        }
        throw new EngineException("Unrecognized deduplication strategy", sourceInformation, EngineErrorType.PARSER);
    }

    private MaxVersionDeduplicationStrategy visitMaxVersionDeduplicationStrategy(PersistenceParserGrammar.MaxVersionDeduplicationStrategyContext ctx)
    {
        MaxVersionDeduplicationStrategy deduplicationStrategy = new MaxVersionDeduplicationStrategy();
        deduplicationStrategy.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // version field
        PersistenceParserGrammar.DeduplicationMaxVersionFieldContext versionFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationMaxVersionField(), "versionField", deduplicationStrategy.sourceInformation);
        deduplicationStrategy.versionField = PureGrammarParserUtility.fromIdentifier(versionFieldContext.identifier());

        return deduplicationStrategy;
    }

    private DuplicateCountDeduplicationStrategy visitDuplicateCountDeduplicationStrategy(PersistenceParserGrammar.DuplicateCountDeduplicationStrategyContext ctx)
    {
        DuplicateCountDeduplicationStrategy deduplicationStrategy = new DuplicateCountDeduplicationStrategy();
        deduplicationStrategy.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // duplicate count name
        PersistenceParserGrammar.DeduplicationDuplicateCountNameContext duplicateCountNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deduplicationDuplicateCountName(), "duplicateCountName", deduplicationStrategy.sourceInformation);
        deduplicationStrategy.duplicateCountName = PureGrammarParserUtility.fromGrammarString(duplicateCountNameContext.STRING().getText(), true);

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
        appendOnly.filterDuplicates = visitFilterDuplicates(filterDuplicatesContext);

        return appendOnly;
    }

    private boolean visitFilterDuplicates(PersistenceParserGrammar.FilterDuplicatesContext filterDuplicatesContext)
    {
        if (filterDuplicatesContext.TRUE() != null)
        {
            return true;
        }
        else if (filterDuplicatesContext.FALSE() != null)
        {
            return false;
        }
        throw new EngineException("Unrecognized value for filter duplicates", walkerSourceInformation.getSourceInformation(filterDuplicatesContext), EngineErrorType.PARSER);
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
        throw new EngineException("Unrecognized merge strategy", sourceInformation, EngineErrorType.PARSER);
    }

    private DeleteIndicatorMergeStrategy visitDeleteIndicatorMergeScheme(PersistenceParserGrammar.DeleteIndicatorMergeStrategyContext ctx)
    {
        DeleteIndicatorMergeStrategy mergeStrategy = new DeleteIndicatorMergeStrategy();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // delete field
        PersistenceParserGrammar.MergeStrategyDeleteFieldContext deleteFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mergeStrategyDeleteField(), "deleteField", sourceInformation);
        mergeStrategy.deleteField = PureGrammarParserUtility.fromIdentifier(deleteFieldContext.identifier());

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
        throw new EngineException("Unrecognized auditing", sourceInformation, EngineErrorType.PARSER);
    }

    private DateTimeAuditing visitBatchDateTimeAuditScheme(PersistenceParserGrammar.DateTimeAuditingContext ctx)
    {
        DateTimeAuditing auditing = new DateTimeAuditing();
        auditing.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // date time field name
        PersistenceParserGrammar.DateTimeNameContext dateTimeNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeName(), "dateTimeName", auditing.sourceInformation);
        auditing.dateTimeName = PureGrammarParserUtility.fromGrammarString(dateTimeNameContext.STRING().getText(), true);

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
        throw new EngineException("Unrecognized transaction milestoning", sourceInformation, EngineErrorType.PARSER);
    }

    private BatchIdTransactionMilestoning visitBatchIdTransactionMilestoning(PersistenceParserGrammar.BatchIdTransactionMilestoningContext ctx)
    {
        BatchIdTransactionMilestoning milestoning = new BatchIdTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in field name
        PersistenceParserGrammar.BatchIdInNameContext batchIdInNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInName(), "batchIdInName", milestoning.sourceInformation);
        milestoning.batchIdInName = PureGrammarParserUtility.fromGrammarString(batchIdInNameContext.STRING().getText(), true);

        // batchId out field name
        PersistenceParserGrammar.BatchIdOutNameContext batchIdOutNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutName(), "batchIdOutName", milestoning.sourceInformation);
        milestoning.batchIdOutName = PureGrammarParserUtility.fromGrammarString(batchIdOutNameContext.STRING().getText(), true);

        return milestoning;
    }

    private DateTimeTransactionMilestoning visitDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.DateTimeTransactionMilestoningContext ctx)
    {
        DateTimeTransactionMilestoning milestoning = new DateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime in field name
        PersistenceParserGrammar.DateTimeInNameContext dateTimeInNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInName(), "dateTimeInName", milestoning.sourceInformation);
        milestoning.dateTimeInName = PureGrammarParserUtility.fromGrammarString(dateTimeInNameContext.STRING().getText(), true);

        // datetime out field name
        PersistenceParserGrammar.DateTimeOutNameContext dateTimeOutNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutName(), "dateTimeOutName", milestoning.sourceInformation);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromGrammarString(dateTimeOutNameContext.STRING().getText(), true);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromGrammarString(dateTimeOutNameContext.STRING().getText(), true);

        // transaction derivation (optional)
        PersistenceParserGrammar.TransactionDerivationContext transactionDerivationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.transactionDerivation(), "derivation", milestoning.sourceInformation);
        milestoning.derivation = transactionDerivationContext == null ? null : visitTransactionDerivation(transactionDerivationContext);

        return milestoning;
    }

    private BatchIdAndDateTimeTransactionMilestoning visitBatchIdAndDateTimeTransactionMilestoningScheme(PersistenceParserGrammar.BothTransactionMilestoningContext ctx)
    {
        BatchIdAndDateTimeTransactionMilestoning milestoning = new BatchIdAndDateTimeTransactionMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // batchId in field name
        PersistenceParserGrammar.BatchIdInNameContext batchIdInNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdInName(), "batchIdInName", milestoning.sourceInformation);
        milestoning.batchIdInName = PureGrammarParserUtility.fromGrammarString(batchIdInNameContext.STRING().getText(), true);

        // batchId out field name
        PersistenceParserGrammar.BatchIdOutNameContext batchIdOutNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.batchIdOutName(), "batchIdOutName", milestoning.sourceInformation);
        milestoning.batchIdOutName = PureGrammarParserUtility.fromGrammarString(batchIdOutNameContext.STRING().getText(), true);

        // datetime in field name
        PersistenceParserGrammar.DateTimeInNameContext dateTimeInNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeInName(), "dateTimeInName", milestoning.sourceInformation);
        milestoning.dateTimeInName = PureGrammarParserUtility.fromGrammarString(dateTimeInNameContext.STRING().getText(), true);

        // datetime out field name
        PersistenceParserGrammar.DateTimeOutNameContext dateTimeOutNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeOutName(), "dateTimeOutName", milestoning.sourceInformation);
        milestoning.dateTimeOutName = PureGrammarParserUtility.fromGrammarString(dateTimeOutNameContext.STRING().getText(), true);

        // transaction derivation (optional)
        PersistenceParserGrammar.TransactionDerivationContext transactionDerivationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.transactionDerivation(), "derivation", milestoning.sourceInformation);
        milestoning.derivation = transactionDerivationContext == null ? null : visitTransactionDerivation(transactionDerivationContext);

        return milestoning;
    }

    // transaction derivation

    private TransactionDerivation visitTransactionDerivation(PersistenceParserGrammar.TransactionDerivationContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.sourceSpecifiesInTransactionDerivation() != null)
        {
            return visitSourceSpecifiesInDate(ctx.sourceSpecifiesInTransactionDerivation());
        }
        else if (ctx.sourceSpecifiesInOutTransactionDerivation() != null)
        {
            return visitSourceSpecifiesInOutDate(ctx.sourceSpecifiesInOutTransactionDerivation());
        }
        throw new EngineException("Unrecognized transaction derivation", sourceInformation, EngineErrorType.PARSER);
    }

    private TransactionDerivation visitSourceSpecifiesInDate(PersistenceParserGrammar.SourceSpecifiesInTransactionDerivationContext ctx)
    {
        SourceSpecifiesInDateTime transactionDerivation = new SourceSpecifiesInDateTime();
        transactionDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time in field
        PersistenceParserGrammar.TransactionDerivationInFieldContext transactionDerivationInFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDerivationInField(), "sourceDateTimeInField", transactionDerivation.sourceInformation);
        transactionDerivation.sourceDateTimeInField = PureGrammarParserUtility.fromIdentifier(transactionDerivationInFieldContext.identifier());

        return transactionDerivation;
    }

    private TransactionDerivation visitSourceSpecifiesInOutDate(PersistenceParserGrammar.SourceSpecifiesInOutTransactionDerivationContext ctx)
    {
        SourceSpecifiesInAndOutDateTime transactionDerivation = new SourceSpecifiesInAndOutDateTime();
        transactionDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time in field
        PersistenceParserGrammar.TransactionDerivationInFieldContext transactionDerivationInFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDerivationInField(), "sourceDateTimeInField", transactionDerivation.sourceInformation);
        transactionDerivation.sourceDateTimeInField = PureGrammarParserUtility.fromIdentifier(transactionDerivationInFieldContext.identifier());

        // source date time out field
        PersistenceParserGrammar.TransactionDerivationOutFieldContext transactionDerivationOutFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transactionDerivationOutField(), "sourceDateTimeOutField", transactionDerivation.sourceInformation);
        transactionDerivation.sourceDateTimeOutField = PureGrammarParserUtility.fromIdentifier(transactionDerivationOutFieldContext.identifier());

        return transactionDerivation;
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
        throw new EngineException("Unrecognized validity milestoning", sourceInformation, EngineErrorType.PARSER);
    }

    private DateTimeValidityMilestoning visitDateTimeValidityMilestoningScheme(PersistenceParserGrammar.DateTimeValidityMilestoningContext ctx)
    {
        DateTimeValidityMilestoning milestoning = new DateTimeValidityMilestoning();
        milestoning.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // datetime from field name
        PersistenceParserGrammar.DateTimeFromNameContext validityDateTimeFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeFromName(), "dateTimeFromName", milestoning.sourceInformation);
        milestoning.dateTimeFromName = PureGrammarParserUtility.fromGrammarString(validityDateTimeFromPropertyContext.STRING().getText(), true);

        // datetime thru field name
        PersistenceParserGrammar.DateTimeThruNameContext validityDateTimeThruPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.dateTimeThruName(), "dateTimeThruName", milestoning.sourceInformation);
        milestoning.dateTimeThruName = PureGrammarParserUtility.fromGrammarString(validityDateTimeThruPropertyContext.STRING().getText(), true);

        // validity derivation
        PersistenceParserGrammar.ValidityDerivationContext validityDerivationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivation(), "derivation", milestoning.sourceInformation);
        milestoning.derivation = visitValidityDerivation(validityDerivationContext);

        return milestoning;
    }

    // validity derivation

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

        // source date time from field
        PersistenceParserGrammar.ValidityDerivationFromFieldContext validityDerivationFromPropertyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationFromField(), "sourceDateTimeFromField", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromField = PureGrammarParserUtility.fromIdentifier(validityDerivationFromPropertyContext.identifier());

        return validityDerivation;
    }

    private ValidityDerivation visitSourceSpecifiesFromThruDate(PersistenceParserGrammar.SourceSpecifiesFromThruValidityDerivationContext ctx)
    {
        SourceSpecifiesFromAndThruDateTime validityDerivation = new SourceSpecifiesFromAndThruDateTime();
        validityDerivation.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // source date time from field
        PersistenceParserGrammar.ValidityDerivationFromFieldContext validityDerivationFromFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationFromField(), "sourceDateTimeFromField", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeFromField = PureGrammarParserUtility.fromIdentifier(validityDerivationFromFieldContext.identifier());

        // source date time thru field
        PersistenceParserGrammar.ValidityDerivationThruFieldContext validityDerivationThruFieldContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.validityDerivationThruField(), "sourceDateTimeThruField", validityDerivation.sourceInformation);
        validityDerivation.sourceDateTimeThruField = PureGrammarParserUtility.fromIdentifier(validityDerivationThruFieldContext.identifier());

        return validityDerivation;
    }
}
