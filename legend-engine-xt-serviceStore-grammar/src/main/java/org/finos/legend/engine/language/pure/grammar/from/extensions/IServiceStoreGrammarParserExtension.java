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

package org.finos.legend.engine.language.pure.grammar.from.extensions;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.SpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecuritySchemeRequirement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;

public interface IServiceStoreGrammarParserExtension extends PureGrammarParserExtension
{
    static List<IServiceStoreGrammarParserExtension> getExtensions()
    {
        return Lists.mutable.withAll(ServiceLoader.load(IServiceStoreGrammarParserExtension.class));
    }

    static SecurityScheme process(SpecificationSourceCode code, List<Function<SpecificationSourceCode, SecurityScheme>> processors)
    {
        return ListIterate
                .collect(processors, processor -> processor.apply(code))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported security scheme type '" + code.getType() + "'", code.getSourceInformation(), EngineErrorType.PARSER));
    }

    @Deprecated
    default List<Function<String, SecurityScheme>> getExtraSecuritySchemesParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, SecurityScheme>> getExtraSecuritySchemeParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<String, SecuritySchemeRequirement>> getExtraSecurityParsers()
    {
        return Collections.emptyList();
    }

}
