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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.runtime.MasteryRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

public interface IMasteryParserExtension extends PureGrammarParserExtension
{
    static List<IMasteryParserExtension> getExtensions()
    {
        return Lists.mutable.ofAll(ServiceLoader.load(IMasteryParserExtension.class));
    }

    static <T extends SpecificationSourceCode, U> U process(T code, List<Function<T, U>> processors, String type)
    {
        return ListIterate
                .collect(processors, processor -> processor.apply(code))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + code.getType() + "'", code.getSourceInformation(), EngineErrorType.PARSER));
    }

    default List<Function<SpecificationSourceCode, Connection>> getExtraMasteryConnectionParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, MasteryRuntime>> getExtraMasteryRuntimeParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, Trigger>> getExtraTriggerParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, CredentialSecret>> getExtraCredentialSecretParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, AcquisitionProtocol>> getExtraAcquisitionProtocolParsers()
    {
        return Collections.emptyList();
    }

    default List<Function<SpecificationSourceCode, Authorization>> getExtraAuthorizationParsers()
    {
        return Collections.emptyList();
    }
}