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

package org.finos.legend.engine.plan.execution.result.serialization;

import org.apache.commons.lang.SystemUtils;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class TemporaryFile implements Closeable
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final String localDevTempPath = "Z:/";
    private String fileName;
    public Path path;
    private final String tempPath;

    public TemporaryFile(String tempPath)
    {
        this.tempPath = tempPath;
        this.fileName = randomString(8) + ".txt"; //we probably want a better way to do this
        this.path = Paths.get(this.getTemporaryPathForFile());
    }

    public String getTemporaryPathForFile()
    {
        Path parentPath = Paths.get(SystemUtils.IS_OS_LINUX ? this.tempPath : localDevTempPath);
        return parentPath.resolve(fileName).toString();
    }

    public void writeFile(Serializer source) throws Exception
    {

        LOGGER.info(new LogInfo(null, LoggingEventType.TEMP_FILE_CREATED, fileName).toString());
        try (OutputStream outputStream = new FileOutputStream(path.toString()))
        {
            source.stream(outputStream);

            outputStream.close();
        }
    }

    private String randomString(int len)
    {
        final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
        {
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }

    @Override
    public void close()
    {
        try
        {
            Files.deleteIfExists(path);
            LOGGER.info(new LogInfo(null, LoggingEventType.TEMP_FILE_DELETED, fileName).toString());
        }
        catch (Exception e)
        {
            LOGGER.error(new LogInfo(null, LoggingEventType.TEMP_FILE_DELETE_ERROR, new ErrorResult(1, e).getMessage()).toString());

        }

    }
}
