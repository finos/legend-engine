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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.string;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.eclipse.collections.impl.test.Verify;

public abstract class AbstractTestChunk extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/testChunk.pure");
        runtime.compile();
    }

    @Test
    public void testChunkZeroSize()
    {
        compileTestSource("/test/testChunk.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "   'the quick brown fox jumps over the lazy dog'->chunk(0)\n" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Invalid chunk size: 0", "/test/testChunk.pure", 3, 51, e);
    }

    @Test
    public void testChunkNegativeSize()
    {
        compileTestSource("/test/testChunk.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "   'the quick brown fox jumps over the lazy dog'->chunk(-1)\n" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Invalid chunk size: -1", "/test/testChunk.pure", 3, 51, e);
    }

    @Test
    public void testChunkEmptyString()
    {
        compileTestSource("/test/testChunk.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "   ''->chunk(1)\n" +
                        "}\n");
        CoreInstance result = execute("test():Any[*]");
        Verify.assertInstanceOf(InstanceValue.class, result);
        Verify.assertEmpty(((InstanceValue)result)._values());
    }

    @Test
    public void testChunkLargerThanString()
    {
        compileTestSource("/test/testChunk.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "   'the quick brown fox jumped over the lazy dog'->chunk(1000)\n" +
                        "}\n");
        CoreInstance result = execute("test():Any[*]");
        Verify.assertInstanceOf(InstanceValue.class, result);
        Verify.assertListsEqual(Lists.mutable.with("the quick brown fox jumped over the lazy dog"), ((InstanceValue)result)._values().toList());
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(Lists.mutable.<CodeRepository>withAll(getCodeRepositories())
                .with(new GenericCodeRepository("test", null, "platform", "core_functions_unclassified"))));
    }
}
