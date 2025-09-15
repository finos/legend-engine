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

package org.finos.legend.engine.pure.execution.runtime.java.extension.shared;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.InputStream;
import java.util.Scanner;

public class PythonExecutionUtil
{
    private PythonExecutionUtil()
    {
        // utility class
    }

    public static CoreInstance executePythonScript(String script, ProcessorSupport processorSupport)
    {
        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder("python3", "-c", script);
            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();
            Scanner outputScanner = new Scanner(inputStream).useDelimiter("\\A");
            Scanner errorScanner = new Scanner(errorStream).useDelimiter("\\A");

            int exitCode = process.waitFor();
            String output = outputScanner.hasNext() ? outputScanner.next() : "";
            String error = errorScanner.hasNext() ? errorScanner.next() : "";
            outputScanner.close();
            errorScanner.close();

            CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
            Instance.setValuesForProperty(coreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(exitCode), "Integer", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance(output, "String", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance(error, "String", null)), processorSupport);
            return coreInstance;
        }
        catch (Exception e)
        {
            CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
            Instance.setValuesForProperty(coreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(-1), "Integer", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance("", "String", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance(e.getMessage(), "String", null)), processorSupport);
            return coreInstance;
        }
    }
}
