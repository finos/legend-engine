// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.protobuf.generation.descriptors.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;

public class FileService
{
    public static final String PROTO = ".proto";

    public List<File> writeToTempFolder(List<GenerationOutput> generationOutputs, String uniqueId) throws IOException
    {
        ArrayList<File> files = new ArrayList<>();
        for (GenerationOutput generationOutput : generationOutputs)
        {
            Path tempFile = Files.createTempFile(generationOutput.fileName + uniqueId, PROTO);
            files.add(tempFile.toFile());
        }
        return files;
    }

    public byte[] getFileContentInBinary(File file) throws IOException
    {
        return FileUtils.readFileToByteArray(file);
    }

    public void wipeOut(List<File> files)
    {
        files.forEach(FileUtils::deleteQuietly);
    }
}
