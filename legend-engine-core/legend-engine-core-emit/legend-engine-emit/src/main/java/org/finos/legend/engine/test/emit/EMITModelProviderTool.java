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

import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class EMITModelProviderTool
{
    private EMITModelProviderTool()
    {
    }

    public static List<EMITModelDescriptor> load(ClassLoader classLoader, String... resourcePaths)
    {
        EMITModelLoader loader = new EMITModelLoader();
        List<EMITModelDescriptor> result = new ArrayList<>(resourcePaths.length);
        for (String path : resourcePaths)
        {
            URL url = classLoader.getResource(path);
            if (url == null)
            {
                throw new IllegalStateException("EMIT yaml resource not found on classpath: " + path);
            }
            try
            {
                EMITModelDescriptor descriptor = loader.parseDescriptor(url);
                descriptor.setResourcePath(path);
                result.add(descriptor);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Failed to read EMIT yaml resource '" + path + "' from " + url, e);
            }
        }
        return result;
    }
}

