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

package org.finos.legend.engine.repl.shared;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.function.property.Property;
import org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.pure.generated.core_pure_corefunctions_stringExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.util.Collections;

public class RelationClassMappingGenerator
{
    private static String getColumnPureType(Column<?, ?> c)
    {
        return _Column.getColumnType(c)._rawType()._name();
    }

    private static String getPropertyName(String columnName, ExecutionSupport es)
    {
        String sqlSafeColumnName = columnName.replaceAll("[ /@~<>.]", "_").replace("\"", "");
        return core_pure_corefunctions_stringExtension.Root_meta_pure_functions_string_makeCamelCase_String_1__String_1_(sqlSafeColumnName, es);
    }

    public static String generateFunction(String functionName, String expression)
    {
        return generateFunction(functionName, expression, "Any", "1");
    }

    public static String generateFunction(String functionName, String expression, String returnType)
    {
        return generateFunction(functionName, expression, returnType, "1");
    }

    private static String generateFunction(String functionName, String expression, String returnType, String returnMultiplicity)
    {
        return  "###Pure\n" +
                "function " + functionName + "():" + returnType + "[" + returnMultiplicity + "]\n" +
                "{\n" +
                    expression + ";\n" +
                "}\n";
    }

    public static String generateClass(String className, RelationType<?> relationType, CompiledExecutionSupport es)
    {
        Class clazz = new Class();
        clazz.name = className;
        clazz.properties = relationType._columns().collect(c -> 
        {
            Property property = new Property();
            property.name = getPropertyName(c._name(), es);
            property.genericType = new GenericType();
            property.genericType.rawType = new PackageableType(getColumnPureType(c));
            Multiplicity m = _Column.getColumnMultiplicity(c);
            property.multiplicity = new org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity(m._lowerBound()._value().intValue(), m. _upperBound()._value().intValue());
            return property;
        }).toList();
        return "###Pure\n" + renderElement(clazz);
    }

    public static String generateClassMapping(String mappingName, String className, String functionDescriptor, RelationType<?> relationType, CompiledExecutionSupport es)
    {
        Mapping mapping = new Mapping();
        mapping.name = mappingName;
        RelationFunctionClassMapping classMapping = new RelationFunctionClassMapping();
        classMapping.relationFunction = new PackageableElementPointer();
        classMapping.relationFunction.type = PackageableElementType.FUNCTION;
        classMapping.relationFunction.path = functionDescriptor;
        classMapping._class = className;
        classMapping.propertyMappings = relationType._columns().collect(c -> 
        {
            RelationFunctionPropertyMapping propertyMapping = new RelationFunctionPropertyMapping();
            propertyMapping.property = new PropertyPointer();
            propertyMapping.property._class = className;
            propertyMapping.property.property = getPropertyName(c._name(), es);
            propertyMapping.column = c._name();
            return (PropertyMapping) propertyMapping;
        }).toList();
        mapping.classMappings = Collections.singletonList(classMapping);
        return "###Mapping\n" + renderElement(mapping);         
    }
    
    private static String renderElement(PackageableElement element)
    {
        return PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).render(element);
    }
}
