// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationResult;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationRule;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_context_PersistencePlatform;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_Trigger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

public interface IPersistenceCompilerExtension extends CompilerExtension
{
    static List<IPersistenceCompilerExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IPersistenceCompilerExtension.class));
    }

    static Root_meta_pure_persistence_metamodel_context_PersistencePlatform process(PersistencePlatform persistencePlatform, List<Function2<PersistencePlatform, CompileContext, Root_meta_pure_persistence_metamodel_context_PersistencePlatform>> processors, CompileContext context)
    {
        return process(persistencePlatform, processors, context, "persistence platform", persistencePlatform.sourceInformation);
    }

    static Root_meta_pure_persistence_metamodel_trigger_Trigger process(Trigger trigger, List<Function2<Trigger, CompileContext, Root_meta_pure_persistence_metamodel_trigger_Trigger>> processors, CompileContext context)
    {
        return process(trigger, processors, context, "trigger", trigger.sourceInformation);
    }

    static <T, U> U process(T item, List<Function2<T, CompileContext, U>> processors, CompileContext context, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    default List<Function2<PersistencePlatform, CompileContext, Root_meta_pure_persistence_metamodel_context_PersistencePlatform>> getExtraPersistencePlatformProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<Trigger, CompileContext, Root_meta_pure_persistence_metamodel_trigger_Trigger>> getExtraTriggerProcessors()
    {
        return Collections.emptyList();
    }

    default List<ValidationRule<Root_meta_pure_persistence_metamodel_PersistenceContext>> getExtraValidationRules()
    {
        return Collections.emptyList();
    }
}
