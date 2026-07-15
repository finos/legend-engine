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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.BindingTransformer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMappingVisitor;

public class RelationFunctionPropertyMapping extends PropertyMapping
{
    /**
     * Set when the property RHS is the bare-column form ({@code propName: COL}). The compiler lowers this
     * to a synthetic lambda {@code { $src.COL}} at SecondPass. Mutually exclusive with {@link #valueFn}.
     */
    public String column;

    /**
     * Set when the property RHS is a Pure expression over {@code $src} ({@code propName: $src.COL1 + $src.COL2}).
     * The lambda body is the user expression; the compiler injects the {@code $src} parameter typed at the
     * relation function's row type at SecondPass. Mutually exclusive with {@link #column}.
     */
    public LambdaFunction valueFn;

    public BindingTransformer bindingTransformer;
    public String enumMappingId;

    @Override
    public <T> T accept(PropertyMappingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
