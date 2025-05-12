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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.io;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public abstract class AbstractTestReadFile extends AbstractPureTestWithCoreCompiled
{
    private static final String TEST_FILE_NAME = "/read_file_test/test.pure";
    private static final String TEST_TEXT_FILE_NAME = "/read_file_test/io/readFileTestText.txt";

    @After
    public void cleanRuntime()
    {
        runtime.delete(TEST_FILE_NAME);
        runtime.compile();
    }

    @Test
    public void testReadFileNoLineSeparator()
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function readTestFile():String[0..1]\n" +
                        "{\n" +
                        "  readFile('" + StringEscape.escape(TEST_TEXT_FILE_NAME) + "')\n" +
                        "}\n"
        );
        String expected = getTestText();
        String actual = executeAndGetStringResult("readTestFile():String[0..1]");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testReadFileNewLine()
    {
        testReadFileWithLineSeparator("\n");
    }

    @Test
    public void testReadFileCarriageReturn()
    {
        testReadFileWithLineSeparator("\r");
    }

    @Test
    public void testReadFileNewLineCarriageReturn()
    {
        testReadFileWithLineSeparator("\n\r");
    }

    @Test
    public void testReadFileCarriageReturnNewLine()
    {
        testReadFileWithLineSeparator("\r\n");
    }

    @Test
    public void testReadFileEmptyString()
    {
        testReadFileWithLineSeparator("");
    }

    private void testReadFileWithLineSeparator(String lineSeparator)
    {
        compileTestSource(
                TEST_FILE_NAME,
                "function readTestFile():String[0..1]\n" +
                        "{\n" +
                        "  readFile('" + StringEscape.escape(TEST_TEXT_FILE_NAME) + "', '" + StringEscape.escape(lineSeparator) + "')\n" +
                        "}\n"
        );
        String expected = getTestText().replaceAll("\\R", lineSeparator);
        String actual = executeAndGetStringResult("readTestFile():String[0..1]");
        Assert.assertEquals(expected, actual);
    }

    private String executeAndGetStringResult(String functionDesc)
    {
        CoreInstance func = runtime.getFunction(functionDesc);
        Assert.assertNotNull(functionDesc, func);
        CoreInstance result = functionExecution.start(func, Lists.immutable.empty());
        return PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values));
    }

    private String getTestText()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String resourceName = TEST_TEXT_FILE_NAME.substring(1);
        URL url = classLoader.getResource(resourceName);
        if (url == null)
        {
            throw new RuntimeException("Could not find resource: " + resourceName);
        }
        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))
        {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1)
            {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    protected static MutableRepositoryCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        repositories.add(new GenericCodeRepository("read_file_test", null, "platform", "core_functions_unclassified"));
        return new CompositeCodeStorage(new ClassLoaderCodeStorage(repositories));
    }
}
