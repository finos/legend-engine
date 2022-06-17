//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.fromModel;

import org.finos.legend.engine.external.format.json.JsonExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.fromModel.ModelToSchemaConfiguration;

public class ModelToJsonSchemaConfiguration extends ModelToSchemaConfiguration
{
    // URL to the specification which your schema should be compatible with
    public String specificationUrl;

    //Generate JSON schema constraints from class constraints, unsupported functions will not generate
    public Boolean useConstraints;

    // Include related types in a definitions element on the schema, ignored if generating openAPI. if true , dependencies generate to definitions elements
    public Boolean includeAllRelatedTypes;

    // If true, an additional schema will be generated that can be used as a collection of subTypes
    public Boolean generateAnyOfSubType;

    // If true, constraint functions generate as schema files, if false constraint function content is in-lined with the parent schema
    public Boolean generateConstraintFunctionSchemas;

    // Generate a components or definitions collection, if true, only one file will generate with a collection of related elements in scope for execution and there will be no root element in the schema
    public Boolean createSchemaCollection;

    // Generate milestoned properties on schemas. If true - the auto-gen milestoned properties will be included on generated schemas
    public Boolean generateMilestoneProperties;

    public ModelToJsonSchemaConfiguration(String specificationUrl)
    {
        super(JsonExternalFormatExtension.TYPE);
        this.specificationUrl = specificationUrl;
    }

    public ModelToJsonSchemaConfiguration()
    {
        super(JsonExternalFormatExtension.TYPE);
    }
}
