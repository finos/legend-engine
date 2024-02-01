//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.pure.runtime.extensions.tests.config;

import org.finos.legend.engine.pure.code.core.TestLegendPureCoreExtension;

public class TestLegendPureCoreExtensionOne implements TestLegendPureCoreExtension
{
    @Override
    public String functionFile()
    {
        return "core_external/extensions/extensionsTest.pure";
    }

    @Override
    public String functionSignature()
    {
        return "meta::pure::extension::runtime::test::testExtensionOne__Extension_1_";
    }
}
