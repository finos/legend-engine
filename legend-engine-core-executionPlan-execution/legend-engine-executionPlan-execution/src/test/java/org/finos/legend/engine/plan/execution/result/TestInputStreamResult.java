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

package org.finos.legend.engine.plan.execution.result;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class TestInputStreamResult
{
    @Test
    public void testInputStreamClose()
    {
        TestableCloseable closeable = new TestableCloseable();
        TestableInputStream inputStream = new TestableInputStream("Dummy Value".getBytes(StandardCharsets.UTF_8));

        Result result = new InputStreamResult(inputStream, Collections.emptyList(), Collections.singletonList(closeable));

        Assert.assertFalse(closeable.isClosed);
        Assert.assertFalse(inputStream.isClosed);
        result.close();
        Assert.assertTrue(closeable.isClosed);
        Assert.assertTrue(inputStream.isClosed);
    }

    private static class TestableCloseable implements Closeable
    {
        public boolean isClosed = false;

        @Override
        public void close() throws IOException
        {
            isClosed = true;
        }
    }

    private static class TestableInputStream extends ByteArrayInputStream
    {
        public boolean isClosed = false;

        public TestableInputStream(byte[] buf)
        {
            super(buf);
        }

        @Override
        public void close()
        {
            this.isClosed = true;
        }
    }
}
