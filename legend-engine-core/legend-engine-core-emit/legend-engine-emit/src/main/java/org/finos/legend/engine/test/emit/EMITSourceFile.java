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

package org.finos.legend.engine.test.emit;

import java.nio.file.Path;

public class EMITSourceFile
{
    private final String virtualPath;
    private final Path absolutePath;
    private final boolean primary;

    public EMITSourceFile(String virtualPath, Path absolutePath, boolean primary)
    {
        this.virtualPath = virtualPath;
        this.absolutePath = absolutePath;
        this.primary = primary;
    }

    public String getVirtualPath()
    {
        return this.virtualPath;
    }

    public Path getAbsolutePath()
    {
        return this.absolutePath;
    }

    public boolean isPrimary()
    {
        return this.primary;
    }
}
