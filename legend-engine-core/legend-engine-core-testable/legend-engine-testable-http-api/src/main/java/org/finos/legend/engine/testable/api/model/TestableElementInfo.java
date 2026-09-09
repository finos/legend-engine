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

package org.finos.legend.engine.testable.api.model;

public class TestableElementInfo
{
    public String path;
    public String type;
    public boolean hasTestSuites;
    public boolean hasLegacyTests;
    public int testSuiteCount;

    public TestableElementInfo()
    {
    }

    public TestableElementInfo(String path, String type, boolean hasTestSuites, boolean hasLegacyTests, int testSuiteCount)
    {
        this.path = path;
        this.type = type;
        this.hasTestSuites = hasTestSuites;
        this.hasLegacyTests = hasLegacyTests;
        this.testSuiteCount = testSuiteCount;
    }
}

