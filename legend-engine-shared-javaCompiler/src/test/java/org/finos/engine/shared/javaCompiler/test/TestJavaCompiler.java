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

package org.finos.engine.shared.javaCompiler.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.engine.shared.javaCompiler.StringJavaSource;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class TestJavaCompiler
{
    private final String code = "package engine.generated;" +
            "public class Example" +
            "{" +
            "    public static String execute()\n" +
            "    {\n" +
            "       return \"ok\";" +
            "    }\n" +
            "}";

    @Test
    public void testSourceCompiler() throws Exception
    {
        EngineJavaCompiler c = new EngineJavaCompiler();
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        Assert.assertEquals("ok", execute(c));
    }

    @Test
    public void testSaveAndLoadCompiler() throws Exception
    {
        EngineJavaCompiler c = new EngineJavaCompiler();
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        MutableMap<String, String> save = c.save();

        EngineJavaCompiler other = new EngineJavaCompiler();
        other.load(save);
        Assert.assertEquals("ok", execute(other));
    }

    @Test
    public void testSaveJSONSerialization() throws Exception
    {
        EngineJavaCompiler c = new EngineJavaCompiler();
        c.compile(Lists.mutable.with(StringJavaSource.newStringJavaSource("engine.generated", "Example", code)));
        MutableMap<String, String> save = c.save();
        Assert.assertTrue(new ObjectMapper().writeValueAsString(save).startsWith("{\"engine.generated.Example\":\""));
    }

    private String execute(EngineJavaCompiler c) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        Class<?> cl = c.getClassLoader().loadClass("engine.generated.Example");
        return (String) cl.getMethod("execute").invoke(null);
    }
}
