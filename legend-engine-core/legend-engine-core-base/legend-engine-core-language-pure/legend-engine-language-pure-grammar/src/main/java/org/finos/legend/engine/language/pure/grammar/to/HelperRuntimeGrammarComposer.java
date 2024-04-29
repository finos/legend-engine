// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.SingleConnectionEngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.StoreProviderPointer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.appendTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabSize;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperRuntimeGrammarComposer
{
    private static String renderIdentifiedConnection(IdentifiedConnection identifiedConnection, int baseIndentation, DEPRECATED_PureGrammarComposerCore transformer)
    {
        if (identifiedConnection.connection instanceof ConnectionPointer)
        {
            return getTabString(baseIndentation) + PureGrammarComposerUtility.convertIdentifier(identifiedConnection.id) + ": " + PureGrammarComposerUtility.convertPath(identifiedConnection.connection.accept(transformer));
        }
        return getTabString(baseIndentation) + PureGrammarComposerUtility.convertIdentifier(identifiedConnection.id) + ":\n" +
                getTabString(baseIndentation) + "#{\n" +
                getTabString(baseIndentation + 1) + HelperConnectionGrammarComposer.getConnectionValueName(identifiedConnection.connection, transformer.toContext()) + "\n" +
                identifiedConnection.connection.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(transformer).withIndentation(getTabSize(baseIndentation + 1), true).build()) + "\n" +
                getTabString(baseIndentation) + "}#";
    }

    public static String renderRuntimeValue(EngineRuntime engineRuntime, int baseIndentation, boolean isEmbeddedRuntime, DEPRECATED_PureGrammarComposerCore transformer)
    {
        StringBuilder builder = new StringBuilder();
        if (!isEmbeddedRuntime || !engineRuntime.mappings.isEmpty())
        {
            appendTabString(builder.append("\n"), baseIndentation).append("mappings:\n");
            appendTabString(builder, baseIndentation).append("[\n");
            builder.append(engineRuntime.mappings.stream().map(mappingPointer -> getTabString(baseIndentation + 1) + mappingPointer.path).collect(Collectors.joining(",\n"))).append(engineRuntime.mappings.isEmpty() ? "" : "\n");
            appendTabString(builder, baseIndentation).append("];");
        }
        return engineRuntime instanceof SingleConnectionEngineRuntime
                ? builder.append(renderSingleConnectionRuntimeValue((SingleConnectionEngineRuntime) engineRuntime, baseIndentation)).toString()
                : builder.append(renderEngineRuntimeValue(engineRuntime, baseIndentation, isEmbeddedRuntime, transformer)).toString();
    }

    private static String renderSingleConnectionRuntimeValue(SingleConnectionEngineRuntime singleConnectionEngineRuntime, int baseIndentation)
    {
        StringBuilder builder = new StringBuilder();
        if (!singleConnectionEngineRuntime.connectionStores.isEmpty())
        {
            appendTabString(builder.append("\n"), baseIndentation).append("connection: ").append(PureGrammarComposerUtility.convertPath(singleConnectionEngineRuntime.connectionStores.get(0).connectionPointer.connection)).append(";");
        }
        return builder.toString();
    }

    private static String renderEngineRuntimeValue(EngineRuntime engineRuntime, int baseIndentation, boolean isEmbeddedRuntime, DEPRECATED_PureGrammarComposerCore transformer)
    {
        StringBuilder builder = new StringBuilder();
        if (!engineRuntime.connections.isEmpty() && !engineRuntime.connections.parallelStream().allMatch(storeConnections -> storeConnections.storeConnections.isEmpty()))
        {
            List<String> storeConnectionStrings = new ArrayList<>();
            engineRuntime.connections.forEach(storeConnections ->
            {
                if (!storeConnections.storeConnections.isEmpty())
                {
                    storeConnectionStrings.add(
                            getTabString(baseIndentation + 1) + PureGrammarComposerUtility.convertPath(storeConnections.store.path) + ":\n" +
                                    getTabString(baseIndentation + 1) + "[\n" +
                                    (LazyIterate.collect(storeConnections.storeConnections, identifiedConnection -> renderIdentifiedConnection(identifiedConnection, baseIndentation + 2, transformer))).makeString(",\n") + "\n" +
                                    getTabString(baseIndentation + 1) + "]"
                    );
                }
            });
            appendTabString(builder.append("\n"), baseIndentation).append("connections:\n");
            appendTabString(builder, baseIndentation).append("[\n");
            builder.append(String.join(",\n", storeConnectionStrings));
            appendTabString(builder.append("\n"), baseIndentation).append("];");
        }
        if (!engineRuntime.connectionStores.isEmpty())
        {
            List<String> connectionStoreStrings = new ArrayList<>();
            engineRuntime.connectionStores.forEach(connectionPointerStore ->
            {
                if (!connectionPointerStore.storePointers.isEmpty())
                {
                    connectionStoreStrings.add(
                            getTabString(baseIndentation + 1) + PureGrammarComposerUtility.convertPath(connectionPointerStore.connectionPointer.connection) + ":\n" +
                                    getTabString(baseIndentation + 1) + "[\n" +
                                    (LazyIterate.collect(connectionPointerStore.storePointers, storePointer -> getTabString(baseIndentation + 2) + renderStoreProviderPointer(storePointer))).makeString(",\n") + "\n" +
                                    getTabString(baseIndentation + 1) + "]"
                    );
                }
            });
            appendTabString(builder.append("\n"), baseIndentation).append("connectionStores:\n");
            appendTabString(builder, baseIndentation).append("[\n");
            builder.append(String.join(",\n", connectionStoreStrings));
            appendTabString(builder.append("\n"), baseIndentation).append("];");
        }
        return builder.toString();
    }

    public static String renderStoreProviderPointer(StoreProviderPointer storeProviderPointer)
    {
        return (storeProviderPointer.type.equals(PackageableElementType.STORE) ?  "" : ("(" + storeProviderPointer.type.toString().toLowerCase() + ") ")) + PureGrammarComposerUtility.convertPath(storeProviderPointer.path);
    }
}
