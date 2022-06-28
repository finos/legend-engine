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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.HelperConnectionGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.ConnectionValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.PrimitiveTypeValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.ServiceParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.ServiceParameterValue;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertPath;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabSize;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperPersistenceContextComposer
{
    private HelperPersistenceContextComposer()
    {
    }

    public static String renderPersistenceContext(PersistenceContext persistenceContext, int indentLevel, PureGrammarComposerContext context)
    {
        return "PersistenceContext " + convertPath(persistenceContext.getPath()) + "\n" +
                "{\n" +
                renderPersistencePointer(persistenceContext.persistence, indentLevel) +
                renderPersistencePlatform(persistenceContext.platform, indentLevel, context) +
                renderServiceParameters(persistenceContext.serviceParameters, indentLevel, context) +
                (persistenceContext.sinkConnection == null ? "" : renderConnection(persistenceContext.sinkConnection, "sinkConnection", indentLevel, context)) +
                "}";
    }

    private static String renderPersistencePointer(String persistencePath, int indentLevel)
    {
        return getTabString(indentLevel) + "persistence: " + convertPath(persistencePath) + ";\n";
    }

    private static String renderPersistencePlatform(PersistencePlatform persistencePlatform, int indentLevel, PureGrammarComposerContext context)
    {
        List<IPersistenceComposerExtension> extensions = IPersistenceComposerExtension.getExtensions(context);
        String persistencePlatformText = IPersistenceComposerExtension.process(persistencePlatform, ListIterate.flatCollect(extensions, IPersistenceComposerExtension::getExtraPersistencePlatformComposers), indentLevel, context);

        return persistencePlatformText.isEmpty() ? "" : getTabString(indentLevel) + "platform: " + persistencePlatformText;
    }

    private static String renderServiceParameters(List<ServiceParameter> serviceParameters, int indentLevel, PureGrammarComposerContext context)
    {
        return !serviceParameters.isEmpty()
                ? getTabString(indentLevel) + "serviceParameters:\n" +
                getTabString(indentLevel) + "[\n" +
                ListIterate.collect(serviceParameters, sp -> renderServiceParameter(sp, indentLevel + 1, context)).makeString(",\n") +
                getTabString(indentLevel) + "];\n"
                : "";
    }

    private static String renderServiceParameter(ServiceParameter serviceParameter, int indentLevel, PureGrammarComposerContext context)
    {
        return getTabString(indentLevel) + serviceParameter.name + '=' + renderServiceParameterValue(serviceParameter.value, indentLevel, context);
    }

    private static String renderServiceParameterValue(ServiceParameterValue value, int indentLevel, PureGrammarComposerContext context)
    {
        if (value instanceof PrimitiveTypeValue)
        {
            return ((PrimitiveTypeValue) value).primitiveType.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
        }
        else if (value instanceof ConnectionValue)
        {
            return renderConnection(((ConnectionValue) value).connection, null, indentLevel, context);
        }
        throw new UnsupportedOperationException(
                "Service parameter value '" + value + "' of class " + value.getClass() + " is not valid. Value must be a primitive type or a connection");
    }

    private static String renderConnection(Connection connection, String prefix, int indentLevel, PureGrammarComposerContext context)
    {
        DEPRECATED_PureGrammarComposerCore composerCore = DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build();
        if (connection instanceof ConnectionPointer)
        {
            return (prefix == null ? "" : getTabString(indentLevel) + prefix + ": ") + PureGrammarComposerUtility.convertPath(connection.accept(composerCore)) + (prefix == null ? "" : ";\n");
        }
        return (prefix == null ? "\n" : getTabString(indentLevel) + prefix + ":\n") +
                getTabString(indentLevel) + "#{\n" +
                getTabString(indentLevel + 1) + HelperConnectionGrammarComposer.getConnectionValueName(connection, composerCore.toContext()) + "\n" +
                connection.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(composerCore).withIndentation(getTabSize(indentLevel + 1), true).build()) + "\n" +
                getTabString(indentLevel) + "}#" + (prefix == null ? "" : ";") + "\n";
    }
}
