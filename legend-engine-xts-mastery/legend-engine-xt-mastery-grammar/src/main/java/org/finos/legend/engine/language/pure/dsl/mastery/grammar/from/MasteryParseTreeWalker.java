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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordService;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceDependency;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceStatus;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.LegendServiceAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.dataProvider.DataProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.CollectionEquality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionKeyType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.runtime.MasteryRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class MasteryParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final DomainParser domainParser;
    private final List<Function<SpecificationSourceCode, Connection>> connectionProcessors;
    private final List<Function<SpecificationSourceCode, MasteryRuntime>> masteryRuntimeProcessors;
    private final List<Function<SpecificationSourceCode, Trigger>> triggerProcessors;
    private final List<Function<SpecificationSourceCode, Authorization>> authorizationProcessors;
    private final List<Function<SpecificationSourceCode, AcquisitionProtocol>> acquisitionProtocolProcessors;


    private static final String SIMPLE_PRECEDENCE_LAMBDA = "{input: %s[1]| true}";
    private static final String INPUT = "\\$input";
    private static final String DOLLAR_SIGN = "\\$";
    private static final String PRECEDENCE_LAMBDA_WITH_FILTER = "{input: %s[1]| %s}";
    private static final String DATA_PROVIDER_STRING = "DataProvider";

    public MasteryParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation,
                                  Consumer<PackageableElement> elementConsumer,
                                  ImportAwareCodeSection section,
                                  DomainParser domainParser,
                                  List<Function<SpecificationSourceCode, Connection>> connectionProcessors,
                                  List<Function<SpecificationSourceCode, MasteryRuntime>> masteryRuntimeProcessors,
                                  List<Function<SpecificationSourceCode, Trigger>> triggerProcessors,
                                  List<Function<SpecificationSourceCode, Authorization>> authorizationProcessors,
                                  List<Function<SpecificationSourceCode, AcquisitionProtocol>> acquisitionProtocolProcessors)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.domainParser = domainParser;
        this.connectionProcessors = connectionProcessors;
        this.masteryRuntimeProcessors = masteryRuntimeProcessors;
        this.triggerProcessors = triggerProcessors;
        this.authorizationProcessors = authorizationProcessors;
        this.acquisitionProtocolProcessors = acquisitionProtocolProcessors;
    }

    public void visit(MasteryParserGrammar.DefinitionContext ctx)
    {
        List<PackageableElement> packageableElements = ctx.elementDefinition().stream().map(this::visitElement).collect(toList());
        packageableElements.stream().peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        validateMasteryRuntime(packageableElements);
    }

    private PackageableElement visitElement(MasteryParserGrammar.ElementDefinitionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        if (ctx.masterRecordDefinition() != null)
        {
            return visitMasterRecordDefinition(ctx.masterRecordDefinition());
        }
        else if (ctx.dataProviderDef() != null)
        {
            return visitDataProvider(ctx.dataProviderDef());
        }
        else if (ctx.connection() != null)
        {
            return visitConnection(ctx.connection());
        }
        else if (ctx.masteryRuntime() != null)
        {
            return visitMasteryRuntime(ctx.masteryRuntime());
        }

        throw new EngineException("Unrecognized element", sourceInformation, EngineErrorType.PARSER);
    }

    private MasterRecordDefinition visitMasterRecordDefinition(MasteryParserGrammar.MasterRecordDefinitionContext ctx)
    {
        MasterRecordDefinition masterRecordDefinition = new MasterRecordDefinition();
        masterRecordDefinition.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        masterRecordDefinition._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        masterRecordDefinition.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        //modelClass
        MasteryParserGrammar.ModelClassContext modelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelClass(), "modelClass", walkerSourceInformation.getSourceInformation(ctx));
        masterRecordDefinition.modelClass = visitQualifiedName(modelClassContext.qualifiedName());

        //IdentityResolution
        MasteryParserGrammar.IdentityResolutionContext identityResolutionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.identityResolution(), "identityResolution", masterRecordDefinition.sourceInformation);
        masterRecordDefinition.identityResolution = visitIdentityResolution(identityResolutionContext);

        //Master Record Sources
        MasteryParserGrammar.RecordSourcesContext recordSourcesContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.recordSources(), "recordSources", masterRecordDefinition.sourceInformation);
        masterRecordDefinition.sources = ListIterate.collect(recordSourcesContext.recordSource(), this::visitRecordSource);

        //Vendor Precedence
        MasteryParserGrammar.PrecedenceRulesContext precedenceRulesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.precedenceRules(), "precedenceRules", masterRecordDefinition.sourceInformation);
        if (precedenceRulesContext != null)
        {
            Map<String, Set<String>> allUniquePrecedenceRules = new HashMap<>();
            masterRecordDefinition.precedenceRules = ListIterate.flatCollect(precedenceRulesContext.precedenceRule(), precedenceRuleContext -> visitPrecedenceRules(precedenceRuleContext, allUniquePrecedenceRules));
        }

        //Post Curation Enrichment Service
        MasteryParserGrammar.PostCurationEnrichmentServiceContext postCurationEnrichmentServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.postCurationEnrichmentService(), "postCurationEnrichmentService", masterRecordDefinition.sourceInformation);
        if (postCurationEnrichmentServiceContext != null)
        {
            masterRecordDefinition.postCurationEnrichmentService = visitQualifiedName(postCurationEnrichmentServiceContext.qualifiedName());
        }

        //Collection Equality
        MasteryParserGrammar.CollectionEqualitiesContext collectionEqualitiesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.collectionEqualities(), "collectionEqualities", masterRecordDefinition.sourceInformation);
        if (collectionEqualitiesContext != null)
        {
            masterRecordDefinition.collectionEqualities = ListIterate.collect(collectionEqualitiesContext.collectionEquality(), this::visitCollectionEquality);
        }

        //Elastic Search Config
        MasteryParserGrammar.PublishToElasticSearchContext publishToElasticSearchContext  = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.publishToElasticSearch(), "publishToElasticSearch", masterRecordDefinition.sourceInformation);
        masterRecordDefinition.publishToElasticSearch = evaluateBoolean(publishToElasticSearchContext, (publishToElasticSearchContext != null ? publishToElasticSearchContext.boolean_value() : null), null);
        MasteryParserGrammar.ElasticSearchTransformServiceContext elasticSearchTransformServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.elasticSearchTransformService(), "elasticSearchTransformService", masterRecordDefinition.sourceInformation);
        if (elasticSearchTransformServiceContext != null)
        {
            masterRecordDefinition.elasticSearchTransformService = visitQualifiedName(elasticSearchTransformServiceContext.qualifiedName());
        }

        //Exception Workflow Service
        MasteryParserGrammar.ExceptionWorkflowTransformServiceContext exceptionWorkflowTransformServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.exceptionWorkflowTransformService(), "exceptionWorkflowTransformService", masterRecordDefinition.sourceInformation);
        if (exceptionWorkflowTransformServiceContext != null)
        {
            masterRecordDefinition.exceptionWorkflowTransformService = visitQualifiedName(exceptionWorkflowTransformServiceContext.qualifiedName());
        }

        return masterRecordDefinition;
    }

    private DataProvider  visitDataProvider(MasteryParserGrammar.DataProviderDefContext ctx)
    {
        DataProvider dataProvider = new DataProvider();
        dataProvider.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        dataProvider._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        dataProvider.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        String dataProviderTypeText = ctx.identifier().getText().trim();

        dataProvider.dataProviderType = extractDataProviderTypeValue(dataProviderTypeText).trim();
        dataProvider.dataProviderId = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier()).replaceAll("::", "_");

        return dataProvider;
    }


    private List<PrecedenceRule> visitPrecedenceRules(MasteryParserGrammar.PrecedenceRuleContext ctx, Map<String, Set<String>> allUniquePrecedenceRules)
    {
        if (ctx.sourcePrecedenceRule() != null)
        {
            return visitSourcePrecedenceRule(ctx.sourcePrecedenceRule(),
                    allUniquePrecedenceRules);
        }
        else if (ctx.deleteRule() != null)
        {
            return singletonList(visitDeleteRules(ctx.deleteRule(), allUniquePrecedenceRules));
        }
        else if (ctx.createRule() != null)
        {
            return singletonList(visitCreateRules(ctx.createRule(), allUniquePrecedenceRules));
        }
        else if (ctx.conditionalRule() != null)
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
        if (ruleScopeContext != null)
        {
            Set<String> uniqueScopes = new HashSet<>();
            precedenceRule.scopes = ListIterate.collect(ruleScopeContext.scope(), scopeContext ->
                    visitRuleScopeWithoutPrecedence(scopeContext, uniqueScopes, precedenceRule));
        }
        MasteryParserGrammar.PathContext pathContext = ctx.path();
        if (pathContext != null)
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
        if (ctx.pathExtension() != null)
        {
            propertyPaths.addAll(ListIterate.collect(ctx.pathExtension(), this::visitPathExtension));
        }
        return propertyPaths;
    }

    private Lambda visitMasterRecordFilter(MasteryParserGrammar.MasterRecordFilterContext ctx)
    {
        String qualifiedName = ctx.qualifiedName().getText();
        if (ctx.filter() != null)
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
        propertyPath.property = subPathContext.validString().getText();
        if (ctx.filter() != null)
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
        String inputFilter = ctx.getText().replaceAll(DOLLAR_SIGN, INPUT);
        return domainParser.parseLambda(
                format(PRECEDENCE_LAMBDA_WITH_FILTER, propertyName, inputFilter),
                "", 0, 0, walkerSourceInformation.getReturnSourceInfo());
    }

    private Lambda visitLambdaWithoutFilter(String propertyName)
    {
        return domainParser.parseLambda(format(SIMPLE_PRECEDENCE_LAMBDA, propertyName),
                "", 0, 0, walkerSourceInformation.getReturnSourceInfo());
    }

    private RuleScope visitRuleScopeWithoutPrecedence(MasteryParserGrammar.ScopeContext ctx, Set<String> uniqueScopes,PrecedenceRule precedenceRule)
    {
        validateUniqueRuleScope(uniqueScopes, ctx.validScopeType().getText(), precedenceRule);
        RuleScope recordSourceScope = visitRuleScope(ctx.validScopeType());
        if (ctx.precedence() != null)
        {
            throw  new EngineException(format("Precedence is not expected on rule scopes for %s", precedenceRule.getType()), EngineErrorType.PARSER);
        }
        return recordSourceScope;
    }

    private void visitRuleScopeWithPrecedence(MasteryParserGrammar.ScopeContext ctx, Map<Long, SourcePrecedenceRule> precedenceRuleMap, Set<String> uniqueScopes, SourcePrecedenceRule sourcePrecedenceRule)
    {
        validateUniqueRuleScope(uniqueScopes, ctx.validScopeType().getText(), sourcePrecedenceRule);
        RuleScope ruleScope = visitRuleScope(ctx.validScopeType());
        if (ctx.precedence() == null)
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
        if (ctx.recordSourceScope() != null)
        {
            MasteryParserGrammar.RecordSourceScopeContext recordSourceScopeContext = ctx.recordSourceScope();
            return visitRecordSourceScope(recordSourceScopeContext);
        }
        if (ctx.dataProviderTypeScope() != null)
        {
            MasteryParserGrammar.DataProviderTypeScopeContext dataProviderTypeScopeContext = ctx.dataProviderTypeScope();
            return visitDataProvideTypeScope(dataProviderTypeScopeContext);
        }

        if (ctx.dataProviderIdScope() != null)
        {
            MasteryParserGrammar.DataProviderIdScopeContext dataProviderIdScopeContext = ctx.dataProviderIdScope();
            return visitDataProviderIdScope(dataProviderIdScopeContext);
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

    private RuleScope visitDataProvideTypeScope(MasteryParserGrammar.DataProviderTypeScopeContext ctx)
    {
        DataProviderTypeScope dataProviderTypeScope = new DataProviderTypeScope();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        String dataProviderType = ctx.validString().getText();

        dataProviderTypeScope.sourceInformation = sourceInformation;
        dataProviderTypeScope.dataProviderType = dataProviderType;
        return dataProviderTypeScope;
    }

    private RuleScope visitDataProviderIdScope(MasteryParserGrammar.DataProviderIdScopeContext ctx)
    {
        DataProviderIdScope dataProviderIdScope = new DataProviderIdScope();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        String dataProviderId = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());

        dataProviderIdScope.sourceInformation = sourceInformation;
        dataProviderIdScope.dataProviderId = dataProviderId;
        return dataProviderIdScope;
    }



    private RuleAction visitAction(MasteryParserGrammar.ValidActionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.OVERWRITE() != null)
        {
            return RuleAction.Overwrite;
        }
        if (ctx.BLOCK() != null)
        {
            return RuleAction.Block;
        }
        throw new EngineException("Unrecognized rule action", sourceInformation, EngineErrorType.PARSER);
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
        source.sequentialData = evaluateBoolean(sequentialDataContext, (sequentialDataContext != null ? sequentialDataContext.boolean_value() : null), null);

        MasteryParserGrammar.StagedLoadContext stagedLoadContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.stagedLoad(), "stagedLoad", source.sourceInformation);
        source.stagedLoad = evaluateBoolean(stagedLoadContext, (stagedLoadContext != null ? stagedLoadContext.boolean_value() : null), null);

        MasteryParserGrammar.CreatePermittedContext createPermittedContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.createPermitted(), "createPermitted", source.sourceInformation);
        source.createPermitted = evaluateBoolean(createPermittedContext, (createPermittedContext != null ? createPermittedContext.boolean_value() : null), null);

        MasteryParserGrammar.CreateBlockedExceptionContext createBlockedExceptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.createBlockedException(), "createBlockedException", source.sourceInformation);
        source.createBlockedException = evaluateBoolean(createBlockedExceptionContext, (createBlockedExceptionContext != null ? createBlockedExceptionContext.boolean_value() : null), null);

        MasteryParserGrammar.AllowFieldDeleteContext allowFieldDeleteContext  = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.allowFieldDelete(), "allowFieldDelete", source.sourceInformation);
        source.allowFieldDelete = evaluateBoolean(allowFieldDeleteContext, (allowFieldDeleteContext != null ? allowFieldDeleteContext.boolean_value() : null), null);

        MasteryParserGrammar.TimeoutInMinutesContext timeoutInMinutesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.timeoutInMinutes(), "timeoutInMinutes", source.sourceInformation);
        if (timeoutInMinutesContext != null)
        {
            source.timeoutInMinutes = Integer.parseInt(timeoutInMinutesContext.INTEGER().getText());
        }

        MasteryParserGrammar.DataProviderContext dataProviderContext  = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.dataProvider(), "dataProvider", source.sourceInformation);

        if (dataProviderContext != null)
        {
            source.dataProvider = PureGrammarParserUtility.fromQualifiedName(dataProviderContext.qualifiedName().packagePath() == null ? Collections.emptyList() : dataProviderContext.qualifiedName().packagePath().identifier(), dataProviderContext.qualifiedName().identifier());
        }

        MasteryParserGrammar.RaiseExceptionWorkflowContext raiseExceptionWorkflowContext  = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.raiseExceptionWorkflow(), "raiseExceptionWorkflow", source.sourceInformation);
        source.raiseExceptionWorkflow = evaluateBoolean(raiseExceptionWorkflowContext, (raiseExceptionWorkflowContext != null ? raiseExceptionWorkflowContext.boolean_value() : null), null);

        MasteryParserGrammar.RunProfileContext runProfileContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.runProfile(), "runProfile", source.sourceInformation);
        source.runProfile = visitRunProfile(runProfileContext);

        MasteryParserGrammar.DependenciesContext dependenciesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.dependencies(), "dependencies", source.sourceInformation);
        if (dependenciesContext != null)
        {
            source.dependencies = ListIterate.collect(dependenciesContext.recordSourceDependency(), this::visitRecordSourceDependency);
        }

        // record Service
        MasteryParserGrammar.RecordServiceContext recordServiceContext  = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.recordService(), "recordService", source.sourceInformation);
        if (recordServiceContext != null)
        {
            source.recordService = visitRecordService(recordServiceContext);
        }

        // trigger
        MasteryParserGrammar.TriggerContext triggerContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.trigger(), "trigger", source.sourceInformation);
        if (triggerContext != null)
        {
            source.trigger = visitTriggerSpecification(triggerContext);
        }

        // trigger authorization
        MasteryParserGrammar.AuthorizationContext authorizationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authorization(), "authorization", source.sourceInformation);
        if (authorizationContext != null)
        {
          source.authorization = IMasteryParserExtension.process(extraSpecificationCode(authorizationContext.islandSpecification(), walkerSourceInformation), authorizationProcessors, "authorization");
        }

        return source;
    }

    private RecordService visitRecordService(MasteryParserGrammar.RecordServiceContext ctx)
    {
        RecordService recordService = new RecordService();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);


        MasteryParserGrammar.ParseServiceContext parseServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.parseService(), "parseService", sourceInformation);
        MasteryParserGrammar.TransformServiceContext transformServiceContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.transformService(), "transformService", sourceInformation);

        if (parseServiceContext != null)
        {
            recordService.parseService = visitQualifiedName(parseServiceContext.qualifiedName());
        }

        if (transformServiceContext != null)
        {
            recordService.transformService = visitQualifiedName(transformServiceContext.qualifiedName());
        }

        MasteryParserGrammar.AcquisitionProtocolContext acquisitionProtocolContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.acquisitionProtocol(), "acquisitionProtocol", sourceInformation);
        if (acquisitionProtocolContext != null)
        {
            recordService.acquisitionProtocol = acquisitionProtocolContext.qualifiedName() != null
                    ? visitLegendServiceAcquisitionProtocol(acquisitionProtocolContext.qualifiedName())
                    : IMasteryParserExtension.process(extraSpecificationCode(acquisitionProtocolContext.islandSpecification(), walkerSourceInformation), acquisitionProtocolProcessors, "acquisition protocol");
        }

        return recordService;
    }

    private Boolean evaluateBoolean(ParserRuleContext context, MasteryParserGrammar.Boolean_valueContext booleanValueContext, Boolean defaultVal)
    {
        Boolean result;
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
        if (ctx.RECORD_SOURCE_STATUS_DECOMMISSIONED() != null)
        {
            return RecordSourceStatus.Decommissioned;
        }

        throw new EngineException("Unrecognized record status", sourceInformation, EngineErrorType.PARSER);
    }

    private Profile visitRunProfile(MasteryParserGrammar.RunProfileContext ctx)
    {
        if (ctx == null || ctx.isEmpty())
        {
            return null;
        }
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.RECORD_SOURCE_RUN_PROFILE_XTRA_SMALL() != null)
        {
            return Profile.ExtraSmall;
        }
        if (ctx.RECORD_SOURCE_RUN_PROFILE_SMALL() != null)
        {
            return Profile.Small;
        }
        if (ctx.RECORD_SOURCE_RUN_PROFILE_MEDIUM() != null)
        {
            return Profile.Medium;
        }
        if (ctx.RECORD_SOURCE_RUN_PROFILE_LARGE() != null)
        {
            return Profile.Large;
        }

        throw new EngineException("Unrecognized run profile", sourceInformation, EngineErrorType.PARSER);
    }

    private RecordSourceDependency visitRecordSourceDependency(MasteryParserGrammar.RecordSourceDependencyContext recordSourceDependencyContext)
    {
        RecordSourceDependency recordSourceDependency = new RecordSourceDependency();
        recordSourceDependency.sourceInformation = walkerSourceInformation.getSourceInformation(recordSourceDependencyContext);
        recordSourceDependency.dependentRecordSourceId = PureGrammarParserUtility.fromIdentifier(recordSourceDependencyContext.masteryIdentifier());
        return recordSourceDependency;
    }

    /*
     * Identity and Resolution
     */
    private IdentityResolution visitIdentityResolution(MasteryParserGrammar.IdentityResolutionContext ctx)
    {
        IdentityResolution identityResolution = new IdentityResolution();
        identityResolution.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

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
        MasteryParserGrammar.ResolutionQueryKeyTypeContext resolutionQueryKeyTypeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.resolutionQueryKeyType(), "keyType", sourceInformation);
        if (resolutionQueryKeyTypeContext != null)
        {
            resolutionQuery.keyType = visitResolutionKeyType(resolutionQueryKeyTypeContext);
        }

        //optional
        MasteryParserGrammar.ResolutionQueryOptionalContext resolutionQueryOptionalContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.resolutionQueryOptional(), "optional", sourceInformation);
        resolutionQuery.optional = evaluateBoolean(resolutionQueryOptionalContext, (resolutionQueryOptionalContext != null ? resolutionQueryOptionalContext.boolean_value() : null), null);

        //precedence - Field 'precedence' should be specified only once
        MasteryParserGrammar.ResolutionQueryPrecedenceContext resolutionQueryPrecedenceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.resolutionQueryPrecedence(), "precedence", sourceInformation);
        resolutionQuery.precedence = Integer.parseInt(resolutionQueryPrecedenceContext.INTEGER().getText());

        //filter
        MasteryParserGrammar.ResolutionQueryFilterContext resolutionQueryFilterContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.resolutionQueryFilter(), "filter", sourceInformation);
        if (resolutionQueryFilterContext != null)
        {
            resolutionQuery.filter = visitLambda(resolutionQueryFilterContext.lambdaFunction());
        }

        return resolutionQuery;
    }

    private List<Lambda> visitQueryExpressions(MasteryParserGrammar.QueryExpressionsContext ctx)
    {

        List<MasteryParserGrammar.LambdaFunctionContext> lambdaFunctionContexts = ctx.lambdaFunction();
        return ListIterate.collect(lambdaFunctionContexts, this::visitLambda).toList();
    }

    private Lambda visitLambda(MasteryParserGrammar.LambdaFunctionContext ctx)
    {
        return domainParser.parseLambda(ctx.getText(), "", 0, 0, walkerSourceInformation.getReturnSourceInfo());
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

    /**********
     * connection
     **********/

    private Connection visitConnection(MasteryParserGrammar.ConnectionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        MasteryParserGrammar.SpecificationContext specificationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.specification(), "specification", sourceInformation);

        Connection connection = IMasteryParserExtension.process(extraSpecificationCode(specificationContext.islandSpecification(), walkerSourceInformation), connectionProcessors, "connection");

        connection.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        connection._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        connection.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return connection;
    }

    /**********
     * mastery runtime
     **********/
    private MasteryRuntime visitMasteryRuntime(MasteryParserGrammar.MasteryRuntimeContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        MasteryParserGrammar.RuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.runtime(), "runtime", sourceInformation);

        MasteryRuntime masteryRuntime = IMasteryParserExtension.process(extraSpecificationCode(runtimeContext.islandSpecification(), walkerSourceInformation), masteryRuntimeProcessors, "mastery runtime");

        masteryRuntime.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        masteryRuntime._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        masteryRuntime.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return masteryRuntime;
    }

    private String extractDataProviderTypeValue(String dataProviderTypeText)
    {
        if (!dataProviderTypeText.endsWith(DATA_PROVIDER_STRING))
        {
            throw new EngineException(format("Invalid data provider type definition '%s'. Valid syntax is '<DataProviderType>DataProvider", dataProviderTypeText), EngineErrorType.PARSER);
        }

        int index = dataProviderTypeText.indexOf(DATA_PROVIDER_STRING);
        return dataProviderTypeText.substring(0, index);
    }

    private Trigger visitTriggerSpecification(MasteryParserGrammar.TriggerContext ctx)
    {
        return IMasteryParserExtension.process(extraSpecificationCode(ctx.islandSpecification(), walkerSourceInformation), triggerProcessors, "trigger");
    }

    private SpecificationSourceCode extraSpecificationCode(MasteryParserGrammar.IslandSpecificationContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        StringBuilder text = new StringBuilder();
        MasteryParserGrammar.IslandValueContext islandValueContext = ctx.islandValue();
        if (islandValueContext != null)
        {
            for (MasteryParserGrammar.IslandValueContentContext fragment : islandValueContext.islandValueContent())
            {
                text.append(fragment.getText());
            }
            String textToParse = text.length() > 0 ? text.substring(0, text.length() - 2) : text.toString();

            // prepare island grammar walker source information
            int startLine = islandValueContext.ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + islandValueContext.ISLAND_OPEN().getSymbol().getCharPositionInLine() + islandValueContext.ISLAND_OPEN().getSymbol().getText().length();
            ParseTreeWalkerSourceInformation triggerValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation triggerValueSourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return new SpecificationSourceCode(textToParse, ctx.islandType().getText(), triggerValueSourceInformation, triggerValueWalkerSourceInformation);
        }
        else
        {
            SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return new SpecificationSourceCode(text.toString(), ctx.islandType().getText(), sourceInformation, walkerSourceInformation);
        }
    }

    public LegendServiceAcquisitionProtocol visitLegendServiceAcquisitionProtocol(MasteryParserGrammar.QualifiedNameContext ctx)
    {

        LegendServiceAcquisitionProtocol legendServiceAcquisitionProtocol = new LegendServiceAcquisitionProtocol();
        legendServiceAcquisitionProtocol.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        legendServiceAcquisitionProtocol.service = PureGrammarParserUtility.fromQualifiedName(ctx.packagePath() == null ? Collections.emptyList() : ctx.packagePath().identifier(), ctx.identifier());
        return legendServiceAcquisitionProtocol;
    }

    private CollectionEquality visitCollectionEquality(MasteryParserGrammar.CollectionEqualityContext ctx)
    {
        CollectionEquality collectionEquality = new CollectionEquality();
        collectionEquality.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        MasteryParserGrammar.ModelClassContext modelClassContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.modelClass(), "modelClass", collectionEquality.sourceInformation);
        collectionEquality.modelClass = visitQualifiedName(modelClassContext.qualifiedName());
        MasteryParserGrammar.EqualityFunctionContext equalityFunctionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.equalityFunction(), "equalityFunction", collectionEquality.sourceInformation);
        collectionEquality.equalityFunction = visitQualifiedName(equalityFunctionContext.qualifiedName());

        return collectionEquality;
    }

    private String visitQualifiedName(MasteryParserGrammar.QualifiedNameContext qualifiedNameContext)
    {
        return PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() : qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
    }

    private void validateMasteryRuntime(List<PackageableElement> packageableElements)
    {
        List<MasteryRuntime> masteryRuntimes = ListIterate.selectInstancesOf(packageableElements, MasteryRuntime.class);
        //validate master record definitions is unique across all mastery runtimes
        Set<String> uniqueMasterRecordDefinitions = new HashSet<>();
        List<String> masterRecordDefinitions = ListIterate.flatCollect(masteryRuntimes, m -> m.masterRecordDefinitions);
        List<String> duplicateMasterRecords = ListIterate.select(masterRecordDefinitions, m -> !uniqueMasterRecordDefinitions.add(m));
        if (!duplicateMasterRecords.isEmpty())
        {
            throw new EngineException(format("The following master record definitions %s appear multiple times. There cannot be duplicate masterRecordDefinitions across one or multiple mastery runtimes.", duplicateMasterRecords), EngineErrorType.PARSER);
        }
    }

}
