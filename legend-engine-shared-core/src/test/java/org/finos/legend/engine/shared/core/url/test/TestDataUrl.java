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

package org.finos.legend.engine.shared.core.url.test;

import org.junit.Assert;
import org.junit.Test;

public class TestDataUrl extends UrlTest
{
    @Test
    public void testWithoutMimeOrEncoding()
    {
        Assert.assertEquals("Hello, World!", readUrl("data:,Hello%2C%20World!"));
    }

    @Test
    public void testWithSimpleMimeNoBase64Encoding()
    {
        Assert.assertEquals("Hello, World!", readUrl("data:text/plain,Hello%2C%20World!"));
    }

    @Test
    public void testWithSimpleMimeBase64Encoded()
    {
        Assert.assertEquals("Hello, World!", readUrl("data:text/plain;base64,SGVsbG8sIFdvcmxkIQ=="));
    }

    @Test
    public void testWithSpecialCharacterHash()
    {
        Assert.assertEquals("{\"test\":\"a#b\"}", readUrl("data:application/json,{\"test\":\"a#b\"}"));
    }

    @Test
    public void testWithSpecialCharacterQuestionMark()
    {
        Assert.assertEquals("{\"test\":\"a?b\"}", readUrl("data:application/json,{\"test\":\"a?b\"}"));
    }
}
