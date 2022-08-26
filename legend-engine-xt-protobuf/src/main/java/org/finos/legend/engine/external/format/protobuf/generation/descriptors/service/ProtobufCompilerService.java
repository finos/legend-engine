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

import com.github.os72.protocjar.Protoc;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ProtobufCompilerService
{
    public File generateDescriptorSet(List<File> protoFiles, Path dir) throws IOException, InterruptedException
    {
        Path descriptorSet = Files.createFile(FileSystems.getDefault().getPath(dir.toString(), "descriptor-set.pb"));
        List<String> args = protoFiles.stream()
            .map(File::getName)
            .collect(Collectors.toList());
        args.add("--descriptor_set_out=" + descriptorSet.toAbsolutePath());
        args.add("--proto_path=" + dir);
        Protoc.runProtoc(args.toArray(new String[]{}));
        return descriptorSet.toFile();
    }
}
