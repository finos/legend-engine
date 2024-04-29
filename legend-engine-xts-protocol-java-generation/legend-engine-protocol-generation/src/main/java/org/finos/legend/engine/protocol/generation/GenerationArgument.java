// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.protocol.generation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;

public class GenerationArgument
{
    public final String outputDirectory;
    public final String configJson;
    public final RichIterable<String> dependencies;

    private GenerationArgument(String outputDirectory, String configJson, RichIterable<String> dependencies)
    {
        this.outputDirectory = outputDirectory;
        this.configJson = configJson;
        this.dependencies = dependencies;
    }

    public static GenerationArgument fromArgs(String... args) throws Exception
    {
        String json = readConfigFile(args[0]);
        RichIterable<String> dependencies = Lists.fixedSize.empty();
        if (args.length > 2)
        {
            dependencies = Lists.mutable.with(args[2].split(","))
                    .collect(String::trim)
                    .collect(Functions.throwing(GenerationArgument::readConfigFile));
        }

        return new GenerationArgument(args[1], json, dependencies);
    }

    private static String readConfigFile(String file) throws Exception
    {
        try (InputStream inputStream = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(file), () -> '\'' + file + "' not found on classpath"))
        {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }

}
