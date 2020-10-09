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

package org.finos.legend.engine.plan.platform;

import org.finos.legend.engine.plan.platform.java.JavaSourceHelper;
import org.junit.Assert;
import org.junit.Test;

public class TestJavaSourceHelper
{
    @Test
    public void testToValidJavaIdentifier()
    {
        Assert.assertEquals("_", JavaSourceHelper.toValidJavaIdentifier(""));
        Assert.assertEquals("$", JavaSourceHelper.toValidJavaIdentifier("", '$'));
        Assert.assertEquals("_assert", JavaSourceHelper.toValidJavaIdentifier("assert"));
        Assert.assertEquals("$assert", JavaSourceHelper.toValidJavaIdentifier("assert", '$'));
        Assert.assertEquals("assert5", JavaSourceHelper.toValidJavaIdentifier("assert", '5'));
        Assert.assertEquals("a_b_c", JavaSourceHelper.toValidJavaIdentifier("a.b.c"));
        Assert.assertEquals("a_b_c", JavaSourceHelper.toValidJavaIdentifier("a-b-c"));
        Assert.assertEquals("_3abc", JavaSourceHelper.toValidJavaIdentifier("3abc"));
        Assert.assertEquals("a__b__c", JavaSourceHelper.toValidJavaIdentifier("a..b..c", false));
        Assert.assertEquals("abc$d$e", JavaSourceHelper.toValidJavaIdentifier("abc..d.#.e", '$', true));
        Assert.assertEquals("abc_def_ghi", JavaSourceHelper.toValidJavaIdentifier("abc::def::ghi", true));
        Assert.assertEquals("_3abc", JavaSourceHelper.toValidJavaIdentifier("3abc"));
        Assert.assertEquals("_3", JavaSourceHelper.toValidJavaIdentifier("3"));
        Assert.assertEquals("$", JavaSourceHelper.toValidJavaIdentifier("%", '$'));
        for (String validIdentifier : new String[]{"validIdentifier", "a", "bcd", "e05___"})
        {
            Assert.assertSame(validIdentifier, validIdentifier, JavaSourceHelper.toValidJavaIdentifier(validIdentifier));
            Assert.assertSame(validIdentifier, validIdentifier, JavaSourceHelper.toValidJavaIdentifier(validIdentifier, '$'));
            Assert.assertSame(validIdentifier, validIdentifier, JavaSourceHelper.toValidJavaIdentifier(validIdentifier, '_', true));
        }
    }
}
