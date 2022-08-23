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

package org.finos.legend.engine.external.shared.format.model.transformation.toModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.Generator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperExternalFormat;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class SchemaToModelGenerator extends Generator
{
    private final String pureVersion;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final Map<String, ExternalFormatExtension<?>> schemaExtensions;
    private final Method packageableElementTransformer;

    public SchemaToModelGenerator(PureModel pureModel, String pureVersion, Map<String, ExternalFormatExtension<?>> schemaExtensions)
    {
        super(pureModel);
        this.pureVersion = pureVersion;
        this.schemaExtensions = schemaExtensions;

        try
        {
            Class<?> cls = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + pureVersion + "_transfers_metamodel");
            this.packageableElementTransformer = cls.getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformPackageableElement_PackageableElement_1__Extension_MANY__PackageableElement_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public PureModelContextData generate(SchemaToModelConfiguration configuration, String sourceSchemaSet, boolean generateBinding, String targetBindingPath)
    {
        if (generateBinding)
        {
            if (targetBindingPath == null || !targetBindingPath.matches("[A-Za-z0-9_]+(::[A-Za-z0-9_]+)+"))
            {
                throw new IllegalArgumentException("Invalid path provided for target binding");
            }
        }

        Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = HelperExternalFormat.getSchemaSet(sourceSchemaSet, pureModel.getContext());

        ExternalFormatExtension<?> schemaExtension = schemaExtensions.get(schemaSet._format());
        if (schemaExtension == null)
        {
            throw new IllegalArgumentException("Unknown schema format: " + schemaSet._format());
        }
        if (!(schemaExtension instanceof ExternalFormatModelGenerationExtension))
        {
            throw new UnsupportedOperationException("Model generation not supported for " + schemaExtension.getFormat());
        }

        List<? extends PackageableElement> packageableElements = ((ExternalFormatModelGenerationExtension) schemaExtension).generateModel(schemaSet, configuration, pureModel);

        PureModelContextData.Builder builder = PureModelContextData.newBuilder().withSerializer(new Protocol("pure", pureVersion));
        if (generateBinding)
        {
            ModelUnit modelUnit = new ModelUnit();
            modelUnit.packageableElementIncludes = ListIterate.collect(packageableElements, this::elementToPath);

            builder.addElement(generateBinding(targetBindingPath, sourceSchemaSet, configuration.sourceSchemaId, schemaExtension.getContentTypes().get(0), modelUnit));
        }

        return toPureModelContextData(packageableElements, builder);
    }

    private PureModelContextData toPureModelContextData(List<? extends PackageableElement> packageableElements, PureModelContextData.Builder builder)
    {
        LazyIterate.collect(packageableElements, element ->
        {
            try
            {
                Object result = packageableElementTransformer.invoke(null, element, Lists.mutable.empty(), pureModel.getExecutionSupport());
                String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(result, pureModel.getExecutionSupport());
                return objectMapper.readValue(json, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement.class);
            }
            catch (IOException | ReflectiveOperationException e)
            {
                throw new RuntimeException("Unable to transform - " + elementToPath(element), e);
            }
        }).forEach(builder::addElement);

        return builder.build();
    }
}

