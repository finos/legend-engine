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

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.test;

import org.finos.legend.engine.external.format.json.specifications.TestLoaderRoundTrip;
import org.junit.Test;

public class TestOpenAPIv3_0_3 extends TestLoaderRoundTrip
{
    @Test
    public void testString()
    {
        test("OpenAPIv3_0_3/string.json");
    }

    @Test
    public void testInteger()
    {
        test("OpenAPIv3_0_3/integer.json");
    }

    @Test
    public void testArray()
    {
        test("OpenAPIv3_0_3/array.json");
    }

    @Test
    public void testLegacy()
    {
        test("OpenAPIv3_0_3/legacyTests.json");
    }

    @Test
    public void testEmpty()
    {
        test("OpenAPIv3_0_3/empty.json");
    }

    @Test
    public void testObject()
    {
        test("OpenAPIv3_0_3/object.json");
    }

    @Test
    public void testXml()
    {
        test("OpenAPIv3_0_3/xml.json");
    }
}
