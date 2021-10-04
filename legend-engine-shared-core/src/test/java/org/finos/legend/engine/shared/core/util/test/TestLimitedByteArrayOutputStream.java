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

package org.finos.legend.engine.shared.core.util.test;

import org.finos.legend.engine.shared.core.util.LimitedByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class TestLimitedByteArrayOutputStream
{
    @Test
    public void belowLimit()
    {
        LimitedByteArrayOutputStream stream = new LimitedByteArrayOutputStream(10);
        stream.write("abcdefg".getBytes(StandardCharsets.UTF_8), 0, 7);
        Assert.assertEquals("abcdefg", stream.toString());
    }

    @Test
    public void atLimit()
    {
        LimitedByteArrayOutputStream stream = new LimitedByteArrayOutputStream(10);
        stream.write("abcdefghij".getBytes(StandardCharsets.UTF_8), 0, 10);
        Assert.assertEquals("abcdefghij", stream.toString());
    }

    @Test
    public void beyondLimit()
    {
        LimitedByteArrayOutputStream stream = new LimitedByteArrayOutputStream(10);
        stream.write("abcdefghijklmnopq".getBytes(StandardCharsets.UTF_8), 0, 17);
        Assert.assertEquals("abcdefghij...", stream.toString());
    }
}
