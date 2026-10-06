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

package org.finos.legend.engine.testable.api;

import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.testable.api.model.TestableElementCoverageResult;
import org.finos.legend.engine.testable.api.model.TestableElementInfo;
import org.finos.legend.engine.testable.extension.TestableRunnerExtension;
import org.finos.legend.engine.testable.extension.TestableRunnerExtensionLoader;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestableElementCoverageAnalyzer
{
    private TestableElementCoverageAnalyzer()
    {
    }

    public static TestableElementCoverageResult analyze(PureModelContextData data)
    {
        Map<String, List<TestableElementInfo>> elementsByType = data.getElements().stream()
                .map(TestableElementCoverageAnalyzer::analyzeElement)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(info -> info.type));

        return new TestableElementCoverageResult(elementsByType);
    }

    private static TestableElementInfo analyzeElement(PackageableElement element)
    {
        TestableRunnerExtension extension = TestableRunnerExtensionLoader.getExtensionForElement(element);
        if (extension == null)
        {
            return null;
        }

        int testCount = extension.getTestCount(element);
        boolean hasLegacyTests = extension.hasLegacyTests(element);
        return new TestableElementInfo(element.getPath(), element.getClass().getSimpleName(), testCount > 0, hasLegacyTests, testCount);
    }
}