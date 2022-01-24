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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.external.shared.format.model.toModel.SchemaToModelConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.*;

import java.util.List;

/**
 * Defines an extension to be implemented to define an external format schema type.
 *
 * To implement a schema format it is also necessary to implement a metamodel that represents the
 * compiled version of the external schema.  This is a metamodel class graph expressed in PURE.  It
 * will represent the semantically checked and canonicalized data model (for example inclusions maybe
 * inlined and references reconciled).
 *
 */
public interface ExternalFormatExtension<
        Metamodel extends Root_meta_external_shared_format_metamodel_SchemaDetail,
        ModelGenConfig extends SchemaToModelConfiguration,
        SchemaGenConfig extends ModelToSchemaConfiguration>
{
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
    String getFormat();

    /**
     * Returns the content types (see https://www.iana.org/assignments/media-types/media-types.xhtml) supported
     * by this format.
     */
    List<String> getContentTypes();

    /**
     * Called to compile an external format schema set  of this format.  This should verify the schema contents
     * are lexically and semantically valid. Errors are reported by throwing an
     * {@link org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException}.
     */
    Metamodel compileSchema(ExternalSchemaCompileContext context);

    /**
     * Called as part of Binding compilation to verify that the model and schema match such that serialization and deserialization
     * can occur.  This provides early warning to the user as they change the schema and model of changes that would break data flows.
     * The compilation fails if the result returned is a meta_external_shared_format_binding_validation_FailedCorrelation.
     */
    Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context);

    /**
     * Called to convert a compiled schema detail back to its textual (schema-specific grammar) form
     */
    String metamodelToText(Metamodel schemaDetail);

    /**
     * Determines whether this extension supports model generation.
     */
    default boolean supportsModelGeneration()
    {
        return false;
    }

    /**
     * Provides the properties used to configure model generation.
     */
    default RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter>  getModelGenerationProperties(PureModel pureModel)
    {
        if (this.supportsModelGeneration())
        {
            throw new IllegalStateException("Must supply properties if supporting generation");
        }
        return Lists.mutable.empty();
    }

    /**
     * Called when an external schema of this format is used to generate a PURE model.
     */
    Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schema, ModelGenConfig config, PureModel pureModel);

    /**
     * Determines whether this extension supports schema generation.
     */
    default boolean supportsSchemaGeneration()
    {
        return false;
    }

    /**
     * Provides the properties used to configure model generation.
     */
    default RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter>  getSchemaGenerationProperties(PureModel pureModel)
    {
        if (this.supportsSchemaGeneration())
        {
            throw new IllegalStateException("Must supply properties if supporting generation");
        }
        return Lists.mutable.empty();
    }

    /**
     * Called when a PURE model is used to generate an external schema of this format.
     */
    Root_meta_external_shared_format_binding_Binding generateSchema(SchemaGenConfig config, PureModel pureModel);

    /**
     * Called to obtain the PackageableElement names (elementToPath) that are required to be registered for elementToPath.  This is usually the serializer
     * extension functions.
     */
    List<String> getRegisterablePackageableElementNames();
}
