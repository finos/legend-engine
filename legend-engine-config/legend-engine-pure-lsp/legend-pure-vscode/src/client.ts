// Copyright 2024 Goldman Sachs
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

import * as vscode from 'vscode';
import * as path from 'path';
import { spawn, ChildProcess } from 'child_process';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    StreamInfo,
    TransportKind
} from 'vscode-languageclient/node';

const CLIENT_ID = 'pureLsp';
const CLIENT_NAME = 'Pure Language Server';

/**
 * Creates and returns a new LanguageClient instance.
 */
export async function createLanguageClient(context: vscode.ExtensionContext): Promise<LanguageClient> {
    const serverOptions = await getServerOptions(context);
    const clientOptions = getClientOptions();

    const client = new LanguageClient(
        CLIENT_ID,
        CLIENT_NAME,
        serverOptions,
        clientOptions
    );

    return client;
}

/**
 * Starts the language client.
 */
export async function startLanguageClient(client: LanguageClient): Promise<void> {
    await client.start();
}

/**
 * Stops the language client.
 */
export async function stopLanguageClient(client: LanguageClient): Promise<void> {
    if (client.isRunning()) {
        await client.stop();
    }
}

/**
 * Gets the server options for starting the LSP server.
 */
async function getServerOptions(context: vscode.ExtensionContext): Promise<ServerOptions> {
    const config = vscode.workspace.getConfiguration('pureLsp');

    // Get Java executable path
    const javaHome = config.get<string>('java.home') || process.env.JAVA_HOME || '';
    const javaExecutable = javaHome
        ? path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
        : 'java';

    // Get server JAR path
    let serverPath = config.get<string>('server.path') || '';

    if (!serverPath) {
        // Use bundled server JAR
        serverPath = context.asAbsolutePath(
            path.join('server', 'legend-engine-pure-lsp-server-shaded.jar')
        );
    }

    // Get additional server arguments
    const serverArgs = config.get<string[]>('server.args') || [];

    // Build command line arguments
    const args = [
        '-jar',
        serverPath,
        '--stdio',
        ...serverArgs
    ];

    console.log(`Starting Pure LSP server with: ${javaExecutable} ${args.join(' ')}`);

    const serverOptions: ServerOptions = {
        run: {
            command: javaExecutable,
            args: args,
            transport: TransportKind.stdio
        },
        debug: {
            command: javaExecutable,
            args: [
                '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005',
                ...args
            ],
            transport: TransportKind.stdio
        }
    };

    return serverOptions;
}

/**
 * Gets the client options for the language client.
 */
function getClientOptions(): LanguageClientOptions {
    const config = vscode.workspace.getConfiguration('pureLsp');

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'pure' },
            { scheme: 'untitled', language: 'pure' }
        ],
        synchronize: {
            // Synchronize configuration changes
            configurationSection: 'pureLsp',
            // Watch for .pure file changes in the workspace
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.pure')
        },
        outputChannelName: CLIENT_NAME,
        traceOutputChannel: vscode.window.createOutputChannel('Pure LSP Trace'),
        revealOutputChannelOn: 4, // RevealOutputChannelOn.Never
        initializationOptions: {
            // Pass any initialization options to the server
        },
        middleware: {
            // Optional middleware for customizing client behavior
        }
    };

    return clientOptions;
}

/**
 * Checks if Java is available and returns its version.
 */
export async function checkJavaVersion(): Promise<string | null> {
    return new Promise((resolve) => {
        const config = vscode.workspace.getConfiguration('pureLsp');
        const javaHome = config.get<string>('java.home') || process.env.JAVA_HOME || '';
        const javaExecutable = javaHome
            ? path.join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
            : 'java';

        const child = spawn(javaExecutable, ['-version']);
        let output = '';

        child.stderr.on('data', (data) => {
            output += data.toString();
        });

        child.on('close', (code) => {
            if (code === 0) {
                // Extract version from output
                const match = output.match(/version "([^"]+)"/);
                resolve(match ? match[1] : 'unknown');
            } else {
                resolve(null);
            }
        });

        child.on('error', () => {
            resolve(null);
        });
    });
}
