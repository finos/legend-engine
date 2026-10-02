// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * How the module reads the JSON the Rust library returns, streamed straight into protocol objects: no native library
 * needed.
 */
public class TestRustResultReading
{
    @Test
    public void testClassInstanceOfAnAbsentExtensionIsUnsupported()
    {
        // An island whose engine extension is not on this classpath, as the expression's value.
        RustExpressionResult<ValueSpecification> result = RustExpressionResult.read(json("{'value':{'_type':'classInstance','type':'noSuchIsland','value':{}}}"), ValueSpecification.class, true);
        Assert.assertTrue(result.errorMessage, result.isUnsupported());
        Assert.assertEquals("no protocol extension for class instance type 'noSuchIsland'", result.errorMessage);
    }

    @Test
    public void testNestedClassInstanceOfAnAbsentExtensionIsUnsupported()
    {
        // The same deep in a lambda: the reader's failure comes through the enclosing objects' readers.
        RustExpressionResult<LambdaFunction> result = RustExpressionResult.read(json("{'value':{'_type':'lambda','parameters':[],'body':[{'_type':'func','function':'f','parameters':[{'_type':'classInstance','type':'noSuchIsland','value':{}}]}]}}"), LambdaFunction.class, true);
        Assert.assertTrue(result.errorMessage, result.isUnsupported());
        Assert.assertEquals("no protocol extension for class instance type 'noSuchIsland'", result.errorMessage);
    }

    @Test
    public void testElementOfAnAbsentExtensionIsUnsupported()
    {
        RustModelResult result = RustModelResult.read(json("{'elements':[{'_type':'noSuchElement','package':'a','name':'b'}]}"), true);
        Assert.assertTrue(result.errorMessage, result.isUnsupported());
        Assert.assertEquals("no protocol extension for type 'noSuchElement'", result.errorMessage);
    }

    @Test
    public void testErrorKeepsItsPositionWithoutSourceInformation()
    {
        RustSectionResult result = RustSectionResult.read(json("{'error':{'kind':'parser','message':'unexpected token','sourceInformation':{'sourceId':'s','startLine':1,'startColumn':2,'endLine':3,'endColumn':4}}}"), false);
        Assert.assertTrue(result.isError());
        Assert.assertEquals(RustSectionResult.PARSER_ERROR, result.errorKind);
        Assert.assertEquals("unexpected token", result.errorMessage);
        Assert.assertEquals(1, result.errorSourceInformation.startLine);
        Assert.assertEquals(4, result.errorSourceInformation.endColumn);
    }

    @Test
    public void testImportsAndUnknownFields()
    {
        RustSectionResult result = RustSectionResult.read(json("{'later':{'a':[1,{'b':2}]},'elements':[],'imports':['a::b','c']}"), true);
        Assert.assertFalse(result.errorMessage, result.isError());
        Assert.assertEquals(Arrays.asList("a::b", "c"), result.imports);
        Assert.assertTrue(result.elements.isEmpty());
        Assert.assertNull(RustSectionResult.read(json("{'elements':[]}"), true).imports);
    }

    @Test
    public void testProtocolDivergenceIsAnInternalError()
    {
        RustModelResult result = RustModelResult.read(json("{'elements':[{'_type':'class','package':'a','name':'B','properties':'not a list'}]}"), true);
        Assert.assertEquals(RustSectionResult.INTERNAL_ERROR, result.errorKind);
        Assert.assertTrue(result.errorMessage, result.errorMessage.startsWith("Rust protocol JSON does not match the engine protocol: "));
        Assert.assertEquals(RustSectionResult.INTERNAL_ERROR, RustModelResult.read(json("['not an object']"), true).errorKind);
        Assert.assertEquals(RustSectionResult.INTERNAL_ERROR, RustModelResult.read(null, true).errorKind);
    }

    private static byte[] json(String singleQuoted)
    {
        return singleQuoted.replace('\'', '"').getBytes(StandardCharsets.UTF_8);
    }
}
