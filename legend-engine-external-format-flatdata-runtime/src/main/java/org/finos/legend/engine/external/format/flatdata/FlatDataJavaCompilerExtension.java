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

import org.finos.legend.engine.external.format.flatdata.read.IFlatDataDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataNumber;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataTemporal;
import org.finos.legend.engine.external.format.flatdata.write.IFlatDataSerializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.shared.ExternalFormatJavaCompilerExtension;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataBooleanAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataLongAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalData;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;
import org.finos.legend.engine.plan.compilation.GeneratePureConfig;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassListFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;

import java.util.LinkedHashMap;
import java.util.Map;

public class FlatDataJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    private static final String PURE_PACKAGE = "meta::external::format::flatdata::executionPlan::engine::";
    private static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        DEPENDENCIES.put("meta::external::format::flatdata::executionPlan::RawFlatData", RawFlatData.class);
        DEPENDENCIES.put("meta::external::format::flatdata::executionPlan::RawFlatDataValue", RawFlatDataValue.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IFlatDataSerializeExecutionNodeSpecifics", IFlatDataSerializeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IFlatDataDeserializeExecutionNodeSpecifics", IFlatDataDeserializeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataContext", FlatDataContext.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ParsedFlatData", ParsedFlatData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ParsedFlatDataToObject", ParsedFlatDataToObject.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_ObjectToParsedFlatData", ObjectToParsedFlatData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatData", FlatData.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_FlatDataSection", FlatDataSection.class);
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
    public ClassPathFilter getExtraClassPathFilter()
    {
        return new ClassListFilter(DEPENDENCIES.values());
    }

    /**
     *  Use to generate content of core_external_format_flatdata/executionPlan/engine.pure (see GeneratePureConfig)
     */
    public static void main(String[] args)
    {
        GeneratePureConfig extension = new GeneratePureConfig("externalFormatFlatdata", ExternalFormatJavaCompilerExtension.class, PURE_PACKAGE);
        DEPENDENCIES.forEach(extension::addClass);
        System.out.println(extension.generate());
    }
}