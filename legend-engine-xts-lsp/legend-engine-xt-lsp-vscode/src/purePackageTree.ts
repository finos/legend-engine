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
    Event,
    EventEmitter,
    TreeDataProvider,
    TreeItem,
    TreeItemCollapsibleState,
    ThemeIcon,
    Uri,
    window,
    commands,
    Range,
    Position,
} from 'vscode';
import { LanguageClient } from 'vscode-languageclient/node';

interface PackageChildInfo {
    name: string;
    qualifiedPath: string;
    kind: string;
    isPackage: boolean;
    childCount: number;
    uri: string | null;
    line: number | null;
}

export class PurePackageTreeProvider implements TreeDataProvider<PurePackageNode> {
    private readonly client: LanguageClient;
    private readonly _onDidChangeTreeData = new EventEmitter<PurePackageNode | undefined>();
    readonly onDidChangeTreeData: Event<PurePackageNode | undefined> = this._onDidChangeTreeData.event;

    constructor(client: LanguageClient) {
        this.client = client;
    }

    refresh(): void {
        this._onDidChangeTreeData.fire(undefined);
    }

    getTreeItem(element: PurePackageNode): TreeItem {
        return element;
    }

    async getChildren(element?: PurePackageNode): Promise<PurePackageNode[]> {
        const packagePath = element ? element.qualifiedPath : '::';

        try {
            const children: PackageChildInfo[] = await this.client.sendRequest(
                'legend/getPackageChildren',
                packagePath
            );

            return children.map((child) => {
                if (child.isPackage) {
                    return new PurePackageNode(
                        child.name,
                        child.qualifiedPath,
                        child.kind,
                        true,
                        child.childCount,
                        null,
                        null
                    );
                } else {
                    return new PurePackageNode(
                        child.name,
                        child.qualifiedPath,
                        child.kind,
                        false,
                        0,
                        child.uri,
                        child.line
                    );
                }
            });
        } catch (e) {
            return [];
        }
    }
}

const KIND_ICONS: Record<string, string> = {
    Package: 'symbol-namespace',
    Class: 'symbol-class',
    Enumeration: 'symbol-enum',
    ConcreteFunctionDefinition: 'symbol-function',
    NativeFunction: 'symbol-function',
    Profile: 'symbol-interface',
    Association: 'symbol-struct',
    Measure: 'symbol-number',
    Unit: 'symbol-number',
};

export class PurePackageNode extends TreeItem {
    public readonly qualifiedPath: string;

    constructor(
        public readonly name: string,
        qualifiedPath: string,
        public readonly kind: string,
        public readonly isPackage: boolean,
        childCount: number,
        uri: string | null,
        line: number | null
    ) {
        super(
            isPackage ? `${name} (${childCount})` : name,
            isPackage
                ? TreeItemCollapsibleState.Collapsed
                : TreeItemCollapsibleState.None
        );

        this.qualifiedPath = qualifiedPath;
        this.tooltip = qualifiedPath;
        this.description = isPackage ? '' : kind;
        this.iconPath = new ThemeIcon(KIND_ICONS[kind] || 'symbol-misc');
        this.contextValue = isPackage ? 'package' : 'element';

        // Click on a leaf element opens the file at the definition
        if (!isPackage && uri) {
            const targetUri = Uri.parse(uri);
            const targetLine = (line && line > 0) ? line - 1 : 0;
            this.command = {
                command: 'vscode.open',
                title: 'Go to Definition',
                arguments: [
                    targetUri,
                    {
                        selection: new Range(
                            new Position(targetLine, 0),
                            new Position(targetLine, 0)
                        ),
                    },
                ],
            };
        }
    }
}
