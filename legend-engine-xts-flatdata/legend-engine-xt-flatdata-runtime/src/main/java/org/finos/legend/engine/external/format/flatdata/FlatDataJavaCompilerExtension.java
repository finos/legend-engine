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

package org.finos.legend.engine.external.format.flatdata;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataNumber;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataTemporal;
import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.flatdata.write.IFlatDataSerializeExecutionNodeSpecifics;
import org.finos.legend.engine.plan.compilation.GeneratePureConfig;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilters;

import java.util.LinkedHashMap;
import java.util.Map;

public class FlatDataJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    private static final String PURE_PACKAGE = "meta::external::format::flatdata::executionPlan::platformBinding::legendJava::";
    private static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        DEPENDENCIES.put("meta::external::format::flatdata::executionPlan::model::RawFlatData", RawFlatData.class);
        DEPENDENCIES.put("meta::external::format::flatdata::executionPlan::model::RawFlatDataValue", RawFlatDataValue.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IFlatDataSerializeExecutionNodeSpecifics", IFlatDataSerializeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IFlatDataDeserializeExecutionNodeSpecifics", IFlatDataDeserializeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataContext", FlatDataContext.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ParsedFlatData", ParsedFlatData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ParsedFlatDataToObject", ParsedFlatDataToObject.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ObjectToParsedFlatData", ObjectToParsedFlatData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatData", FlatData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataSection", FlatDataSection.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataProperty", FlatDataProperty.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataRecordType", FlatDataRecordType.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataRecordField", FlatDataRecordField.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataDataType", FlatDataDataType.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataString", FlatDataString.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataBoolean", FlatDataBoolean.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataInteger", FlatDataInteger.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataDecimal", FlatDataDecimal.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataDate", FlatDataDate.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataDateTime", FlatDataDateTime.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataNumber", FlatDataNumber.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataTemporal", FlatDataTemporal.class);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "FlatData");
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return ClassPathFilters.fromClasses(DEPENDENCIES.values());
    }

    /**
     * Use to generate content of core_external_format_flatdata/executionPlan/engine.pure (see GeneratePureConfig)
     */
    public static void main(String[] args)
    {
        GeneratePureConfig extension = new GeneratePureConfig("externalFormatFlatdata", FlatDataJavaCompilerExtension.class, PURE_PACKAGE);
        DEPENDENCIES.forEach(extension::addClass);
        System.out.println(extension.generate());
    }
}