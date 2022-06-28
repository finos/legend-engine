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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePlatform;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface IPersistenceComposerExtension extends PureGrammarComposerExtension
{
    static List<IPersistenceComposerExtension> getExtensions(PureGrammarComposerContext context)
    {
        return ListIterate.selectInstancesOf(context.extensions, IPersistenceComposerExtension.class);
    }

    static String process(PersistencePlatform persistencePlatform, List<Function3<PersistencePlatform, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(persistencePlatform, processors, indentLevel, context, "Persistence Platform", persistencePlatform.sourceInformation);
    }

    static <T> String process(T item, List<Function3<T, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, indentLevel, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.PARSER));
    }

    default List<Function3<PersistencePlatform, Integer, PureGrammarComposerContext, String>> getExtraPersistencePlatformComposers()
    {
        return Collections.emptyList();
    }
}
