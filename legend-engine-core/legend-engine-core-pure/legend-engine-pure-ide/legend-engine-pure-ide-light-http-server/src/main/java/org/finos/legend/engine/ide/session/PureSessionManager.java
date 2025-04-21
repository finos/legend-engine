// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.ide.session;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.ide.SourceLocationConfiguration;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;

public class PureSessionManager
{
    private PureSession session;
    private SourceLocationConfiguration sourceLocationConfiguration;
    private boolean debugMode;
    private MutableList<RepositoryCodeStorage> repositories;
    private boolean hasSession;

    public PureSessionManager(SourceLocationConfiguration sourceLocationConfiguration, boolean debugMode, MutableList<RepositoryCodeStorage> repositories)
    {
        this.sourceLocationConfiguration = sourceLocationConfiguration;
        this.debugMode = debugMode;
        this.repositories = repositories;
        this.hasSession = false;
    }

    public synchronized void createSession()
    {
        this.session = new PureSession(this.sourceLocationConfiguration, this.debugMode, this.repositories);
        this.hasSession = true;
    }

    public synchronized PureSession getSession()
    {
        if (!this.hasSession)
        {
            throw new IllegalStateException("Session has not been created yet. Please create a session before accessing it.");
        }

        return this.session;
    }

    public synchronized void resetSession()
    {
        this.session = new PureSession(this.sourceLocationConfiguration, this.debugMode, this.repositories);
    }

    public void setDebugMode(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    public void setSourceLocationConfiguration(SourceLocationConfiguration sourceLocationConfiguration)
    {
        this.sourceLocationConfiguration = sourceLocationConfiguration;
    }

    public void setRepositories(MutableList<RepositoryCodeStorage> repositories)
    {
        this.repositories = repositories;
    }
}
