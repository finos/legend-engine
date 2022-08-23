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

package org.finos.legend.engine.external.shared.format.model;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaDetail;

import java.util.List;

/**
 * Defines an extension to be implemented to define an external format schema type.
 * <p>
 * To implement a schema format it is necessary to implement a metamodel that represents the
 * compiled version of the external schema.  This is a metamodel class graph expressed in PURE.  It
 * will represent the semantically checked and canonicalized data model (for example inclusions maybe
 * inlined and references reconciled).
 */
public interface ExternalFormatExtension<Metamodel extends Root_meta_external_shared_format_metamodel_SchemaDetail>
{
    /**
     * Returns the contract for this external format written in PURE
     */
    Root_meta_external_shared_format_ExternalFormatContract<Metamodel> getExternalFormatContract();

    /**
     * Called to compile an external format schema set of this format.  This should verify the schema contents
     * are lexically and semantically valid. Errors are reported by throwing an
     * {@link org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException}.
     */
    Metamodel compileSchema(ExternalSchemaCompileContext context);

    /**
     * Called to convert a compiled schema detail back to its textual (schema-specific grammar) form
     */
    String metamodelToText(Metamodel schemaDetail, PureModel pureModel);

    /**
     * Returns the format of external schema this extension represents.  This will be the name used
     * to express the format as part of the schema (e.g. FORMAT_NAME):
     *
     * <pre>
     *     ###ExternalFormat
     *     Schema my::Schema
     *     {
     *         format: FORMAT_NAME
     *     }
     *     ~~START~~
     *     schema_content
     *     ~~END~~
     * </pre>
     */
    default String getFormat()
    {
        return getExternalFormatContract()._id();
    }

    /**
     * Returns the content types (see https://www.iana.org/assignments/media-types/media-types.xhtml) supported
     * by this format.
     */
    default List<String> getContentTypes()
    {
        return FastList.newList(getExternalFormatContract()._contentTypes());
    }

    /**
     * Called as part of Binding compilation to verify that the model and schema match such that serialization and deserialization
     * can occur.  This provides early warning to the user as they change the schema and model of changes that would break data flows.
     * The compilation fails if the result returned is a meta_external_shared_format_binding_validation_FailedBindingDetail.
     */
    default Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        Root_meta_external_shared_format_ExternalFormatContract<Metamodel> externalFormatContract = getExternalFormatContract();

        if (externalFormatContract._externalFormatBindingValidator() == null)
        {
            throw new EngineException("Format " + getFormat() + " does not support binding validation", SourceInformation.getUnknownSourceInformation(), EngineErrorType.COMPILATION);
        }

        return externalFormatContract.validateBinding(binding, context.getExecutionSupport());
    }
}
