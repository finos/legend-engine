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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.ProtobufGenerationService;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationInput;
import org.finos.legend.engine.external.shared.format.generations.GenerationOutput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.core.profile.CommonProfile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    public void generateDescriptors() throws IOException, InterruptedException
    {
        List<GenerationOutput> generationOutputs = Lists.fixedSize.of(new GenerationOutput("content",
            "fileName", "format"));
        when(protobufGenerationService.generateProtobufOutput(any(), any())).thenReturn(generationOutputs);
        List<File> protoFiles = Lists.fixedSize.of(new File("path-to-proto-file"));
        when(fileService.writeToDir(eq(generationOutputs), any())).thenReturn(protoFiles);
        when(protobufCompilerService.generateDescriptorSet(eq(protoFiles), any())).thenReturn(new File("descriptor"));
        byte[] bytes = {0, 1};
        when(fileService.getFileContentInBinary(new File("descriptor"))).thenReturn(bytes);

        assertThat(protobufDescriptorGenerationService
                .generateDescriptor(new ProtobufGenerationInput(), FastList.newListWith(new CommonProfile())),
            is(bytes));

        verify(fileService, times(1)).wipeOut(any());
    }

    @Test
    public void wipingOutEvenOnException() throws IOException, InterruptedException
    {
        List<GenerationOutput> generationOutputs = Lists.fixedSize.of(new GenerationOutput("content",
            "fileName", "format"));
        when(protobufGenerationService.generateProtobufOutput(any(), any())).thenReturn(generationOutputs);
        List<File> protoFiles = Lists.fixedSize.of(new File("path-to-proto-file"));
        when(fileService.writeToDir(eq(generationOutputs), any())).thenReturn(protoFiles);
        when(protobufCompilerService.generateDescriptorSet(eq(protoFiles), any()))
            .thenThrow(new RuntimeException("something's wrong"));

        assertThrows(RuntimeException.class, () -> protobufDescriptorGenerationService
            .generateDescriptor(new ProtobufGenerationInput(), FastList.newListWith(new CommonProfile())));

        verify(fileService, times(1)).wipeOut(any());
    }
}
