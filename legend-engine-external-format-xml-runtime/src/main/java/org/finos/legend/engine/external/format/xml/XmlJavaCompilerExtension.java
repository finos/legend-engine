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

package org.finos.legend.engine.external.format.xml;

import org.finos.legend.engine.external.format.xml.read.DeserializeContext;
import org.finos.legend.engine.external.format.xml.read.IXmlDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.external.format.xml.read.ReadHandler;
import org.finos.legend.engine.external.format.xml.read.ValueProcessor;
import org.finos.legend.engine.external.format.xml.read.XmlDataRecord;
import org.finos.legend.engine.external.format.xml.read.handlers.All;
import org.finos.legend.engine.external.format.xml.read.handlers.AnySurroundingElements;
import org.finos.legend.engine.external.format.xml.read.handlers.Attribute;
import org.finos.legend.engine.external.format.xml.read.handlers.Choice;
import org.finos.legend.engine.external.format.xml.read.handlers.Document;
import org.finos.legend.engine.external.format.xml.read.handlers.Element;
import org.finos.legend.engine.external.format.xml.read.handlers.FlexCollectionElement;
import org.finos.legend.engine.external.format.xml.read.handlers.Particle;
import org.finos.legend.engine.external.format.xml.read.handlers.Sequence;
import org.finos.legend.engine.external.format.xml.read.handlers.TextContent;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddBooleanToObject;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddDoubleToObject;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddEnumToObject;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddLongToObject;
import org.finos.legend.engine.external.format.xml.read.valueProcessors.AddObjectToObject;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BooleanSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.DoubleSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.LongSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypeHandler;
import org.finos.legend.engine.external.shared.ExternalFormatJavaCompilerExtension;
import org.finos.legend.engine.plan.compilation.GeneratePureConfig;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.shared.javaCompiler.ClassListFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;

import java.util.LinkedHashMap;
import java.util.Map;

public class XmlJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    private static final String PURE_PACKAGE = "meta::external::format::xml::executionPlan::engine::";
    private static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        DEPENDENCIES.put("meta::external::format::xml::executionPlan::XmlDataRecord", XmlDataRecord.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IXmlDeserializeExecutionNodeSpecifics", IXmlDeserializeExecutionNodeSpecifics.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_DeserializeContext", DeserializeContext.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_SimpleTypeHandler", SimpleTypeHandler.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_BooleanSimpleTypeHandler", BooleanSimpleTypeHandler.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_LongSimpleTypeHandler", LongSimpleTypeHandler.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_DoubleSimpleTypeHandler", DoubleSimpleTypeHandler.class);

        DEPENDENCIES.put(PURE_PACKAGE + "_ValueProcessor", ValueProcessor.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_AddBooleanToObject", AddBooleanToObject.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_AddDoubleToObject", AddDoubleToObject.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_AddEnumToObject", AddEnumToObject.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_AddLongToObject", AddLongToObject.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_AddObjectToObject", AddObjectToObject.class);

        DEPENDENCIES.put(PURE_PACKAGE + "h_ReadHandler", ReadHandler.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_All", All.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_AnySurroundingElements", AnySurroundingElements.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_Attribute", Attribute.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_Choice", Choice.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_Document", Document.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_Element", Element.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_FlexCollectionElement", FlexCollectionElement.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_Particle", Particle.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_Sequence", Sequence.class);
        DEPENDENCIES.put(PURE_PACKAGE + "h_TextContent", TextContent.class);
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return new ClassListFilter(DEPENDENCIES.values());
    }

    /**
     *  Use to generate content of core_external_format_xml/executionPlan/engine.pure (see GeneratePureConfig)
     */
    public static void main(String[] args)
    {
        GeneratePureConfig extension = new GeneratePureConfig("externalFormatXml", ExternalFormatJavaCompilerExtension.class, PURE_PACKAGE);
        DEPENDENCIES.forEach(extension::addClass);
        System.out.println(extension.generate());
    }

}
