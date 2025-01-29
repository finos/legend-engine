// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.autocomplete;

import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.repl.autocomplete.handlers.*;
import org.finos.legend.engine.repl.autocomplete.parser.ParserFixer;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.List;
import java.util.Objects;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_QUALIFIED_PATH;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_SIGNATURE;

public class Completer
{
    private final String header;
    private final int lineOffset;
    private final MutableMap<String, FunctionHandler> handlers;
    private final Supplier<PureModel> pureModel;
    private final MutableList<CompleterExtension> extensions;

    public Completer(String buildCodeContext)
    {
        this(buildCodeContext, Lists.mutable.empty());
    }

    public Completer(String buildCodeContext, MutableList<CompleterExtension> extensions)
    {
        this(() -> Compiler.compile(PureGrammarParser.newInstance().parseModel(buildCodeContext), null, Identity.getAnonymousIdentity().getName()), extensions);
    }

    public Completer(String buildCodeContext, MutableList<CompleterExtension> extensions, LegendInterface legendInterface)
    {
        this(() -> legendInterface.compile(legendInterface.parse(buildCodeContext)), extensions);
    }

    public Completer(PureModel pureModel, MutableList<CompleterExtension> extensions)
    {
        this(() -> pureModel, extensions);
    }

    private Completer(Supplier<PureModel> pureModel, MutableList<CompleterExtension> extensions)
    {
        this.pureModel = pureModel;
        this.extensions = extensions;
        this.header = "\n###Pure\n" +
                "import meta::pure::functions::relation::*;\n" +
                "function " + REPL_RUN_FUNCTION_SIGNATURE + "{\n";
        this.lineOffset = StringUtils.countMatches(header, "\n") + 1;
        this.handlers = Lists.mutable.with(
                new CastHandler(),
                new FilterHandler(),
                new FromHandler(),
                new RenameHandler(),
                new ExtendHandler(),
                new GroupByHandler(),
                new PivotHandler(),
                new SortHandler(),
                new JoinHandler(),
                new AsOfJoinHandler(),
                new SelectHandler(),
                new DistinctHandler(),
                new OverHandler()
        ).toMap(FunctionHandler::functionName, x -> x);
    }

    public CompletionResult complete(String value)
    {
        // We assume that the code we are editing is the:
        //      - topExpression
        //              (edit an Accessor, or find a functionName that would apply to a left parameter)
        //              Examples:
        //                  1->p[...]
        //                  #>[...]
        //      - a parameter of the topExpression if the topExpression is a function application
        //              (We can propose parameters like the 2nd parameter is a JoinType, or propose a column name for a ColSpec parameter)
        //              Examples:
        //                  ->groupBy(~c[...]
        //                  ->groupBy(~col,Joi[...]
        //                  ->rename(~c[...]
        //      - an element within a lambda, parameter (deep or not) of topExpression
        //              The deep parameter need to have a Lambda processingContext resolved (variable bound to a type)
        //              Examples:
        //                  ->filter(x|$x.c[...]

        try
        {
            ValueSpecification vs = parseValueSpecification(ParserFixer.fixCode(value));
            ValueSpecification topExpression = findTopExpression(vs);
            ValueSpecification currentExpression = findPartiallyWrittenExpression(vs, lineOffset, value.length());
            ProcessingContext processingContext = new ProcessingContext("");
            return processValueSpecification(topExpression, currentExpression, pureModel.get(), processingContext);
        }
        catch (EngineException e)
        {
            if (!e.getMessage().contains(ParserFixer.magicToken))
            {
                return new CompletionResult(e);
            }
            return new CompletionResult(new EngineException("parsing error", new SourceInformation("", 6, 1, 6, value.length()), EngineErrorType.PARSER));
        }
    }

    public CompletionResult processValueSpecification(ValueSpecification topExpression, ValueSpecification currentExpression, PureModel pureModel, ProcessingContext processingContext)
    {
        if (topExpression instanceof ClassInstance)
        {
            if (topExpression == currentExpression)
            {
                return processClassInstance((ClassInstance) topExpression, pureModel);
            }
        }
        else if (topExpression instanceof AppliedFunction)
        {
            AppliedFunction currentFunc = (AppliedFunction) topExpression;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification leftCompiledVS = currentFunc.parameters.get(0).accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext));
            GenericType leftType = leftCompiledVS._genericType();
            String currentlyTypeFunctionName = currentFunc.function.replace(ParserFixer.magicToken, "");
            FunctionHandler handler = handlers.get(currentFunc.function);
            if (currentExpression == topExpression)
            {
                // The top function name is being written, propose candidates
                return new CompletionResult(getFunctionCandidates(leftCompiledVS, pureModel, null).select(c -> c.startsWith(currentlyTypeFunctionName)).collect(c -> new CompletionItem(c, c + "(")));
            }
            else if (handler != null)
            {
                // The function has been written, let's try to propose some parameters
                MutableList<CompletionItem> proposed = handler.proposedParameters(currentFunc, leftType, pureModel, this, processingContext, currentExpression);
                if (!proposed.isEmpty())
                {
                    return new CompletionResult(proposed);
                }
                // No parameters are proposed, let's populate the processingContext to attempt to process the currentExpression with proper variable sets
                // The current expression is deep within the parameters of the function (probably within a Lambda)
                // The expression could be an AppliedProperty within an AppliedFunction, or an AppliedProperty etc...
                handler.handleFunctionAppliedParameters(currentFunc, leftType, processingContext, pureModel);
                if (currentExpression != topExpression)
                {
                    return processValueSpecification(currentExpression, currentExpression, pureModel, processingContext);
                }
            }
            return new CompletionResult(Lists.mutable.empty());
        }
        else if (topExpression instanceof AppliedProperty)
        {
            if (topExpression == currentExpression)
            {
                AppliedProperty appliedProperty = (AppliedProperty) topExpression;
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification leftCompiledVS = appliedProperty.parameters.get(0).accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext));
                String typedProperty = appliedProperty.property.replace(ParserFixer.magicToken, "");
                return new CompletionResult(extractPropertiesOrColumnsFromType(leftCompiledVS).select(c -> c.startsWith(typedProperty)).collect(c -> new CompletionItem(c, PureGrammarComposerUtility.convertIdentifier(c))));
            }
        }
        return new CompletionResult(Lists.mutable.empty());
    }

    private CompletionResult processClassInstance(ClassInstance topExpression, PureModel pureModel)
    {
        Object islandExpr = topExpression.value;
        CompletionResult result = this.extensions.collect(x -> x.extraClassInstanceProcessor(islandExpr, pureModel)).getFirst();
        if (result != null)
        {
            return result;
        }
        return new CompletionResult(Lists.mutable.empty());
    }


    //--------------------------------------------------------------------
    // Helpers to propose Column names (used in propose parameters code)
    //--------------------------------------------------------------------
    public static MutableList<CompletionItem> proposeColumnNamesForEditColSpec(AppliedFunction currentFunc, GenericType leftType)
    {
        if (currentFunc.parameters.size() == 2 && currentFunc.parameters.get(1) instanceof ClassInstance)
        {
            Object pivot = ((ClassInstance) currentFunc.parameters.get(1)).value;
            return proposeColumnNamesForEditColSpec(pivot, leftType);
        }
        return Lists.mutable.empty();
    }

    public static MutableList<CompletionItem> proposeColumnNamesForEditColSpec(Object object, GenericType leftType)
    {
        if (object instanceof ColSpec)
        {
            return proposeColumnNamesForEditColSpecOne((ColSpec) object, leftType);
        }
        else if (object instanceof ColSpecArray)
        {
            List<ColSpec> colSpecList = ((ColSpecArray) object).colSpecs;
            return proposeColumnNamesForEditColSpecOne(colSpecList.get(colSpecList.size() - 1), leftType);
        }
        return Lists.mutable.empty();
    }

    private static MutableList<CompletionItem> proposeColumnNamesForEditColSpecOne(ColSpec colSpec, GenericType leftType)
    {
        RelationType<?> r = (RelationType<?>) leftType._typeArguments().getFirst()._rawType();
        String typedColName = colSpec.name.replace(ParserFixer.magicToken, "");
        return r._columns().select(c -> c._name().startsWith(typedColName)).collect(FunctionAccessor::_name).collect(c -> new CompletionItem(c, PureGrammarComposerUtility.convertIdentifier(c))).toList();
    }
    //--------------------------------------------------------------------


    private ValueSpecification parseValueSpecification(String value)
    {
        String code = header + value + "\n" + "\n}";
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(code);
        Function func = (Function) ListIterate.select(pureModelContextData.getElements(), s -> s.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).getFirst();
        return func.body.get(0);
    }

    private MutableList<String> getFunctionCandidates(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification leftValueSpecification, PureModel pureModel, String functionContext)
    {
        GenericType leftType = leftValueSpecification._genericType();
        Multiplicity multiplicity = leftValueSpecification._multiplicity();
        //PureModel pureModel = compilationResult.getPureModel();
        if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(leftType._rawType(), pureModel.getType(M3Paths.Relation), pureModel.getExecutionSupport().getProcessorSupport()))
        {
            // May want to assert the mul to 1
            return Lists.mutable.with("cast", "distinct", "drop", "select", "extend", "filter", "from", "groupBy", "pivot", "join", "asOfJoin", "limit", "rename", "size", "slice", "sort");
        }
        else if (leftType._rawType().getName().equals("String"))
        {
            if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(multiplicity))
            {
                return Lists.mutable.with("contains", "startsWith", "endsWith", "toLower", "toUpper", "lpad", "rpad", "parseInteger", "parseFloat");
            }
            else
            {
                return Lists.mutable.with("count", "joinStrings", "uniqueValueOnly");
            }
        }
        else if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(leftType._rawType(), pureModel.getType(M3Paths.Number), pureModel.getExecutionSupport().getProcessorSupport()))
        {
            if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(multiplicity))
            {
                return Lists.mutable.with("abs", "pow", "sqrt", "exp");
            }
            else
            {
                return Lists.mutable.with("sum", "mean", "average", "min", "max", "count", "percentile", "variancePopulation", "varianceSample", "stdDevPopulation", "stdDevSample");
            }
        }
        else if (leftType._rawType().getName().equals("ColSpec"))
        {
            // May want to assert the mul to 1
            return Lists.mutable.with("ascending", "descending");
        }
        else
        {
            return Lists.mutable.with("project");
        }
    }

    private MutableList<String> extractPropertiesOrColumnsFromType(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification leftValueSpecification)
    {
        GenericType leftType = leftValueSpecification._genericType();
        GenericType genericType = leftType._typeArguments().getFirst();
        Type type;
        if (genericType != null)
        {
            type = genericType._rawType();
        }
        else
        {
            type = leftType._rawType();
        }

        if (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>)
        {
            return ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) type)._properties().collect(CoreInstance::getName).toList();
        }
        else if (type instanceof RelationType)
        {
            return ((RelationType<?>) type)._columns().collect(FunctionAccessor::_name).toList();
        }
        else if (type instanceof Enumeration)
        {
            return (((Enumeration<?>) type)._values().collect(Object::toString).toList());
        }
        return Lists.mutable.empty();
    }


    public ValueSpecification findPartiallyWrittenExpression(ValueSpecification vs, int line, int column)
    {
        return vs.accept(new ValueSpecificationDefaultVisitor<ValueSpecification>()
        {
            final int _line = line;
            final int _column = column;

            @Override
            public ValueSpecification visit(Lambda lambda)
            {
                return ListIterate.collect(lambda.body, a -> a.accept(this)).select(Objects::nonNull).getFirst();
            }

            @Override
            public ValueSpecification visit(Collection collection)
            {
                return ListIterate.collect(collection.values, vs -> vs.accept(this)).select(Objects::nonNull).getFirst();
            }

            @Override
            public ValueSpecification visit(AppliedFunction appliedFunction)
            {
                if (checkIfCurrent(appliedFunction.sourceInformation, _line, _column))
                {
                    ValueSpecification result = ListIterate.collect(appliedFunction.parameters, vs -> vs.accept(this)).select(Objects::nonNull).getFirst();
                    if (result != null)
                    {
                        return result;
                    }
                    return appliedFunction;
                }
                else
                {
                    return ListIterate.collect(appliedFunction.parameters, vs -> vs.accept(this)).select(Objects::nonNull).getFirst();
                }
            }

            @Override
            public ValueSpecification visit(AppliedProperty appliedProperty)
            {
                if (checkIfCurrent(appliedProperty.sourceInformation, _line, _column))
                {
                    return appliedProperty;
                }
                return null;
            }

            @Override
            public ValueSpecification visit(ClassInstance ci)
            {
                if (checkIfCurrent(ci.sourceInformation, _line, _column))
                {
                    if (ci.value instanceof ColSpec)
                    {
                        ColSpec co = (ColSpec) ci.value;
                        ValueSpecification res = Lists.mutable.with(co.function1, co.function2).select(Objects::nonNull).collect(c -> c.accept(this)).select(Objects::nonNull).getFirst();
                        return res == null ? ci : res;
                    }
                    else if (ci.value instanceof ColSpecArray)
                    {
                        ColSpecArray coA = (ColSpecArray) ci.value;
                        ValueSpecification res = ListIterate.flatCollect(coA.colSpecs, co -> Lists.mutable.with(co.function1, co.function2).select(Objects::nonNull).collect(c -> c.accept(this))).select(Objects::nonNull).getFirst();
                        return res == null ? ci : res;
                    }
                    return ci;
                }
                return null;
            }
        });
    }

    private static boolean checkIfCurrent(SourceInformation sourceInformation, int _line, int _column)
    {
        //_column = _column - 1;
        // SourceInformation column -1 so that we can account for -> or .
//        System.out.println(sourceInformation.startLine + " <= " + _line + " <= " + sourceInformation.endLine);
//        System.out.println((sourceInformation.startColumn - 1) + " <= " + _column + " <= " + sourceInformation.endColumn);
//        System.out.println(sourceInformation.startLine <= _line &&
//                _line <= sourceInformation.endLine &&
//                (sourceInformation.startColumn - 1) <= _column &&
//                _column <= sourceInformation.endColumn);
        return sourceInformation != null && sourceInformation.startLine <= _line &&
                _line <= sourceInformation.endLine &&
                (sourceInformation.startColumn - 1) <= _column &&
                _column <= sourceInformation.endColumn;
    }

    public ValueSpecification findTopExpression(ValueSpecification vs)
    {
        return vs.accept(new ValueSpecificationDefaultVisitor<ValueSpecification>()
        {
            @Override
            public ValueSpecification visit(AppliedFunction appliedFunction)
            {
                return appliedFunction;
            }

            @Override
            public ValueSpecification visit(ClassInstance classInstance)
            {
                return classInstance;
            }
        });
    }
}
