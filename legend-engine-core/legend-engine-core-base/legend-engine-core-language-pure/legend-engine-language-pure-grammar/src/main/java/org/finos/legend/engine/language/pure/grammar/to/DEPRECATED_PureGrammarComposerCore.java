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
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.domain.StrictTimeParseTreeWalker;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MergeOperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwarePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.SingleConnectionEngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CByteArray;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTreeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.SubTypeGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderFunctionTestSuites;
import static org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer.*;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabSize;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public final class DEPRECATED_PureGrammarComposerCore implements
        PackageableElementVisitor<String>,
        ValueSpecificationVisitor<String>,
        ClassMappingVisitor<String>,
        PropertyMappingVisitor<String>,
        ConnectionVisitor<String>
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

    private int baseTabLevel = 1;

    private DEPRECATED_PureGrammarComposerCore(DEPRECATED_PureGrammarComposerCore.Builder builder)
    {
        this.indentationString = builder.indentationString;
        this.renderStyle = builder.renderStyle;
        this.isVariableInFunctionSignature = builder.isVariableInFunctionSignature;
        this.isValueSpecificationExternalParameter = builder.isValueSpecificationExternalParameter;
        this.isPropertyBracketExpressionModeEnabled = builder.isPropertyBracketExpressionModeEnabled;
    }

    public int getBaseTabLevel()
    {
        return baseTabLevel;
    }

    public void setBaseTabLevel(int baseTabLevel)
    {
        this.baseTabLevel = baseTabLevel;
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

        public static Builder newInstance(DEPRECATED_PureGrammarComposerCore grammarTransformer)
        {
            Builder builder = new Builder();
            builder.indentationString = grammarTransformer.indentationString;
            builder.renderStyle = grammarTransformer.renderStyle;
            builder.isVariableInFunctionSignature = grammarTransformer.isVariableInFunctionSignature;
            builder.isValueSpecificationExternalParameter = grammarTransformer.isValueSpecificationExternalParameter;
            builder.isPropertyBracketExpressionModeEnabled = grammarTransformer.isPropertyBracketExpressionModeEnabled;
            return builder;
        }

        public static Builder newInstance(PureGrammarComposerContext context)
        {
            Builder builder = new Builder();
            builder.indentationString = context.getIndentationString();
            builder.renderStyle = context.getRenderStyle();
            builder.isVariableInFunctionSignature = context.isVariableInFunctionSignature();
            builder.isValueSpecificationExternalParameter = context.isValueSpecificationExternalParameter();
            builder.isPropertyBracketExpressionModeEnabled = context.isPropertyBracketExpressionModeEnabled();
            return builder;
        }

        public static Builder newInstance()
        {
            return new Builder();
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

        public DEPRECATED_PureGrammarComposerCore build()
        {
            return new DEPRECATED_PureGrammarComposerCore(this);
        }
    }

    public PureGrammarComposerContext toContext()
    {
        return PureGrammarComposerContext.Builder.newInstance(this).build();
    }

    public static String computeIndentationString(DEPRECATED_PureGrammarComposerCore grammarTransformer, int count)
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

    private static final ImmutableSet<String> CORE_FUNCTIONS_WITH_PREFIX_RENDERING = Sets.immutable.of("if", "over");

    // ----------------------------------------------- GENERAL -----------------------------------------------

    @Override
    public String visit(PackageableElement element)
    {
        return unsupported(element.getClass());
    }

    @Override
    public String visit(SectionIndex sectionIndex)
    {
        return unsupported(SectionIndex.class);
    }


    // ----------------------------------------------- DOMAIN -----------------------------------------------

    @Override
    public String visit(Profile profile)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Profile ").append(PureGrammarComposerUtility.convertPath(profile.getPath())).append("\n").append("{\n");
        if (profile.stereotypes != null && !profile.stereotypes.isEmpty())
        {
            builder.append(getTabString()).append("stereotypes: [").append(Lists.mutable.withAll(profile.stereotypes).collect(profileStereoType -> PureGrammarComposerUtility.convertIdentifier(profileStereoType.value)).makeString(", ")).append("];\n");
        }
        if (profile.tags != null && !profile.tags.isEmpty())
        {
            builder.append(getTabString()).append("tags: [").append(Lists.mutable.withAll(profile.tags).collect(profileTag -> PureGrammarComposerUtility.convertIdentifier(profileTag.value)).makeString(", ")).append("];\n");
        }
        return builder.append("}").toString();
    }

    @Override
    public String visit(Enumeration _enum)
    {
        return "Enum " + HelperDomainGrammarComposer.renderAnnotations(_enum.stereotypes, _enum.taggedValues) + PureGrammarComposerUtility.convertPath(_enum.getPath()) +
                "\n{\n" +
                LazyIterate.collect(_enum.values, enumValue -> getTabString() + HelperDomainGrammarComposer.renderEnumValue(enumValue)).makeString(",\n") + (_enum.values.isEmpty() ? "" : "\n") +
                "}";
    }

    @Override
    public String visit(Measure measure)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Measure ").append(PureGrammarComposerUtility.convertPath(measure.getPath()));
        builder.append("\n");
        builder.append("{\n");
        if (measure.canonicalUnit != null)
        {
            builder.append(getTabString()).append(measure.canonicalUnit.conversionFunction != null ? "*" : "").append(HelperDomainGrammarComposer.renderUnit(measure.canonicalUnit, this)).append("\n");
        }
        if (measure.nonCanonicalUnits != null && !measure.nonCanonicalUnits.isEmpty())
        {
            builder.append(LazyIterate.collect(measure.nonCanonicalUnits, ncu -> getTabString() + HelperDomainGrammarComposer.renderUnit(ncu, this)).makeString("\n")).append("\n");
        }
        return builder.append("}").toString();
    }

    @Override
    public String visit(Class _class)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Class ").append(HelperDomainGrammarComposer.renderAnnotations(_class.stereotypes, _class.taggedValues)).append(PureGrammarComposerUtility.convertPath(_class.getPath()));
        if (!_class.superTypes.isEmpty())
        {
            builder.append(" extends ").append(_class.superTypes.stream().map(x -> x.path).collect(Collectors.joining(", ")));
        }
        builder.append("\n");
        if (!_class.constraints.isEmpty())
        {
            builder.append("[\n");
            builder.append(LazyIterate.collect(_class.constraints, constraint -> getTabString() + HelperDomainGrammarComposer.renderConstraint(constraint, _class.constraints, this)).makeString(",\n")).append("\n");
            builder.append("]\n");
        }
        builder.append("{\n");
        if (!_class.properties.isEmpty())
        {
            builder.append(LazyIterate.collect(_class.properties, p -> getTabString() + HelperDomainGrammarComposer.renderProperty(p, this) + ";").makeString("\n")).append("\n");
        }
        if (!_class.qualifiedProperties.isEmpty())
        {
            builder.append(LazyIterate.collect(_class.qualifiedProperties, p -> getTabString() + HelperDomainGrammarComposer.renderDerivedProperty(p, this) + ";").makeString("\n")).append("\n");
        }
        return builder.append("}").toString();
    }

    @Override
    public String visit(Association association)
    {
        return "Association " + HelperDomainGrammarComposer.renderAnnotations(association.stereotypes, association.taggedValues) + PureGrammarComposerUtility.convertPath(association.getPath()) + "\n" +
                "{\n" +
                LazyIterate.collect(association.properties, p -> getTabString() + HelperDomainGrammarComposer.renderProperty(p, this) + ";").makeString("\n") + (association.properties.isEmpty() ? "" : "\n") +
                LazyIterate.collect(association.qualifiedProperties, p -> getTabString() + HelperDomainGrammarComposer.renderDerivedProperty(p, this) + ";").makeString("\n") + (association.qualifiedProperties.isEmpty() ? "" : "\n") +
                "}";
    }

    @Override
    public String visit(Function function)
    {
        return "function " + HelperDomainGrammarComposer.renderAnnotations(function.stereotypes, function.taggedValues) + PureGrammarComposerUtility.convertPath(HelperValueSpecificationGrammarComposer.getFunctionName(function))
                + "(" + LazyIterate.collect(function.parameters, p -> p.accept(Builder.newInstance(this).withVariableInFunctionSignature().build())).makeString(", ") + ")"
                + ": " + HelperValueSpecificationGrammarComposer.printGenericType(function.returnGenericType, this) + "[" + HelperDomainGrammarComposer.renderMultiplicity(function.returnMultiplicity) + "]\n" +
                "{\n" +
                LazyIterate.collect(function.body, b -> "  " + b.accept(Builder.newInstance(this).withIndentation(getTabSize(1)).build())).makeString(";\n") + (function.body.size() > 1 ? ";" : "") +
                "\n}" +
                renderFunctionTestSuites(function, toContext());
    }


    // ----------------------------------------------- MAPPING -----------------------------------------------

    @Override
    public String visit(Mapping mapping)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Mapping").append(" ").append(PureGrammarComposerUtility.convertPath(mapping.getPath()));
        builder.append("\n(\n");
        boolean isMappingContentEmpty = true;
        PureGrammarComposerContext context = this.toContext();
        if (!mapping.includedMappings.isEmpty())
        {
            isMappingContentEmpty = false;
            builder.append(LazyIterate.collect(mapping.includedMappings, mappingInclude -> getTabString() + HelperMappingGrammarComposer.renderMappingInclude(mappingInclude, context)).makeString("\n"));
            builder.append("\n");
        }
        if (mapping.classMappings != null && !mapping.classMappings.isEmpty())
        {
            builder.append(isMappingContentEmpty ? "" : "\n");
            isMappingContentEmpty = false;
            builder.append(LazyIterate.collect(mapping.classMappings, classMapping -> getTabString() + (classMapping.root ? "*" : "")
                    + classMapping._class + HelperMappingGrammarComposer.renderClassMappingId(classMapping)
                    + (classMapping.extendsClassMappingId != null ? " extends " + HelperMappingGrammarComposer.renderMappingId(classMapping.extendsClassMappingId) : "")
                    + classMapping.accept(this)).makeString("\n"));
            builder.append("\n");
        }
        if (!mapping.associationMappings.isEmpty())
        {
            builder.append(isMappingContentEmpty ? "" : "\n");
            isMappingContentEmpty = false;
            builder.append(LazyIterate.collect(mapping.associationMappings, associationMapping -> getTabString() + HelperMappingGrammarComposer.renderAssociationMapping(associationMapping, context)).makeString("\n"));
            builder.append("\n");
        }
        if (!mapping.enumerationMappings.isEmpty())
        {
            builder.append(isMappingContentEmpty ? "" : "\n");
            isMappingContentEmpty = false;
            builder.append(LazyIterate.collect(mapping.enumerationMappings, enumerationMapping -> getTabString() + HelperMappingGrammarComposer.renderEnumerationMapping(enumerationMapping)).makeString("\n"));
            builder.append("\n");
        }
        if (!mapping.tests.isEmpty())
        {
            builder.append(isMappingContentEmpty ? "" : "\n");
            builder.append(getTabString()).append("MappingTests\n").append(getTabString()).append("[\n");
            builder.append(LazyIterate.collect(mapping.tests, mappingTest -> getTabString() + HelperMappingGrammarComposer.renderMappingTest(mappingTest, this)).makeString(",\n")).append(mapping.tests.isEmpty() ? "" : "\n");
            builder.append(getTabString()).append("]\n");
        }
        if (mapping.testSuites != null && !mapping.testSuites.isEmpty())
        {
            builder.append(isMappingContentEmpty ? "" : "\n");
            builder.append(getTabString()).append("testSuites:\n").append(getTabString()).append("[\n");
            builder.append(LazyIterate.collect(mapping.testSuites, mappingTestSuite -> getTabString() + HelperMappingGrammarComposer.renderMappingTestSuite(mappingTestSuite, this)).makeString(",\n")).append("\n");
            builder.append(getTabString()).append("]\n");
        }
        return builder.append(")").toString();
    }

    @Override
    public String visit(ClassMapping classMapping)
    {
        PureGrammarComposerContext context = this.toContext();
        return context.extraClassMappingComposers.stream().map(composer -> composer.value(classMapping, context)).filter(Objects::nonNull).findFirst().orElseGet(() -> unsupported(classMapping.getClass()));
    }

    @Override
    public String visit(PropertyMapping propertyMapping)
    {
        return unsupported(propertyMapping.getClass());
    }

    public String visit(OperationClassMapping operationClassMapping)
    {
        if (operationClassMapping instanceof MergeOperationClassMapping)
        {
            return ": " + "Operation\n" +
                    getTabString() + "{\n" +
                    getTabString(2) + OperationClassMapping.opsToFunc.get(operationClassMapping.operation) + "([" + LazyIterate.collect(operationClassMapping.parameters, Functions.identity()).makeString(",") + "]," +
                    ((MergeOperationClassMapping) operationClassMapping).validationFunction.body.get(0).accept(this) + ")\n" +
                    getTabString() + "}";
        }
        else
        {
            return ": " + "Operation\n" +
                    getTabString() + "{\n" +
                    getTabString(2) + OperationClassMapping.opsToFunc.get(operationClassMapping.operation) + '(' + LazyIterate.collect(operationClassMapping.parameters, Functions.identity()).makeString(",") + ")\n" +
                    getTabString() + "}";
        }
    }

    @Override
    public String visit(PureInstanceClassMapping pureInstanceClassMapping)
    {
        String pureFilter = "";
        if (pureInstanceClassMapping.filter != null)
        {
            pureInstanceClassMapping.filter.parameters = Collections.emptyList();
            String filterString = pureInstanceClassMapping.filter.accept(this).replaceFirst("\\|", "");
            pureFilter = getTabString(2) + "~filter " + filterString + "\n";
        }
        return ": " + "Pure\n" +
                getTabString(getBaseTabLevel()) + "{\n" +
                (pureInstanceClassMapping.srcClass == null ? "" : getTabString(getBaseTabLevel() + 1) + "~src " + pureInstanceClassMapping.srcClass + "\n") + pureFilter +
                LazyIterate.collect(pureInstanceClassMapping.propertyMappings, propertyMapping -> getTabString(getBaseTabLevel() + 1) + propertyMapping.accept(this)).makeString(",\n") + (pureInstanceClassMapping.propertyMappings.isEmpty() ? "" : "\n") +
                getTabString(getBaseTabLevel()) + "}";
    }

    @Override
    public String visit(PurePropertyMapping purePropertyMapping)
    {
        purePropertyMapping.transform.parameters = Collections.emptyList();
        String lambdaString = purePropertyMapping.transform.accept(this).replaceFirst("\\|", "");
        return PureGrammarComposerUtility.renderPossibleLocalMappingProperty(purePropertyMapping) +
                (purePropertyMapping.explodeProperty != null && purePropertyMapping.explodeProperty ? "*" : "") +
                (purePropertyMapping.target == null || purePropertyMapping.target.isEmpty() ? "" : "[" + PureGrammarComposerUtility.convertIdentifier(purePropertyMapping.target) + "]") +
                (purePropertyMapping.enumMappingId == null ? "" : ": EnumerationMapping " + purePropertyMapping.enumMappingId) +
                ": " + lambdaString;
    }

    @Override
    public String visit(XStorePropertyMapping propertyMapping)
    {
        return unsupported(XStorePropertyMapping.class);
    }

    @Override
    public String visit(AggregationAwareClassMapping classMapping)
    {
        String mainMapping = "";
        if (classMapping.mainSetImplementation != null)
        {
            setBaseTabLevel(2);
            mainMapping = "~mainMapping" + classMapping.mainSetImplementation.accept(this);
            setBaseTabLevel(1);
        }
        return ": " + "AggregationAware " + "\n" +
                getTabString() + "{\n" +
                getTabString(2) + "Views" + ":" + " [\n" +
                (classMapping.aggregateSetImplementations == null ? "" : LazyIterate.collect(classMapping.aggregateSetImplementations, implementation -> getTabString(3) + HelperMappingGrammarComposer.renderAggregateSetImplementationContainer(implementation, this)).makeString(",\n")) +
                getTabString(2) + "],\n" +
                getTabString(2) + mainMapping + "\n" +
                getTabString() + "}";
    }

    @Override
    public String visit(RelationFunctionClassMapping classMapping)
    {
        return ": Relation\n" +
        getTabString(getBaseTabLevel()) + "{\n" +
        getTabString(getBaseTabLevel() + 1) + "~func " + classMapping.relationFunction + "\n" +
        LazyIterate.collect(classMapping.propertyMappings, pm -> getTabString(getBaseTabLevel() + 1) + pm.accept(this)).makeString(",\n") + (classMapping.propertyMappings.isEmpty() ? "" : "\n") +
        getTabString(getBaseTabLevel()) + "}";
    }

    @Override
    public String visit(AggregationAwarePropertyMapping propertyMapping)
    {
        return unsupported(AggregationAwarePropertyMapping.class);
    }

    @Override
    public String visit(RelationFunctionPropertyMapping propertyMapping)
    {
        return PureGrammarComposerUtility.renderPossibleLocalMappingProperty(propertyMapping) +
                ": " + PureGrammarComposerUtility.convertIdentifier(propertyMapping.column, false);
    }

    // ----------------------------------------------- CONNECTION -----------------------------------------------

    @Override
    public String visit(PackageableConnection packageableConnection)
    {
        return HelperConnectionGrammarComposer.getConnectionValueName(packageableConnection.connectionValue, this.toContext()) +
                " " + PureGrammarComposerUtility.convertPath(packageableConnection.getPath()) + "\n" +
                packageableConnection.connectionValue.accept(this);
    }

    @Override
    public String visit(Connection connection)
    {
        PureGrammarComposerContext context = this.toContext();
        Optional<org.eclipse.collections.api.tuple.Pair<String, String>> connectionValueString = context.extraConnectionValueComposers.stream().map(composer -> composer.value(connection, context)).filter(Objects::nonNull).findFirst();
        return connectionValueString.orElseGet(() -> Tuples.pair(null, unsupported(connection.getClass()))).getTwo();
    }

    @Override
    public String visit(ConnectionPointer connectionPointer)
    {
        return connectionPointer.connection;
    }

    @Override
    public String visit(JsonModelConnection jsonModelConnection)
    {
        int baseIndentation = 0;
        return this.indentationString + getTabString(baseIndentation) + "{\n" +
                this.indentationString + getTabString(baseIndentation + 1) + "class: " + jsonModelConnection._class + ";\n" +
                this.indentationString + getTabString(baseIndentation + 1) + "url: " + convertString(jsonModelConnection.url, true) + ";\n" +
                this.indentationString + getTabString(baseIndentation) + "}";
    }

    @Override
    public String visit(XmlModelConnection xmlModelConnection)
    {
        int baseIndentation = 0;
        return this.indentationString + getTabString(baseIndentation) + "{\n" +
                this.indentationString + getTabString(baseIndentation + 1) + "class: " + xmlModelConnection._class + ";\n" +
                this.indentationString + getTabString(baseIndentation + 1) + "url: " + convertString(xmlModelConnection.url, true) + ";\n" +
                this.indentationString + getTabString(baseIndentation) + "}";
    }

    @Override
    public String visit(ModelConnection modelConnection)
    {
        return unsupported(ModelConnection.class);
    }

    @Override
    public String visit(ModelChainConnection modelChainConnection)
    {
        int baseIndentation = 0;
        String mappingsValue = "";
        if (modelChainConnection.mappings != null)
        {
            mappingsValue = modelChainConnection.mappings.stream().map(m -> this.indentationString + getTabString(baseIndentation + 2) + m).collect(Collectors.joining(",\n")) + (modelChainConnection.mappings.isEmpty() ? "" : "\n");
        }
        String mappings = "[\n" + mappingsValue + this.indentationString + getTabString(baseIndentation + 1) + "]";
        return this.indentationString + getTabString(baseIndentation) + "{\n" +
                this.indentationString + getTabString(baseIndentation + 1) + "mappings: " + mappings + ";\n" +
                this.indentationString + getTabString(baseIndentation) + "}";
    }


    // ----------------------------------------------- RUNTIME -----------------------------------------------

    @Override
    public String visit(PackageableRuntime packageableRuntime)
    {
        String runtimeType = packageableRuntime.runtimeValue instanceof SingleConnectionEngineRuntime
                ? "SingleConnectionRuntime "
                : "Runtime ";
        return runtimeType + PureGrammarComposerUtility.convertPath(packageableRuntime.getPath()) + "\n" +
                "{" +
                HelperRuntimeGrammarComposer.renderRuntimeValue(packageableRuntime.runtimeValue, 1, false, this) +
                "\n}";
    }


    // ---------------------------------------- VALUE SPECIFICATION ---------------------------------------

    @Override
    public String visit(ValueSpecification valueSpecification)
    {
        return unsupported(valueSpecification.getClass());
    }

    @Override
    public String visit(org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class _class)
    {
        return HelperValueSpecificationGrammarComposer.printFullPath(_class.fullPath, this);
    }

    @Override
    public String visit(Enum _enum)
    {
        return HelperValueSpecificationGrammarComposer.printFullPath(_enum.fullPath, this);
    }

    @Override
    public String visit(PackageableElementPtr packageableElementPtr)
    {
        return HelperValueSpecificationGrammarComposer.printFullPath(packageableElementPtr.fullPath, this);
    }

    @Override
    public String visit(EnumValue enumValue)
    {
        return HelperValueSpecificationGrammarComposer.printFullPath(enumValue.fullPath, this) + "." + enumValue.value;
    }

    @Override
    public String visit(Variable variable)
    {
        return (this.isRenderingHTML() ? "<span class='pureGrammar-var'>" : "") +
                (this.isVariableInFunctionSignature ? "" : "$") +
                PureGrammarComposerUtility.convertIdentifier(variable.name) +
                (this.isRenderingHTML() ? "</span>" : "") +
                (variable.genericType != null ? ": " + HelperValueSpecificationGrammarComposer.printGenericType(variable.genericType, this) + "[" + HelperDomainGrammarComposer.renderMultiplicity(variable.multiplicity) + "]" : "");
    }

    @Override
    public String visit(Lambda lambda)
    {
        if (lambda.body == null)
        {
            return "";
        }
        boolean addWrapper = lambda.body.size() > 1 || lambda.parameters.size() > 1;
        boolean addCR = lambda.body.size() > 1;
        return (addWrapper ? "{" : "")
                + (lambda.parameters.isEmpty() ? "" : LazyIterate.collect(lambda.parameters, variable -> variable.accept(Builder.newInstance(this).withVariableInFunctionSignature().build())).makeString(","))
                + "|" + (addCR ? this.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) : "")
                + LazyIterate.collect(lambda.body, valueSpecification -> valueSpecification.accept(addCR ? DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this).withIndentation(getTabSize(1)).build() : this)).makeString(";" + this.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)))
                + (addCR ? ";" + this.returnChar() : "") + (addWrapper ? this.indentationString + "}" : "");
    }

    @Override
    public String visit(ClassInstance iv)
    {
        switch (iv.type)
        {
            case "path":
                Path path = (Path) iv.value;
                if (this.isRenderingHTML())
                {
                    int index = path.startType.lastIndexOf("::");
                    String type = index == -1 ?
                            "<span class='pureGrammar-packageableElement'>" + PureGrammarComposerUtility.convertPath(path.startType) + "</span>" :
                            "<span class='pureGrammar-package'>" + PureGrammarComposerUtility.convertPath(path.startType.substring(0, index + 2)) + "</span><span class='pureGrammar-packageableElement'>" + PureGrammarComposerUtility.convertIdentifier(path.startType.substring(index + 2)) + "</span>";
                    return "#/" + type + (path.path.isEmpty() ? "" : "/" + ListAdapter.adapt(path.path).collect(p -> HelperValueSpecificationGrammarComposer.renderPathElement(p, this)).makeString("/")) + (path.name == null || "".equals(path.name) ? "" : "!" + path.name) + "#";
                }
                return "#/" + PureGrammarComposerUtility.convertPath(path.startType) + (path.path.isEmpty() ? "" : "/" + ListAdapter.adapt(path.path).collect(p -> HelperValueSpecificationGrammarComposer.renderPathElement(p, this)).makeString("/")) + (path.name == null || "".equals(path.name) ? "" : "!" + path.name) + "#";
            case "rootGraphFetchTree":
                RootGraphFetchTree rootGraphFetchTree = (RootGraphFetchTree) iv.value;
                return processGraphFetchTree(rootGraphFetchTree);
            case ">":
                return "#>{" + Lists.mutable.withAll(((RelationStoreAccessor) iv.value).path).makeString(".") + "}#";
            case "colSpec":
                ColSpec col = (ColSpec) iv.value;
                return "~" + HelperValueSpecificationGrammarComposer.printColSpec(col, this);
            case "colSpecArray":
                ColSpecArray colArray = (ColSpecArray) iv.value;
                return printColSpecArray(colArray, this);
            case "keyExpression":
                KeyExpression keyExpression = (KeyExpression) iv.value;
                return PureGrammarParserUtility.removeQuotes(keyExpression.key.accept(this)) + "=" + keyExpression.expression.accept(this);
            case "primitiveType":
                PrimitiveType primitiveType = (PrimitiveType) iv.value;
                return primitiveType.fullPath;
            case "listInstance":
                PureList pureList = (PureList) iv.value;
                return LazyIterate.collect(pureList.values, v -> v.accept(this)).makeString("list([", ",", "])");
            case "aggregateValue":
                AggregateValue aggregateValue = (AggregateValue) iv.value;
                return (this.isRenderingHTML() ? "<span class='pureGrammar-function'>" : "") + "agg" + (this.isRenderingHTML() ? "</span>" : "") + "(" + aggregateValue.mapFn.accept(this) + ", " + aggregateValue.aggregateFn.accept(this) + ")";
            case "tdsOlapRank":
                TdsOlapRank tdsOlapRank = (TdsOlapRank) iv.value;
                return (this.isRenderingHTML() ? "<span class='pureGrammar-function'>" : "") + "olapGroupBy" + (this.isRenderingHTML() ? "</span>" : "") + "(" + tdsOlapRank.function.accept(this) + ")";
            case "tdsOlapAggregation":
                TdsOlapAggregation tdsOlapAggregation = (TdsOlapAggregation) iv.value;
                return (this.isRenderingHTML() ? "<span class='pureGrammar-function'>" : "") + "olapGroupBy" + (this.isRenderingHTML() ? "</span>" : "") + "(" + tdsOlapAggregation.function.accept(this) + ")";
            case "tdsAggregateValue":
                TDSAggregateValue tdsAggregateValue = (TDSAggregateValue) iv.value;
                return (this.isRenderingHTML() ? "<span class='pureGrammar-function'>" : "") + "agg" + (this.isRenderingHTML() ? "</span>" : "") + "(" + convertString(tdsAggregateValue.name, true) + "," + tdsAggregateValue.mapFn.accept(this) + ", " + tdsAggregateValue.aggregateFn.accept(this) + ")";

            default:
                PureGrammarComposerContext context = this.toContext();
                Function2<Object, PureGrammarComposerContext, String> val = context.extraEmbeddedPureComposers.get(iv.type);
                if (val != null)
                {
                    return val.value(iv.value, context);
                }
                throw new RuntimeException("/* Unsupported instance value " + iv.type + " */");
        }
    }

    @Override
    public String visit(AppliedFunction appliedFunction)
    {
        String _function = appliedFunction.function;
        int index = _function.lastIndexOf("::");
        String function = index == -1 ? _function : _function.substring(index + 2);
        List<ValueSpecification> parameters = appliedFunction.parameters;

        if ("getAll".equals(function))
        {
            return parameters.get(0).accept(this) + "." + HelperValueSpecificationGrammarComposer.renderFunctionName("all", this) + "(" + LazyIterate.collect(parameters.subList(1, parameters.size()), v -> v.accept(this)).makeString(", ") + ")";
        }
        else if ("getAllVersions".equals(function))
        {
            return parameters.get(0).accept(this) + "." + HelperValueSpecificationGrammarComposer.renderFunctionName("allVersions", this) + "(" + LazyIterate.collect(parameters.subList(1, parameters.size()), v -> v.accept(this)).makeString(", ") + ")";
        }
        else if ("letFunction".equals(function))
        {
            return "let " + PureGrammarComposerUtility.convertIdentifier(((CString) parameters.get(0)).value) + " = " + parameters.get(1).accept(this);
        }
        else if ("cast".equals(function))
        {
            String castType = parameters.get(1).accept(this);
            if (parameters.get(1) instanceof GenericTypeInstance)
            {
                castType = "@" + castType;
            }
            return possiblyAddParenthesis(parameters.get(0), this) + "->" + _function + "(" + castType + ")";
        }
        else if ("subType".equals(function))
        {
            return parameters.get(0).accept(this) + "->" + _function + "(@" + parameters.get(1).accept(this) + ")";
        }
        else if ("new".equals(function))
        {
            ValueSpecification param = parameters.get(parameters.size() - 1);
            List<ValueSpecification> values = param instanceof Collection ? ((Collection) param).values : Collections.singletonList(param);
            String type;
            if (parameters.get(0) instanceof GenericTypeInstance)
            {
                type = printGenericType(((GenericTypeInstance) parameters.get(0)).genericType.typeArguments.get(0), this);
            }
            else
            {
                type = parameters.get(0).accept(this);
            }
            return "^" + type + "(" + Lists.mutable.withAll(values).collect(v -> v.accept(this)).makeString(" , ") + ")";
        }
        else if ("not".equals(function))
        {
            if (parameters.get(0) instanceof AppliedFunction && ((AppliedFunction) parameters.get(0)).function.equals("equal"))
            {
                return possiblyAddParenthesis("not", ((AppliedFunction) parameters.get(0)).parameters.get(0), this) + " != " + possiblyAddParenthesis("not", ((AppliedFunction) parameters.get(0)).parameters.get(1), this);
            }
            return "!" + possiblyAddParenthesis("not", parameters.get(0), this);
        }
        // Special case divide_Decimal_1__Decimal_1__Integer_1__Decimal_1_ so that we don't use infix version
        else if ("divide".equals(function) && parameters.size() == 3)
        {
            return HelperValueSpecificationGrammarComposer.renderFunction(appliedFunction, this);
        }
        else if (isInfix(appliedFunction))
        {
            // handle arithmetic operators that we might need to add parenthesis to force precedence
            if (parameters.get(0) instanceof Collection && ("plus".equals(function) || "minus".equals(function) || "times".equals(function) || "divide".equals(function)))
            {
                return LazyIterate.collect(((Collection) parameters.get(0)).values, v -> possiblyAddParenthesis(function, v, this)).makeString(" " + HelperValueSpecificationGrammarComposer.SPECIAL_INFIX.get(function) + " ");
            }
            else if ("minus".equals(function))
            {
                return "-" + possiblyAddParenthesis("minus", parameters.get(0), this);
            }
            // for this case, we will render function name 'and()', 'or()', etc. instead of trying to swap out to use infix operators like '&&', '||', etc.
            else if (parameters.size() == 1)
            {
                return HelperValueSpecificationGrammarComposer.renderFunction(appliedFunction, this);
            }
            // NOTE: we only handle case when there are 2 parameters because that is the expected form for infix operators
            boolean toCreateNewLine = this.isRenderingPretty() &&
                    !HelperValueSpecificationGrammarComposer.isPrimitiveValue(parameters.get(0)) &&
                    !HelperValueSpecificationGrammarComposer.isPrimitiveValue(parameters.get(1));
            return possiblyAddParenthesis(function, parameters.get(0), this)
                    + " "
                    + HelperValueSpecificationGrammarComposer.SPECIAL_INFIX.get(function)
                    + (toCreateNewLine ? this.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) : " ")
                    + possiblyAddParenthesis(function, parameters.get(1), this);
        }
        else if (CORE_FUNCTIONS_WITH_PREFIX_RENDERING.contains(function))
        {
            return HelperValueSpecificationGrammarComposer.renderFunctionName(function, this) + "(" +
                    (this.isRenderingPretty() ? this.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) : "") +
                    ListIterate.collect(parameters, p -> p.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this).withIndentation(getTabSize(1)).build()))
                            .makeString("," + (this.isRenderingPretty() ? this.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) : " ")) +
                    (this.isRenderingPretty() ? this.returnChar() + DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(0)) : "") + ")";
        }
        return HelperValueSpecificationGrammarComposer.renderFunction(appliedFunction, this);
    }

    @Override
    public String visit(AppliedProperty appliedProperty)
    {
        String propertyOwner = appliedProperty.parameters.get(0).accept(this);
        StringBuilder stringBuilder = new StringBuilder(propertyOwner);
        if (this.isPropertyBracketExpressionModeEnabled && propertyOwner.equals("$src"))
        {
            stringBuilder.append(appliedProperty.parameters.subList(1, appliedProperty.parameters.size()).stream().map(l -> l.accept(this)).collect(Collectors.joining(", ", "[", "]")));
        }
        else
        {
            stringBuilder.append(".");
            stringBuilder.append(this.isRenderingHTML() ? "<span class=pureGrammar-property>" : "").append(PureGrammarComposerUtility.convertIdentifier(appliedProperty.property)).append(this.isRenderingHTML() ? "</span>" : "");
            if (appliedProperty.parameters.size() > 1)
            {
                stringBuilder.append(appliedProperty.parameters.subList(1, appliedProperty.parameters.size()).stream().map(l -> l.accept(this)).collect(Collectors.joining(", ", "(", ")")));
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String visit(Collection collection)
    {
        return HelperValueSpecificationGrammarComposer.renderCollection(collection.values, v -> possiblyAddParenthesis((ValueSpecification) v, this), this);
    }

    @Override
    public String visit(CInteger cInteger)
    {
        return HelperValueSpecificationGrammarComposer.renderInteger(cInteger.value, this);
    }

    @Override
    public String visit(CDecimal cDecimal)
    {
        return HelperValueSpecificationGrammarComposer.renderDecimal(cDecimal.value, this);
    }

    @Override
    public String visit(CString cString)
    {
        return HelperValueSpecificationGrammarComposer.renderString(cString.value, this);
    }

    @Override
    public String visit(CBoolean cBoolean)
    {
        return HelperValueSpecificationGrammarComposer.renderBoolean(cBoolean.value, this);
    }

    @Override
    public String visit(CFloat cFloat)
    {
        return HelperValueSpecificationGrammarComposer.renderFloat(cFloat.value, this);
    }

    @Override
    public String visit(CDateTime cDateTime)
    {
        return HelperValueSpecificationGrammarComposer.renderDate(cDateTime.value, this);
    }

    @Override
    public String visit(CStrictDate cStrictDate)
    {
        String strictDateValue = HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(cStrictDate.value);
        return this.isValueSpecificationExternalParameter ? strictDateValue.replaceFirst(Character.toString(StrictTimeParseTreeWalker.STRICT_TIME_PREFIX), "") : strictDateValue;
    }

    @Override
    public String visit(CStrictTime CStrictTime)
    {
        String strictTimeValue = HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(CStrictTime.value);
        return this.isValueSpecificationExternalParameter ? strictTimeValue.replaceFirst(Character.toString(StrictTimeParseTreeWalker.STRICT_TIME_PREFIX), "") : strictTimeValue;
    }

    @Override
    public String visit(CLatestDate cLatestDate)
    {
        return "%latest";
    }

    @Override
    public String visit(CByteArray cByteArray)
    {
        return "toBytes(" + HelperValueSpecificationGrammarComposer.renderString(new String(cByteArray.value, StandardCharsets.UTF_8), this) + ")";
    }

    @Override
    public String visit(MappingInstance mappingInstance)
    {
        return mappingInstance.fullPath;
    }


    public String processGraphFetchTree(GraphFetchTree graphFetchTree, int tab)
    {
        return graphFetchTree.accept(new GraphFetchTreeVisitor<String>()
        {
            @Override
            public String visit(PropertyGraphFetchTree valueSpecification)
            {
                return processGraphFetchTree(valueSpecification);
            }

            @Override
            public String visit(RootGraphFetchTree valueSpecification)
            {
                return processGraphFetchTree(valueSpecification);
            }

            @Override
            public String visit(SubTypeGraphFetchTree valueSpecification)
            {
                return processGraphFetchTree(valueSpecification);
            }
        });
    }

    private String processGraphFetchTree(RootGraphFetchTree rootGraphFetchTree)
    {
        String subTreeString = "";
        String subTypeTreeString = "";
        if (rootGraphFetchTree.subTrees != null && !rootGraphFetchTree.subTrees.isEmpty())
        {
            subTreeString = rootGraphFetchTree.subTrees.stream().map(x -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this).withIndentation(getTabSize(1)).build().processGraphFetchTree(x, getTabSize(1))).collect(Collectors.joining("," + (this.isRenderingPretty() ? this.returnChar() : "")));
        }
        if (rootGraphFetchTree.subTypeTrees != null && !rootGraphFetchTree.subTypeTrees.isEmpty())
        {
            subTypeTreeString = subTypeTreeString + rootGraphFetchTree.subTypeTrees.stream().map(x -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this).withIndentation(getTabSize(1)).build().processGraphFetchTree(x, getTabSize(1))).collect(Collectors.joining("," + (this.isRenderingPretty() ? this.returnChar() : "")));
        }
        return "#{" + (this.isRenderingPretty() ? this.returnChar() : "") +
                DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) + HelperValueSpecificationGrammarComposer.printFullPath(rootGraphFetchTree._class, this) + "{" + (this.isRenderingPretty() ? this.returnChar() : "") +
                subTreeString + (!subTreeString.isEmpty() && !subTypeTreeString.isEmpty() ? (this.isRenderingPretty() ? "," + this.returnChar() : ",") : "") + subTypeTreeString + (this.isRenderingPretty() ? this.returnChar() : "") +
                DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) + "}" + (this.isRenderingPretty() ? this.returnChar() : "") +
                this.indentationString + "}#";
    }

    public String processGraphFetchTree(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        String aliasString = "";
        if (propertyGraphFetchTree.alias != null)
        {
            aliasString = convertString(propertyGraphFetchTree.alias, false) + ":"; // we do not need to escape here because the alias need to be a valid string
        }

        String subTreeString = "";
        if (propertyGraphFetchTree.subTrees != null && !propertyGraphFetchTree.subTrees.isEmpty())
        {
            subTreeString = "{" + (this.isRenderingPretty() ? this.returnChar() : "") +
                    propertyGraphFetchTree.subTrees.stream().map(x -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this).withIndentation(getTabSize(1)).build().processGraphFetchTree(x, getTabSize(1))).collect(Collectors.joining("," + (this.isRenderingPretty() ? this.returnChar() : ""))) + (this.isRenderingPretty() ? this.returnChar() : "") +
                    DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) + "}";
        }

        String parametersString = "";
        if (propertyGraphFetchTree.parameters != null && !propertyGraphFetchTree.parameters.isEmpty())
        {
            parametersString = propertyGraphFetchTree.parameters.stream().map(x -> x.accept(this)).collect(Collectors.joining(", ", "(", ")"));
        }

        String subTypeString = "";
        if (propertyGraphFetchTree.subType != null)
        {
            subTypeString = "->subType(@" + HelperValueSpecificationGrammarComposer.printFullPath(propertyGraphFetchTree.subType, this) + ")";
        }

        return DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) + aliasString + propertyGraphFetchTree.property + parametersString + subTypeString + subTreeString;
    }

    public String processGraphFetchTree(SubTypeGraphFetchTree subTypeGraphFetchTree)
    {
        String subTreeString = "";
        if (subTypeGraphFetchTree.subTrees != null && !subTypeGraphFetchTree.subTrees.isEmpty())
        {
            subTreeString = "{" + (this.isRenderingPretty() ? this.returnChar() : "") +
                    subTypeGraphFetchTree.subTrees.stream().map(x -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this).withIndentation(getTabSize(1)).build().processGraphFetchTree(x, getTabSize(1))).collect(Collectors.joining("," + (this.isRenderingPretty() ? this.returnChar() : ""))) + (this.isRenderingPretty() ? this.returnChar() : "") +
                    DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) + "}";
        }
        String subTypeString = "->subType(@" + HelperValueSpecificationGrammarComposer.printFullPath(subTypeGraphFetchTree.subTypeClass, this) + ")";

        return DEPRECATED_PureGrammarComposerCore.computeIndentationString(this, getTabSize(1)) + subTypeString + subTreeString;
    }

    @Override
    public String visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        return unsupported(UnknownAppliedFunction.class);
    }

    @Override
    public String visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        return appliedQualifiedProperty.parameters.get(0).accept(this)
                + "."
                + (this.isRenderingHTML() ? "<span class=pureGrammar-property>" : "") + PureGrammarComposerUtility.convertIdentifier(appliedQualifiedProperty.qualifiedProperty) + (this.isRenderingHTML() ? "</span>" : "")
                + (appliedQualifiedProperty.parameters.size() > 1 ? "(" + LazyIterate.collect(appliedQualifiedProperty.parameters.subList(1, appliedQualifiedProperty.parameters.size()), l -> l.accept(this)).makeString(", ") + ")" : "");
    }

    @Override
    public String visit(GenericTypeInstance genericTypeInstance)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(printGenericType(genericTypeInstance.genericType, this));
        return builder.toString();
    }

    @Override
    public String visit(Whatever whatever)
    {
        return unsupported(Whatever.class);
    }

    @Override
    public String visit(HackedUnit hackedUnit)
    {
        return "@" + hackedUnit.fullPath;
    }

    @Override
    public String visit(UnitInstance unitInstance)
    {
        return unitInstance.unitValue.toString() + " " + unitInstance.unitType;
    }

    @Override
    public String visit(UnitType unitType)
    {
        return unitType.fullPath;
    }

    @Override
    public String visit(KeyExpression keyExpression)
    {
        return PureGrammarParserUtility.removeQuotes(keyExpression.key.accept(this)) + "=" + keyExpression.expression.accept(this);
    }

    @Override
    public String visit(PrimitiveType primitiveType)
    {
        return primitiveType.fullPath;
    }

    @Override
    public String visit(DataElement dataElement)
    {
        return CorePureGrammarComposer.renderDataElement(dataElement, PureGrammarComposerContext.Builder.newInstance().build());
    }
}
