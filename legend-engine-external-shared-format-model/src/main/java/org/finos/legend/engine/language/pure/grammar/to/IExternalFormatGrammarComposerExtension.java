// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Objects;

public interface IExternalFormatGrammarComposerExtension extends PureGrammarComposerExtension
{
    static List<IExternalFormatGrammarComposerExtension> getExtensions(PureGrammarComposerContext context)
    {
        return ListIterate.selectInstancesOf(context.extensions, IExternalFormatGrammarComposerExtension.class);
    }

    static String process(ExternalSource externalSource, List<Function2<ExternalSource, PureGrammarComposerContext, String>> processors, PureGrammarComposerContext context)
    {
        return process(externalSource, processors, context, "External Source Specification", externalSource.sourceInformation);
    }

    static <T> String process(T item, List<Function2<T, PureGrammarComposerContext, String>> processors, PureGrammarComposerContext context, String type, SourceInformation srcInfo) {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.PARSER));
    }

    default List<Function2<ExternalSource, PureGrammarComposerContext, String>> getExtraExternalSourceSpecificationComposers() {
        return FastList.newList();
    }
}
