package org.finos.legend.engine.external.format.cpb.schema.generations;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_cpb_generation_CpbConfig;
import org.finos.legend.pure.generated.core_external_format_cpb_integration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_;

public class CpbGenerationConfig extends GenerationConfiguration
{
    /**
     * A sample checkbox for cloud persistent backends that does nothing.
    */
    public String comment;


    public Root_meta_external_format_cpb_generation_CpbConfig process(PureModel pureModel)
    {
        Root_meta_external_format_cpb_generation_CpbConfig cpbConfig = core_external_format_cpb_integration.Root_meta_external_format_cpb_generation_defaultConfig__CpbConfig_1_(pureModel.getExecutionSupport());
        List<PackageableElement> scopeElements = ListIterate.collect(this.generationScope(), e -> Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(e, pureModel.getExecutionSupport()));
        cpbConfig._scopeElements((RichIterable<? extends PackageableElement>) scopeElements);

        if (comment != null)
        {
            cpbConfig._comment(comment);
        }

        //PureMap _namespaceOverride = null;
        //cpbConfig._namespaceOverride(_namespaceOverride);

        return cpbConfig;
    }
}