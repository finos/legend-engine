package org.finos.legend.engine.generation.extension;

import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.extension.GenerationMode;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationConfigurationDescription;
import org.finos.legend.engine.external.shared.format.imports.description.ImportConfigurationDescription;
import org.finos.legend.engine.generation.service.OpenApiGenerationService;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;

import java.util.List;

public class OpenApiGenerationExtension implements GenerationExtension {
    @Override
    public String getLabel() {
        return "OpenAPI";
    }

    @Override
    public String getKey() {
        return "openapi_spec";
    }

    @Override
    public GenerationMode getMode() {
        return GenerationMode.Code;
    }

    @Override
    public GenerationConfigurationDescription getGenerationDescription() {
        return null;
    }

    @Override
    public ImportConfigurationDescription getImportDescription() {
        return null;
    }

    @Override
    public Root_meta_pure_generation_metamodel_GenerationConfiguration defaultConfig(CompileContext context) {
        return null;
    }

    @Override
    public Object getService(ModelManager modelManager) {
        return new OpenApiGenerationService(modelManager);
    }

    @Override
    public List<Root_meta_pure_generation_metamodel_GenerationOutput> generateFromElement(PackageableElement element, CompileContext compileContext) {
        return null;
    }
}
