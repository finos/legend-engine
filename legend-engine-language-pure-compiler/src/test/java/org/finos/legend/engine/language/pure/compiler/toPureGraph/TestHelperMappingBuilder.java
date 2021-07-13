// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingEnumSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingIntegerSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingStringSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enum_Impl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;

public class TestHelperMappingBuilder {
  @Test
  public void testConvertSourceValues_Empty() {
    Assert.assertTrue(HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class),
        new ArrayList<>(),
        Mockito.mock(CompileContext.class)).isEmpty());
  }

  @Test
  public void testConvertSourceValues_Simple_Integer() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = null;
    List<Object> values = Collections.singletonList(42);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals(42L, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_Simple_String() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = null;
    List<Object> values = Collections.singletonList("foo");

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals("foo", result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_Simple_null() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = null;
    List<Object> values = Collections.singletonList(null);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals(null, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_TwoSimpleStrings() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = null;
    List<Object> values = Arrays.asList("foo", "bar");

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals("foo", result.toList().get(0));
    Assert.assertEquals("bar", result.toList().get(1));
  }

  @Test
  public void testConvertSourceValues_SourceType_STRING() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = "STRING";

    List<Object> values = Collections.singletonList("foo");

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals("foo", result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_SourceType_INTEGER_fromString() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = "INTEGER";

    List<Object> values = Collections.singletonList("42");

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals(42L, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_SourceType_null() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = "INTEGER";

    List<Object> values = Collections.singletonList(null);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals(null, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_SourceType_ENUM() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = "ENUM";

    List<Object> values = Collections.singletonList("VALUE");

    CompileContext compileContext = Mockito.mock(CompileContext.class);
    Mockito.when(compileContext.resolveEnumValue(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("VALUE"));

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, compileContext);

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("VALUE", ((Root_meta_pure_metamodel_type_Enum_Impl) result.toList().get(0)).getName());
  }

  @Test
  public void testConvertSourceValues_SourceType_INTEGER_fromInteger() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = "INTEGER";

    List<Object> values = Collections.singletonList(42);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals(42L, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_TwoSourceType_INTEGER() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.sourceType = "INTEGER";

    List<Object> values = Arrays.asList("42", "43");

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        enumerationMapping, values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(values.size(), result.size());
    Assert.assertEquals(42L, result.toList().get(0));
    Assert.assertEquals(43L, result.toList().get(1));
  }

  @Test
  public void testConvertSourceValues_StructuredSourceValue_Integer() {
    // Arrange
    EnumValueMappingIntegerSourceValue enumValueMappingIntegerSourceValue = new EnumValueMappingIntegerSourceValue();
    enumValueMappingIntegerSourceValue.value = 42;
    List<Object> values = Collections.singletonList(enumValueMappingIntegerSourceValue);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(42L, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_StructuredSourceValue_String() {
    // Arrange
    EnumValueMappingStringSourceValue enumValueMappingStringSourceValue = new EnumValueMappingStringSourceValue();
    enumValueMappingStringSourceValue.value = "foo";
    List<Object> values = Collections.singletonList(enumValueMappingStringSourceValue);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("foo", result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_StructuredSourceValue_Enum() {
    // Arrange
    EnumValueMappingEnumSourceValue enumValueMappingEnumSourceValue = new EnumValueMappingEnumSourceValue();
    enumValueMappingEnumSourceValue.enumeration = "ENUM";
    enumValueMappingEnumSourceValue.value = "VALUE";
    List<Object> values = Collections.singletonList(enumValueMappingEnumSourceValue);

    CompileContext compileContext = Mockito.mock(CompileContext.class);
    Mockito.when(compileContext.resolveEnumValue(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("VALUE"));

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, compileContext);

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("VALUE", ((Root_meta_pure_metamodel_type_Enum_Impl) result.toList().get(0)).getName());
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_EmptyMap() {
    // Arrange
    List<Object> values = Collections.singletonList(new HashMap<>());

    // Act and Assert
    Assert.assertThrows(UnsupportedOperationException.class,
        () -> HelperMappingBuilder.convertSourceValues(
            Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class)));
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_WrongType() {
    // Arrange
    HashMap value = new HashMap();
    value.put("fullPath", "path");
    value.put("values", Collections.emptyList());
    value.put("_type", "foo");
    value.put("value", "foo");

    List<Object> values = Collections.singletonList(value);

    // Act
    Assert.assertThrows(UnsupportedOperationException.class,
        () -> HelperMappingBuilder.convertSourceValues(
            Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class)));
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_String_Empty() {
    // Arrange
    HashMap value = new HashMap();
    value.put("fullPath", "path");
    value.put("values", Collections.emptyList());
    value.put("_type", "string");
    value.put("value", "foo");

    List<Object> values = Collections.singletonList(value);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_String() {
    // Arrange
    HashMap value = new HashMap();
    value.put("fullPath", "path");
    value.put("values", Collections.singletonList("foo"));
    value.put("_type", "string");
    value.put("value", null);

    List<Object> values = Collections.singletonList(value);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("foo", result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_Integer() {
    // Arrange
    HashMap value = new HashMap();
    value.put("fullPath", "path");
    value.put("values", Collections.singletonList(42));
    value.put("_type", "integer");
    value.put("value", null);

    List<Object> values = Collections.singletonList(value);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals(42L, result.toList().get(0));
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_Collection() {
    // Arrange
    HashMap value1 = new HashMap();
    value1.put("fullPath", "path");
    value1.put("values", Collections.singletonList("foo"));
    value1.put("_type", "string");
    value1.put("value", null);

    HashMap value2 = new HashMap();
    value2.put("fullPath", "path");
    value2.put("values", Collections.singletonList(42));
    value2.put("_type", "integer");
    value2.put("value", null);


    HashMap value = new HashMap();
    value.put("fullPath", "path");
    value.put("values", Arrays.asList(value1, value2));
    value.put("_type", "collection");
    value.put("value", null);

    List<Object> values = Collections.singletonList(value);

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, Mockito.mock(CompileContext.class));

    // Assert
    Assert.assertEquals(2, result.size());
    Assert.assertEquals("foo", ((FastList) result.toList().get(0)).get(0));
    Assert.assertEquals(42L, ((FastList) result.toList().get(1)).get(0));
  }

  @Test
  public void testConvertSourceValues_WithTypeFlag_Enum() {
    // Arrange
    HashMap value = new HashMap();
    value.put("fullPath", "path");
    value.put("values", Collections.emptyList());
    value.put("_type", "enumValue");
    value.put("value", "VALUE");

    List<Object> values = Collections.singletonList(value);

    CompileContext compileContext = Mockito.mock(CompileContext.class);
    Mockito.when(compileContext.resolveEnumValue(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("VALUE"));

    // Act
    RichIterable<?> result = HelperMappingBuilder.convertSourceValues(
        Mockito.mock(EnumerationMapping.class), values, compileContext);

    // Assert
    Assert.assertEquals(1, result.size());
    Assert.assertEquals("VALUE", ((Root_meta_pure_metamodel_type_Enum_Impl) result.toList().get(0)).getName());
  }
}

