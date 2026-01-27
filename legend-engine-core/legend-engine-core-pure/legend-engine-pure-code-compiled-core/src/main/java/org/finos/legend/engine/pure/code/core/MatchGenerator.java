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

package org.finos.legend.engine.pure.code.core;

import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class MatchGenerator
{
    public static void main(String... args) throws Exception
    {
        String pkg = args[0];
        String className = args[1];
        String inputFunctionsFile = args[2];
        Path tagetDirectory = Paths.get(args[3]);

        FunctionExecutionInterpreted functionExecutionInterpreted = PureTestBuilderInterpreted.getFunctionExecutionInterpreted();
        ProcessorSupport processorSupport = functionExecutionInterpreted.getProcessorSupport();
        ModelRepository modelRepository = functionExecutionInterpreted.getPureRuntime().getModelRepository();

        RichIterable<? extends CoreInstance> functions = Lists.mutable.withAll(readFunctionFile(inputFunctionsFile)).collect(processorSupport::package_getByUserPath);

        CoreInstance generate = processorSupport.package_getByUserPath("meta::legend::compiler::match::generate_String_1__String_1__Function_MANY__String_1_");

        String code = functionExecutionInterpreted.start(generate,
                Lists.mutable.with(
                        ValueSpecificationBootstrap.newStringLiteral(modelRepository, pkg, processorSupport),
                        ValueSpecificationBootstrap.newStringLiteral(modelRepository, className, processorSupport),
                        ValueSpecificationBootstrap.wrapValueSpecification(functions, true, processorSupport)
                )
        ).getValueForMetaPropertyToOne(M3Properties.values).getName();

        Path output = tagetDirectory.resolve(Paths.get(pkg.replaceAll("\\.", "/") + "/" + className + ".java"));
        Files.createDirectories(output.getParent());
        Files.write(output, code.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static List<String> readFunctionFile(String file) throws Exception
    {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file))
        {
            assert inputStream != null;
            return IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
        }
    }
}
