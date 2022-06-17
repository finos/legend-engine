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

package org.finos.legend.engine.external.format.json.specifications.openapiv3_0_3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonInclude(Include.NON_NULL)
@JsonDeserialize(
        builder = OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder.class
)
public class OpenAPIv3_0_3XML
{
    public String name;
    public String namespace;
    public String prefix;
    public Boolean attribute;
    public Boolean wrapped;

    protected OpenAPIv3_0_3XML(OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder b)
    {
        if (b.nameSet)
        {
            this.name = b.nameValue;
        }
        else
        {
            this.name = null;
        }

        if (b.namespaceSet)
        {
            this.namespace = b.namespaceValue;
        }
        else
        {
            this.namespace = null;
        }

        if (b.prefixSet)
        {
            this.prefix = b.prefixValue;
        }
        else
        {
            this.prefix = null;
        }

        if (b.attributeSet)
        {
            this.attribute = b.attributeValue;
        }
        else
        {
            this.attribute = null;
        }

        if (b.wrappedSet)
        {
            this.wrapped = b.wrappedValue;
        }
        else
        {
            this.wrapped = null;
        }
    }

    @JsonPOJOBuilder(
            withPrefix = ""
    )
    public static class OpenAPIv3_0_3XMLBuilder
    {
        private boolean nameSet;
        private String nameValue;
        private boolean namespaceSet;
        private String namespaceValue;
        private boolean prefixSet;
        private String prefixValue;
        private boolean attributeSet;
        private Boolean attributeValue;
        private boolean wrappedSet;
        private Boolean wrappedValue;

        public OpenAPIv3_0_3XML build()
        {
            return new OpenAPIv3_0_3XML(this);
        }

        public OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder name(String name)
        {
            this.nameValue = name;
            this.nameSet = true;
            return this;
        }

        public OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder namespace(String namespace)
        {
            this.namespaceValue = namespace;
            this.namespaceSet = true;
            return this;
        }

        public OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder prefix(String prefix)
        {
            this.prefixValue = prefix;
            this.prefixSet = true;
            return this;
        }

        public OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder attribute(Boolean attribute)
        {
            this.attributeValue = attribute;
            this.attributeSet = true;
            return this;
        }

        public OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder wrapped(Boolean wrapped)
        {
            this.wrappedValue = wrapped;
            this.wrappedSet = true;
            return this;
        }
    }

    public static OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder builder()
    {
        return new OpenAPIv3_0_3XML.OpenAPIv3_0_3XMLBuilder();
    }

}
