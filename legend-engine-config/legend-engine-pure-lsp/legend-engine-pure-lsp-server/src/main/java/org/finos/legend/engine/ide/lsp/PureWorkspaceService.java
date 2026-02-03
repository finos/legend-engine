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

package org.finos.legend.engine.ide.lsp;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.finos.legend.engine.ide.lsp.session.LSPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkspaceService implementation for Pure language.
 */
public class PureWorkspaceService implements WorkspaceService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureWorkspaceService.class);

    private final LSPSession session;

    public PureWorkspaceService(LSPSession session)
    {
        this.session = session;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params)
    {
        LOGGER.debug("Configuration changed: {}", params.getSettings());
        // Handle configuration changes if needed
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params)
    {
        LOGGER.debug("Watched files changed: {}", params.getChanges());
        // Handle file system changes if needed
        // This could trigger recompilation of affected files
    }
}
