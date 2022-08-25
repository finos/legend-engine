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
import java.util.List;
import jersey.repackaged.com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;


public class FileServiceTest
{
    private FileService fileService = new FileService();

    @Test
    public void filesHasProperNames() throws IOException
    {
        List<File> files = fileService
            .writeToTempFolder(Lists.newArrayList(new GenerationOutput("content", "filename", "format")));
        String tempDir = FileUtils.getTempDirectory().getPath();
        assertThat(files.get(0).getParent(), is(tempDir));
        assertThat(files.get(0).getName(), containsString("filename-"));
        assertThat(files.get(0).getName(), containsString(".proto"));
    }
}
