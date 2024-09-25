// Copyright 2021 Goldman Sachs
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

import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractCompiledStateIntegrityTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestCoreCompiledStateIntegrity extends AbstractCompiledStateIntegrityTest
{
    @BeforeClass
    public static void initialize()
    {
        MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findCodeRepositories()));
        initialize(codeStorage);
    }

    @Test
    @Ignore
    @Override
    public void testReferenceUsages()
    {
        // TODO fix this test
        super.testReferenceUsages();
    }
}
