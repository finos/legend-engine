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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourcePartition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceStatus;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionKeyType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.compiler.Context;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

public class MasteryParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    private final DomainParser domainParser;

    public MasteryParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, DomainParser domainParser)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.domainParser = domainParser;
    }

    public void visit(MasteryParserGrammar.DefinitionContext ctx)
    {
        ctx.mastery().stream().map(this::visitMastery).peek(e -> this.section.elements.add((e.getPath()))).forEach(this.elementConsumer);
    }

    private MasterRecordDefinition visitMastery(MasteryParserGrammar.MasteryContext ctx)
    {
        MasterRecordDefinition masterRecordDefinition = new MasterRecordDefinition();
        masterRecordDefinition.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        masterRecordDefinition._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        masterRecordDefinition.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        //modelClass
        MasteryParserGrammar.ModelClassContext modelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelClass(), "modelClass", walkerSourceInformation.getSourceInformation(ctx));
        masterRecordDefinition.modelClass = visitModelClass(modelClassContext);

        //IdentityResolution
        MasteryParserGrammar.IdentityResolutionContext identityResolutionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.identityResolution(), "identityResolution", masterRecordDefinition.sourceInformation);
        masterRecordDefinition.identityResolution = visitIdentityResolution(identityResolutionContext);

        //Master Record Sources
        MasteryParserGrammar.RecordSourcesContext recordSourcesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.recordSources(), "recordSources", masterRecordDefinition.sourceInformation);
        masterRecordDefinition.sources = ListIterate.collect(recordSourcesContext.recordSource(), this::visitRecordSource);

        return masterRecordDefinition;
    }

    /*
     * Record Sources
     */

    private RecordSource visitRecordSource(MasteryParserGrammar.RecordSourceContext ctx)
    {
        RecordSource source = new RecordSource();
        source.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        MasteryParserGrammar.IdContext idContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.id(), "id", source.sourceInformation);
        source.id = PureGrammarParserUtility.fromGrammarString(idContext.STRING().getText(), true);

        MasteryParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.description(), "description", source.sourceInformation);
        source.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);

        MasteryParserGrammar.RecordStatusContext statusContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.recordStatus(), "status", source.sourceInformation);
        source.status = visitRecordStatus(statusContext);

        MasteryParserGrammar.SequentialDataContext sequentialDataContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.sequentialData(), "sequentialData", source.sourceInformation);
        source.sequentialData = evaluateBoolean(sequentialDataContext, (sequentialDataContext != null ? sequentialDataContext.boolean_value() : null), null);

        MasteryParserGrammar.StagedLoadContext stagedLoadContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.stagedLoad(), "stagedLoad", source.sourceInformation);
        source.stagedLoad = evaluateBoolean(stagedLoadContext, (stagedLoadContext != null ? stagedLoadContext.boolean_value() : null), null);

        MasteryParserGrammar.CreatePermittedContext createPermittedContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.createPermitted(), "createPermitted", source.sourceInformation);
        source.createPermitted = evaluateBoolean(createPermittedContext, (createPermittedContext != null ? createPermittedContext.boolean_value() : null), null);

        MasteryParserGrammar.CreateBlockedExceptionContext createBlockedExceptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.createBlockedException(), "createBlockedException", source.sourceInformation);
        source.createBlockedException = evaluateBoolean(createBlockedExceptionContext, (createBlockedExceptionContext != null ? createBlockedExceptionContext.boolean_value() : null), null);

        //Tags
        MasteryParserGrammar.TagsContext tagsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.tags(), "tags", source.sourceInformation);
        if (tagsContext != null)
        {
            ListIterator stringIterator = tagsContext.STRING().listIterator();
            while (stringIterator.hasNext())
            {
                source.tags.add(PureGrammarParserUtility.fromGrammarString(stringIterator.next().toString(), true));
            }
        }

        //Services
        MasteryParserGrammar.ParseServiceContext parseServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.parseService(), "parseService", source.sourceInformation);
        if (parseServiceContext != null)
        {
            source.parseService = PureGrammarParserUtility.fromQualifiedName(parseServiceContext.qualifiedName().packagePath() == null ? Collections.emptyList() : parseServiceContext.qualifiedName().packagePath().identifier(), parseServiceContext.qualifiedName().identifier());
        }

        MasteryParserGrammar.TransformServiceContext transformServiceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transformService(), "transformService", source.sourceInformation);
        source.transformService = PureGrammarParserUtility.fromQualifiedName(transformServiceContext.qualifiedName().packagePath() == null ? Collections.emptyList() : transformServiceContext.qualifiedName().packagePath().identifier(), transformServiceContext.qualifiedName().identifier());

        //Partitions
        MasteryParserGrammar.SourcePartitionsContext partitionsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourcePartitions(), "partitions", source.sourceInformation);
        source.partitions = ListIterate.collect(partitionsContext.sourcePartiton(), this::visitRecordSourcePartition);

        return source;
    }

    private Boolean evaluateBoolean(ParserRuleContext context, MasteryParserGrammar.Boolean_valueContext booleanValueContext, Boolean defaultVal)
    {
        Boolean result = null;
        if (context == null)
        {
            result = defaultVal;
        }
        else if (booleanValueContext.TRUE() != null)
        {
            result = Boolean.TRUE;
        }
        else if (booleanValueContext.FALSE() != null)
        {
            result = Boolean.FALSE;
        }
        else
        {
            result = defaultVal;
        }
        return result;
    }

    private RecordSourceStatus visitRecordStatus(MasteryParserGrammar.RecordStatusContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.RECORD_SOURCE_STATUS_DEVELOPMENT() != null)
        {
            return RecordSourceStatus.Development;
        }
        if (ctx.RECORD_SOURCE_STATUS_TEST_ONLY() != null)
        {
            return RecordSourceStatus.TestOnly;
        }
        if (ctx.RECORD_SOURCE_STATUS_PRODUCTION() != null)
        {
            return RecordSourceStatus.Production;
        }
        if (ctx.RECORD_SOURCE_STATUS_DORMANT() != null)
        {
            return RecordSourceStatus.Dormant;
        }
        if (ctx.RECORD_SOURCE_STATUS_DECOMMINISSIONED() != null)
        {
            return RecordSourceStatus.Decommissioned;
        }

        throw new EngineException("Unrecognized record status", sourceInformation, EngineErrorType.PARSER);
    }

    private RecordSourcePartition visitRecordSourcePartition(MasteryParserGrammar.SourcePartitonContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        RecordSourcePartition partition = new RecordSourcePartition();

        MasteryParserGrammar.IdContext idContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.id(), "id", sourceInformation);
        partition.id = PureGrammarParserUtility.fromGrammarString(idContext.STRING().getText(), true);

        MasteryParserGrammar.TagsContext tagsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.tags(), "tags", sourceInformation);
        if (tagsContext != null)
        {
            ListIterator stringIterator = tagsContext.STRING().listIterator();
            while (stringIterator.hasNext())
            {
                partition.tags.add(PureGrammarParserUtility.fromGrammarString(stringIterator.next().toString(), true));
            }
        }
        return partition;
    }

    /*
     * Identity and Resolution
     */
    private IdentityResolution visitIdentityResolution(MasteryParserGrammar.IdentityResolutionContext ctx)
    {
        IdentityResolution identityResolution = new IdentityResolution();
        identityResolution.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        //modelClass
        MasteryParserGrammar.ModelClassContext modelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelClass(), "modelClass", identityResolution.sourceInformation);
        identityResolution.modelClass = visitModelClass(modelClassContext);

        //queries
        MasteryParserGrammar.ResolutionQueriesContext resolutionQueriesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.resolutionQueries(), "resolutionQueries", identityResolution.sourceInformation);
        identityResolution.resolutionQueries = ListIterate.collect(resolutionQueriesContext.resolutionQuery(), this::visitResolutionQuery);

        return identityResolution;
    }

    private ResolutionQuery visitResolutionQuery(MasteryParserGrammar.ResolutionQueryContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        ResolutionQuery resolutionQuery = new ResolutionQuery();

        //queries
        resolutionQuery.queries = (List<Lambda>) ListIterate.flatCollect(ctx.queryExpressions(), this::visitQueryExpressions);

        //keyType
        MasteryParserGrammar.ResolutionQueryKeyTypeContext resolutionQueryKeyTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.resolutionQueryKeyType(), "keyType", sourceInformation);
        resolutionQuery.keyType = visitResolutionKeyType(resolutionQueryKeyTypeContext);

        //precedence - Field 'precedence' should be specified only once
        MasteryParserGrammar.ResolutionQueryPrecedenceContext resolutionQueryPrecedenceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.resolutionQueryPrecedence(), "precedence", sourceInformation);
        resolutionQuery.precedence = Integer.parseInt(resolutionQueryPrecedenceContext.INTEGER().getText());

        return resolutionQuery;
    }

    private List<Lambda> visitQueryExpressions(MasteryParserGrammar.QueryExpressionsContext ctx)
    {

        List<MasteryParserGrammar.LambdaFunctionContext> lambdaFunctionContexts = ctx.lambdaFunction();
        return ListIterate.collect(lambdaFunctionContexts, this::visitLambda).toList();
    }

    private Lambda visitLambda(MasteryParserGrammar.LambdaFunctionContext ctx)
    {
        Lambda lambda = domainParser.parseLambda(ctx.getText(), "", 0, 0, true);
        return lambda;
    }


    private String visitModelClass(MasteryParserGrammar.ModelClassContext ctx)
    {
        MasteryParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        return PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
    }

    private ResolutionKeyType visitResolutionKeyType(MasteryParserGrammar.ResolutionQueryKeyTypeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.RESOLUTION_QUERY_KEY_TYPE_GENERATED_PRIMARY_KEY() != null)
        {
            return ResolutionKeyType.GeneratedPrimaryKey;
        }
        if (ctx.RESOLUTION_QUERY_KEY_TYPE_SUPPLIED_PRIMARY_KEY() != null)
        {
            return ResolutionKeyType.SuppliedPrimaryKey;
        }
        if (ctx.RESOLUTION_QUERY_KEY_TYPE_ALTERNATE_KEY() != null)
        {
            return ResolutionKeyType.AlternateKey;
        }
        if (ctx.RESOLUTION_QUERY_KEY_TYPE_OPTIONAL() != null)
        {
            return ResolutionKeyType.Optional;
        }

        throw new EngineException("Unrecognized resolution key type", sourceInformation, EngineErrorType.PARSER);
    }
}
