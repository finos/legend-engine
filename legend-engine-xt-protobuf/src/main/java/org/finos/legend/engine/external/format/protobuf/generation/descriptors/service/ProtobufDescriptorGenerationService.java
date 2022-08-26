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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.ProtobufGenerationService;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationInput;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtobufDescriptorGenerationService
{
    private final ProtobufGenerationService protobufGenerationService;
    private final FileService fileService;
    private final ProtobufCompilerService protobufCompilerService;
    private static final Logger LOGGER = LoggerFactory.getLogger("Alloy Execution Server");

    public ProtobufDescriptorGenerationService(ProtobufGenerationService protobufGenerationService,
                                               FileService fileService,
                                               ProtobufCompilerService protobufCompilerService)
    {
        this.protobufGenerationService = protobufGenerationService;
        this.fileService = fileService;

        this.protobufCompilerService = protobufCompilerService;
    }

    public byte[] generateDescriptor(ProtobufGenerationInput generateProtobufInput,
                                     MutableList<CommonProfile> commonProfiles) throws IOException, InterruptedException
    {
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(commonProfiles, LoggingEventType.GENERATE_PROTOBUF_DESCRIPTOR_START).toString());


        List<GenerationOutput> generationOutputs =
            protobufGenerationService.generateProtobufOutput(generateProtobufInput, commonProfiles);

        Path tempDirectory = Files.createTempDirectory("protobuf-descriptor-generation");
        try
        {
            List<File> filesWritten = fileService.writeToDir(generationOutputs, tempDirectory);
            File descriptorSet = protobufCompilerService.generateDescriptorSet(filesWritten, tempDirectory);
            return fileService.getFileContentInBinary(descriptorSet);
        } finally
        {
            fileService.wipeOut(tempDirectory);
            LOGGER.info(new LogInfo(commonProfiles, LoggingEventType.GENERATE_PROTOBUF_DESCRIPTOR_STOP,
                (double) System.currentTimeMillis() - start).toString());
        }
    }
}
