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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.AbstractCompiledStateIntegrityTest;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestCoreCompiledStateIntegrity extends AbstractCompiledStateIntegrityTest
{
    @BeforeClass
    public static void initialize()
    {
        MutableCodeStorage codeStorage = new PureCodeStorage(null, new ClassLoaderCodeStorage(Lists.mutable.with(CodeRepository.newPlatformCodeRepository()).withAll(CodeRepositoryProviderHelper.findCodeRepositories())));
        initialize(codeStorage);
    }

    @Test
    @Ignore
    @Override
    public void testPackageHasChildrenWithDuplicateNames()
    {
        // TODO fix this test
        super.testPackageHasChildrenWithDuplicateNames();
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
