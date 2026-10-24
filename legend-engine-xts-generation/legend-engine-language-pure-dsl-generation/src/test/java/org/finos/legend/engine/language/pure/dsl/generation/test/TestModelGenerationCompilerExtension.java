// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.generation.test;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtension;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;
import java.util.List;

/**
 * Resolves a generation element under {@link #PACKAGE_PREFIX} to the class of that path, so a
 * generation specification can be run in this module's tests. Paired with
 * {@link TestModelGenerationExtension}. The prefix is deliberately its own: a resolver that
 * answered for other paths would change what the compilation tests here see.
 */
public class TestModelGenerationCompilerExtension implements GenerationCompilerExtension
{
    public static final String PACKAGE_PREFIX = "test::modelgen::";

    @Override
    public CompilerExtension build()
    {
        return new TestModelGenerationCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Function3<String, SourceInformation, CompileContext, PackageableElement>> getExtraModelGenerationSpecificationResolvers()
    {
        return Lists.fixedSize.with((path, sourceInformation, context) ->
        {
            if ((path == null) || !path.startsWith(PACKAGE_PREFIX))
            {
                return null;
            }
            return context.resolveClass(path, sourceInformation);
        });
    }
}
