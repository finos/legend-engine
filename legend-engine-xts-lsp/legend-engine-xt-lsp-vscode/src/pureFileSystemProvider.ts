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

import {
    Disposable,
    Event,
    EventEmitter,
    FileChangeEvent,
    FileSystemProvider,
    FileStat,
    FileType,
    Uri,
} from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

/**
 * Read-only FileSystemProvider for the `pure://` scheme.
 * Fetches Pure source content from the LSP server on demand via
 * the custom `legend/getSourceContent` request.
 */
export class PureFileSystemProvider implements FileSystemProvider, Disposable {
    private readonly client: LanguageClient;
    private readonly contentCache = new Map<string, Uint8Array>();
    private readonly _onDidChangeFile = new EventEmitter<FileChangeEvent[]>();
    readonly onDidChangeFile: Event<FileChangeEvent[]> = this._onDidChangeFile.event;

    constructor(client: LanguageClient) {
        this.client = client;
    }

    dispose(): void {
        this._onDidChangeFile.dispose();
    }

    watch(): Disposable {
        // Pure sources are read-only; no watching needed
        return { dispose: () => {} };
    }

    async stat(uri: Uri): Promise<FileStat> {
        // Ensure content is fetched so we can report size
        const content = await this.getContent(uri);
        return {
            type: FileType.File,
            ctime: 0,
            mtime: 0,
            size: content.length,
        };
    }

    readDirectory(): [string, FileType][] {
        return [];
    }

    createDirectory(): void {
        throw new Error('Pure sources are read-only');
    }

    async readFile(uri: Uri): Promise<Uint8Array> {
        return this.getContent(uri);
    }

    writeFile(): void {
        throw new Error('Pure sources are read-only');
    }

    delete(): void {
        throw new Error('Pure sources are read-only');
    }

    rename(): void {
        throw new Error('Pure sources are read-only');
    }

    private async getContent(uri: Uri): Promise<Uint8Array> {
        const key = uri.toString();
        const cached = this.contentCache.get(key);
        if (cached) {
            return cached;
        }

        // Request content from the LSP server
        const sourceId = uri.path; // e.g., /core/pure/extensions/extension.pure
        const content: string | null = await this.client.sendRequest(
            'legend/getSourceContent',
            sourceId
        );

        if (content === null || content === undefined) {
            throw new Error(`Source not found: ${sourceId}`);
        }

        const bytes = new TextEncoder().encode(content);
        this.contentCache.set(key, bytes);
        return bytes;
    }

    /** Clear the cache (e.g., on reindex). */
    clearCache(): void {
        this.contentCache.clear();
    }
}
