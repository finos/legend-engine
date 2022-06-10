package org.finos.legend.pure.code.core;

import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;

public class CoreIdeMetadataCodeRepositoryProvider implements CodeRepositoryProvider
{
    @Override
    public CodeRepository repository()
    {
        return GenericCodeRepository.build("core_ide_metadata.definition.json");
    }
}