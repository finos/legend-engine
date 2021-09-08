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

package org.finos.legend.engine.plan.execution.result.test.serialization;

import org.apache.commons.lang.SystemUtils;
import org.finos.legend.engine.plan.execution.result.serialization.TemporaryFile;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class TestTemporaryFile
{
    @Test
    public void testGetTemporaryPathForFile()
    {
        TemporaryFile tempFileWithoutEndingSlash = new TemporaryFile("/tmp");
        TemporaryFile tempFileWithEndingSlash = new TemporaryFile("/tmp/");
        if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX)
        {
            Assert.assertEquals(1, countPathSeparators(tempFileWithoutEndingSlash));
            Assert.assertEquals(1, countPathSeparators(tempFileWithEndingSlash));
        }
        else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX)
        {
            Assert.assertEquals(2, countPathSeparators(tempFileWithoutEndingSlash));
            Assert.assertEquals(2, countPathSeparators(tempFileWithEndingSlash));
        }
        else
        {
            String systemTempDirectory = System.getProperty("java.io.tmpdir");
            long expectedPathSeparatorCount = systemTempDirectory.chars().filter(ch -> ch == File.pathSeparatorChar).count();
            Assert.assertEquals(expectedPathSeparatorCount, countPathSeparators(tempFileWithoutEndingSlash));
            Assert.assertEquals(expectedPathSeparatorCount, countPathSeparators(tempFileWithEndingSlash));
        }
    }

    private long countPathSeparators(TemporaryFile temporaryFile)
    {
        return temporaryFile.getTemporaryPathForFile().chars().filter(ch -> ch == File.pathSeparatorChar).count();
    }
}
