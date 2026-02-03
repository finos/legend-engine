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
import { createLanguageClient, startLanguageClient, stopLanguageClient } from './client';
import { LanguageClient } from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
    console.log('Pure Language extension is activating...');

    // Create and start the language client
    try {
        client = await createLanguageClient(context);
        await startLanguageClient(client);
        console.log('Pure Language Server started successfully');
    } catch (error) {
        console.error('Failed to start Pure Language Server:', error);
        vscode.window.showErrorMessage(`Failed to start Pure Language Server: ${error}`);
        return;
    }

    // Register commands
    context.subscriptions.push(
        vscode.commands.registerCommand('pureLsp.restartServer', async () => {
            vscode.window.showInformationMessage('Restarting Pure Language Server...');

            if (client) {
                await stopLanguageClient(client);
            }

            try {
                client = await createLanguageClient(context);
                await startLanguageClient(client);
                vscode.window.showInformationMessage('Pure Language Server restarted successfully');
            } catch (error) {
                vscode.window.showErrorMessage(`Failed to restart Pure Language Server: ${error}`);
            }
        })
    );

    // Add client to subscriptions for cleanup
    if (client) {
        context.subscriptions.push(client);
    }

    console.log('Pure Language extension activated');
}

export async function deactivate(): Promise<void> {
    console.log('Pure Language extension is deactivating...');

    if (client) {
        await stopLanguageClient(client);
    }

    console.log('Pure Language extension deactivated');
}
