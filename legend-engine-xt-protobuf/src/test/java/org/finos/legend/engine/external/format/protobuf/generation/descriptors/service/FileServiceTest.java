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
import java.util.List;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class FileServiceTest
{
    private FileService fileService = new FileService();

    @Test
    public void filesHasProperNames() throws IOException
    {
        Path tempDirectory = Files.createTempDirectory("test");
        List<File> files = fileService
            .writeToDir(Lists.fixedSize.of(new GenerationOutput("content", "filename.proto", "format")),
                tempDirectory);
        assertThat(files.get(0).getParent(), is(tempDirectory.toString()));
        assertThat(files.get(0).getName(), is("filename.proto"));
    }
}
