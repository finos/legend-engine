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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
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

public interface IMasteryComposerExtension extends PureGrammarComposerExtension
{

    static List<IMasteryComposerExtension> getExtensions(PureGrammarComposerContext context)
    {
        return ListIterate.selectInstancesOf(context.extensions, IMasteryComposerExtension.class);
    }

    static String process(Trigger trigger, List<Function3<Trigger, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(trigger, processors, indentLevel, context, "trigger", trigger.sourceInformation);
    }

    static String process(AcquisitionProtocol acquisitionProtocol, List<Function3<AcquisitionProtocol, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(acquisitionProtocol, processors, indentLevel, context, "acquisition protocol", acquisitionProtocol.sourceInformation);
    }

    static String process(Connection connection, List<Function3<Connection, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(connection, processors, indentLevel, context, "connection", connection.sourceInformation);
    }

    static String process(MasteryRuntime masteryRuntime, List<Function3<MasteryRuntime, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(masteryRuntime, processors, indentLevel, context, "mastery runtime", masteryRuntime.sourceInformation);
    }

    static String process(AuthenticationStrategy authenticationStrategy, List<Function3<AuthenticationStrategy, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(authenticationStrategy, processors, indentLevel, context, "authentication strategy", authenticationStrategy.sourceInformation);
    }

    static String process(CredentialSecret secret, List<Function3<CredentialSecret, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(secret, processors, indentLevel, context, "secret", secret.sourceInformation);
    }

    static String process(Authorization authorization, List<Function3<Authorization, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context)
    {
        return process(authorization, processors, indentLevel, context, "authorization", authorization.sourceInformation);
    }

    static <T> String process(T item, List<Function3<T, Integer, PureGrammarComposerContext, String>> processors, int indentLevel, PureGrammarComposerContext context, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, indentLevel, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.PARSER));
    }

    default List<Function3<Connection, Integer, PureGrammarComposerContext, String>> getExtraMasteryConnectionComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<MasteryRuntime, Integer, PureGrammarComposerContext, String>> getExtraMasteryRuntimeComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<Trigger, Integer, PureGrammarComposerContext, String>> getExtraTriggerComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<AuthenticationStrategy, Integer, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<CredentialSecret, Integer, PureGrammarComposerContext, String>> getExtraSecretComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<AcquisitionProtocol, Integer, PureGrammarComposerContext, String>> getExtraAcquisitionProtocolComposers()
    {
        return Collections.emptyList();
    }

    default List<Function3<Authorization, Integer, PureGrammarComposerContext, String>> getExtraAuthorizationComposers()
    {
        return Collections.emptyList();
    }
}
