//  Copyright 2026 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.ide;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Config JSON shape for {@link LegendLspServer}. Every field mirrors a configuration knob already
 * exposed by the pure-lsp-server dev-loop skill / LegendPureLspServer's own CLI flags - this exists so
 * a human can write/edit the same parameters directly instead of relying on that skill to compute them.
 */
public class LegendLspServerConfiguration
{
    /**
     * Loopback TCP port the LSP server listens on (LegendPureLspServer's {@code --socket}/
     * {@code -Dlegend.lsp.socketPort} mode). Required - this class only supports socket mode, since the
     * point of running it under an IDE is that a separate LSP client (e.g. the Legend Pure IntelliJ
     * plugin, configured to connect rather than spawn its own process) connects in over this port while
     * this JVM sits under the IDE's own debugger.
     */
    public int port;

    /**
     * Filesystem directories to scan for workspace repos (each is expected to contain, somewhere
     * below it, module directories with a matching {@code &lt;name&gt;.definition.json} under their
     * {@code src/main/resources}) - equivalent to one
     * {@code --repo-root} per entry in the pure-lsp-server skill / pure-lsp-roots. A repo found under
     * ANY of these loads from live source and overrides the same-named repo that would otherwise load
     * from this module's own classpath jars. Relative paths resolve against the process's working
     * directory. Empty means "everything from the classpath jars, nothing source-backed."
     */
    public List<String> repoRoots = new java.util.ArrayList<>();

    /**
     * Repo names to treat as classpath-provided regardless of whether a same-named repo is also found
     * under repoRoots - passed straight through to LegendPureSession/PureRuntimeManager's
     * classpathRepositoryNames parameter. Most users can leave this empty; it mainly matters for
     * SHARED-mode debug-session scoping (see LegendDebug.ExecutionMode server-side).
     */
    public Set<String> classpathRepositoryNames = new LinkedHashSet<>();

    /**
     * Pure runtime options (the flags read by {@code isOptionSet('X')} in Pure, e.g.
     * {@code ForceInterpreted}, {@code ExecPlan}, {@code PlanLocal}, {@code FullInteractiveExec},
     * {@code ExecDebug}, {@code ShowLocalPlan} - see execution.pure / router_entry.pure). Each entry
     * becomes the system property {@code pure.options.<key>=<value>}, read live on every
     * execute()/go() call - equivalent to the pure-lsp-set-option skill's /set-option endpoint, but
     * fixed at launch instead of toggled at runtime.
     */
    public Map<String, Boolean> pureOptions = new LinkedHashMap<>();

    /**
     * Optional: backend Server integration for plan-gen + compiled execution instead of a purely
     * interpreted session (see the engine-server-start skill and pure-lsp-start's "backend Server
     * wanted" mode). Leave null for interpreted-only.
     */
    public BackendServerConfiguration backendServer;

    /**
     * Escape hatch for any other {@code -D} system property not covered by a named field above -
     * applied as-is, key/value verbatim.
     */
    public Map<String, String> additionalSystemProperties = new LinkedHashMap<>();

    /**
     * The {@code -Dlegend.test.*} properties the pure-lsp-start skill passes as {@code --jvm-arg}s when
     * a real backend Server (started via engine-server-start) is wanted for plan generation/execution,
     * named here instead of left as raw system-property strings to spell.
     */
    public static class BackendServerConfiguration
    {
        /**
         * {@code legend.test.server.host} - typically {@code 127.0.0.1}.
         */
        public String host;

        /**
         * {@code legend.test.server.port} - the backend Server's HTTP port.
         */
        public String port;

        /**
         * {@code legend.test.clientVersion} - e.g. {@code vX_X_X}.
         */
        public String clientVersion;

        /**
         * {@code legend.test.serverVersion} - e.g. {@code v1}.
         */
        public String serverVersion;

        /**
         * {@code legend.test.serializationKind} - e.g. {@code json}.
         */
        public String serializationKind;

        /**
         * {@code legend.test.h2.port} - the backend Server's embedded H2 port.
         */
        public String h2Port;
    }
}
