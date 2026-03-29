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

package org.finos.legend.engine.lsp;

/**
 * Simple logging utility that writes to stderr so output is visible
 * in the VS Code Output panel ("Legend Pure LSP" channel).
 *
 * Uses stderr because stdout is reserved for JSON-RPC communication.
 * The server's main() method redirects System.out/System.err to
 * FileDescriptor.err, so these messages appear in the extension's
 * output channel.
 */
public class LspLog
{
    public static void info(String message)
    {
        System.err.println("[LSP] " + message);
    }

    public static void info(String format, Object... args)
    {
        System.err.println("[LSP] " + String.format(format.replace("{}", "%s"), args));
    }

    public static void warn(String message)
    {
        System.err.println("[LSP-WARN] " + message);
    }

    public static void debug(String message)
    {
        System.err.println("[LSP-DEBUG] " + message);
    }
}
