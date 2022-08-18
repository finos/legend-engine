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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_FailedBindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ModelUnit;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ModelUnit_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.Collections;
import java.util.Map;

public class BindingCompiler
{
    private final Map<String, ExternalFormatExtension> externalFormatExtensions;
    private final MutableMap<String, Root_meta_external_shared_format_binding_Binding> bindingIndex = Maps.mutable.empty();
    private final MutableMap<String, ExternalFormatSchemaSet> srcSchemaIndex = Maps.mutable.empty();

    public BindingCompiler(Map<String, ExternalFormatExtension> externalFormatExtensions)
    {
        this.externalFormatExtensions = externalFormatExtensions;
    }

    public Processor<Binding> getProcessor()
    {
        return Processor.newProcessor(Binding.class, Collections.singletonList(ExternalFormatSchemaSet.class), this::firstPass, this::secondPass, this::thirdPass, this::fourthPass);
    }

    public Root_meta_external_shared_format_binding_Binding getCompiledBinding(String fullPath)
    {
        return bindingIndex.get(fullPath);
    }

    // First pass - create and index schemas
    private PackageableElement firstPass(Binding srcSchemaOp, CompileContext context)
    {
        Root_meta_external_shared_format_binding_Binding binding = new Root_meta_external_shared_format_binding_Binding_Impl(srcSchemaOp.name)
                ._name(srcSchemaOp.name)
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::external::shared::format::binding::Binding")));

        String path = context.pureModel.buildPackageString(srcSchemaOp._package, srcSchemaOp.name);
        this.bindingIndex.put(path, binding);
        context.pureModel.storesIndex.put(path, binding);
        return binding;
    }

    // Second pass - resolve schemas and model elements
    private void secondPass(Binding srcBinding, CompileContext context)
    {
        String path = context.pureModel.buildPackageString(srcBinding._package, srcBinding.name);
        Root_meta_external_shared_format_binding_Binding compiled = bindingIndex.get(path);

        compiled._contentType(srcBinding.contentType);

        if (srcBinding.schemaSet != null)
        {
            Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = HelperExternalFormat.getSchemaSet(srcBinding.schemaSet, srcBinding.sourceInformation, context);
            if (srcBinding.schemaId != null && schemaSet._schemas().noneSatisfy(s -> srcBinding.schemaId.equals(s._id())))
            {
                throw new EngineException("ID '" + srcBinding.schemaId + "' does not exist in SchemaSet '" + srcBinding.schemaSet + "'", srcBinding.sourceInformation, EngineErrorType.COMPILATION);
            }
            compiled._schemaSet(schemaSet)
                    ._schemaId(srcBinding.schemaId);
        }

        Root_meta_pure_model_unit_ModelUnit modelUnit = new Root_meta_pure_model_unit_ModelUnit_Impl("", null, context.pureModel.getClass("meta::pure::model::unit::ModelUnit"))
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::pure::model::unit::ModelUnit")))
                ._packageableElementIncludes(ListIterate.collect(srcBinding.modelUnit.packageableElementIncludes, pe -> context.pureModel.getPackageableElement(pe, srcBinding.sourceInformation)))
                ._packageableElementExcludes(ListIterate.collect(srcBinding.modelUnit.packageableElementExcludes, pe -> context.pureModel.getPackageableElement(pe, srcBinding.sourceInformation)));

        compiled._modelUnit(modelUnit);
    }

    // Third pass - validation
    private void thirdPass(Binding srcBinding, CompileContext context)
    {
        String path = context.pureModel.buildPackageString(srcBinding._package, srcBinding.name);
        Root_meta_external_shared_format_binding_Binding compiled = bindingIndex.get(path);

        if (compiled._schemaId() != null && compiled._schemaSet()._schemas().noneSatisfy(s -> compiled._schemaId().equals(s._id())))
        {
            throw new EngineException("ID '" + compiled._schemaId() + "' does not exist in SchemaSet '" + srcBinding.schemaSet + "'", srcBinding.sourceInformation, EngineErrorType.COMPILATION);
        }

        ExternalFormatExtension schemaExtension = getExtension(compiled, srcBinding);
        if (compiled._schemaSet() != null && !schemaExtension.getFormat().equals(compiled._schemaSet()._format()))
        {
            throw new EngineException("Content type and SchemaSet format do not match", srcBinding.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    // Fourth pass - ensure correlation using the extension
    private void fourthPass(Binding srcBinding, CompileContext context)
    {
        String path = context.pureModel.buildPackageString(srcBinding._package, srcBinding.name);
        Root_meta_external_shared_format_binding_Binding compiled = bindingIndex.get(path);

        ExternalFormatExtension schemaExtension = getExtension(compiled, srcBinding);
        Root_meta_external_shared_format_binding_validation_BindingDetail bindingDetail = schemaExtension.bindDetails(compiled, context);
        if (bindingDetail instanceof Root_meta_external_shared_format_binding_validation_FailedBindingDetail)
        {
            Root_meta_external_shared_format_binding_validation_FailedBindingDetail failed = (Root_meta_external_shared_format_binding_validation_FailedBindingDetail) bindingDetail;
            throw new EngineException("Model and schema are mismatched:\n" + failed._errorMessages().makeString("\n"), srcBinding.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

    private ExternalFormatExtension getExtension(Root_meta_external_shared_format_binding_Binding binding, Binding srcBinding)
    {
        return externalFormatExtensions.values().stream()
                .filter(ext -> ext.getContentTypes().contains(binding._contentType()))
                .findFirst()
                .orElseThrow(() -> new EngineException("Unknown contentType '" + binding._contentType() + "'", srcBinding.sourceInformation, EngineErrorType.COMPILATION));
    }

}
