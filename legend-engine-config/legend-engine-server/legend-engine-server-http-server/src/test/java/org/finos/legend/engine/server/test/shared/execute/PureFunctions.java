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

package org.finos.legend.engine.server.test.shared.execute;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PureFunctions
{
    public static String alloy_metadataServer_pureModelFromMapping(String path, String version, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, ExecutionSupport executionSupport)
    {
        return buildBasePureModelFrom(path, "Mapping", version, extensions, executionSupport);
    }

    public static String alloy_metadataServer_pureModelFromStore(String _package, String version, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, ExecutionSupport executionSupport)
    {
        return buildBasePureModelFrom(_package, "Store", version, extensions, executionSupport);
    }

    private static String buildBasePureModelFrom(String path, String type, String version, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, ExecutionSupport executionSupport)
    {
        String className = "org.finos.legend.pure.generated.core_pure_protocol_" + version + "_scan_buildBasePureModel";
        String methodName = "Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_buildBasePureModelFrom" + type + "Str_String_1__Extension_MANY__String_1_";
        try
        {
            Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(className);
            Method method = cls.getMethod(methodName, String.class, RichIterable.class, ExecutionSupport.class);
            return (String) method.invoke(null, path, extensions, executionSupport);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Unsupported version: " + version + " (could not find class " + className + ")", e);
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("Unsupported version: " + version + " (could not find method " + methodName + " in class " + className + ")", e);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new RuntimeException("Error building model from " + type.toLowerCase() + " for version " + version, cause);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error building model from " + type.toLowerCase() + " for version " + version, e);
        }
    }
}
