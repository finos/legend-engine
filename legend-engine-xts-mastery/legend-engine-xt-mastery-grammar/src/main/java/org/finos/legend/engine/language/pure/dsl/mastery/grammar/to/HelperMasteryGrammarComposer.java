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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.dataProvider.DataProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.CollectionEquality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolutionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.chomp;
import static org.apache.commons.lang3.StringUtils.chop;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;


/**
 * Convert the protocol to a String parsable by the grammar.
 */
public class HelperMasteryGrammarComposer
{

    private static final String PRECEDENCE_LAMBDA_WITH_FILTER_PREFIX = "\\{?input: .*\\[1]\\|\\$input\\.";
    private static final String INPUT = "input";
    private static final String BRACKETS = "\\(|\\)";
    private static final String PRECEDENCE_LAMBDA_WITH_FILTER_SUFFIX = ".*";

    private HelperMasteryGrammarComposer()
    {
    }

    public static String renderMasterRecordDefinition(MasterRecordDefinition masterRecordDefinition, int indentLevel, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("MasterRecordDefinition ").append(convertPath(masterRecordDefinition.getPath())).append("\n")
                .append("{\n")
                .append(renderAttribute("modelClass", masterRecordDefinition.modelClass, indentLevel))
                .append(renderIdentityResolution(masterRecordDefinition.identityResolution, indentLevel, context));
        if (masterRecordDefinition.precedenceRules != null)
        {
            builder.append(renderPrecedenceRules(masterRecordDefinition.precedenceRules, indentLevel, context));
        }
        if (masterRecordDefinition.postCurationEnrichmentService != null)
        {
            builder.append(renderAttribute("postCurationEnrichmentService", masterRecordDefinition.postCurationEnrichmentService, indentLevel));
        }
        if (masterRecordDefinition.publishToElasticSearch != null)
        {
            builder.append(renderAttribute("publishToElasticSearch", String.valueOf(masterRecordDefinition.publishToElasticSearch), indentLevel));
        }
        if (masterRecordDefinition.elasticSearchTransformService != null)
        {
            builder.append(renderAttribute("elasticSearchTransformService", masterRecordDefinition.elasticSearchTransformService, indentLevel));
        }
        if (masterRecordDefinition.exceptionWorkflowTransformService != null)
        {
            builder.append(renderAttribute("exceptionWorkflowTransformService", masterRecordDefinition.exceptionWorkflowTransformService, indentLevel));
        }
        if (masterRecordDefinition.collectionEqualities != null)
        {
            builder.append(renderCollectionEqualities(masterRecordDefinition.collectionEqualities, indentLevel));
        }
        builder.append(renderRecordSources(masterRecordDefinition.sources, indentLevel, context))
                .append("}");
        return builder.toString();
    }

    public static String renderDataProvider(DataProvider dataProvider)
    {
        return dataProvider.dataProviderType +
                "DataProvider " +
                convertPath(dataProvider.getPath()) + ";\n";
    }

    /*
     * MasterRecordDefinition Attributes
     */
    private static String renderAttribute(String field, String modelClass, int indentLevel)
    {
        return getTabString(indentLevel) + field + ": " + modelClass + ";\n";
    }

    private static String renderCollectionEqualities(List<CollectionEquality> collectionEqualities, int indentLevel)
    {
        StringBuilder collectionEqualitiesString = new StringBuilder().append(getTabString(indentLevel)).append("collectionEqualities: [\n");
        ListIterate.forEachWithIndex(collectionEqualities, (collectionEquality, i) ->
        {
            collectionEqualitiesString.append(i > 0 ? ",\n" : "")
                    .append(getTabString(indentLevel + 1)).append("{\n")
                    .append(getTabString(indentLevel + 2)).append("modelClass: ").append(collectionEquality.modelClass).append(";\n")
                    .append(getTabString(indentLevel + 2)).append("equalityFunction: ").append(collectionEquality.equalityFunction).append(";\n")
                    .append(getTabString(indentLevel + 1)).append("}");
        });
        collectionEqualitiesString.append("\n").append(getTabString(indentLevel)).append("]\n");
        return collectionEqualitiesString.toString();
    }


    /*
     * MasterRecordSources
     */
    private static String renderRecordSources(List<RecordSource> sources, int indentLevel, PureGrammarComposerContext context)
    {
        StringBuilder sourcesStr = new StringBuilder()
                .append(getTabString(indentLevel)).append("recordSources:\n")
                .append(getTabString(indentLevel)).append("[\n");
                ListIterate.forEachWithIndex(sources, (source, i) ->
                {
                    sourcesStr.append(i > 0 ? ",\n" : "");
                    sourcesStr.append(source.accept(new RecordSourceComposer(indentLevel, context)));
                    sourcesStr.append(getTabString(indentLevel + 1)).append("}");
                });
                sourcesStr.append("\n").append(getTabString(indentLevel)).append("]\n");
        return sourcesStr.toString();
    }

    private static class RecordSourceComposer implements RecordSourceVisitor<String>
    {
        private final int indentLevel;
        private final PureGrammarComposerContext context;

        private RecordSourceComposer(int indentLevel, PureGrammarComposerContext context)
        {
            this.indentLevel = indentLevel;
            this.context = context;
        }

        @Override
        public String visit(RecordSource recordSource)
        {
            return getTabString(indentLevel + 1) + recordSource.id + ": {\n" +
                    getTabString(indentLevel + 2) + "description: " + convertString(recordSource.description, true) + ";\n" +
                    getTabString(indentLevel + 2) + "status: " + recordSource.status + ";\n" +
                    getTabString(indentLevel + 2) + renderRecordService(recordSource, indentLevel + 2) +
                    (recordSource.dataProvider != null ? getTabString(indentLevel + 2) + "dataProvider: " + recordSource.dataProvider + ";\n" : "") +
                    (recordSource.trigger != null ? getTabString(indentLevel + 2) + renderTrigger(recordSource.trigger, indentLevel + 2) : "") +
                    (recordSource.sequentialData != null ? getTabString(indentLevel + 2) + "sequentialData: " + recordSource.sequentialData + ";\n" : "") +
                    (recordSource.stagedLoad != null ? getTabString(indentLevel + 2) + "stagedLoad: " + recordSource.stagedLoad + ";\n" : "") +
                    (recordSource.createPermitted != null ? getTabString(indentLevel + 2) + "createPermitted: " + recordSource.createPermitted + ";\n" : "") +
                    (recordSource.createBlockedException != null ? getTabString(indentLevel + 2) + "createBlockedException: " + recordSource.createBlockedException + ";\n" : "") +
                    (recordSource.allowFieldDelete != null ? getTabString(indentLevel + 2) + "allowFieldDelete: " + recordSource.allowFieldDelete + ";\n" : "") +
                    (recordSource.raiseExceptionWorkflow != null ? getTabString(indentLevel + 2) + "raiseExceptionWorkflow: " + recordSource.raiseExceptionWorkflow + ";\n" : "") +
                    (recordSource.runProfile != null ? getTabString(indentLevel + 2) + "runProfile: " + recordSource.runProfile + ";\n" : "") +
                    (recordSource.timeoutInMinutes != null ? getTabString(indentLevel + 2) + "timeoutInMinutes: " + recordSource.timeoutInMinutes + ";\n" : "") +
                    (recordSource.dependencies != null ? renderRecordSourceDependencies(recordSource.dependencies, indentLevel + 2) : "") +
                    (recordSource.authorization != null ? renderAuthorization(recordSource.authorization, indentLevel + 2) : "");
        }

        private String renderRecordService(RecordSource recordSource, int indentLevel)
        {
            String parseService = recordSource.parseService;
            String transformService = recordSource.transformService;
            AcquisitionProtocol acquisitionProtocol = null;
            RecordService recordService = recordSource.recordService;
            if (recordService != null)
            {
                if (recordService.parseService != null)
                {
                    parseService = recordService.parseService;
                }
                if (recordService.transformService != null)
                {
                    transformService = recordService.transformService;
                }
                acquisitionProtocol = recordService.acquisitionProtocol;
            }
            return  "recordService: {\n" +
                    (parseService != null ? getTabString(indentLevel + 1) + "parseService: " + parseService + ";\n" : "") +
                    (transformService != null ? getTabString(indentLevel + 1) + "transformService: " + transformService + ";\n" : "") +
                    (acquisitionProtocol != null ? renderAcquisition(acquisitionProtocol, indentLevel) : "") +
                    getTabString(indentLevel) + "};\n";
        }

        private String renderRecordSourceDependencies(List<RecordSourceDependency> dependencies, int indentLevel)
        {
            StringBuilder recordSourceDependenciesString = new StringBuilder().append(getTabString(indentLevel)).append("dependencies: [\n");
            ListIterate.forEachWithIndex(dependencies, (dependency, i) ->
            {
                recordSourceDependenciesString.append(i > 0 ? ",\n" : "")
                        .append(getTabString(indentLevel + 1)).append("RecordSourceDependency {")
                        .append(dependency.dependentRecordSourceId).append("}");
            });
            recordSourceDependenciesString.append("\n").append(getTabString(indentLevel)).append("];\n");
            return recordSourceDependenciesString.toString();
        }

        private String renderAcquisition(AcquisitionProtocol acquisitionProtocol, int indentLevel)
        {
            List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
            String text = IMasteryComposerExtension.process(acquisitionProtocol, ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraAcquisitionProtocolComposers), indentLevel, context);
            return getTabString(indentLevel + 1) + "acquisitionProtocol: " + text;
        }

        private String renderTrigger(Trigger trigger, int indentLevel)
        {
            List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
            String triggerText = IMasteryComposerExtension.process(trigger, ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraTriggerComposers), indentLevel, context);
            return "trigger: " + triggerText + ";\n";
        }

        private String renderAuthorization(Authorization authorization, int indentLevel)
        {
            List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
            String authorizationText = IMasteryComposerExtension.process(authorization, ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraAuthorizationComposers), indentLevel, context);
            return getTabString(indentLevel) + "authorization: " + authorizationText;
        }
    }

    /*
     * Identity and Resolution
     */

    private static String renderIdentityResolution(IdentityResolution identityResolution, int indentLevel, PureGrammarComposerContext context)
    {
        return identityResolution.accept(new IdentityResolutionComposer(indentLevel, context));
    }

    private static String renderPrecedenceRules(List<PrecedenceRule> precedenceRules, int indentLevel, PureGrammarComposerContext context)
    {
        Map<Pair<String, String>, StringBuilder> uniqueSourcePrecedenceRules = new LinkedHashMap<>();
        StringBuilder nonSourcePrecedenceRulesBuilder = new StringBuilder()
                .append(getTabString(indentLevel)).append("precedenceRules: [");

        ListIterate.forEach(precedenceRules, (precedenceRule) ->
        {
            String precedenceRuleString = precedenceRule.accept(new PrecedenceRuleComposer(indentLevel + 1, context, uniqueSourcePrecedenceRules));
            nonSourcePrecedenceRulesBuilder.append(precedenceRuleString);
            nonSourcePrecedenceRulesBuilder.append(!precedenceRuleString.isEmpty() ? "," : "");
        });
        return combinePrecedenceRules(uniqueSourcePrecedenceRules, nonSourcePrecedenceRulesBuilder.toString(), indentLevel);
    }

    private static String combinePrecedenceRules(Map<Pair<String, String>, StringBuilder> uniqueSourcePrecedenceRules, String nonSourcePrecedenceRules, int indentLevel)
    {
        StringBuilder allPrecedenceRules = new StringBuilder();
        if (uniqueSourcePrecedenceRules.isEmpty())
        {
            nonSourcePrecedenceRules = chop(nonSourcePrecedenceRules);
            nonSourcePrecedenceRules = chomp(nonSourcePrecedenceRules);
        }
        allPrecedenceRules.append(nonSourcePrecedenceRules);
        List<StringBuilder> sourcePrecedenceRules = new ArrayList<>(uniqueSourcePrecedenceRules.values());
        ListIterate.forEachWithIndex(sourcePrecedenceRules, (sourcePrecedenceRule, i) ->
        {
            allPrecedenceRules.append(i > 0 ? "," : "");
            String sourcePrecedenceSTring = chop(sourcePrecedenceRule.toString());
            allPrecedenceRules.append(sourcePrecedenceSTring).append("\n");
            allPrecedenceRules.append(getTabString(indentLevel + 2)).append("];\n");
            allPrecedenceRules.append(getTabString(indentLevel + 1)).append("}");
        });
        allPrecedenceRules.append("\n").append(getTabString(indentLevel)).append("]\n");
        return allPrecedenceRules.toString();
    }

    private static class PrecedenceRuleComposer implements PrecedenceRuleVisitor<String>
    {
        private final int indentLevel;
        private final PureGrammarComposerContext context;
        private final Map<Pair<String, String>, StringBuilder> uniqueSourcePrecedenceRules;

        private PrecedenceRuleComposer(int indentLevel, PureGrammarComposerContext context, Map<Pair<String, String>, StringBuilder> uniqueSourcePrecedenceRules)
        {
            this.indentLevel = indentLevel;
            this.context = context;
            this.uniqueSourcePrecedenceRules = uniqueSourcePrecedenceRules;
        }

        @Override
        public String visit(PrecedenceRule precedenceRule)
        {
            String path = visitPath(precedenceRule.masterRecordFilter, precedenceRule.paths);
            StringBuilder builder = new StringBuilder("\n");
            if (precedenceRule instanceof SourcePrecedenceRule)
            {
                visitSourcePrecedenceRule(precedenceRule, builder, path);
                return "";
            }
            else
            {
                if (precedenceRule instanceof DeleteRule)
                {
                    buildPrecedenceSubType(builder, precedenceRule.getType());
                }
                else if (precedenceRule instanceof CreateRule)
                {
                    buildPrecedenceSubType(builder, precedenceRule.getType());
                }
                else if (precedenceRule instanceof ConditionalRule)
                {
                    visitConditionalRule(precedenceRule, builder);
                }
                else
                {
                    throw new EngineException("Unrecognized precedence rule", EngineErrorType.COMPOSER);
                }
                visitPrecedenceRule(precedenceRule, builder, path);
                return builder.toString();
            }
        }

        private void buildPrecedenceSubType(StringBuilder builder, String precedenceSubType)
        {
            builder.append(getTabString(indentLevel)).append(precedenceSubType).append(": {\n");
        }

        private void visitConditionalRule(PrecedenceRule precedenceRule, StringBuilder builder)
        {
            ConditionalRule conditionalRule = (ConditionalRule) precedenceRule;
            buildPrecedenceSubType(builder, precedenceRule.getType());
            builder.append(getTabString(indentLevel + 1)).append("predicate: ")
                    .append(lambdaToString(conditionalRule.predicate, context)).append(";\n");
        }

        private void visitPrecedenceRule(PrecedenceRule precedenceRule, StringBuilder builder, String path)
        {
            String ruleScope = visitRuleScopeWithoutPrecedence(precedenceRule.scopes);
            builder.append(getTabString(indentLevel + 1)).append("path: ").append(path).append(";\n");
            if (!ruleScope.isEmpty())
            {
                builder.append(getTabString(indentLevel + 1)).append("ruleScope: [").append(ruleScope).append("\n");
                builder.append(getTabString(indentLevel + 1)).append("];\n");
            }
            builder.append(getTabString(indentLevel)).append("}");
        }

        private void visitSourcePrecedenceRule(PrecedenceRule precedenceRule, StringBuilder builder, String path)
        {
            SourcePrecedenceRule sourcePrecedenceRule = (SourcePrecedenceRule) precedenceRule;
            String action = sourcePrecedenceRule.action.name();
            String ruleScope = visitRuleScopeWithPrecedence(sourcePrecedenceRule.scopes, sourcePrecedenceRule.precedence);
            buildPrecedenceSubType(builder, precedenceRule.getType());
            builder.append(getTabString(indentLevel + 1)).append("path: ").append(path).append(";\n")
                    .append(getTabString(indentLevel + 1)).append("action: ").append(action).append(";\n")
                    .append(getTabString(indentLevel + 1)).append("ruleScope: [").append(ruleScope);
            uniqueSourcePrecedenceRules.merge(Pair.of(path, action), builder, (existingValue, newValue) ->
            {
                existingValue.append(ruleScope);
                return existingValue;
            });
        }

        private String visitRuleScopeWithPrecedence(List<RuleScope> ruleScope, Long precedence)
        {
            StringBuilder builder = new StringBuilder();
            ListIterate.forEach(ruleScope, scope ->
            {
                builder.append("\n");
                builder.append(visitRuleScope(scope));
                builder.append(", precedence: ").append(precedence).append("},");
            });
            return builder.toString();
        }

        private String visitRuleScopeWithoutPrecedence(List<RuleScope> ruleScopes)
        {
            StringBuilder builder = new StringBuilder();
            ListIterate.forEachWithIndex(ruleScopes, (scope, i) ->
            {
                builder.append(i > 0 ? ",\n" : "\n");
                builder.append(visitRuleScope(scope)).append("}");
            });
            return builder.toString();
        }

        private String visitPath(Lambda masterRecordFilter, List<PropertyPath> propertyPaths)
        {
            StringBuilder builder = new StringBuilder();
            builder.append(masterRecordFilter.parameters.get(0)._class);
            builder.append(visitLambda(masterRecordFilter));
            ListIterate.forEach(propertyPaths, propertyPath ->
            {
               builder.append(".").append(propertyPath.property);
               builder.append(visitLambda(propertyPath.filter));
            });
            return builder.toString();
        }

        private String visitLambda(Lambda lambda)
        {
            StringBuilder builder = new StringBuilder();
            String lambdaStr = lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()).replaceAll(BRACKETS, "");
            if (lambdaStr.matches(PRECEDENCE_LAMBDA_WITH_FILTER_PREFIX + PRECEDENCE_LAMBDA_WITH_FILTER_SUFFIX))
            {
                String filterPath = lambdaStr.replaceAll(PRECEDENCE_LAMBDA_WITH_FILTER_PREFIX, "").replace(INPUT, "");
                builder.append("{$.").append(filterPath).append("}");
            }
            return builder.toString();
        }

        private String visitRuleScope(RuleScope ruleScope)
        {
            StringBuilder builder = new StringBuilder();
            builder.append(getTabString(indentLevel + 2));
            if (ruleScope instanceof RecordSourceScope)
            {
                RecordSourceScope recordSourceScope = (RecordSourceScope) ruleScope;
                return builder.append("RecordSourceScope {").append(recordSourceScope.recordSourceId).toString();
            }
            if (ruleScope instanceof DataProviderTypeScope)
            {
                DataProviderTypeScope dataProviderTypeScope = (DataProviderTypeScope) ruleScope;
                return builder.append("DataProviderTypeScope {").append(dataProviderTypeScope.dataProviderType).toString();
            }
            if (ruleScope instanceof DataProviderIdScope)
            {
                DataProviderIdScope dataProviderTypeScope = (DataProviderIdScope) ruleScope;
                return builder.append("DataProviderIdScope {").append(dataProviderTypeScope.dataProviderId).toString();
            }
            return "";
        }
    }

    private static String renderResolutionQueries(IdentityResolution identityResolution, int indentLevel, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(indentLevel)).append("resolutionQueries:");
        builder.append("\n").append(getTabString(indentLevel + 2)).append("[");
        ListIterate.forEachWithIndex(identityResolution.resolutionQueries, (resolutionQuery, i) ->
        {
            builder.append(i > 0 ? "," : "").append("\n").append(getTabString(indentLevel + 3)).append("{");
            builder.append("\n").append(getTabString(indentLevel + 4)).append("queries: [ ");
            builder.append(renderQueries(resolutionQuery, indentLevel + 5, context));
            builder.append(getTabString(indentLevel + 4)).append("         ];\n");
            if (resolutionQuery.keyType != null)
            {
                builder.append(getTabString(indentLevel + 4)).append("keyType: ").append(resolutionQuery.keyType).append(";\n");
            }
            if (resolutionQuery.optional != null)
            {
                builder.append(getTabString(indentLevel + 4)).append("optional: ").append(resolutionQuery.optional).append(";\n");
            }
            builder.append(getTabString(indentLevel + 4)).append("precedence: ").append(resolutionQuery.precedence).append(";\n");
            if (resolutionQuery.filter != null)
            {
                builder.append(getTabString(indentLevel + 4)).append("filter: ").append(lambdaToString(resolutionQuery.filter, context)).append(";\n");
            }
            builder.append(getTabString(indentLevel + 3)).append("}");
        });
        builder.append("\n").append(getTabString(indentLevel + 2)).append("]");
        return builder.toString();
    }

    private static class IdentityResolutionComposer implements IdentityResolutionVisitor<String>
    {
        private final int indentLevel;
        private final PureGrammarComposerContext context;

        private IdentityResolutionComposer(int indentLevel, PureGrammarComposerContext context)
        {
            this.indentLevel = indentLevel;
            this.context = context;
        }

        @Override
        public String visit(IdentityResolution val)
        {
            return getTabString(indentLevel) + "identityResolution: \n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel) + renderResolutionQueries(val, this.indentLevel, this.context) + "\n" +
                    getTabString(indentLevel) + "}\n";
        }
    }

    private static String renderQueries(ResolutionQuery query, int indentLevel, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        ListIterate.forEachWithIndex(query.queries, (lambda, i) ->
        {
            if (i > 0)
            {
                builder.append(",\n").append(getTabString(indentLevel)).append("         ");
            }
            builder.append(lambdaToString(lambda, context));

        });
        return builder.append("\n").toString();
    }

    private static String lambdaToString(Lambda lambda, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        String lambdaStr = lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
        if (!lambdaStr.startsWith("{"))
        {
            builder.append("{").append(lambdaStr).append("}");
        }
        else
        {
            builder.append(lambdaStr);
        }
        return builder.toString();
    }
}
