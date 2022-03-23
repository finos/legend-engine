package org.finos.legend.engine.external.format.awspersistence.extension;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.external.format.awspersistence.model.AwsPersistenceGenerationConfig;
import org.finos.legend.engine.external.format.awspersistence.AwsPersistenceGenerationService;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.extension.GenerationMode;
import org.finos.legend.engine.external.shared.format.generations.description.FileGenerationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationConfigurationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationProperty;
import org.finos.legend.engine.external.shared.format.imports.description.ImportConfigurationDescription;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.pure.generated.*;

import java.util.ArrayList;
import java.util.List;

public class AwsPersistenceGenerationExtension implements GenerationExtension
{
    @Override
    public String getLabel()
    {
        return "AwsPersistence";
    }

    @Override
    public String getKey()
    {
        return "awsPersistence";
    }

    @Override
    public GenerationMode getMode()
    {
        return GenerationMode.Code;
    }

    @Override
    public GenerationConfigurationDescription getGenerationDescription()
    {
        return new GenerationConfigurationDescription()
        {
            @Override
            public String getLabel()
            {
                return AwsPersistenceGenerationExtension.this.getLabel();
            }

            @Override
            public String getKey()
            {
                return AwsPersistenceGenerationExtension.this.getKey();
            }

            @Override
            public List<GenerationProperty> getProperties(PureModel pureModel)
            {
                return FileGenerationDescription.extractGenerationProperties(core_persistence_external_format_awspersistence_integration.Root_meta_external_format_awspersistence_generation_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport()));
            }
        };
    }

    @Override
    public ImportConfigurationDescription getImportDescription()
    {
        return null;
    }

    @Override
    public Root_meta_pure_generation_metamodel_GenerationConfiguration defaultConfig(CompileContext context)
    {
        return core_persistence_external_format_awspersistence_integration.Root_meta_external_format_awspersistence_generation_defaultConfig__AwsPersistenceConfig_1_(context.pureModel.getExecutionSupport());
    }

    @Override
    public Object getService(ModelManager modelManager)
    {
        return new AwsPersistenceGenerationService(modelManager);
    }

    @Override
    public List<Root_meta_pure_generation_metamodel_GenerationOutput> generateFromElement(PackageableElement element, CompileContext compileContext)
    {
        if (element instanceof FileGenerationSpecification)
        {
            FileGenerationSpecification specification = (FileGenerationSpecification) element;
            AwsPersistenceGenerationConfig awspersistenceConfig = AwsPersistenceGenerationConfigFromFileGenerationSpecificationBuilder.build(specification);
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output = core_persistence_external_format_awspersistence_transformation.Root_meta_external_format_awspersistence_generation_generateAwsPersistenceFromPureWithScope_AwsPersistenceConfig_1__AwsPersistenceOutput_MANY_(awspersistenceConfig.process(compileContext.pureModel), compileContext.pureModel.getExecutionSupport());
            return new ArrayList<>(output.toList());
        }
        return null;
    }
}
