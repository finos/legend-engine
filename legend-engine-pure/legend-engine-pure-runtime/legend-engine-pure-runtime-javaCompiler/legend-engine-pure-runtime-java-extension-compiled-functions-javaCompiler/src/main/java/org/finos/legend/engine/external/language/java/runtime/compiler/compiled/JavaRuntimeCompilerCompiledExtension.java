//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.language.java.runtime.compiler.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.external.language.java.runtime.compiler.compiled.natives.CompileAndExecuteJava;
import org.finos.legend.engine.external.language.java.runtime.compiler.compiled.natives.CompileJava;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaSourceCodeGenerator;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.Procedure3;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.Procedure4;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureFunction1;

import java.util.List;
import java.util.function.Function;

public class JavaRuntimeCompilerCompiledExtension implements CompiledExtension
{
    @Override
    public List<StringJavaSource> getExtraJavaSources()
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public List<Native> getExtraNatives()
    {
        return Lists.fixedSize.with(new CompileJava(), new CompileAndExecuteJava());
    }

    @Override
    public List<Procedure3<CoreInstance, JavaSourceCodeGenerator, ProcessorContext>> getExtraPackageableElementProcessors()
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public List<Procedure4<CoreInstance, CoreInstance, ProcessorContext, ProcessorSupport>> getExtraClassMappingProcessors()
    {
        return Lists.fixedSize.empty();
    }

    @Override
    public RichIterable<? extends Pair<String, Function<? super CoreInstance, String>>> getExtraIdBuilders(ProcessorSupport processorSupport)
    {
        return Lists.immutable.empty();
    }

    @Override
    public SetIterable<String> getExtraCorePath()
    {
        return Sets.immutable.empty();
    }

    @Override
    public PureFunction1<Object, Object> getExtraFunctionEvaluation(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> func, Bridge bridge, ExecutionSupport es)
    {
        return null;
    }

    @Override
    public String getRelatedRepository()
    {
        return "core_external_language_java_compiler";
    }

    public static CompiledExtension extension()
    {
        return new JavaRuntimeCompilerCompiledExtension();
    }
}
