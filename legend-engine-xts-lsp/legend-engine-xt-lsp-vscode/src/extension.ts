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

export function activate(context: ExtensionContext): void {
    const jarPath = resolveServerJar();
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

            // Listen for showMessage to detect reindex completion and clear caches
            client.onNotification('window/showMessage', (params: any) => {
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
