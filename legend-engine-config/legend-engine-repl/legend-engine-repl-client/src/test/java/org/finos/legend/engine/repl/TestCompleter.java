// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl;

import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.junit.Assert;
import org.junit.Test;

public class TestCompleter
{
    @Test
    public void testPrimitives()
    {
        Assert.assertEquals("[abs , abs(], [pow , pow(], [sqrt , sqrt(], [exp , exp(]", checkResultNoException(new Completer("").complete("1->")));
        Assert.assertEquals("[contains , contains(], [startsWith , startsWith(], [endsWith , endsWith(], [toLower , toLower(], [toUpper , toUpper(], [lpad , lpad(], [rpad , rpad(], [parseInteger , parseInteger(], [parseFloat , parseFloat(]", checkResultNoException(new Completer("").complete("'a'->")));
        Assert.assertEquals("[sum , sum(], [mean , mean(], [average , average(], [min , min(], [max , max(], [count , count(], [percentile , percentile(], [variancePopulation , variancePopulation(], [varianceSample , varianceSample(], [stdDevPopulation , stdDevPopulation(], [stdDevSample , stdDevSample(]", checkResultNoException(new Completer("").complete("[1,2]->")));
    }

    @Test
    public void testArrowOnType()
    {
        Assert.assertEquals("[project , project(]", checkResultNoException(new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->")));
        Assert.assertEquals("", checkResultNoException(new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->fu")));
    }

    //--------
    // Filter
    //--------
    @Test
    public void testDotInFilter()
    {
        Assert.assertEquals("[name , name], [other , other]", checkResultNoException(new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->filter(x|$x.")));
    }

    @Test
    public void testDotInFilterDeepClass()
    {
        Assert.assertEquals("[name , name], [other , other]", checkResultNoException(new Completer("Class x::A{name:String[1];other:Integer[1];}").complete("x::A.all()->filter(x|'x'+[1,2]->map(z|$z+$x.")));
    }

    private String checkResultNoException(CompletionResult completion)
    {
        Assert.assertNull(completion.getEngineException());
        return completion.getCompletion().makeString(", ");
    }
}
