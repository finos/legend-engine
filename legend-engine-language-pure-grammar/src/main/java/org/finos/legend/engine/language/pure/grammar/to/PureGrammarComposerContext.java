// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;

import java.util.List;

public class PureGrammarComposerContext
{
    private final String indentationString;
    private final RenderStyle renderStyle;
    /**
     * This flag will notify the transformer that the value specification should be treated as external parameter value
     * this is the use case in service test when we try to construct the parameter value map for running service Pure
     * execution test. Technically this will make us leave the value alone, e.g. no quotes surrounding string value.
     */
    private final boolean isValueSpecificationExternalParameter;
    /**
     * This flag will notify the transformer that the variable being processed is in the signature of the function,
     * hence omitting the `$` symbol preceding the variable.
     */
    private final boolean isVariableInFunctionSignature;
    // TODO PropertyBracketExpression is deprecated.  Remove flag and related processing once all use has been addressed
    private final boolean isPropertyBracketExpressionModeEnabled;
    public final List<PureGrammarComposerExtension> extensions;
    public final List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> extraSectionComposers;
    public final List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> extraFreeSectionComposers;
    public final List<Function2<ClassMapping, PureGrammarComposerContext, String>> extraClassMappingComposers;
    public final List<Function2<AssociationMapping, PureGrammarComposerContext, String>> extraAssociationMappingComposers;
    public final List<Function2<Connection, PureGrammarComposerContext, org.eclipse.collections.api.tuple.Pair<String, String>>> extraConnectionValueComposers;
    public final List<Function2<InputData, PureGrammarComposerContext, String>> extraMappingTestInputDataComposers;
    public final List<Function2<EmbeddedData, PureGrammarComposerContext, ContentWithType>> extraEmbeddedDataComposers;
    public final List<Function2<TestAssertion, PureGrammarComposerContext, ContentWithType>> extraTestAssertionComposers;
    public final List<Function2<ValueSpecification, PureGrammarComposerContext, String>> extraEmbeddedPureComposers;

    protected PureGrammarComposerContext(Builder builder)
    {
        this.indentationString = builder.indentationString;
        this.renderStyle = builder.renderStyle;
        this.isVariableInFunctionSignature = builder.isVariableInFunctionSignature;
        this.isValueSpecificationExternalParameter = builder.isValueSpecificationExternalParameter;
        this.isPropertyBracketExpressionModeEnabled = builder.isPropertyBracketExpressionModeEnabled;
        // extensions
        this.extensions = PureGrammarComposerExtensionLoader.extensions();
        this.extraSectionComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraSectionComposers);
        this.extraFreeSectionComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraFreeSectionComposers);
        this.extraClassMappingComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraClassMappingComposers);
        this.extraAssociationMappingComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraAssociationMappingComposers);
        this.extraConnectionValueComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraConnectionValueComposers);
        this.extraMappingTestInputDataComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraMappingTestInputDataComposers);
        this.extraEmbeddedDataComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraEmbeddedDataComposers);
        this.extraEmbeddedPureComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraEmbeddedPureComposers);
        this.extraTestAssertionComposers = ListIterate.flatCollect(this.extensions, PureGrammarComposerExtension::getExtraTestAssertionComposers);
    }

    public String getIndentationString()
    {
        return indentationString;
    }

    public boolean isValueSpecificationExternalParameter()
    {
        return isValueSpecificationExternalParameter;
    }

    public RenderStyle getRenderStyle()
    {
        return renderStyle;
    }

    public boolean isVariableInFunctionSignature()
    {
        return isVariableInFunctionSignature;
    }

    public boolean isPropertyBracketExpressionModeEnabled()
    {
        return isPropertyBracketExpressionModeEnabled;
    }

    public static class Builder
    {
        private String indentationString = "";
        private RenderStyle renderStyle = RenderStyle.STANDARD;
        private boolean isValueSpecificationExternalParameter = false;
        private boolean isVariableInFunctionSignature = false;
        private boolean isPropertyBracketExpressionModeEnabled = false;

        private Builder()
        {
            // hide constructor
        }

        public static Builder newInstance(PureGrammarComposerContext composerContext)
        {
            Builder builder = new Builder();
            builder.indentationString = composerContext.indentationString;
            builder.renderStyle = composerContext.renderStyle;
            builder.isVariableInFunctionSignature = composerContext.isVariableInFunctionSignature;
            builder.isValueSpecificationExternalParameter = composerContext.isValueSpecificationExternalParameter;
            builder.isPropertyBracketExpressionModeEnabled = composerContext.isPropertyBracketExpressionModeEnabled;
            return builder;
        }

        // WIP: to be removed when we remove the deprecated composer
        public static Builder newInstance(DEPRECATED_PureGrammarComposerCore DEPRECATED_context)
        {
            Builder builder = new Builder();
            builder.indentationString = DEPRECATED_context.getIndentationString();
            builder.renderStyle = DEPRECATED_context.getRenderStyle();
            builder.isVariableInFunctionSignature = DEPRECATED_context.isVariableInFunctionSignature();
            builder.isValueSpecificationExternalParameter = DEPRECATED_context.isValueSpecificationExternalParameter();
            builder.isPropertyBracketExpressionModeEnabled = DEPRECATED_context.isPropertyBracketExpressionModeEnabled();
            return builder;
        }

        public static Builder newInstance()
        {
            return new Builder();
        }

        public PureGrammarComposerContext build()
        {
            return new PureGrammarComposerContext(this);
        }

        public Builder withRenderStyle(RenderStyle renderStyle)
        {
            this.renderStyle = renderStyle;
            return this;
        }

        public Builder withValueSpecificationAsExternalParameter()
        {
            this.isValueSpecificationExternalParameter = true;
            return this;
        }

        public Builder withVariableInFunctionSignature()
        {
            this.isVariableInFunctionSignature = true;
            return this;
        }

        public Builder withPropertyBracketExpressionModeEnabled()
        {
            this.isPropertyBracketExpressionModeEnabled = true;
            return this;
        }

        public Builder withIndentationString(String indentationString)
        {
            this.indentationString = indentationString;
            return this;
        }

        public Builder withIndentation(int count)
        {
            return this.withIndentation(count, false);
        }

        public Builder withIndentation(int count, boolean indentInStandardRenderingMode)
        {
            String space = RenderStyle.PRETTY_HTML.equals(this.renderStyle) ? "<span class='pureGrammar-space'></span>" : " ";
            this.indentationString = RenderStyle.PRETTY.equals(this.renderStyle) || RenderStyle.PRETTY_HTML.equals(this.renderStyle) || indentInStandardRenderingMode
                    ? this.indentationString + StringUtils.repeat(space, count) : this.indentationString;
            return this;
        }
    }

    public static String computeIndentationString(PureGrammarComposerContext grammarTransformer, int count)
    {
        return Builder.newInstance(grammarTransformer).withIndentation(count).indentationString;
    }

    public boolean isRenderingPretty()
    {
        return RenderStyle.PRETTY.equals(this.renderStyle) || RenderStyle.PRETTY_HTML.equals(this.renderStyle);
    }

    public boolean isRenderingHTML()
    {
        return RenderStyle.PRETTY_HTML.equals(this.renderStyle);
    }

    public String returnChar()
    {
        return this.isRenderingHTML() ? "</BR>\n" : "\n";
    }
}
