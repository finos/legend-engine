package org.finos.legend.engine.external.format.daml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.format.daml.toModel.DamlToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.haskell.metamodel.HaskellModule;
import org.finos.legend.engine.protocol.haskell.metamodel.Translator;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_language_haskell_binding_toPure_HaskellModuleContainer;
import org.finos.legend.pure.generated.Root_meta_external_language_haskell_binding_toPure_HaskellModuleContainer_Impl;

import java.util.Collections;
import java.util.List;

public class DamlFormatExtension implements ExternalFormatExtension<Root_meta_external_language_haskell_binding_toPure_HaskellModuleContainer, DamlToModelConfiguration, ModelToSchemaConfiguration>
{

    @Override
    public String getFormat() {
        return "DAML";
    }

    @Override
    public List<String> getContentTypes() {
        return Collections.emptyList();
    }

    @Override
    public Root_meta_external_language_haskell_binding_toPure_HaskellModuleContainer compileSchema(ExternalSchemaCompileContext context) {
        try
        {
            return new Root_meta_external_language_haskell_binding_toPure_HaskellModuleContainer_Impl("")
                    ._module(
                            new Translator().translate(
                                    new ObjectMapper().readValue(context.getContent(), HaskellModule.class),
                                    context.getPureModel()
                            )
                    );
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context) {
        return null;
    }

    @Override
    public String metamodelToText(Root_meta_external_language_haskell_binding_toPure_HaskellModuleContainer schemaDetail, PureModel pureModel) {
        return null;
    }

    @Override
    public boolean supportsModelGeneration()
    {
        return true;
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schema, DamlToModelConfiguration damlToModelConfiguration, PureModel pureModel) {
        return null;
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToSchemaConfiguration modelToSchemaConfiguration, PureModel pureModel) {
        return null;
    }

    @Override
    public List<String> getRegisterablePackageableElementNames() {
        return Collections.emptyList();
    }
}
