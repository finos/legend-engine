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

package org.finos.legend.engine.protocol.graphQL.v1.typeSystem;

public interface TypeSystemDefinitionVisitor<T>
{
    T visit(ScalarTypeDefinition scalarTypeDefinition);
    T visit(EnumTypeDefinition enumTypeDefinition);
    T visit(UnionTypeDefinition unionTypeDefinition);
    T visit(InterfaceTypeDefinition interfaceTypeDefinition);
    T visit(ObjectTypeDefinition objectTypeDefinition);
    T visit(DirectiveDefinition directiveDefinition);
    T visit(SchemaDefinition schemaDefinition);
}
