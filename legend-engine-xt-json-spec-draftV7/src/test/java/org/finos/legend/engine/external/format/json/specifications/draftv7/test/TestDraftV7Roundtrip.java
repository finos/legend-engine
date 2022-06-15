/*
 *  Copyright 2022 Goldman Sachs
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.finos.legend.engine.external.format.json.specifications.draftv7.test;

import org.finos.legend.engine.external.format.json.specifications.TestLoaderRoundTrip;
import org.junit.Test;

public class TestDraftV7Roundtrip extends TestLoaderRoundTrip
{
    @Test
    public void testString()
    {
        test("DraftV7/string.json");
    }

    @Test
    public void testInteger()
    {
        test("DraftV7/integer.json");
    }

    @Test
    public void testArray()
    {
        test("DraftV7/array.json");
    }

    @Test
    public void testFragment()
    {
        test("DraftV7/fragment.json");
    }

    @Test
    public void testMultiType()
    {
        test("DraftV7/multiType.json");
    }

    @Test
    public void testObject()
    {
        test("DraftV7/object.json");
    }

    @Test
    public void testIfThenElse()
    {
        test("DraftV7/if-then-else.json");
    }
}
