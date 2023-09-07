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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolutionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;


/**
 * Convert the protocol to a String parsable by the grammar.
 */
public class HelperMasteryGrammarComposer
{

    private static final String PRECEDENCE_LAMBDA_WITH_FILTER_PREFIX = "\\{?input: .*\\[1]\\|\\$input\\.";
    private static final String PRECEDENCE_LAMBDA_WITH_FILTER_SUFFIX = ".*";

    private HelperMasteryGrammarComposer()
    {
    }

    public static String renderMastery(MasterRecordDefinition masterRecordDefinition, int indentLevel, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("MasterRecordDefinition ").append(convertPath(masterRecordDefinition.getPath())).append("\n")
                .append("{\n")
                .append(renderModelClass(masterRecordDefinition.modelClass, indentLevel))
                .append(renderIdentityResolution(masterRecordDefinition.identityResolution, indentLevel, context));
        if (masterRecordDefinition.precedenceRules != null)
        {
            builder.append(renderPrecedenceRules(masterRecordDefinition.precedenceRules, indentLevel, context));
        }
        builder.append(renderRecordSources(masterRecordDefinition.sources, indentLevel))
                .append("}");
        return builder.toString();
    }

    /*
     * MasterRecordDefinition Attributes
     */
    private static String renderModelClass(String modelClass, int indentLevel)
    {
        return getTabString(indentLevel) + "modelClass: " + modelClass + ";\n";
    }

    /*
     * MasterRecordSources
     */
    private static String renderRecordSources(List<RecordSource> sources, int indentLevel)
    {
        StringBuilder sourcesStr = new StringBuilder()
                .append(getTabString(indentLevel)).append("recordSources:\n")
                .append(getTabString(indentLevel)).append("[\n");
                ListIterate.forEachWithIndex(sources, (source, i) ->
                {
                    sourcesStr.append(i > 0 ? ",\n" : "");
                    sourcesStr.append(source.accept(new RecordSourceComposer(indentLevel)));
                    sourcesStr.append(getTabString(indentLevel + 1)).append("}");
                });
                sourcesStr.append("\n").append(getTabString(indentLevel)).append("]\n");
        return sourcesStr.toString();
    }

    private static class RecordSourceComposer implements RecordSourceVisitor<String>
    {
        private final int indentLevel;

        private RecordSourceComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(RecordSource recordSource)
        {
            return getTabString(indentLevel + 1) + recordSource.id + ": {\n" +
                    getTabString(indentLevel + 2) + "description: " + convertString(recordSource.description, true) + ";\n" +
                    getTabString(indentLevel + 2) + "status: " + recordSource.status + ";\n" +
                    (recordSource.parseService != null ? (getTabString(indentLevel + 2) + "parseService: " + recordSource.parseService + ";\n") : "") +
                    getTabString(indentLevel + 2) + "transformService: " + recordSource.transformService + ";\n" +
                    (recordSource.sequentialData != null ? getTabString(indentLevel + 2) + "sequentialData: " + recordSource.sequentialData + ";\n" : "") +
                    (recordSource.stagedLoad != null ? getTabString(indentLevel + 2) + "stagedLoad: " + recordSource.stagedLoad + ";\n" : "") +
                    (recordSource.createPermitted != null ? getTabString(indentLevel + 2) + "createPermitted: " + recordSource.createPermitted + ";\n" : "") +
                    (recordSource.createBlockedException != null ? getTabString(indentLevel + 2) + "createBlockedException: " + recordSource.createBlockedException + ";\n" : "") +
                    ((recordSource.getTags() != null && !recordSource.getTags().isEmpty()) ? getTabString(indentLevel + 1) + renderTags(recordSource, indentLevel) + "\n" : "") +
                    getTabString(indentLevel + 1) + renderPartitions(recordSource, indentLevel) + "\n";
        }
    }

    private static String renderPartitions(RecordSource source, int indentLevel)
    {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(getTabString(indentLevel)).append("partitions:\n");
        strBuf.append(getTabString(indentLevel + 2)).append("[");
        ListIterate.forEachWithIndex(source.partitions, (partition, i) ->
        {
            strBuf.append(i > 0 ? "," : "").append("\n");
            strBuf.append(renderPartition(partition, indentLevel + 3)).append("\n");
            strBuf.append(getTabString(indentLevel + 3)).append("}");
        });
        strBuf.append("\n").append(getTabString(indentLevel + 2)).append("]");
        return strBuf.toString();
    }

    private static String renderTags(Tagable tagable, int indentLevel)
    {
        return getTabString(indentLevel) + "tags: [" + LazyIterate.collect(tagable.getTags(), t -> convertString(t, true)).makeString(", ") + "];";
    }

    private static String renderPartition(RecordSourcePartition partition, int indentLevel)
    {
        StringBuilder builder = new StringBuilder().append(getTabString(indentLevel)).append(partition.id).append(": {");
        builder.append((partition.getTags() != null && !partition.getTags().isEmpty()) ? "\n" + renderTags(partition, indentLevel + 1) : "");
        return builder.toString();
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

        ListIterate.forEachWithIndex(precedenceRules, (precedenceRule, i) ->
        {
            String precedenceRuleString = precedenceRule.accept(new PrecedenceRuleComposer(indentLevel + 1, context, uniqueSourcePrecedenceRules));
            nonSourcePrecedenceRulesBuilder.append(precedenceRuleString);
            nonSourcePrecedenceRulesBuilder.append(i < precedenceRules.size() && !precedenceRuleString.equals("") ? "," : "");
        });
        return combinePrecedenceRules(uniqueSourcePrecedenceRules, nonSourcePrecedenceRulesBuilder.toString(), indentLevel);
    }

    private static String combinePrecedenceRules(Map<Pair<String, String>, StringBuilder> uniqueSourcePrecedenceRules, String nonSourcePrecedenceRules, int indentLevel)
    {
        StringBuilder allPrecedenceRules = new StringBuilder();
        if (uniqueSourcePrecedenceRules.isEmpty())
        {
            nonSourcePrecedenceRules = StringUtils.chomp(nonSourcePrecedenceRules);
        }
        allPrecedenceRules.append(nonSourcePrecedenceRules);
        List<StringBuilder> sourcePrecedenceRules = new ArrayList<>(uniqueSourcePrecedenceRules.values());
        ListIterate.forEachWithIndex(sourcePrecedenceRules, (sourcePrecedenceRule, i) ->
        {
            allPrecedenceRules.append(i > 0 ? "," : "");
            String sourcePrecedenceSTring = StringUtils.chop(sourcePrecedenceRule.toString());
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
            String lambdaStr = lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
            if (lambdaStr.matches(PRECEDENCE_LAMBDA_WITH_FILTER_PREFIX + PRECEDENCE_LAMBDA_WITH_FILTER_SUFFIX))
            {
                String filterPath = lambdaStr.replaceAll(PRECEDENCE_LAMBDA_WITH_FILTER_PREFIX, "");
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
                return builder.append("RecordSourceScope { ").append(recordSourceScope.recordSourceId).toString();
            }
            if (ruleScope instanceof DataProviderTypeScope)
            {
                DataProviderTypeScope dataProviderTypeScope = (DataProviderTypeScope) ruleScope;
                return builder.append("DataProviderTypeScope { ").append(dataProviderTypeScope.dataProviderType.name()).toString();
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
            builder.append(getTabString(indentLevel + 4)).append("keyType: ").append(resolutionQuery.keyType).append(";\n");
            builder.append(getTabString(indentLevel + 4)).append("precedence: ").append(resolutionQuery.precedence).append(";\n");
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
                    getTabString(indentLevel + 1) + "modelClass: " + val.modelClass + ";\n" +
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
