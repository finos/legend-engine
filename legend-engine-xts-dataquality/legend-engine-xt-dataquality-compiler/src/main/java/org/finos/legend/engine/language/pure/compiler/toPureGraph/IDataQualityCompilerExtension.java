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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityPersistenceStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityPersistenceStrategy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public interface IDataQualityCompilerExtension extends CompilerExtension
{
    static Stream<IDataQualityCompilerExtension> getExtensions(CompileContext context)
    {
        return context.getCompilerExtensions()
                .getExtensions()
                .stream()
                .filter(IDataQualityCompilerExtension.class::isInstance)
                .map(IDataQualityCompilerExtension.class::cast);
    }

    static Root_meta_external_dataquality_DataQualityPersistenceStrategy process(
            DataQualityPersistenceStrategy persistenceStrategy,
            List<Function2<DataQualityPersistenceStrategy, CompileContext, Root_meta_external_dataquality_DataQualityPersistenceStrategy>> processors,
            CompileContext context)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(persistenceStrategy, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported persistenceStrategy type '" + persistenceStrategy.getClass() + "'", persistenceStrategy.sourceInformation, EngineErrorType.COMPILATION));
    }

    default List<Function2<DataQualityPersistenceStrategy, CompileContext, Root_meta_external_dataquality_DataQualityPersistenceStrategy>> getExtraDataQualityPersistenceStrategyProcessors()
    {
        return Collections.emptyList();
    }
}
