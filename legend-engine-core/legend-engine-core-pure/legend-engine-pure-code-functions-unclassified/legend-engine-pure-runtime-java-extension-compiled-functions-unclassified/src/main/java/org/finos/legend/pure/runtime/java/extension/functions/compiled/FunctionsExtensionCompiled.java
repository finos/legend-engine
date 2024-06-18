// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License",
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

package org.finos.legend.pure.runtime.java.extension.functions.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteAlloyTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.MayExecuteLegendTest;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.Profile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Decrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.cipher.Encrypt;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.Get;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.collection.Repeat;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfWeekNumber;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.DayOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Now;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.Today;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.date.WeekOfYear;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.hash.Hash;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.ReadFile;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.http.Http;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.lang.MutateAdd;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.math.StdDev;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.CompileValueSpecification;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.FunctionDescriptorToId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.IsSourceReadOnly;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.IsValidFunctionDescriptor;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewAssociation;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewClass;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewEnumeration;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewLambdaFunction;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.meta.NewQualifiedProperty;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.CurrentUserId;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.Guid;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.runtime.IsOptionSet;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.ASCII;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Char;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Chunk;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.DecodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.DecodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EncodeBase64;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.EncodeUrl;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.JaroWinklerSimilarity;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.LevenshteinDistance;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.string.Matches;
import org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.tracing.TraceSpan;

import java.util.List;

public class FunctionsExtensionCompiled implements CompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.with(StringJavaSource.newStringJavaSource("org.finos.legend.pure.generated", "FunctionsGen",
                "package org.finos.legend.pure.generated;\n" +
                        "\n" +
                        "import org.eclipse.collections.api.RichIterable;\n" +
                        "import org.eclipse.collections.api.factory.Lists;\n" +
                        "import org.eclipse.collections.api.list.ListIterable;\n" +
                        "import org.eclipse.collections.api.list.MutableList;\n" +
                        "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.Package;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;\n" +
                        "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;\n" +
                        "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                        "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                        "import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\n" +
                        "import org.finos.legend.pure.m4.coreinstance.SourceInformation;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.delta.CodeBlockDeltaCompiler;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.DefendedFunction;\n" +
                        "import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;\n" +
                        "import org.finos.legend.pure.runtime.java.shared.http.HttpMethod;\n" +
                        "import org.finos.legend.pure.runtime.java.shared.http.HttpRawHelper;\n" +
                        "import org.finos.legend.pure.runtime.java.shared.http.URLScheme;\n" +
                        "\n" +
                        "public class FunctionsGen extends org.finos.legend.pure.runtime.java.extension.functions.compiled.FunctionsHelper\n" +
                        "{\n" +
                        "    public static Root_meta_pure_functions_io_http_HTTPResponse executeHttpRaw(Root_meta_pure_functions_io_http_URL url, Object method, String mimeType, String body, ExecutionSupport executionSupport)\n" +
                        "    {\n" +
                        "        URLScheme scheme = URLScheme.http;\n" +
                        "        if (url._scheme() != null)\n" +
                        "        {\n" +
                        "            scheme = URLScheme.valueOf(url._scheme()._name());\n" +
                        "        }\n" +
                        "        return (Root_meta_pure_functions_io_http_HTTPResponse) HttpRawHelper.toHttpResponseInstance(HttpRawHelper.executeHttpService(scheme, url._host(), (int) url._port(), url._path(), HttpMethod.valueOf(((Enum) method)._name()), mimeType, body), ((CompiledExecutionSupport) executionSupport).getProcessorSupport());\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Root_meta_pure_functions_meta_CompilationResult compileCodeBlock(String source, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        Root_meta_pure_functions_meta_CompilationResult result = null;\n" +
                        "        if (source != null)\n" +
                        "        {\n" +
                        "            CodeBlockDeltaCompiler.CompilationResult compilationResult = CodeBlockDeltaCompiler.compileCodeBlock(source, ((CompiledExecutionSupport) es));\n" +
                        "            result = convertCompilationResult(compilationResult);\n" +
                        "        }\n" +
                        "        return result;\n" +
                        "    }\n" +
                        "\n" +
                        "    public static RichIterable<Root_meta_pure_functions_meta_CompilationResult> compileCodeBlocks(RichIterable<? extends String> sources, ExecutionSupport es)\n" +
                        "    {\n" +
                        "        RichIterable<CodeBlockDeltaCompiler.CompilationResult> compilationResults = CodeBlockDeltaCompiler.compileCodeBlocks(sources, ((CompiledExecutionSupport) es));\n" +
                        "        MutableList<Root_meta_pure_functions_meta_CompilationResult> results = FastList.newList(sources.size());\n" +
                        "\n" +
                        "        for (CodeBlockDeltaCompiler.CompilationResult compilationResult : compilationResults)\n" +
                        "        {\n" +
                        "            results.add(convertCompilationResult(compilationResult));\n" +
                        "        }\n" +
                        "        return results;\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    private static Root_meta_pure_functions_meta_CompilationResult convertCompilationResult(CodeBlockDeltaCompiler.CompilationResult compilationResult)\n" +
                        "    {\n" +
                        "        Root_meta_pure_functions_meta_CompilationResult result = new Root_meta_pure_functions_meta_CompilationResult_Impl(\"\");\n" +
                        "\n" +
                        "        if (compilationResult.getFailureMessage() != null)\n" +
                        "        {\n" +
                        "            Root_meta_pure_functions_meta_CompilationFailure failure = new Root_meta_pure_functions_meta_CompilationFailure_Impl(\"\");\n" +
                        "            failure._message(compilationResult.getFailureMessage());\n" +
                        "\n" +
                        "            SourceInformation si = compilationResult.getFailureSourceInformation();\n" +
                        "\n" +
                        "            if (si != null)\n" +
                        "            {\n" +
                        "                Root_meta_pure_functions_meta_SourceInformation sourceInformation = new Root_meta_pure_functions_meta_SourceInformation_Impl(\"\");\n" +
                        "                sourceInformation._column(si.getColumn());\n" +
                        "                sourceInformation._line(si.getLine());\n" +
                        "                sourceInformation._endColumn(si.getEndColumn());\n" +
                        "                sourceInformation._endLine(si.getEndLine());\n" +
                        "                sourceInformation._startColumn(si.getStartColumn());\n" +
                        "                sourceInformation._startLine(si.getStartLine());\n" +
                        "                failure._sourceInformation(sourceInformation);\n" +
                        "            }\n" +
                        "            result._failure(failure);\n" +
                        "        }\n" +
                        "        else\n" +
                        "        {\n" +
                        "            ConcreteFunctionDefinition<?> cfd = (ConcreteFunctionDefinition<?>) compilationResult.getResult();\n" +
                        "            result._result(cfd._expressionSequence().getFirst());\n" +
                        "        }\n" +
                        "        return result;\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object alloyTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular)\n" +
                        "    {\n" +
                        "        return alloyTest(es, alloyTest, regular, CoreGen.bridge);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static Object legendTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function regular)\n" +
                        "    {\n" +
                        "        return legendTest(es, alloyTest, regular,  CoreGen.bridge);\n" +
                        "    }\n" +
                        "\n" +
                        "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> newClass(String fullPathString, MetadataAccessor ma, SourceInformation si)\n" +
                        "    {\n" +
                        "        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);\n" +
                        "        if (fullPath.isEmpty())\n" +
                        "        {\n" +
                        "            throw new PureExecutionException(null, \"Cannot create a new Class: '\" + fullPathString + \"'\");\n" +
                        "        }\n" +
                        "        String name = fullPath.getLast();\n" +
                        "        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl(\"Root\")._name(\"Root\"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public Package valueOf(String s)\n" +
                        "            {\n" +
                        "                return new Package_Impl(s);\n" +
                        "            }\n" +
                        "        });\n" +
                        "        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<Any> _class = new Root_meta_pure_metamodel_type_Class_Impl(name)._name(name)._package(_package);\n" +
                        "        return _class._classifierGenericType(\n" +
                        "                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")\n" +
                        "                                ._rawType(ma.getClass(\"Root::meta::pure::metamodel::type::Class\"))\n" +
                        "                                ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(_class))))\n" +
                        "                ._generalizations(Lists.immutable.of(\n" +
                        "                        new Root_meta_pure_metamodel_relationship_Generalization_Impl(\"Anonymous_StripedId\")\n" +
                        "                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(ma.getTopType()))\n" +
                        "                                ._specific(_class)));\n" +
                        "    }\n" +
                        "\n" +
                        "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association newAssociation(String fullPathString, Property p1, Property p2, MetadataAccessor ma, SourceInformation si)\n" +
                        "    {\n" +
                        "        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);\n" +
                        "        if (fullPath.isEmpty())\n" +
                        "        {\n" +
                        "            throw new PureExecutionException(null, \"Cannot create a new Association: '\" + fullPathString + \"'\");\n" +
                        "        }\n" +
                        "        String name = fullPath.getLast();\n" +
                        "        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl(\"Root\")._name(\"Root\"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public Package valueOf(String s)\n" +
                        "            {\n" +
                        "                return new Package_Impl(s);\n" +
                        "            }\n" +
                        "        });\n" +
                        "        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association = new Root_meta_pure_metamodel_relationship_Association_Impl(name)._name(name)._package(_package);\n" +
                        "        return _association._propertiesAdd(p1)._propertiesAdd(p2)._classifierGenericType(\n" +
                        "                new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")\n" +
                        "                        ._rawType(ma.getClass(\"Root::meta::pure::metamodel::relationship::Association\")));\n" +
                        "    }\n" +
                        "\n" +
                        "    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> newEnumeration(final String fullPathString, RichIterable values, MetadataAccessor ma, SourceInformation si)\n" +
                        "    {\n" +
                        "        ListIterable<String> fullPath = PackageableElement.splitUserPath(fullPathString);\n" +
                        "        if (fullPath.isEmpty())\n" +
                        "        {\n" +
                        "            throw new PureExecutionException(null, \"Cannot create a new Enumeration: '\" + fullPathString + \"'\");\n" +
                        "        }\n" +
                        "        String name = fullPath.getLast();\n" +
                        "        String packageName = fullPath.subList(0, fullPath.size() - 1).makeString(\"::\");\n" +
                        "        org.finos.legend.pure.m3.coreinstance.Package _package = Pure.buildPackageIfNonExistent(new Package_Impl(\"Root\")._name(\"Root\"), fullPath.subList(0, fullPath.size() - 1), si, new DefendedFunction<String, Package>()\n" +
                        "        {\n" +
                        "            @Override\n" +
                        "            public Package valueOf(String s)\n" +
                        "            {\n" +
                        "                return new Package_Impl(s);\n" +
                        "            }\n" +
                        "        });\n" +
                        "        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<Any> _enumeration = new Root_meta_pure_metamodel_type_Enumeration_Impl<Any>(name)._name(name)._package(_package);\n" +
                        "        return _enumeration._classifierGenericType(\n" +
                        "                        new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")\n" +
                        "                                ._rawType(ma.getClass(\"Root::meta::pure::metamodel::type::Enumeration\"))\n" +
                        "                                ._typeArguments(Lists.immutable.of(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(_enumeration))))\n" +
                        "                ._generalizations(Lists.immutable.of(\n" +
                        "                        new Root_meta_pure_metamodel_relationship_Generalization_Impl(\"Anonymous_StripedId\")\n" +
                        "                                ._general(new Root_meta_pure_metamodel_type_generics_GenericType_Impl(\"Anonymous_StripedId\")._rawType(ma.getClass(\"Root::meta::pure::metamodel::type::Enum\")))\n" +
                        "                                ._specific(_enumeration)))\n" +
                        "                ._values(values.collect(new DefendedFunction<String, PureEnum>()\n" +
                        "                {\n" +
                        "                    public PureEnum valueOf(String valueName)\n" +
                        "                    {\n" +
                        "                        return new PureEnum(valueName, fullPathString);\n" +
                        "                    }\n" +
                        "                }));\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    public static Object traceSpan(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function function, String operationName, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function funcToGetTags, boolean tagsCritical)\n" +
                        "    {\n" +
                        "        return FunctionsGen.traceSpan(es, function, operationName, funcToGetTags, tagsCritical, CoreGen.bridge);\n" +
                        "    }\n" +
                        "}"));
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(
                // Cipher
                new Decrypt(),
                new Encrypt(),

                // Collection
                new Get(),
                new Repeat(),

                //Date
                new DayOfWeekNumber(),
                new DayOfYear(),
                new Now(),
                new Today(),
                new WeekOfYear(),

                //Hash
                new Hash(),

                //IO
                new Http(),
                new ReadFile(),

                //Lang
                new MutateAdd(),

                //Math
                new StdDev(),

                // Meta
                new CompileValueSpecification(),
                new FunctionDescriptorToId(),
                new IsSourceReadOnly(),
                new IsValidFunctionDescriptor(),
                new NewAssociation(),
                new NewClass(),
                new NewEnumeration(),
                new NewLambdaFunction(),
                new NewProperty(),
                new NewQualifiedProperty(),

                //Runtime
                new CurrentUserId(),
                new IsOptionSet(),
                new Guid(),

                //String
                new ASCII(),
                new Char(),
                new Chunk(),
                new DecodeBase64(),
                new EncodeBase64(),
                new DecodeUrl(),
                new EncodeUrl(),
                new Matches(),
                new JaroWinklerSimilarity(),
                new LevenshteinDistance(),

                //Tracing
                new TraceSpan(),

                // LegendTests
                new MayExecuteAlloyTest(),
                new MayExecuteLegendTest(),

                //Tools
                new Profile()
        );
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_functions_unclassified";
    }

    public static CompiledExtension extension()
    {
        return new FunctionsExtensionCompiled();
    }
}
