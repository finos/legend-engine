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

package org.finos.legend.engine.external.format.json.model;

import java.util.Map;

public class JSONSchemaDiscriminator
{
    public String propertyName;
    public Map<String, String> mapping;

    JSONSchemaDiscriminator(String propertyName, Map<String, String> mapping)
    {
        this.propertyName = propertyName;
        this.mapping = mapping;
    }

    public static class JSONSchemaDiscriminatorBuilder
    {
        private String propertyName;
        private Map<String, String> mapping;

        public JSONSchemaDiscriminator.JSONSchemaDiscriminatorBuilder propertyName(String propertyName)
        {
            this.propertyName = propertyName;
            return this;
        }

        public JSONSchemaDiscriminator.JSONSchemaDiscriminatorBuilder mapping(Map<String, String> mapping)
        {
            this.mapping = mapping;
            return this;
        }

        public JSONSchemaDiscriminator build()
        {
            String propertyNameValue = this.propertyName;
            Map<String, String> mappingValue = this.mapping;
            return new JSONSchemaDiscriminator(propertyNameValue, mappingValue);
        }
    }

    public static JSONSchemaDiscriminator.JSONSchemaDiscriminatorBuilder builder()
    {
        return new JSONSchemaDiscriminator.JSONSchemaDiscriminatorBuilder();
    }
}
