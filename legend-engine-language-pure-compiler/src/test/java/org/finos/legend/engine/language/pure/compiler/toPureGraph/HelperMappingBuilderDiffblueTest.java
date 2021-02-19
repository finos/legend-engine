package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingEnumSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingIntegerSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingStringSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enum_Impl;
import org.junit.Test;

public class HelperMappingBuilderDiffblueTest {
  @Test
  public void testConvertSourceValues() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    // Act and Assert
    assertTrue(HelperMappingBuilder
        .convertSourceValues(enumerationMapping, new ArrayList<Object>(), mock(CompileContext.class))
        .toList()
        .isEmpty());
  }

  @Test
  public void testConvertSourceValues10() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "STRING";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues11() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = null;
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues12() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "INTEGER";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues13() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = null;
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");
    objectList.add("42");

    // Act and Assert
    assertEquals(2,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues14() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "INTEGER";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");
    objectList.add("42");

    // Act and Assert
    assertEquals(2,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues15() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("INTEGER");
    CompileContext compileContext = mock(CompileContext.class);
    when(compileContext.resolveEnumValue(anyString(), anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("42"));

    // Act and Assert
    MutableList<?> toListResult = HelperMappingBuilder
        .convertSourceValues(enumerationMapping, objectList, compileContext)
        .toList();
    assertEquals(1, toListResult.size());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._taggedValues.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._stereotypes.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0)).getCompileStates().toList().isEmpty());
    verify(compileContext).resolveEnumValue(anyString(), anyString());
  }

  @Test
  public void testConvertSourceValues16() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();
    EnumValueMappingEnumSourceValue enumValueMappingEnumSourceValue = new EnumValueMappingEnumSourceValue();
    enumValueMappingEnumSourceValue.enumeration = "STRING";
    enumValueMappingEnumSourceValue.value = "42";

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add(enumValueMappingEnumSourceValue);
    CompileContext compileContext = mock(CompileContext.class);
    when(compileContext.resolveEnumValue(anyString(), anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("42"));

    // Act and Assert
    MutableList<?> toListResult = HelperMappingBuilder
        .convertSourceValues(enumerationMapping, objectList, compileContext)
        .toList();
    assertEquals(1, toListResult.size());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._taggedValues.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._stereotypes.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0)).getCompileStates().toList().isEmpty());
    verify(compileContext).resolveEnumValue(anyString(), anyString());
  }

  @Test
  public void testConvertSourceValues17() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();
    EnumValueMappingIntegerSourceValue enumValueMappingIntegerSourceValue = new EnumValueMappingIntegerSourceValue();
    enumValueMappingIntegerSourceValue.value = 42;

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add(enumValueMappingIntegerSourceValue);

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues18() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();
    EnumValueMappingStringSourceValue enumValueMappingStringSourceValue = new EnumValueMappingStringSourceValue();
    enumValueMappingStringSourceValue.value = "42";

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add(enumValueMappingStringSourceValue);

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues19() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = null;
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add(2);

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues2() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList arrayList = new ArrayList();
    arrayList.add(new HashMap<Object, Object>());

    // Act and Assert
    assertThrows(UnsupportedOperationException.class,
        () -> HelperMappingBuilder.convertSourceValues(enumerationMapping, arrayList, mock(CompileContext.class)));
  }

  @Test
  public void testConvertSourceValues20() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "INTEGER";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add(2);

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues21() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "INTEGER";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add(null);

    // Act and Assert
    assertEquals(1,
        HelperMappingBuilder.convertSourceValues(enumerationMapping, objectList, mock(CompileContext.class))
            .toList()
            .size());
  }

  @Test
  public void testConvertSourceValues3() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    HashMap hashMap = new HashMap();
    hashMap.put((Object) "fullPath", "foo");
    hashMap.put((Object) "values", new ArrayList<Object>());
    hashMap.put((Object) "_type", (Object) "string");
    hashMap.put((Object) "value", "foo");

    ArrayList arrayList = new ArrayList();
    arrayList.add(hashMap);

    // Act and Assert
    assertTrue(HelperMappingBuilder.convertSourceValues(enumerationMapping, arrayList, mock(CompileContext.class))
        .toList()
        .isEmpty());
  }

  @Test
  public void testConvertSourceValues4() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    HashMap hashMap = new HashMap();
    hashMap.put((Object) "fullPath", "foo");
    hashMap.put((Object) "values", new ArrayList<Object>());
    hashMap.put((Object) "_type", (Object) "integer");
    hashMap.put((Object) "value", "foo");

    ArrayList arrayList = new ArrayList();
    arrayList.add(hashMap);

    // Act and Assert
    assertTrue(HelperMappingBuilder.convertSourceValues(enumerationMapping, arrayList, mock(CompileContext.class))
        .toList()
        .isEmpty());
  }

  @Test
  public void testConvertSourceValues5() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    HashMap hashMap = new HashMap();
    hashMap.put((Object) "fullPath", "foo");
    hashMap.put((Object) "values", new ArrayList<Object>());
    hashMap.put((Object) "_type", (Object) "collection");
    hashMap.put((Object) "value", "foo");

    ArrayList arrayList = new ArrayList();
    arrayList.add(hashMap);

    // Act and Assert
    assertTrue(HelperMappingBuilder.convertSourceValues(enumerationMapping, arrayList, mock(CompileContext.class))
        .toList()
        .isEmpty());
  }

  @Test
  public void testConvertSourceValues6() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    HashMap hashMap = new HashMap();
    hashMap.put((Object) "fullPath", "foo");
    hashMap.put((Object) "values", new ArrayList<Object>());
    hashMap.put((Object) "_type", "foo");
    hashMap.put((Object) "value", "foo");

    ArrayList arrayList = new ArrayList();
    arrayList.add(hashMap);

    // Act and Assert
    assertThrows(UnsupportedOperationException.class,
        () -> HelperMappingBuilder.convertSourceValues(enumerationMapping, arrayList, mock(CompileContext.class)));
  }

  @Test
  public void testConvertSourceValues7() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    HashMap hashMap = new HashMap();
    hashMap.put((Object) "fullPath", "foo");
    hashMap.put((Object) "values", new ArrayList<Object>());
    hashMap.put((Object) "_type", (Object) "enumValue");
    hashMap.put((Object) "value", "foo");

    ArrayList arrayList = new ArrayList();
    arrayList.add(hashMap);
    CompileContext compileContext = mock(CompileContext.class);
    when(compileContext.resolveEnumValue(anyString(), anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("42"));

    // Act and Assert
    MutableList<?> toListResult = HelperMappingBuilder
        .convertSourceValues(enumerationMapping, arrayList, compileContext)
        .toList();
    assertEquals(1, toListResult.size());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._taggedValues.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._stereotypes.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0)).getCompileStates().toList().isEmpty());
    verify(compileContext).resolveEnumValue(anyString(), anyString());
  }

  @Test
  public void testConvertSourceValues8() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");
    CompileContext compileContext = mock(CompileContext.class);
    when(compileContext.resolveEnumValue(anyString(), anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("42"));

    // Act and Assert
    MutableList<?> toListResult = HelperMappingBuilder
        .convertSourceValues(enumerationMapping, objectList, compileContext)
        .toList();
    assertEquals(1, toListResult.size());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._taggedValues.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._stereotypes.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0)).getCompileStates().toList().isEmpty());
    verify(compileContext).resolveEnumValue(anyString(), anyString());
  }

  @Test
  public void testConvertSourceValues9() {
    // Arrange
    EnumerationMapping enumerationMapping = new EnumerationMapping();
    enumerationMapping.id = "42";
    enumerationMapping.enumeration = "Enumeration";
    enumerationMapping.enumValueMappings = new ArrayList<EnumValueMapping>();
    enumerationMapping.sourceType = "Source Type";
    enumerationMapping.sourceInformation = new SourceInformation();

    ArrayList<Object> objectList = new ArrayList<Object>();
    objectList.add("42");
    objectList.add("42");
    CompileContext compileContext = mock(CompileContext.class);
    when(compileContext.resolveEnumValue(anyString(), anyString()))
        .thenReturn(new Root_meta_pure_metamodel_type_Enum_Impl("42"));

    // Act and Assert
    MutableList<?> toListResult = HelperMappingBuilder
        .convertSourceValues(enumerationMapping, objectList, compileContext)
        .toList();
    assertEquals(2, toListResult.size());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._taggedValues.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0))._stereotypes.toList().isEmpty());
    assertTrue(((Root_meta_pure_metamodel_type_Enum_Impl) toListResult.get(0)).getCompileStates().toList().isEmpty());
    verify(compileContext, times(2)).resolveEnumValue(anyString(), anyString());
  }
}

