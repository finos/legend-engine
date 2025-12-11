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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class PythonExecutionUtil
{
    private static final String DOCKER_IMAGE = "finos/pylegend:0.10.0";
    private static GenericContainer<?> pythonContainer = new GenericContainer<>(DockerImageName.parse(DOCKER_IMAGE)).withCommand("tail", "-f", "/dev/null");
    private static final Object lock = new Object();

    private PythonExecutionUtil()
    {
        // utility class
    }

    public static CoreInstance executePythonScript(String script, ProcessorSupport processorSupport)
    {
        if (!pythonContainer.isRunning())
        {
            try
            {
                pythonContainer.start();
                Runtime.getRuntime().addShutdownHook(new Thread(pythonContainer::stop));
            }
            catch (Exception e)
            {
                CoreInstance errorCoreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
                Instance.setValuesForProperty(errorCoreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(-1), "Integer", null)), processorSupport);
                Instance.setValuesForProperty(errorCoreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance("", "String", null)), processorSupport);
                Instance.setValuesForProperty(errorCoreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance("Container initialization failed: " + e.getMessage(), "String", null)), processorSupport);
                return errorCoreInstance;
            }
        }

        try
        {
            // Use the already initialized (or currently initializing) singleton container for script execution.
            Container.ExecResult execResult = pythonContainer.execInContainer("python3", "-c", script);

            int exitCode = execResult.getExitCode();
            String output = execResult.getStdout();
            String error = execResult.getStderr();

            CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
            Instance.setValuesForProperty(coreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(exitCode), "Integer", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance(output, "String", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance(error, "String", null)), processorSupport);
            return coreInstance;
        }
        catch (Exception e)
        {
            // Handle exceptions that occur during the script execution itself.
            CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
            Instance.setValuesForProperty(coreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(-1), "Integer", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance("", "String", null)), processorSupport);
            Instance.setValuesForProperty(coreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance(e.getMessage(), "String", null)), processorSupport);
            return coreInstance;
        }
//        try (GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("finos/pylegend:0.10.0")))
//        {
//            container.withCommand("tail","-f","/dev/null");
//            container.start();
//
//            Container.ExecResult execResult = container.execInContainer("python3", "-c", script);
//
//            int exitCode = execResult.getExitCode();
//            String output = execResult.getStdout();
//            String error = execResult.getStderr();
//
//            CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
//            Instance.setValuesForProperty(coreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(exitCode), "Integer", null)), processorSupport);
//            Instance.setValuesForProperty(coreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance(output, "String", null)), processorSupport);
//            Instance.setValuesForProperty(coreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance(error, "String", null)), processorSupport);
//            return coreInstance;
//        }
//        catch (Exception e)
//        {
//            CoreInstance coreInstance = processorSupport.newEphemeralAnonymousCoreInstance("meta::python::execution::PythonExecutionResult");
//            Instance.setValuesForProperty(coreInstance, "exitCode", Lists.immutable.of(processorSupport.newCoreInstance(Integer.toString(-1), "Integer", null)), processorSupport);
//            Instance.setValuesForProperty(coreInstance, "output", Lists.immutable.of(processorSupport.newCoreInstance("", "String", null)), processorSupport);
//            Instance.setValuesForProperty(coreInstance, "error", Lists.immutable.of(processorSupport.newCoreInstance(e.getMessage(), "String", null)), processorSupport);
//            return coreInstance;
//        }
    }
}
