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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.repl.autocomplete.handlers.FilterHandler;
import org.finos.legend.engine.repl.autocomplete.parser.ParserFixer;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.List;
import java.util.Objects;

public class Completer
{
    private final String buildCodeContext;
    private final String header;
    private final int lineOffset;
    private final MutableMap<String, FunctionHandler> handlers;

    public Completer(String buildCodeContext)
    {
        this.buildCodeContext = buildCodeContext;
        this.header =
                buildCodeContext +
                        "\n###Pure\n" +
                        "function _pierre::func():Any[*]{\n";
        this.lineOffset = StringUtils.countMatches(header, "\n") + 1;
        this.handlers = Lists.mutable.with(new FilterHandler()).toMap(FilterHandler::functionName, x -> x);
    }

    public CompletionResult complete(String value)
    {
        try
        {
            ValueSpecification vs = parseValueSpecification(ParserFixer.fixCode(value));

            ValueSpecification topExpression = findTopExpression(vs);

            ValueSpecification currentExpression = findPartiallyWrittenExpression(vs, lineOffset, value.length());

            if (topExpression instanceof ClassInstance)
            {
                Object islandExpr = ((ClassInstance) topExpression).value;
                if (islandExpr instanceof RelationStoreAccessor)
                {
                    MutableList<String> path = Lists.mutable.withAll(((RelationStoreAccessor) islandExpr).path);
                    String writtenPath = path.makeString("::").replace(ParserFixer.magicToken, "");
                    PureModelContextData d = Client.replInterface.parse(buildCodeContext);
                    MutableList<PackageableElement> elements = ListIterate.select(d.getElements(), c -> c instanceof Store && nameMatch(c, writtenPath));
                    if (elements.size() == 1 && writtenPath.startsWith(elements.get(0).getPath()))
                    {
                        Database db = (Database) elements.get(0);
                        String writtenTableName = writtenPath.replace(db.getPath(), "").replace("::", "");
                        List<Table> tables = db.schemas.isEmpty() ? Lists.mutable.empty() : db.schemas.get(0).tables;
                        MutableList<Table> foundTables = ListIterate.select(tables, c -> c.name.startsWith(writtenTableName));
                        if ((foundTables.size() == 1 && foundTables.get(0).name.equals(path.getLast())))
                        {
                            return new CompletionResult(Lists.mutable.empty());
                        }
                        else
                        {
                            return new CompletionResult(foundTables.collect(c -> c.name + "}#"));
                        }
                    }
                    return new CompletionResult(ListIterate.collect(elements, c -> ">{" + PureGrammarComposerUtility.convertPath(c.getPath()) + ".").toList());
                }
            }
            else if (topExpression instanceof AppliedFunction)
            {
                AppliedFunction currentFunc = (AppliedFunction) topExpression;

                Pair<GenericType, PureModel> leftTypeAndModel = compileLeftSideAndExtractType(currentFunc);
                GenericType leftType = leftTypeAndModel.getOne();
                PureModel pureModel = leftTypeAndModel.getTwo();

                if (currentFunc == currentExpression)
                {
                    // The user is currently typing the function name try to autocomplete (considering the left type)
                    String currentlyTypeFunctionName = currentFunc.function.replace(ParserFixer.magicToken, "");
                    return new CompletionResult(getFunctionCandidates(leftType, pureModel, null).select(c -> c.startsWith(currentlyTypeFunctionName)));
                }

                // The user is currently typing applied parameters within the function
                ProcessingContext processingContext = new ProcessingContext("");

                FunctionHandler handler = handlers.get(currentFunc.function);
                if (handler != null)
                {
                    handler.handleFunctionAppliedParameters(currentFunc, leftType, processingContext, pureModel);
                }

                if (currentExpression instanceof AppliedProperty)
                {
                    AppliedProperty appliedProperty = (AppliedProperty) currentExpression;
                    GenericType subLeftGenericType = compileCodePartWithinLambdaWithOneParameter(appliedProperty.parameters.get(0), processingContext, pureModel)._genericType();
                    String typedProperty = appliedProperty.property.replace(ParserFixer.magicToken, "");
                    return new CompletionResult(extractPropertiesOrColumnsFromType(subLeftGenericType).select(c -> c.startsWith(typedProperty)).collect(c -> c.contains(" ") ? "'" + c + "'" : c));
                }
                else if (currentExpression instanceof AppliedFunction)
                {
                    AppliedFunction appliedFunction = (AppliedFunction) currentExpression;
                    GenericType subLeftGenericType = compileCodePartWithinLambdaWithOneParameter(appliedFunction.parameters.get(0), processingContext, pureModel)._genericType();
                    return new CompletionResult(getFunctionCandidates(subLeftGenericType, pureModel, currentFunc.function));
                }
            }


            return new CompletionResult(Lists.mutable.empty());
        }
        catch (EngineException e)
        {
            return new CompletionResult(e);
        }
    }

    private static boolean nameMatch(PackageableElement c, String writtenPath)
    {
        if (c.getPath().length() > writtenPath.length())
        {
            return c.getPath().startsWith(writtenPath);
        }
        else
        {
            return writtenPath.startsWith(c.getPath());
        }
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification compileCodePartWithinLambdaWithOneParameter(ValueSpecification valueSpecification, ProcessingContext processingContext, PureModel pureModel)
    {
        return valueSpecification.accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext));
    }

    private ValueSpecification parseValueSpecification(String value)
    {
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(
                header +
                        value + "\n" +
                        "\n}");

        Function func = (Function) ListIterate.select(pureModelContextData.getElements(), s -> s.getPath().equals("_pierre::func__Any_MANY_")).getFirst();
        ValueSpecification vs = func.body.get(0);
        return vs;
    }

    private MutableList<String> getFunctionCandidates(GenericType leftType, PureModel pureModel, String functionContext)
    {
        if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(leftType._rawType(), pureModel.getType(M3Paths.Relation), pureModel.getExecutionSupport().getProcessorSupport()))
        {
            return Lists.mutable.with("distinct", "drop", "extend", "filter", "from", "groupBy", "join", "limit", "rename", "size", "slice", "sort");
        }
        else if (leftType._rawType().getName().equals("String"))
        {
            return Lists.mutable.with("contains", "startsWith", "endsWith", "toLower", "toUpper", "lpad", "rpad");
        }
        return Lists.mutable.empty();
    }

    private Pair<GenericType, PureModel> compileLeftSideAndExtractType(AppliedFunction currentFunc)
    {
        ValueSpecification leftSide = currentFunc.parameters.get(0);
        PureModelContextData newPureModelContextData = PureGrammarParser.newInstance().parseModel(
                buildCodeContext + "\n###Pure\n" +
                        "function _pierre::helper():Any[*]{\n" +
                        "'a'\n" +
                        "\n}");
        Function newFunc = (Function) ListIterate.select(newPureModelContextData.getElements(), s -> s.getPath().equals("_pierre::helper__Any_MANY_")).getFirst();
        newFunc.body = Lists.mutable.with(leftSide);
        PureModel pureModel = Compiler.compile(newPureModelContextData, null, null);
        ConcreteFunctionDefinition<?> cf = pureModel.getConcreteFunctionDefinition("_pierre::helper__Any_MANY_", null);
        GenericType leftType = cf._expressionSequence().getFirst()._genericType();
        return Tuples.pair(leftType, pureModel);
    }

    private MutableList<String> extractPropertiesOrColumnsFromType(GenericType leftType)
    {
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
        });
    }

    private static boolean checkIfCurrent(SourceInformation sourceInformation, int _line, int _column)
    {
        // SourceInformation column -1 so that we can account for -> or .
//        System.out.println(sourceInformation.startLine + " <= " + _line + " <= " + sourceInformation.endLine);
//        System.out.println((sourceInformation.startColumn - 1) + " <= " + _column + " <= " + sourceInformation.endColumn);
        return sourceInformation.startLine <= _line &&
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
