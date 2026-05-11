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

package org.finos.legend.engine.test.emit.extensions;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtension;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.navigation.M3Paths;

import java.util.Collections;
import java.util.List;

/**
 * Test-only {@link GenerationCompilerExtension} that resolves generation
 * specification nodes for paths in the {@code demo::modelgen::} namespace by
 * looking the element up directly in the compiled Pure model. Paired with
 * {@link EmitDemoModelGenerationExtension} to exercise
 * {@link org.finos.legend.engine.test.emit.EMITPhase#MODEL_GENERATION}.
 */
public class EmitDemoModelGenerationCompilerExtension implements GenerationCompilerExtension
{
    public static final String PACKAGE_PREFIX = "demo::modelgen::";

    @Override
    public CompilerExtension build()
    {
        return new EmitDemoModelGenerationCompilerExtension();
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
            try
            {
                return context.resolveClass(path, sourceInformation);
            }
            catch (Exception ignore)
            {
                int lastColon = path.lastIndexOf(':');
                String pkgName = (lastColon == -1) ? M3Paths.Root : path.substring(0, lastColon - 1);
                String name = (lastColon == -1) ? path : path.substring(lastColon + 1);
                return new Root_meta_pure_metamodel_type_Class_Impl<>(name, null, context.resolveClass(M3Paths.Class))
                        ._name(name)
                        ._package(context.pureModel.getOrCreatePackage(pkgName));
            }
        });
    }
}
