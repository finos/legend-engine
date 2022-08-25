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
import java.util.List;
import jersey.repackaged.com.google.common.collect.Lists;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.ProtobufGenerationService;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationInput;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.core.profile.ProfileManager;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProtobufDescriptorGenerationServiceTest
{
    @InjectMocks
    private ProtobufDescriptorGenerationService protobufDescriptorGenerationService;
    @Mock
    private ProtobufGenerationService protobufGenerationService;
    @Mock
    private FileService fileService;
    @Mock
    private ProtobufCompilerService protobufCompilerService;

    @Test
    public void generateDescriptors()
    {
        List<GenerationOutput> generationOutputs = Lists.newArrayList(new GenerationOutput("content",
            "fileName", "format"));
        when(protobufGenerationService.generateProtobufOutput(any(), any())).thenReturn(generationOutputs);
        String unique_id = "unique_id";
        List<File> protoFiles = Lists.newArrayList(new File("path-to-proto-file"));
        when(fileService.writeToTempFolder(generationOutputs, unique_id)).thenReturn(protoFiles);
        when(protobufCompilerService.generateDiscriptorSet(protoFiles)).thenReturn(new File("descriptor"));
        byte[] bytes = {0, 1};
        when(fileService.getFileContentInBinary(new File("descriptor"))).thenReturn(bytes);

        assertThat(protobufDescriptorGenerationService
                .generateDescriptor(new ProtobufGenerationInput(), Mockito.mock(ProfileManager.class), unique_id),
            is(bytes));

        verify(fileService, times(1))
            .wipeOut(Lists.newArrayList(new File("path-to-proto-file"), new File("descriptor")));
    }

    @Test
    public void wipingOutEvenOnException()
    {
        List<GenerationOutput> generationOutputs = Lists.newArrayList(new GenerationOutput("content",
            "fileName", "format"));
        when(protobufGenerationService.generateProtobufOutput(any(), any())).thenReturn(generationOutputs);
        String unique_id = "unique_id";
        List<File> protoFiles = Lists.newArrayList(new File("path-to-proto-file"));
        when(fileService.writeToTempFolder(generationOutputs, unique_id)).thenReturn(protoFiles);
        when(protobufCompilerService.generateDiscriptorSet(protoFiles))
            .thenThrow(new RuntimeException("something's wrong"));

        assertThrows(RuntimeException.class, () -> protobufDescriptorGenerationService
            .generateDescriptor(new ProtobufGenerationInput(), Mockito.mock(ProfileManager.class), unique_id));

        verify(fileService, times(1)).wipeOut(protoFiles);
    }
}
