package org.finos.legend.engine.external.format.protobuf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.protobuf.toModel.ProtobufToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Translator;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_binding_toPure_introspection_ProtoFileContainer;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_binding_toPure_introspection_ProtoFileContainer_Impl;

import java.util.List;

public class ProtobufFormatExtension implements ExternalFormatExtension<Root_meta_external_format_protobuf_binding_toPure_introspection_ProtoFileContainer, ProtobufToModelConfiguration, ModelToSchemaConfiguration>
{
    private static final String TYPE = "Protobuf";

    //There is no standard content type for protobuf yet
    private static final String CONTENT_TYPE_1 = "application/protobuf";
    private static final String CONTENT_TYPE_2 = " application/vnd.google.protobuf";

    @Override
    public String getFormat()
    {
        return TYPE;
    }

    @Override
    public List<String> getContentTypes()
    {
        return Lists.fixedSize.of(CONTENT_TYPE_1, CONTENT_TYPE_2);
    }

    @Override
    public boolean supportsModelGeneration()
    {
        return true;
    }

    @Override
    public Root_meta_external_format_protobuf_binding_toPure_introspection_ProtoFileContainer compileSchema(ExternalSchemaCompileContext context)
    {
        try
        {
            return new Root_meta_external_format_protobuf_binding_toPure_introspection_ProtoFileContainer_Impl("")
                    ._protoFile(
                            new Translator().translate(
                                    new ObjectMapper().readValue(context.getContent(), ProtoFile.class),
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
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        return null;
    }

    @Override
    public String metamodelToText(Root_meta_external_format_protobuf_binding_toPure_introspection_ProtoFileContainer schemaDetail)
    {
        return null;
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schema, ProtobufToModelConfiguration protobufToModelConfiguration, PureModel pureModel)
    {
        return null;
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToSchemaConfiguration modelToSchemaConfiguration, PureModel pureModel)
    {
        return null;
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        return Lists.mutable.empty();
    }
}
