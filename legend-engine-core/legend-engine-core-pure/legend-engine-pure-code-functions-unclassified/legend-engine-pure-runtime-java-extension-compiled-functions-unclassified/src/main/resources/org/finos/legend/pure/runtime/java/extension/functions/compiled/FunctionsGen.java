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

package org.finos.legend.pure.generated;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.delta.CodeBlockDeltaCompiler;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.shared.http.HttpMethod;
import org.finos.legend.pure.runtime.java.shared.http.HttpRawHelper;
import org.finos.legend.pure.runtime.java.shared.http.URLScheme;

public class FunctionsGen extends org.finos.legend.pure.runtime.java.extension.functions.compiled.FunctionsHelper
{
    public static Root_meta_pure_functions_io_http_HTTPResponse executeHttpRaw(Root_meta_pure_functions_io_http_URL url, Object method, String mimeType, String body, ExecutionSupport executionSupport)
    {
        URLScheme scheme = URLScheme.http;
        if (url._scheme() != null)
        {
            scheme = URLScheme.valueOf(url._scheme()._name());
        }
        return (Root_meta_pure_functions_io_http_HTTPResponse) HttpRawHelper.toHttpResponseInstance(HttpRawHelper.executeHttpService(scheme, url._host(), (int) url._port(), url._path(), HttpMethod.valueOf(((Enum) method)._name()), mimeType, body), ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
    }

    public static Root_meta_pure_functions_meta_CompilationResult compileCodeBlock(String source, ExecutionSupport es)
    {
        Root_meta_pure_functions_meta_CompilationResult result = null;
        if (source != null)
        {
            CodeBlockDeltaCompiler.CompilationResult compilationResult = CodeBlockDeltaCompiler.compileCodeBlock(source, ((CompiledExecutionSupport) es));
            result = convertCompilationResult(compilationResult);
        }
        return result;
    }

    public static RichIterable<Root_meta_pure_functions_meta_CompilationResult> compileCodeBlocks(RichIterable<? extends String> sources, ExecutionSupport es)
    {
        RichIterable<CodeBlockDeltaCompiler.CompilationResult> compilationResults = CodeBlockDeltaCompiler.compileCodeBlocks(sources, ((CompiledExecutionSupport) es));
        MutableList<Root_meta_pure_functions_meta_CompilationResult> results = Lists.mutable.ofInitialCapacity(sources.size());

        for (CodeBlockDeltaCompiler.CompilationResult compilationResult : compilationResults)
        {
            results.add(convertCompilationResult(compilationResult));
        }
        return results;
    }


    private static Root_meta_pure_functions_meta_CompilationResult convertCompilationResult(CodeBlockDeltaCompiler.CompilationResult compilationResult)
    {
        Root_meta_pure_functions_meta_CompilationResult result = new Root_meta_pure_functions_meta_CompilationResult_Impl("");

        if (compilationResult.getFailureMessage() != null)
        {
            Root_meta_pure_functions_meta_CompilationFailure failure = new Root_meta_pure_functions_meta_CompilationFailure_Impl("");
            failure._message(compilationResult.getFailureMessage());

            SourceInformation si = compilationResult.getFailureSourceInformation();

            if (si != null)
            {
                Root_meta_pure_functions_meta_SourceInformation sourceInformation = new Root_meta_pure_functions_meta_SourceInformation_Impl("");
                sourceInformation._column(si.getColumn());
                sourceInformation._line(si.getLine());
                sourceInformation._endColumn(si.getEndColumn());
                sourceInformation._endLine(si.getEndLine());
                sourceInformation._startColumn(si.getStartColumn());
                sourceInformation._startLine(si.getStartLine());
                failure._sourceInformation(sourceInformation);
            }
            result._failure(failure);
        }
        else
        {
            ConcreteFunctionDefinition<?> cfd = (ConcreteFunctionDefinition<?>) compilationResult.getResult();
            result._result(cfd._expressionSequence().getFirst());
        }
        return result;
    }

    public static Object alloyTest(ExecutionSupport es, Function<?> alloyTest, Function<?> regular)
    {
        return alloyTest(es, alloyTest, regular, CoreGen.bridge);
    }

    public static Object legendTest(ExecutionSupport es, Function<?> alloyTest, Function<?> regular)
    {
        return legendTest(es, alloyTest, regular, CoreGen.bridge);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> newClass(String fullPathString, MetadataAccessor ma, SourceInformation si)
    {
        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
        if (fullPath.isEmpty())
        {
            throw new PureExecutionException(null, "Cannot create a new Class: '" + fullPathString + "'", Stacks.mutable.<org.finos.legend.pure.m4.coreinstance.CoreInstance>empty());
        }
        String name = fullPath.getLast();
        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl("Root")._name("Root"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()
        {
            @Override
            public Package valueOf(String s)
            {
                return new Package_Impl(s);
            }
        });
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> _class = new Root_meta_pure_metamodel_type_Class_Impl(name)._name(name)._package(_package);
        return _class._classifierGenericType(
                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")
                                ._rawType(ma.getClass("meta::pure::metamodel::type::Class"))
                                ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(_class))))
                ._generalizations(Lists.immutable.of(
                        new Root_meta_pure_metamodel_relationship_Generalization_Impl("Anonymous_StripedId")
                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(ma.getTopType()))
                                ._specific(_class)));
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association newAssociation(String fullPathString, Property p1, Property p2, MetadataAccessor ma, SourceInformation si)
    {
        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
        if (fullPath.isEmpty())
        {
            throw new PureExecutionException(null, "Cannot create a new Association: '" + fullPathString + "'", Stacks.mutable.<org.finos.legend.pure.m4.coreinstance.CoreInstance>empty());
        }
        String name = fullPath.getLast();
        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl("Root")._name("Root"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()
        {
            @Override
            public Package valueOf(String s)
            {
                return new Package_Impl(s);
            }
        });
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association = new Root_meta_pure_metamodel_relationship_Association_Impl(name)._name(name)._package(_package);
        return _association._propertiesAdd(p1)._propertiesAdd(p2)._classifierGenericType(
                new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")
                        ._rawType(ma.getClass("meta::pure::metamodel::relationship::Association")));
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> newEnumeration(final String fullPathString, RichIterable values, MetadataAccessor ma, SourceInformation si)
    {
        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);
        if (fullPath.isEmpty())
        {
            throw new PureExecutionException(null, "Cannot create a new Enumeration: '" + fullPathString + "'", Stacks.mutable.<org.finos.legend.pure.m4.coreinstance.CoreInstance>empty());
        }
        String name = fullPath.getLast();
        String packageName = fullPath.subList(0, fullPath.size() - 1).makeString("::");
        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl("Root")._name("Root"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()
        {
            @Override
            public Package valueOf(String s)
            {
                return new Package_Impl(s);
            }
        });
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> _enumeration = new Root_meta_pure_metamodel_type_Enumeration_Impl<Any>(name)._name(name)._package(_package);
        return _enumeration._classifierGenericType(
                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")
                                ._rawType(ma.getClass("meta::pure::metamodel::type::Enumeration"))
                                ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(_enumeration))))
                ._generalizations(Lists.immutable.of(
                        new Root_meta_pure_metamodel_relationship_Generalization_Impl("Anonymous_StripedId")
                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_StripedId")._rawType(ma.getClass("meta::pure::metamodel::type::Enum")))
                                ._specific(_enumeration)))
                ._values(values.collect(new DefendedFunction<String, PureEnum>()
                {
                    public PureEnum valueOf(String valueName)
                    {
                        return new PureEnum(valueName, fullPathString);
                    }
                }));
    }


    public static Object traceSpan(ExecutionSupport es, Function<?> function, String operationName, Function<?> funcToGetTags, boolean tagsCritical)
    {
        return traceSpan(es, function, operationName, funcToGetTags, tagsCritical, CoreGen.bridge);
    }
}
