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

package org.finos.legend.engine.lsp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileChangeHandlerTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void toFileChanges_ignoresNonPureFiles()
    {
        UriMapper mapper = new UriMapper();
        FileChangeHandler handler = new FileChangeHandler(mapper);

        List<FileEvent> events = Arrays.asList(
                new FileEvent("file:///a/b/readme.md", FileChangeType.Changed),
                new FileEvent("file:///a/b/pom.xml", FileChangeType.Changed)
        );

        List<LegendPureSession.FileChange> changes = handler.toFileChanges(events);
        Assert.assertTrue(changes.isEmpty());
    }

    @Test
    public void toFileChanges_handlesDeletedFile()
    {
        UriMapper mapper = new UriMapper();
        FileChangeHandler handler = new FileChangeHandler(mapper);

        List<FileEvent> events = Arrays.asList(
                new FileEvent("file:///a/src/main/resources/core/model.pure", FileChangeType.Deleted)
        );

        List<LegendPureSession.FileChange> changes = handler.toFileChanges(events);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(LegendPureSession.FileChangeType.DELETE, changes.get(0).type);
        Assert.assertEquals("/core/model.pure", changes.get(0).sourceId);
        Assert.assertNull(changes.get(0).content);
    }

    @Test
    public void toFileChanges_readsCreatedFile() throws IOException
    {
        File pureFile = tempFolder.newFile("test.pure");
        try (FileWriter w = new FileWriter(pureFile))
        {
            w.write("Class my::Test {}");
        }
        String fileUri = pureFile.toURI().toString();

        UriMapper mapper = new UriMapper();
        FileChangeHandler handler = new FileChangeHandler(mapper);

        List<FileEvent> events = Arrays.asList(
                new FileEvent(fileUri, FileChangeType.Created)
        );

        List<LegendPureSession.FileChange> changes = handler.toFileChanges(events);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(LegendPureSession.FileChangeType.CREATE_OR_MODIFY, changes.get(0).type);
        Assert.assertEquals("Class my::Test {}", changes.get(0).content);
    }

    @Test
    public void toFileChanges_batchesMultipleEvents() throws IOException
    {
        File file1 = tempFolder.newFile("a.pure");
        try (FileWriter w = new FileWriter(file1))
        {
            w.write("Class a::A {}");
        }
        File file2 = tempFolder.newFile("b.pure");
        try (FileWriter w = new FileWriter(file2))
        {
            w.write("Class b::B {}");
        }

        UriMapper mapper = new UriMapper();
        FileChangeHandler handler = new FileChangeHandler(mapper);

        List<FileEvent> events = Arrays.asList(
                new FileEvent(file1.toURI().toString(), FileChangeType.Changed),
                new FileEvent(file2.toURI().toString(), FileChangeType.Changed),
                new FileEvent("file:///x/src/main/resources/core/old.pure", FileChangeType.Deleted)
        );

        List<LegendPureSession.FileChange> changes = handler.toFileChanges(events);
        Assert.assertEquals(3, changes.size());
    }
}
