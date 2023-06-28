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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourcePartition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceStatus;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionKeyType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.*;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class MasteryParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final DomainParser domainParser;


    private static final String SIMPLE_PRECEDENCE_LAMBDA = "{input: %s[1]| true}";
    private static final String PRECEDENCE_LAMBDA_WITH_FILTER = "{input: %s[1]| $input.%s}";

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
        masterRecordDefinition._package = isNull(ctx.qualifiedName().packagePath()) ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
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

        //Vendor Precedence
        MasteryParserGrammar.PrecedenceRulesContext precedenceRulesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.precedenceRules(), "precedenceRules", masterRecordDefinition.sourceInformation);
        if (nonNull(precedenceRulesContext))
        {
            Map<String, Set<String>> allUniquePrecedenceRules = new HashMap<>();
            masterRecordDefinition.precedenceRules = ListIterate.flatCollect(precedenceRulesContext.precedenceRule(), precedenceRuleContext -> visitPrecedenceRules(precedenceRuleContext, allUniquePrecedenceRules));
        }

        return masterRecordDefinition;
    }


    private List<PrecedenceRule> visitPrecedenceRules(MasteryParserGrammar.PrecedenceRuleContext ctx, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        if (nonNull(ctx.sourcePrecedenceRule()))
        {
            return visitSourcePrecedenceRule(ctx.sourcePrecedenceRule(),
                    allUniquePrecedenceRules);
        }
        else if (nonNull(ctx.deleteRule()))
        {
            return singletonList(visitDeleteRules(ctx.deleteRule(), allUniquePrecedenceRules));
        }
        else if (nonNull(ctx.createRule()))
        {
            return singletonList(visitCreateRules(ctx.createRule(), allUniquePrecedenceRules));
        }
        else if (nonNull(ctx.conditionalRule()))
        {
            return singletonList(visitConditionalRule(ctx.conditionalRule(), allUniquePrecedenceRules));
        }
        else
        {
            throw new EngineException("Unrecognized precedence rule", EngineErrorType.PARSER);
        }
    }

    private ConditionalRule visitConditionalRule(MasteryParserGrammar.ConditionalRuleContext ctx, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        ConditionalRule conditionalRule =  new ConditionalRule();
        conditionalRule.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        MasteryParserGrammar.PredicateContext predicateContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.predicate(), "predicate", conditionalRule.sourceInformation);
        conditionalRule.predicate = visitLambda(predicateContext.lambdaFunction());

        ListIterate.forEach(ctx.precedenceRuleBase(), precedenceRuleBaseContext -> visitPrecedenceRuleBase(precedenceRuleBaseContext, conditionalRule, allUniquePrecedenceRules));
        return conditionalRule;
    }

    private DeleteRule visitDeleteRules(MasteryParserGrammar.DeleteRuleContext ctx, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        DeleteRule deleteRule =  new DeleteRule();
        deleteRule.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        ListIterate.forEach(ctx.precedenceRuleBase(), precedenceRuleBaseContext -> visitPrecedenceRuleBase(precedenceRuleBaseContext, deleteRule, allUniquePrecedenceRules));

        return deleteRule;
    }

    private CreateRule visitCreateRules(MasteryParserGrammar.CreateRuleContext ctx, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        CreateRule createRule =  new CreateRule();
        createRule.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        ListIterate.forEach(ctx.precedenceRuleBase(), precedenceRuleBaseContext -> visitPrecedenceRuleBase(precedenceRuleBaseContext, createRule, allUniquePrecedenceRules));

        return createRule;
    }

    private List<PrecedenceRule> visitSourcePrecedenceRule(MasteryParserGrammar.SourcePrecedenceRuleContext ctx, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        SourcePrecedenceRule sourcePrecedenceRule = new SourcePrecedenceRule();
        sourcePrecedenceRule.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        MasteryParserGrammar.ActionContext actionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.action(), "action", sourcePrecedenceRule.sourceInformation);
        sourcePrecedenceRule.action = visitAction(actionContext.validAction());

        MasteryParserGrammar.PathContext pathContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.path(), "path", sourcePrecedenceRule.sourceInformation);
        sourcePrecedenceRule.paths = visitPath(pathContext, sourcePrecedenceRule);

        Set<String> uniqueSourcePrecedenceRules = allUniquePrecedenceRules.computeIfAbsent(sourcePrecedenceRule.getType(), (key) -> new HashSet<>());
        validateUniqueSourcePrecedenceRule(uniqueSourcePrecedenceRules, pathContext, sourcePrecedenceRule.action.name());

        Map<Long, SourcePrecedenceRule> precedenceRules = new HashMap<>();
        MasteryParserGrammar.RuleScopeContext ruleScopeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.ruleScope(), "ruleScope", sourcePrecedenceRule.sourceInformation);
        Set<String> uniqueScopes = new HashSet<>();
        ListIterate.forEach(ruleScopeContext.scope(), scopeContext ->
            visitRuleScopeWithPrecedence(scopeContext, precedenceRules, uniqueScopes, sourcePrecedenceRule)
        );
        return new ArrayList<>(precedenceRules.values());
    }

    private void validateUniqueSourcePrecedenceRule(Set<String> uniqueSourcePrecedenceRules, MasteryParserGrammar.PathContext pathContext, String action)
    {
        String path = StringUtils.deleteWhitespace(pathContext.getText());
        String pathAndAction = path + action;
        if (uniqueSourcePrecedenceRules.contains(pathAndAction))
        {
            throw new EngineException(format("Duplicate SourcePrecedenceRule found for %s and action: %s", path, action), EngineErrorType.PARSER);
        }
        uniqueSourcePrecedenceRules.add(pathAndAction);
    }

    private void visitPrecedenceRuleBase(MasteryParserGrammar.PrecedenceRuleBaseContext ctx, PrecedenceRule precedenceRule, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        MasteryParserGrammar.RuleScopeContext ruleScopeContext = ctx.ruleScope();
        if (nonNull(ruleScopeContext))
        {
            Set<String> uniqueScopes = new HashSet<>();
            precedenceRule.scopes = ListIterate.collect(ruleScopeContext.scope(), scopeContext ->
                    visitRuleScopeWithoutPrecedence(scopeContext, uniqueScopes, precedenceRule));
        }
        MasteryParserGrammar.PathContext pathContext = ctx.path();
        if (nonNull(pathContext))
        {
            precedenceRule.paths = visitPath(pathContext, precedenceRule);
            validateUniquePrecedenceRule(allUniquePrecedenceRules, pathContext, precedenceRule.getType());
        }
    }

    private void validateUniqueRuleScope(Set<String> ruleScopes, String ruleScope, PrecedenceRule precedenceRule)
    {
        if (!ruleScopes.add(ruleScope))
        {
            throw new EngineException(format("Duplicate RuleScope with %s} found for %s", ruleScope, precedenceRule.getType()));
        }
    }

    private void validateUniquePrecedenceRule(Map<String, Set<String>> allUniquePrecedenceRules, MasteryParserGrammar.PathContext pathContext, String precedenceRuleType)
    {
        Set<String> uniquePrecedenceRules = allUniquePrecedenceRules.computeIfAbsent(precedenceRuleType, (key) -> new HashSet<>());
        String path = StringUtils.deleteWhitespace(pathContext.getText());
        if (uniquePrecedenceRules.contains(path))
        {
            throw new EngineException(format("Duplicate %s found for %s", precedenceRuleType, path), EngineErrorType.PARSER);
        }
        uniquePrecedenceRules.add(path);
    }

    private List<PropertyPath> visitPath(MasteryParserGrammar.PathContext ctx, PrecedenceRule precedenceRule)
    {
        List<PropertyPath> propertyPaths = new ArrayList<>();
        precedenceRule.masterRecordFilter = visitMasterRecordFilter(ctx.masterRecordFilter());
        if (nonNull(ctx.pathExtension()))
        {
            propertyPaths.addAll(ListIterate.collect(ctx.pathExtension(), this::visitPathExtension));
        }
        return propertyPaths;
    }

    private Lambda visitMasterRecordFilter(MasteryParserGrammar.MasterRecordFilterContext ctx)
    {
        String qualifiedName = ctx.qualifiedName().getText();
        if (nonNull(ctx.filter()))
        {
            return visitLambdaWithFilter(qualifiedName, ctx.filter().combinedExpression());
        }
        else
        {
            return visitLambdaWithoutFilter(qualifiedName);
        }
    }

    private PropertyPath visitPathExtension(MasteryParserGrammar.PathExtensionContext ctx)
    {
        MasteryParserGrammar.SubPathContext subPathContext = ctx.subPath();
        PropertyPath propertyPath = new PropertyPath();
        propertyPath.property = subPathContext.VALID_STRING().getText();
        if (nonNull(ctx.filter()))
        {
            propertyPath.filter = visitLambdaWithFilter(propertyPath.property, ctx.filter().combinedExpression());
        }
        else
        {
            propertyPath.filter = visitLambdaWithoutFilter(propertyPath.property);
        }
        return propertyPath;
    }



    private Lambda visitLambdaWithFilter(String propertyName, MasteryParserGrammar.CombinedExpressionContext ctx)
    {
        return domainParser.parseLambda(
                format(PRECEDENCE_LAMBDA_WITH_FILTER, propertyName, ctx.getText()),
                "", 0, 0, true);
    }

    private Lambda visitLambdaWithoutFilter(String propertyName)
    {
        return domainParser.parseLambda(format(SIMPLE_PRECEDENCE_LAMBDA, propertyName),
                "", 0, 0, true);
    }

    private RuleScope visitRuleScopeWithoutPrecedence(MasteryParserGrammar.ScopeContext ctx, Set<String> uniqueScopes,PrecedenceRule precedenceRule)
    {
        validateUniqueRuleScope(uniqueScopes, ctx.validScopeType().getText(), precedenceRule);
        RuleScope recordSourceScope = visitRuleScope(ctx.validScopeType());
        if (nonNull(ctx.precedence()))
        {
            throw  new EngineException(format("Precedence is not expected on rule scopes for %s", precedenceRule.getType()), EngineErrorType.PARSER);
        }
        return recordSourceScope;
    }

    private void visitRuleScopeWithPrecedence(MasteryParserGrammar.ScopeContext ctx, Map<Long, SourcePrecedenceRule> precedenceRuleMap, Set<String> uniqueScopes, SourcePrecedenceRule sourcePrecedenceRule)
    {
        validateUniqueRuleScope(uniqueScopes, ctx.validScopeType().getText(), sourcePrecedenceRule);
        RuleScope ruleScope = visitRuleScope(ctx.validScopeType());
        if (isNull(ctx.precedence()))
        {
            throw new EngineException("Precedence is expected on all rule scope on SourcePrecedenceRule", EngineErrorType.PARSER);
        }
        long precedence = Long.parseLong(ctx.precedence().INTEGER().getText());
        if (precedence < 1)
        {
            throw new EngineException("Precedence must me a non negative number", EngineErrorType.PARSER);
        }
        sourcePrecedenceRule.precedence = precedence;
        sourcePrecedenceRule.scopes = newArrayList(ruleScope);
        SourcePrecedenceRule sourcePrecedenceRuleClone = cloneObject(sourcePrecedenceRule, new TypeReference<SourcePrecedenceRule>()
        { });
        precedenceRuleMap.merge(precedence, sourcePrecedenceRuleClone, (existingValue, newValue) ->
        {
            existingValue.scopes.add(ruleScope);
            return existingValue;
        });
    }

    private RuleScope visitRuleScope(MasteryParserGrammar.ValidScopeTypeContext ctx)
    {
        if (nonNull(ctx.recordSourceScope()))
        {
            MasteryParserGrammar.RecordSourceScopeContext recordSourceScopeContext = ctx.recordSourceScope();
            return visitRecordSourceScope(recordSourceScopeContext);
        }
        if (nonNull(ctx.dataProviderTypeScope()))
        {
            MasteryParserGrammar.DataProviderTypeScopeContext dataProviderTypeScopeContext = ctx.dataProviderTypeScope();
            return visitDataProvideTypeScope(dataProviderTypeScopeContext.validDataProviderType());
        }
        return null;
    }

    private RuleScope visitRecordSourceScope(MasteryParserGrammar.RecordSourceScopeContext ctx)
    {
        RecordSourceScope recordSourceScope = new RecordSourceScope();
        recordSourceScope.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        recordSourceScope.recordSourceId = PureGrammarParserUtility.fromIdentifier(ctx.masteryIdentifier());
        return recordSourceScope;
    }

    private RuleScope visitDataProvideTypeScope(MasteryParserGrammar.ValidDataProviderTypeContext ctx)
    {
        DataProviderTypeScope dataProviderTypeScope = new DataProviderTypeScope();
        dataProviderTypeScope.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        dataProviderTypeScope.dataProviderType = visitDataProviderType(ctx);
        return dataProviderTypeScope;
    }

    private RuleAction visitAction(MasteryParserGrammar.ValidActionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (nonNull(ctx.OVERWRITE()))
        {
            return RuleAction.Overwrite;
        }
        if (nonNull(ctx.BLOCK()))
        {
            return RuleAction.Block;
        }
        throw new EngineException("Unrecognized rule action", sourceInformation, EngineErrorType.PARSER);
    }

    private DataProviderType visitDataProviderType(MasteryParserGrammar.ValidDataProviderTypeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (nonNull(ctx.AGGREGATOR()))
        {
            return DataProviderType.Aggregator;
        }
        if (nonNull(ctx.EXCHANGE()))
        {
            return DataProviderType.Exchange;
        }
        throw new EngineException("Unrecognized Data Provider Type", sourceInformation, EngineErrorType.PARSER);
    }

    private <T> T cloneObject(T object, TypeReference<T> typeReference)
    {
        try
        {
            ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
            return mapper.readValue(mapper.writeValueAsString(object), typeReference);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    /*
     * Record Sources
     */

    private RecordSource visitRecordSource(MasteryParserGrammar.RecordSourceContext ctx)
    {
        RecordSource source = new RecordSource();
        source.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        source.id = PureGrammarParserUtility.fromIdentifier(ctx.masteryIdentifier());

        MasteryParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.description(), "description", source.sourceInformation);
        source.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);

        MasteryParserGrammar.RecordStatusContext statusContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.recordStatus(), "status", source.sourceInformation);
        source.status = visitRecordStatus(statusContext);

        MasteryParserGrammar.SequentialDataContext sequentialDataContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.sequentialData(), "sequentialData", source.sourceInformation);
        source.sequentialData = evaluateBoolean(sequentialDataContext, (nonNull(sequentialDataContext) ? sequentialDataContext.boolean_value() : null), null);

        MasteryParserGrammar.StagedLoadContext stagedLoadContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.stagedLoad(), "stagedLoad", source.sourceInformation);
        source.stagedLoad = evaluateBoolean(stagedLoadContext, (nonNull(stagedLoadContext) ? stagedLoadContext.boolean_value() : null), null);

        MasteryParserGrammar.CreatePermittedContext createPermittedContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.createPermitted(), "createPermitted", source.sourceInformation);
        source.createPermitted = evaluateBoolean(createPermittedContext, (nonNull(createPermittedContext) ? createPermittedContext.boolean_value() : null), null);

        MasteryParserGrammar.CreateBlockedExceptionContext createBlockedExceptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.createBlockedException(), "createBlockedException", source.sourceInformation);
        source.createBlockedException = evaluateBoolean(createBlockedExceptionContext, (nonNull(createBlockedExceptionContext) ? createBlockedExceptionContext.boolean_value() : null), null);

        //Tags
        MasteryParserGrammar.TagsContext tagsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.tags(), "tags", source.sourceInformation);
        if (nonNull(tagsContext))
        {
            ListIterator<TerminalNode> stringIterator = tagsContext.STRING().listIterator();
            while (stringIterator.hasNext())
            {
                source.tags.add(PureGrammarParserUtility.fromGrammarString(stringIterator.next().toString(), true));
            }
        }

        //Services
        MasteryParserGrammar.ParseServiceContext parseServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.parseService(), "parseService", source.sourceInformation);
        if (nonNull(parseServiceContext))
        {
            source.parseService = PureGrammarParserUtility.fromQualifiedName(isNull(parseServiceContext.qualifiedName().packagePath()) ? Collections.emptyList() : parseServiceContext.qualifiedName().packagePath().identifier(), parseServiceContext.qualifiedName().identifier());
        }

        MasteryParserGrammar.TransformServiceContext transformServiceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.transformService(), "transformService", source.sourceInformation);
        source.transformService = PureGrammarParserUtility.fromQualifiedName(isNull(transformServiceContext.qualifiedName().packagePath()) ? Collections.emptyList() : transformServiceContext.qualifiedName().packagePath().identifier(), transformServiceContext.qualifiedName().identifier());

        //Partitions
        MasteryParserGrammar.SourcePartitionsContext partitionsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.sourcePartitions(), "partitions", source.sourceInformation);
        source.partitions = ListIterate.collect(partitionsContext.sourcePartition(), this::visitRecordSourcePartition);

        return source;
    }

    private Boolean evaluateBoolean(ParserRuleContext context, MasteryParserGrammar.Boolean_valueContext booleanValueContext, Boolean defaultVal)
    {
        Boolean result;
        if (isNull(context))
        {
            result = defaultVal;
        }
        else if (nonNull(booleanValueContext.TRUE()))
        {
            result = Boolean.TRUE;
        }
        else if (nonNull(booleanValueContext.FALSE()))
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
        if (nonNull(ctx.RECORD_SOURCE_STATUS_DEVELOPMENT()))
        {
            return RecordSourceStatus.Development;
        }
        if (nonNull(ctx.RECORD_SOURCE_STATUS_TEST_ONLY()))
        {
            return RecordSourceStatus.TestOnly;
        }
        if (nonNull(ctx.RECORD_SOURCE_STATUS_PRODUCTION()))
        {
            return RecordSourceStatus.Production;
        }
        if (nonNull(ctx.RECORD_SOURCE_STATUS_DORMANT()))
        {
            return RecordSourceStatus.Dormant;
        }
        if (nonNull(ctx.RECORD_SOURCE_STATUS_DECOMMINISSIONED()))
        {
            return RecordSourceStatus.Decommissioned;
        }

        throw new EngineException("Unrecognized record status", sourceInformation, EngineErrorType.PARSER);
    }

    private RecordSourcePartition visitRecordSourcePartition(MasteryParserGrammar.SourcePartitionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        RecordSourcePartition partition = new RecordSourcePartition();
        partition.id = PureGrammarParserUtility.fromIdentifier(ctx.masteryIdentifier());

        MasteryParserGrammar.TagsContext tagsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.tags(), "tags", sourceInformation);
        if (nonNull(tagsContext))
        {
            ListIterator<TerminalNode> stringIterator = tagsContext.STRING().listIterator();
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
        resolutionQuery.queries = ListIterate.flatCollect(ctx.queryExpressions(), this::visitQueryExpressions);

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
        return domainParser.parseLambda(ctx.getText(), "", 0, 0, true);
    }


    private String visitModelClass(MasteryParserGrammar.ModelClassContext ctx)
    {
        MasteryParserGrammar.QualifiedNameContext qualifiedNameContext = ctx.qualifiedName();
        return PureGrammarParserUtility.fromQualifiedName(isNull(qualifiedNameContext.packagePath()) ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
    }

    private ResolutionKeyType visitResolutionKeyType(MasteryParserGrammar.ResolutionQueryKeyTypeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (nonNull(ctx.RESOLUTION_QUERY_KEY_TYPE_GENERATED_PRIMARY_KEY()))
        {
            return ResolutionKeyType.GeneratedPrimaryKey;
        }
        if (nonNull(ctx.RESOLUTION_QUERY_KEY_TYPE_SUPPLIED_PRIMARY_KEY()))
        {
            return ResolutionKeyType.SuppliedPrimaryKey;
        }
        if (nonNull(ctx.RESOLUTION_QUERY_KEY_TYPE_ALTERNATE_KEY()))
        {
            return ResolutionKeyType.AlternateKey;
        }
        if (nonNull(ctx.RESOLUTION_QUERY_KEY_TYPE_OPTIONAL()))
        {
            return ResolutionKeyType.Optional;
        }

        throw new EngineException("Unrecognized resolution key type", sourceInformation, EngineErrorType.PARSER);
    }
}
