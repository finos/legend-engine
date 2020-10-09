// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.extension;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.slf4j.Logger;

import java.util.List;
import java.util.ServiceLoader;

public class CompilerExtensionLoader
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    public static void logExtensionList()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(LazyIterate.collect(extensions(), extension -> "- " + extension.getClass().getSimpleName()).makeString("Compiler extension(s) loaded:\n", "\n", ""));
        }
    }

    public static List<CompilerExtension> extensions()
    {
        List<CompilerExtension> extensions = Lists.mutable.empty();
        for (CompilerExtension extension : ServiceLoader.load(CompilerExtension.class))
        {
            try
            {
                extensions.add(extension);
            }
            catch (Throwable throwable)
            {
                LOGGER.error("Failed to load compiler extension '" + extension.getClass().getSimpleName() + "'");
                throwable.printStackTrace();
                // Needs to be silent ... during the build process
            }
        }
        return extensions;
    }
}
