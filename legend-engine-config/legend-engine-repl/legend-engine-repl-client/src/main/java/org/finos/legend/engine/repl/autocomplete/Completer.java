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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.*;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.RelationStoreAccessor;
import org.finos.legend.engine.repl.autocomplete.handlers.*;
import org.finos.legend.engine.repl.autocomplete.parser.ParserFixer;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

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
                        "import meta::pure::functions::relation::*;\n" +
                        "function _pierre::func():Any[*]{\n";
        this.lineOffset = StringUtils.countMatches(header, "\n") + 1;
        this.handlers = Lists.mutable.with(
                new FilterHandler(),
                new FromHandler(),
                new RenameHandler(),
                new ExtendHandler(),
                new GroupByHandler(),
                new SortHandler(),
                new JoinHandler(),
                new SelectHandler()
        ).toMap(FunctionHandler::functionName, x -> x);
    }

    public CompletionResult complete(String value)
    {
        try
        {
            ValueSpecification vs = parseValueSpecification(ParserFixer.fixCode(value));
            ValueSpecification topExpression = findTopExpression(vs);
            ValueSpecification currentExpression = findPartiallyWrittenExpression(vs, lineOffset, value.length());

            PureModel pureModel = compile();
            ProcessingContext processingContext = new ProcessingContext("");

            CompletionResult mutable = processVS(topExpression, processingContext, currentExpression, pureModel);

            if (mutable != null)
            {
                return mutable;
            }
            return new CompletionResult(Lists.mutable.empty());
        }
        catch (EngineException e)
        {
            return new CompletionResult(e);
        }
    }

    public CompletionResult processVS(ValueSpecification topExpression, ProcessingContext processingContext, ValueSpecification currentExpression, PureModel pureModel)
    {
        if (topExpression instanceof ClassInstance)
        {
            CompletionResult mutable = processClassInstance((ClassInstance) topExpression, compile());
            if (mutable != null)
            {
                return mutable;
            }
        }
        else if (topExpression instanceof AppliedFunction)
        {
            AppliedFunction currentFunc = (AppliedFunction) topExpression;

            CompilationResult compilationResult = compileLeftSideAndExtractType(currentFunc, processingContext, pureModel);
            GenericType leftType = compilationResult.getGenericType();
            String currentlyTypeFunctionName = currentFunc.function.replace(ParserFixer.magicToken, "");
            FunctionHandler handler = handlers.get(currentFunc.function);
            if (handler == null)
            {
                return new CompletionResult(getFunctionCandidates(compilationResult, pureModel, null).select(c -> c.startsWith(currentlyTypeFunctionName)).collect(c -> new CompletionItem(c, c)));
            }
            else
            {
                MutableList<CompletionItem> proposed = handler.proposedParameters(currentFunc, leftType, pureModel, this, processingContext, currentExpression);
                if (!proposed.isEmpty())
                {
                    return new CompletionResult(proposed);
                }
                handler.handleFunctionAppliedParameters(currentFunc, leftType, processingContext, pureModel);
            }

            if (currentExpression != null)
            {
                return processVS(currentExpression, processingContext, null, pureModel);
            }
            else
            {
                return new CompletionResult(Lists.mutable.empty());
            }
        }
        else if (topExpression instanceof AppliedProperty)
        {
            AppliedProperty appliedProperty = (AppliedProperty) topExpression;
            CompilationResult compilationResult1 = compileCodePartWithinLambdaWithOneParameter(appliedProperty.parameters.get(0), processingContext, pureModel);
            String typedProperty = appliedProperty.property.replace(ParserFixer.magicToken, "");
            return new CompletionResult(extractPropertiesOrColumnsFromType(compilationResult1).select(c -> c.startsWith(typedProperty)).collect(c -> new CompletionItem(c.contains(" ") ? "'" + c + "'" : c)));
        }
        return null;
    }

    public static CompletionResult processClassInstance(ClassInstance topExpression, PureModel pureModel)
    {
        Object islandExpr = topExpression.value;
        if (islandExpr instanceof RelationStoreAccessor)
        {
            MutableList<String> path = Lists.mutable.withAll(((RelationStoreAccessor) islandExpr).path);
            String writtenPath = path.makeString("::").replace(ParserFixer.magicToken, "");
            MutableList<Store> elements = pureModel.getAllStores().select(c -> nameMatch(c, writtenPath)).toList();
            if (elements.size() == 1 &&
                    writtenPath.startsWith(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(elements.get(0))) &&
                    !writtenPath.equals(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(elements.get(0)))
            )
            {
                org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database db = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) elements.get(0);
                String writtenTableName = writtenPath.replace(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(db), "").replace("::", "");
                MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table> tables = db._schemas().isEmpty() ? Lists.mutable.empty() : db._schemas().getFirst()._tables().toList();
                MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table> foundTables = tables.select(c -> c._name().startsWith(writtenTableName));
                if ((foundTables.size() == 1 && foundTables.get(0)._name().equals(path.getLast())))
                {
                    return new CompletionResult(Lists.mutable.empty());
                }
                else
                {
                    return new CompletionResult(foundTables.collect(c -> new CompletionItem(c._name(), c._name() + "}")));
                }
            }
            return new CompletionResult(ListIterate.collect(elements, c -> new CompletionItem(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c), ">{" + org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c))).toList());
        }
        return null;
    }

    private static boolean nameMatch(PackageableElement c, String writtenPath)
    {
        String path = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(c);
        if (path.length() > writtenPath.length())
        {
            return path.startsWith(writtenPath);
        }
        else
        {
            return writtenPath.startsWith(path);
        }
    }

    private static CompilationResult compileCodePartWithinLambdaWithOneParameter(ValueSpecification valueSpecification, ProcessingContext processingContext, PureModel pureModel)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification vs = valueSpecification.accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext));
        return new CompilationResult(vs._genericType(), vs._multiplicity());
    }

    private ValueSpecification parseValueSpecification(String value)
    {
        String code = header +
                value + "\n" +
                "\n}";
        PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(code);
        Function func = (Function) ListIterate.select(pureModelContextData.getElements(), s -> s.getPath().equals("_pierre::func__Any_MANY_")).getFirst();
        return func.body.get(0);
    }

    private MutableList<String> getFunctionCandidates(CompilationResult compilationResult, PureModel pureModel, String functionContext)
    {
        GenericType leftType = compilationResult.getGenericType();
        //PureModel pureModel = compilationResult.getPureModel();
        if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(leftType._rawType(), pureModel.getType(M3Paths.Relation), pureModel.getExecutionSupport().getProcessorSupport()))
        {
            // May want to assert the mul to 1
            return Lists.mutable.with("distinct", "drop", "select", "extend", "filter", "from", "groupBy", "join", "limit", "rename", "size", "slice", "sort");
        }
        else if (leftType._rawType().getName().equals("String"))
        {
            if (Multiplicity.isToOne(compilationResult.getMultiplicity()))
            {
                return Lists.mutable.with("contains", "startsWith", "endsWith", "toLower", "toUpper", "lpad", "rpad", "parseInteger", "parseFloat");
            }
            else
            {
                return Lists.mutable.with("count");
            }
        }
        else if (leftType._rawType().getName().equals("Integer"))
        {
            if (Multiplicity.isToOne(compilationResult.getMultiplicity()))
            {
                return Lists.mutable.with("sqrt", "pow", "exp");
            }
            else
            {
                return Lists.mutable.with("sum", "count");
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

    private PureModel compile()
    {
        PureModelContextData newPureModelContextData = PureGrammarParser.newInstance().parseModel(buildCodeContext);
        return Compiler.compile(newPureModelContextData, null, null);
    }

    private CompilationResult compileLeftSideAndExtractType(AppliedFunction currentFunc, ProcessingContext processingContext, PureModel pureModel)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification vs = currentFunc.parameters.get(0).accept(new ValueSpecificationBuilder(new CompileContext.Builder(pureModel).build(), Lists.mutable.empty(), processingContext));
        return new CompilationResult(vs._genericType(), vs._multiplicity());
    }

    private MutableList<String> extractPropertiesOrColumnsFromType(CompilationResult compilationResult)
    {
        GenericType leftType = compilationResult.getGenericType();
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
                if (ci.value instanceof ColSpec)
                {
                    ColSpec co = (ColSpec) ci.value;
                    return Lists.mutable.with(co.function1, co.function2).select(Objects::nonNull).collect(c -> c.accept(this)).select(Objects::nonNull).getFirst();
                }
                else if (ci.value instanceof ColSpecArray)
                {
                    ColSpecArray coA = (ColSpecArray) ci.value;
                    return ListIterate.flatCollect(coA.colSpecs, co -> Lists.mutable.with(co.function1, co.function2).select(Objects::nonNull).collect(c -> c.accept(this))).select(Objects::nonNull).getFirst();
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
