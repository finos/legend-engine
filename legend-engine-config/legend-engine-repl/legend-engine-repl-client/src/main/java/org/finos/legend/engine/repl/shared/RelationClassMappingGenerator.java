// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.shared;

import org.finos.legend.pure.generated.core_pure_corefunctions_stringExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.relation._Column;

public class RelationClassMappingGenerator
{
    public static RelationType<?> getRelationType(FunctionDefinition<?> relationFunction)
    {
        GenericType lastExpressionType = relationFunction._expressionSequence().toList().getLast()._genericType();
        return (RelationType<?>) lastExpressionType._typeArguments().toList().getFirst()._rawType();
    }
    
    public static String generateClass(FunctionDefinition<?> relationFunction, String className, ExecutionSupport es)
    {
        RelationType<?> relationType = getRelationType(relationFunction);
        return generateClass(relationType, className, es);
    }
    
    private static String getColumnPureType(Column<?, ?> c)
    {
        return _Column.getColumnType(c)._rawType()._name();
    }

    private static String getColumnPureMultiplicity(Column<?, ?> c)
    {
        return Multiplicity.print(_Column.getColumnMultiplicity(c));
    }

    private static String getPropertyName(String columnName, ExecutionSupport es)
    {
        String sqlSafeColumnName = columnName.replaceAll("[ /@~<>.]", "_").replace("\"", "");
        return core_pure_corefunctions_stringExtension.Root_meta_pure_functions_string_makeCamelCase_String_1__String_1_(sqlSafeColumnName, es);
    }
    
    public static String generateClass(RelationType<?> relationType, String className, ExecutionSupport es)
    {
        StringBuilder properties = new StringBuilder();
        relationType._columns().collect(c -> "\t" + getPropertyName(c._name(), es) + ": " + getColumnPureType(c) + getColumnPureMultiplicity(c) + ";\n").appendString(properties, "");

        return  "###Pure\n" +
                "Class " + className + "\n" + 
                "{\n" +
                    properties +
                "}\n";
    }

    public static String generateClassMapping(FunctionDefinition<?> relationFunction, String className, String functionName, ExecutionSupport es)
    {
        RelationType<?> relationType = getRelationType(relationFunction);
        return generateClassMapping(relationType, className, functionName, es);
    }

    public static String generateClassMapping(RelationType<?> relationType, String className, String functionName, ExecutionSupport es)
    {
        StringBuilder propertyMappings = new StringBuilder();
        relationType._columns().collect(c -> "\t" + getPropertyName(c._name(), es) + ": " + c._name()).appendString(propertyMappings, ",\n");

        return  className + ": Relation\n" +
                "{\n" +
                "   ~func " + functionName + "\n" +
                    propertyMappings + "\n" +
                "}\n";
    }
}
