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

import * as path from 'path';
import * as fs from 'fs';
import * as vscode from 'vscode';
import { workspace, ExtensionContext, Uri, commands, window } from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';
import { PureFileSystemProvider } from './pureFileSystemProvider';
import { PurePackageTreeProvider } from './purePackageTree';

let client: LanguageClient | undefined;
let pureFs: PureFileSystemProvider | undefined;
let packageTree: PurePackageTreeProvider | undefined;
let goOutputChannel: import('vscode').OutputChannel | undefined;

// Resolves when PureRuntime is fully initialized (server sends "ready" message)
let serverReady: Promise<void>;
let resolveServerReady: () => void;
serverReady = new Promise(r => { resolveServerReady = r; });

export function activate(context: ExtensionContext): void {
    const jarPath = resolveServerJar();
    console.log('[Legend Pure] Resolved server JAR:', jarPath);
    const jarSize = jarPath ? Math.round(fs.statSync(jarPath).size / 1024 / 1024) : 0;
    console.log(`[Legend Pure] JAR size: ${jarSize}MB${jarSize < 10 ? ' — WARNING: this looks like the thin JAR, expected ~36MB' : ''}`);
    if (!jarPath) {
        window.showErrorMessage(
            'Legend Pure LSP: server JAR not found. ' +
            'Set "legendPure.server.jarPath" in settings or build the server with Maven.'
        );
        return;
    }

    const javaHome = getJavaExecutable();

    const serverOptions: ServerOptions = {
        command: javaHome,
        args: ['-jar', jarPath],
        options: { env: process.env },
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'pure' },
            { scheme: 'pure', language: 'pure' },
        ],
        synchronize: {
            fileEvents: workspace.createFileSystemWatcher('**/*.pure'),
        },
    };

    client = new LanguageClient(
        'legendPureLsp',
        'Legend Pure LSP',
        serverOptions,
        clientOptions
    );

    // Register the executeGo command
    context.subscriptions.push(
        commands.registerCommand('legend.executeGo', async () => {
            if (!client) {
                window.showErrorMessage('Pure LSP not started');
                return;
            }
            if (!goOutputChannel) {
                goOutputChannel = window.createOutputChannel('Pure Go');
            }
            const out = goOutputChannel;
            out.clear();
            out.show(true);
            out.appendLine('Executing go()...');

            try {
                const result: { success: boolean; error: string | null; output: string | null } =
                    await client.sendRequest('legend/executeGo');

                if (result.success) {
                    out.appendLine(result.output || '(no output)');
                    out.appendLine('\n--- Execution complete ---');
                } else {
                    out.appendLine('ERROR: ' + (result.error || 'Unknown error'));
                }
            } catch (e: any) {
                out.appendLine('ERROR: ' + (e.message || e));
            }
        })
    );

    // Register LLM tools (VS Code LanguageModelTool API for Copilot/agents)
    registerLanguageModelTools(context);

    // Start the client and register providers once ready
    client.start().then(() => {
        if (client) {
            // Register pure:// filesystem provider
            pureFs = new PureFileSystemProvider(client);
            context.subscriptions.push(
                workspace.registerFileSystemProvider('pure', pureFs, {
                    isReadonly: true,
                    isCaseSensitive: true,
                })
            );
            context.subscriptions.push(pureFs);

            // Register Pure package tree view
            packageTree = new PurePackageTreeProvider(client);
            context.subscriptions.push(
                window.createTreeView('purePackageTree', {
                    treeDataProvider: packageTree,
                    showCollapseAll: true,
                })
            );

            // Refresh tree and clear caches on reindex
            context.subscriptions.push(
                commands.registerCommand('legend.refreshPackageTree', () => {
                    if (pureFs) {
                        pureFs.clearCache();
                    }
                    if (packageTree) {
                        packageTree.refresh();
                    }
                })
            );

            // Listen for showMessage to detect server readiness and reindex completion
            client.onNotification('window/showMessage', (params: any) => {
                if (params.message && params.message.includes(': ready')) {
                    console.log('[Legend Pure] Server ready — tools are now active');
                    resolveServerReady();
                    // Refresh the package tree now that PureRuntime is initialized
                    if (packageTree) {
                        packageTree.refresh();
                    }
                }
                if (params.message && params.message.includes('reindex complete')) {
                    if (pureFs) {
                        pureFs.clearCache();
                    }
                    if (packageTree) {
                        packageTree.refresh();
                    }
                }
            });
        }
    });
}

export function deactivate(): Thenable<void> | undefined {
    if (pureFs) {
        pureFs.dispose();
        pureFs = undefined;
    }
    if (!client) {
        return undefined;
    }
    return client.stop();
}

function resolveServerJar(): string | undefined {
    // 1. Check user configuration
    const config = workspace.getConfiguration('legendPure');
    const configuredPath = config.get<string>('server.jarPath');
    if (configuredPath && fs.existsSync(configuredPath)) {
        return configuredPath;
    }

    // 2. Look for the JAR relative to this extension (sibling Maven module)
    const extensionDir = path.resolve(__dirname, '..');
    const serverTargetDir = path.resolve(
        extensionDir,
        '..',
        'legend-engine-xt-lsp-server',
        'target'
    );
    if (fs.existsSync(serverTargetDir)) {
        // Look for a shaded/fat JAR first, then any matching JAR
        const files = fs.readdirSync(serverTargetDir);
        const shadedJar = files.find(
            (f) => f.endsWith('-server.jar') || f.endsWith('-shaded.jar') || f.endsWith('-jar-with-dependencies.jar')
        );
        if (shadedJar) {
            return path.join(serverTargetDir, shadedJar);
        }
        // Fall back to the main artifact JAR
        const mainJar = files.find(
            (f) =>
                f.startsWith('legend-engine-xt-lsp-server-') &&
                f.endsWith('.jar') &&
                !f.endsWith('-sources.jar') &&
                !f.endsWith('-javadoc.jar') &&
                !f.endsWith('-tests.jar')
        );
        if (mainJar) {
            return path.join(serverTargetDir, mainJar);
        }
    }

    // 3. Look relative to workspace folders
    const workspaceFolders = workspace.workspaceFolders;
    if (workspaceFolders) {
        for (const folder of workspaceFolders) {
            const targetDir = path.join(
                folder.uri.fsPath,
                'legend-engine-xts-lsp',
                'legend-engine-xt-lsp-server',
                'target'
            );
            if (fs.existsSync(targetDir)) {
                const files = fs.readdirSync(targetDir);
                const shadedJar = files.find(
                    (f) =>
                        f.endsWith('-server.jar') ||
                        f.endsWith('-shaded.jar') ||
                        f.endsWith('-jar-with-dependencies.jar')
                );
                if (shadedJar) {
                    return path.join(targetDir, shadedJar);
                }
                const mainJar = files.find(
                    (f) =>
                        f.startsWith('legend-engine-xt-lsp-server-') &&
                        f.endsWith('.jar') &&
                        !f.endsWith('-sources.jar') &&
                        !f.endsWith('-javadoc.jar') &&
                        !f.endsWith('-tests.jar')
                );
                if (mainJar) {
                    return path.join(targetDir, mainJar);
                }
            }
        }
    }

    return undefined;
}

// ── LLM Tool Registration ──────────────────────────────────────────

function registerLanguageModelTools(context: ExtensionContext): void {
    // Guard: vscode.lm.registerTool requires VS Code 1.99+
    if (!vscode.lm || typeof vscode.lm.registerTool !== 'function') {
        console.log('[Legend Pure] vscode.lm.registerTool not available — skipping tool registration');
        return;
    }

    console.log('[Legend Pure] Registering LLM tools...');

    /** Wait for both client and PureRuntime to be ready */
    async function ensureReady(): Promise<string | null> {
        if (!client) { return 'Pure LSP not started'; }
        // Wait up to 120s for PureRuntime initialization
        const timeout = new Promise<void>(r => setTimeout(r, 120_000));
        await Promise.race([serverReady, timeout]);
        // Check again after waiting
        if (!client) { return 'Pure LSP not started'; }
        return null;
    }

    // Tool 1: Search Pure symbols
    context.subscriptions.push(
        vscode.lm.registerTool('legend-pure-search-symbols', {
            async invoke(options: vscode.LanguageModelToolInvocationOptions<{ query: string }>, token: vscode.CancellationToken) {
                const err = await ensureReady();
                if (err) {
                    return new vscode.LanguageModelToolResult([
                        new vscode.LanguageModelTextPart(err),
                    ]);
                }
                const symbols: any[] = await client.sendRequest(
                    'workspace/symbol',
                    { query: options.input.query }
                );
                if (!symbols || symbols.length === 0) {
                    return new vscode.LanguageModelToolResult([
                        new vscode.LanguageModelTextPart(`No symbols found for "${options.input.query}"`),
                    ]);
                }
                const lines = symbols.slice(0, 50).map((s: any) => {
                    const kind = symbolKindName(s.kind);
                    const uri = s.location?.uri || '';
                    const line = s.location?.range?.start?.line;
                    const loc = line != null ? `${uri}#L${line + 1}` : uri;
                    return `${s.name} (${kind}) — ${loc}`;
                });
                const text = `Found ${symbols.length} symbol(s):\n${lines.join('\n')}`;
                return new vscode.LanguageModelToolResult([
                    new vscode.LanguageModelTextPart(text),
                ]);
            },
            async prepareInvocation(options: vscode.LanguageModelToolInvocationPrepareOptions<{ query: string }>) {
                return { invocationMessage: `Searching Pure symbols for "${options.input.query}"...` };
            },
        })
    );

    // Tool 2: Execute go()
    context.subscriptions.push(
        vscode.lm.registerTool('legend-pure-execute-go', {
            async invoke(options: vscode.LanguageModelToolInvocationOptions<Record<string, never>>, token: vscode.CancellationToken) {
                const err = await ensureReady();
                if (err) {
                    return new vscode.LanguageModelToolResult([new vscode.LanguageModelTextPart(err)]);
                }
                const result: { success: boolean; error: string | null; output: string | null } =
                    await client.sendRequest('legend/executeGo');
                const text = result.success
                    ? (result.output || '(no output)')
                    : `ERROR: ${result.error || 'Unknown error'}`;
                return new vscode.LanguageModelToolResult([
                    new vscode.LanguageModelTextPart(text),
                ]);
            },
            async prepareInvocation() {
                return { invocationMessage: 'Executing Pure go() function...' };
            },
        })
    );

    // Tool 3: Get source content
    context.subscriptions.push(
        vscode.lm.registerTool('legend-pure-get-source', {
            async invoke(options: vscode.LanguageModelToolInvocationOptions<{ sourceId: string }>, token: vscode.CancellationToken) {
                const err = await ensureReady();
                if (err) {
                    return new vscode.LanguageModelToolResult([new vscode.LanguageModelTextPart(err)]);
                }
                const content: string | null = await client!.sendRequest(
                    'legend/getSourceContent',
                    options.input.sourceId
                );
                if (content == null) {
                    return new vscode.LanguageModelToolResult([
                        new vscode.LanguageModelTextPart(`Source not found: ${options.input.sourceId}`),
                    ]);
                }
                return new vscode.LanguageModelToolResult([
                    new vscode.LanguageModelTextPart(content),
                ]);
            },
            async prepareInvocation(options: vscode.LanguageModelToolInvocationPrepareOptions<{ sourceId: string }>) {
                return { invocationMessage: `Reading ${options.input.sourceId}...` };
            },
        })
    );

    console.log('[Legend Pure] 3 LLM tools registered');
}

function symbolKindName(kind: number): string {
    const kinds: Record<number, string> = {
        1: 'File', 2: 'Module', 3: 'Namespace', 4: 'Package', 5: 'Class',
        6: 'Method', 7: 'Property', 8: 'Field', 9: 'Constructor', 10: 'Enum',
        11: 'Interface', 12: 'Function', 13: 'Variable', 14: 'Constant',
        15: 'String', 16: 'Number', 17: 'Boolean', 18: 'Array', 19: 'Object',
        20: 'Key', 21: 'Null', 22: 'EnumMember', 23: 'Struct', 24: 'Event',
        25: 'Operator', 26: 'TypeParameter',
    };
    return kinds[kind] || `Kind(${kind})`;
}

function getJavaExecutable(): string {
    const config = workspace.getConfiguration('legendPure');
    const javaHome = config.get<string>('java.home');
    if (javaHome) {
        const javaBin = path.join(javaHome, 'bin', 'java');
        if (fs.existsSync(javaBin)) {
            return javaBin;
        }
    }
    return 'java';
}
