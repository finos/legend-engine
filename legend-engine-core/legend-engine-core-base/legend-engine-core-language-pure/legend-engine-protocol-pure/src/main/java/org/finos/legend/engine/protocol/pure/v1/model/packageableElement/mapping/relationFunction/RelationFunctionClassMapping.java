// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction;

import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;

import java.util.Collections;
import java.util.List;

public class RelationFunctionClassMapping extends ClassMapping
{
    /**
     * Set when the source form is {@code ~func <descriptor>}. Mutually exclusive with {@link #sourceLambda}.
     */
    public PackageableElementPointer relationFunction;

    /**
     * Set when the source form is {@code ~src <expression>}. The lambda is a zero-arg wrapper around the
     * inline expression and must evaluate to a {@code Relation<...>}. Mutually exclusive with
     * {@link #relationFunction}.
     */
    public LambdaFunction sourceLambda;

    public List<PropertyMapping> propertyMappings;
    public List<String> primaryKey = Collections.emptyList();

    @Override
    public <T> T accept(ClassMappingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
