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

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourcePartition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.Tagable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolutionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertPath;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;


/**
 * Convert the protocol to a String parsable by the grammar.
 */
public class HelperMasteryGrammarComposer
{
    private HelperMasteryGrammarComposer()
    {
    }

    public static String renderMastery(MasterRecordDefinition masterRecordDefinition, int indentLevel, PureGrammarComposerContext context)
    {
        String masteryRendered = "MasterRecordDefinition " + convertPath(masterRecordDefinition.getPath()) + "\n" +
                "{\n" +
                renderModelClass(masterRecordDefinition.modelClass, indentLevel, context) +
                renderIdentityResolution(masterRecordDefinition.identityResolution, indentLevel, context) +
                renderRecordSources(masterRecordDefinition.sources, indentLevel, context) +
                "}";
        return masteryRendered;
    }

    /*
     * MasterRecordDefinition Attributes
     */
    private static String renderModelClass(String modelClass, int indentLevel, PureGrammarComposerContext context)
    {
        return getTabString(indentLevel) + "modelClass: " + modelClass + ";\n";
    }

    /*
     * MasterRecordSources
     */
    private static String renderRecordSources(List<RecordSource> sources, int indentLevel, PureGrammarComposerContext context)
    {
        StringBuilder sourcesStr = new StringBuilder()
                .append(getTabString(indentLevel) + "recordSources:\n")
                .append(getTabString(indentLevel)).append("[\n");
                ListIterate.forEachWithIndex(sources, (source, i) ->
                {
                    sourcesStr.append(i > 0 ? ",\n" : "").append(getTabString(indentLevel + 1)).append("{\n");;
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
        public String visit(RecordSource val)
        {
              return getTabString(indentLevel + 2) + "id: " + convertString(val.id, true) + ";\n" +
                    getTabString(indentLevel + 2) + "description: " + convertString(val.description, true) + ";\n" +
                    getTabString(indentLevel + 2) + "status: " + val.status + ";\n" +
                      (val.parseService != null ? (getTabString(indentLevel + 2) + "parseService: " + val.parseService + ";\n") : "") +
                      getTabString(indentLevel + 2) + "transformService: " + val.transformService + ";\n" +
                      (val.sequentialData != null ? getTabString(indentLevel + 2) + "sequentialData: " + val.sequentialData + ";\n" : "") +
                      (val.stagedLoad != null ? getTabString(indentLevel + 2) + "stagedLoad: " + val.stagedLoad + ";\n" : "") +
                      (val.createPermitted != null ? getTabString(indentLevel + 2) + "createPermitted: " + val.createPermitted + ";\n" : "") +
                      (val.createBlockedException != null ? getTabString(indentLevel + 2) + "createBlockedException: " + val.createBlockedException + ";\n" : "") +
                      ((val.getTags() != null && !val.getTags().isEmpty()) ? getTabString(indentLevel + 1) + renderTags(val, indentLevel) + "\n" : "") +
                    getTabString(indentLevel + 1) + renderPartitions(val, indentLevel) + "\n";
        }
    }

    private static String renderPartitions(RecordSource source, int indentLevel)
    {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(getTabString(indentLevel)).append("partitions:\n");
        strBuf.append(getTabString(indentLevel + 2)).append("[");
        ListIterate.forEachWithIndex(source.partitions, (partition, i) ->
        {
            strBuf.append(i > 0 ? "," : "").append("\n").append(getTabString(indentLevel + 3)).append("{\n");;
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
        StringBuffer strBuf = new StringBuffer().append(getTabString(indentLevel + 1)).append("id: ").append(convertString(partition.id, true)).append(";");
        strBuf.append((partition.getTags() != null && !partition.getTags().isEmpty()) ? "\n" + renderTags(partition, indentLevel + 1) : "");
        return strBuf.toString();
    }

    /*
     * Identity and Resolution
     */

    private static String renderIdentityResolution(IdentityResolution identityResolution, int indentLevel, PureGrammarComposerContext context)
    {
        return identityResolution.accept(new IdentityResolutionComposer(indentLevel, context));
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
            //TODO fix internal bug - Issue with lambda builder that it sometimes wraps the whole lambda with { } so sniff (urrrgh!) for the wrappers and add only if not present
            String lambdaStr = lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
            if (!lambdaStr.startsWith("{"))
            {
                builder.append("{").append(lambdaStr).append("}");
            }
            else
            {
                builder.append(lambdaStr);
            }
        });
        return builder.append("\n").toString();
    }
}
