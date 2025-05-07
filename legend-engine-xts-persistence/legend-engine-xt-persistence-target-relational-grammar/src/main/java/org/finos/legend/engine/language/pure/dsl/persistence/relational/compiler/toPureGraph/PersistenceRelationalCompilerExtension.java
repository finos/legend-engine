// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.relational.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph.IPersistenceCompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.sink.RelationalPersistenceTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.sink.PersistenceTarget;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_target_PersistenceTarget;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PersistenceRelationalCompilerExtension implements IPersistenceCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Persistence", "Relational");
    }

    @Override
    public CompilerExtension build()
    {
        return new PersistenceRelationalCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.emptyList();
    }

    @Override
    public List<Function2<PersistenceTarget, CompileContext, Root_meta_pure_persistence_metamodel_target_PersistenceTarget>> getExtraPersistenceTargetProcessors()
    {
        return Collections.singletonList((persistenceTarget, compileContext) ->
            persistenceTarget instanceof RelationalPersistenceTarget
                ? HelperPersistenceRelationalBuilder.buildRelationalPersistenceTarget((RelationalPersistenceTarget) persistenceTarget, compileContext)
                : null);
    }

    @Override
    public List<Procedure2<PersistenceTarget, Set<PackageableElementPointer>>> getExtraPersistenceTargetPrerequisiteElementsProcessors()
    {
        return Collections.singletonList((persistenceTarget, prerequisiteElements) ->
        {
            if (persistenceTarget instanceof RelationalPersistenceTarget)
            {
                RelationalPersistenceTarget relationalPersistenceTarget = (RelationalPersistenceTarget) persistenceTarget;
                prerequisiteElements.add(relationalPersistenceTarget.database);
            }
        });
    }
}
