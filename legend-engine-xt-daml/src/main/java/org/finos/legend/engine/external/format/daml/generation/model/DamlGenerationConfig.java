package org.finos.legend.engine.external.format.daml.generation.model;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.generated.Root_meta_external_language_daml_generation_DamlConfig;
import org.finos.legend.pure.generated.core_external_language_daml_integration;

import java.util.List;

public class DamlGenerationConfig extends GenerationConfiguration {

    public Root_meta_external_language_daml_generation_DamlConfig transformToPure(PureModel pureModel)
    {
        Root_meta_external_language_daml_generation_DamlConfig config = core_external_language_daml_integration.Root_meta_external_language_daml_generation_defaultConfig__DamlConfig_1_(pureModel.getExecutionSupport());
        List<PackageableElement> scopeElements = ListIterate.collect(this.generationScope(), e -> core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(e, pureModel.getExecutionSupport()));
        return config._scopeElements((RichIterable<? extends PackageableElement>) scopeElements);
    }
}
