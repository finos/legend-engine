package org.finos.legend.engine.external.format.cpb.extension;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.engine.external.format.cpb.schema.generations.CpbGenerationConfig;
import org.finos.legend.engine.external.format.cpb.schema.generations.CpbGenerationService;
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

public class CpbGenerationExtension implements GenerationExtension
{
    @Override
    public String getLabel()
    {
        return "Cpb";
    }

    @Override
    public String getKey()
    {
        return "cpb";
    }

    @Override
    public GenerationMode getMode()
    {
        return GenerationMode.Schema;
    }

    @Override
    public GenerationConfigurationDescription getGenerationDescription()
    {
        return new GenerationConfigurationDescription()
        {
            @Override
            public String getLabel()
            {
                return CpbGenerationExtension.this.getLabel();
            }

            @Override
            public String getKey()
            {
                return CpbGenerationExtension.this.getKey();
            }

            @Override
            public List<GenerationProperty> getProperties(PureModel pureModel)
            {
                return FileGenerationDescription.extractGenerationProperties(core_external_format_cpb_integration.Root_meta_external_format_cpb_generation_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport()));
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
        return core_external_format_cpb_integration.Root_meta_external_format_cpb_generation_defaultConfig__CpbConfig_1_(context.pureModel.getExecutionSupport());
    }

    @Override
    public Object getService(ModelManager modelManager)
    {
        return new CpbGenerationService(modelManager);
    }

    @Override
    public List<Root_meta_pure_generation_metamodel_GenerationOutput> generateFromElement(PackageableElement element, CompileContext compileContext)
    {
        if (element instanceof FileGenerationSpecification)
        {
            FileGenerationSpecification specification = (FileGenerationSpecification) element;
            CpbGenerationConfig cpbConfig = CpbGenerationConfigFromFileGenerationSpecificationBuilder.build(specification);
            RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationOutput> output = core_external_format_cpb_transformation_cpbGenerator.Root_meta_external_format_cpb_generation_generateCpbFromPureWithScope_CpbConfig_1__CpbOutput_MANY_(cpbConfig.process(compileContext.pureModel), compileContext.pureModel.getExecutionSupport());
            return new ArrayList<>(output.toList());
        }
        return null;
    }
}
