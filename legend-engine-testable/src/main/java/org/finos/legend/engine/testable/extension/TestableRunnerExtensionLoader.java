// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.extension;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Testable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestableRunnerExtensionLoader
{
    public static TestRunner forTestable(Testable testable)
    {
        return forTestable(testable, getCurrentThreadClassLoader());
    }

    public static TestRunner forTestable(Testable testable, ClassLoader classLoader)
    {
        return extensions(classLoader).stream()
                .map(ext -> ext.getTestRunner(testable))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No testable runner for " + testable.getClass().getSimpleName()));
    }

    public static Map<String, ? extends TestableRunnerExtension> getClassifierPathToTestableRunnerMap()
    {
        return getClassifierPathToTestableRunnerMap(getCurrentThreadClassLoader());
    }

    public static Map<String, ? extends TestableRunnerExtension> getClassifierPathToTestableRunnerMap(ClassLoader classLoader)
    {
        return extensions(classLoader).stream().collect(Collectors.toMap(TestableRunnerExtension::getSupportedClassifierPath, Function.identity()));
    }

    private static List<TestableRunnerExtension> extensions(ClassLoader classLoader)
    {
        List<TestableRunnerExtension> extensions = Lists.mutable.empty();
        for (TestableRunnerExtension extension : ServiceLoader.load(TestableRunnerExtension.class, classLoader))
        {
            extensions.add(extension);
        }
        return extensions;
    }

    private static ClassLoader getCurrentThreadClassLoader()
    {
        return Thread.currentThread().getContextClassLoader();
    }
}
