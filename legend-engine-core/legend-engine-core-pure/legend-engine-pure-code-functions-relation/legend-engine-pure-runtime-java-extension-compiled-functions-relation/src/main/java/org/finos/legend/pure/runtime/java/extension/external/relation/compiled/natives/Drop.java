// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Relation;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;

public class Drop extends AbstractNativeFunctionGeneric
{
    public Drop()
    {
        super("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.drop", new Class[]{Relation.class, Long.class, ExecutionSupport.class}, false, true, false, "drop_Relation_1__Integer_1__Relation_1_");
    }
}
