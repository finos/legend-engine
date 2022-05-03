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

package org.finos.legend.engine.external.format.xsd.compile;

import org.finos.legend.engine.external.format.xml.shared.XmlReader;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BuiltInDataTypes;
import org.finos.legend.engine.external.format.xml.shared.datatypes.Facet;
import org.finos.legend.engine.external.format.xml.shared.datatypes.FacetType;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypesContext;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAll;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAnnotated;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAnnotation;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAny;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAnyAttribute;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAppInfo;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAttribute;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAttributeGroup;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdChoice;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdComplexContent;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdComplexType;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdContentProcessing;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdDerivationType;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdDocumentation;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdElement;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdEnumeration;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdExtension;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdFacet;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdForm;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdFractionDigits;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdGroup;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdImport;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdInclude;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdLength;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdList;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdMaxExclusive;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdMaxInclusive;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdMaxLength;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdMinExclusive;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdMinInclusive;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdMinLength;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdObject;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdPattern;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdRedefine;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdRestriction;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSchema;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSequence;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSimpleContent;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSimpleType;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdTotalDigits;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdUnion;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdUse;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdWhiteSpace;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XsdParser
{
    private static final String NAMESPACE = XMLConstants.W3C_XML_SCHEMA_NS_URI;

    private final String schema;

    private SimpleTypesContext simpleTypesContext;
    private SimpleTypeHandler<Boolean> xsdBoolean;
    private SimpleTypeHandler<BigDecimal> xsdNonNegativeInteger;
    private SimpleTypeHandler<String> xsdToken;
    private SimpleTypeHandler<QName> xsdQName;
    private SimpleTypeHandler<List<QName>> xsdQNames;
    private SimpleTypeHandler<List<String>> xsdTokens;
    private XmlReader reader;

    public XsdParser(String schema)
    {
        this.schema = schema;
    }

    public XsdSchema parse()
    {
        reader = XmlReader.newReader(new StringReader(schema), this::handleXmlStreamException);
        check(reader.isStartDocument(), "Invalid start of XML document");
        reader.nextTag();

        simpleTypesContext = new SimpleTypesContext(reader.getNamespaceContextSupplier());
        xsdBoolean = simpleTypesContext.handler(BuiltInDataTypes.XS_BOOLEAN);
        xsdNonNegativeInteger = simpleTypesContext.handler(BuiltInDataTypes.XS_NON_NEGATIVE_INTEGER);
        xsdToken = simpleTypesContext.handler(BuiltInDataTypes.XS_TOKEN);
        xsdTokens = simpleTypesContext.defineListType(BuiltInDataTypes.XS_TOKEN);
        xsdQName = simpleTypesContext.handler(BuiltInDataTypes.XS_QNAME);
        xsdQNames = simpleTypesContext.defineListType(BuiltInDataTypes.XS_QNAME);
        XsdSchema xsdSchema = readSchema();

        check(reader.isEndDocument(), "Invalid end of XML document");
        return xsdSchema;
    }

    private ExternalFormatSchemaException handleXmlStreamException(XMLStreamException e)
    {
        Matcher matcher = Pattern.compile("ParseError at .*\nMessage: (.*)").matcher(e.getMessage());
        String message;
        if (matcher.matches())
        {
            message = matcher.group(1);
        }
        else
        {
            Matcher matcher2 = Pattern.compile("(.*)\\s*at\\s*\\[.*]").matcher(e.getMessage());
            message = matcher2.matches() ? matcher2.group(1) : e.getMessage();
        }
        return new ExternalFormatSchemaException(message, e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());
    }

    private XsdAll readAll()
    {
        check(reader.isStartElement(NAMESPACE, "all"), "Expected start of all element");
        XsdAll result = readXsdObject(new XsdAll());
        result.minOccurs = minOccurs();
        result.maxOccurs = maxOccurs();

        reader.nextTag();
        new ChildrenHandler<>(result, "all")
                .whenElement("element", () -> result.items.add(readElement()))
                .process();

        check(reader.isEndElement(NAMESPACE, "all"), "Expected end of all element");
        reader.nextTag();
        return result;
    }

    private XsdAnnotation readAnnotation()
    {
        check(reader.isStartElement(NAMESPACE, "annotation"), "Expected start of annotation element");
        XsdAnnotation result = readXsdObject(new XsdAnnotation());

        reader.nextTag();
        new ChildrenHandler<>(result, "annotation")
                .whenElement("documentation", () -> result.items.add(readDocumentation()))
                .whenElement("appinfo", () -> result.items.add(readAppInfo()))
                .process();

        check(reader.isEndElement(NAMESPACE, "annotation"), "Expected end of annotation element");
        reader.nextTag();
        return result;
    }

    private XsdAny readAny()
    {
        check(reader.isStartElement(NAMESPACE, "any"), "Expected start of any element");
        XsdAny result = readXsdObject(new XsdAny());
        result.minOccurs = minOccurs();
        result.maxOccurs = maxOccurs();
        result.namespace = xsdTokens.parse(reader.getAttributeValueOrDefault("namespace", "##any"));
        result.processContents = XsdContentProcessing.valueOf(stringEnum("skip", "lax", "strict").parse(reader.getAttributeValueOrDefault("processContents", "strict")).toUpperCase());

        reader.nextTag();
        new ChildrenHandler<>(result, "any").process();

        check(reader.isEndElement(NAMESPACE, "any"), "Expected end of any element");
        reader.nextTag();
        return result;
    }

    private XsdAnyAttribute readAnyAttribute()
    {
        check(reader.isStartElement(NAMESPACE, "anyAttribute"), "Expected start of anyAttribute element");
        XsdAnyAttribute result = readXsdObject(new XsdAnyAttribute());
        result.namespace = xsdTokens.parse(reader.getAttributeValueOrDefault("namespace", "##any"));
        result.processContents = XsdContentProcessing.valueOf(stringEnum("skip", "lax", "strict").parse(reader.getAttributeValueOrDefault("processContents", "strict")).toUpperCase());

        reader.nextTag();
        new ChildrenHandler<>(result, "anyAttribute").process();

        check(reader.isEndElement(NAMESPACE, "anyAttribute"), "Expected end of anyAttribute element");
        reader.nextTag();
        return result;
    }

    private XsdAppInfo readAppInfo()
    {
        check(reader.isStartElement(NAMESPACE, "appinfo"), "Expected start of appinfo element");
        XsdAppInfo result = readXsdObject(new XsdAppInfo());
        reader.getRawElementContents();

        check(reader.isEndElement(NAMESPACE, "appinfo"), "Expected end of appinfo element");
        reader.nextTag();
        return result;

    }

    private XsdAttribute readAttribute()
    {
        check(reader.isStartElement(NAMESPACE, "attribute"), "Expected start of attribute element");
        XsdAttribute result = readXsdObject(new XsdAttribute());
        result.name = reader.getAttributeValue("name");
        result.ref = xsdQName.parse(reader.getAttributeValue("ref"));
        result.defaultValue = reader.getAttributeValue("default");
        result.fixedValue = reader.getAttributeValue("fixed");
        result.form = xsdForm("form");
        result.typeName = xsdQName.parse(reader.getAttributeValue("type"));
        result.use = XsdUse.valueOf(stringEnum("prohibited", "optional", "required").parse(reader.getAttributeValueOrDefault("use", "optional")).toUpperCase());

        reader.nextTag();
        new ChildrenHandler<>(result, "attribute")
                .whenElement("simpleType", () -> result.type = readSimpleType())
                .process();

        check(reader.isEndElement(NAMESPACE, "attribute"), "Expected end of attribute element");
        reader.nextTag();
        return result;
    }

    private XsdAttributeGroup readAttributeGroup()
    {
        check(reader.isStartElement(NAMESPACE, "attributeGroup"), "Expected start of attributeGroup element");
        XsdAttributeGroup result = readXsdObject(new XsdAttributeGroup());
        result.name = reader.getAttributeValue("name");
        result.ref = xsdQName.parse(reader.getAttributeValue("ref"));
        result.items = new ArrayList<>();

        reader.nextTag();
        new ChildrenHandler<>(result, "attributeGroup")
                .whenElement("attributeGroup", () -> result.items.add(readAttributeGroup()))
                .whenElement("attribute", () -> result.items.add(readAttribute()))
                .whenElement("anyAttribute", () -> result.anyAttribute = readAnyAttribute())
                .process();

        check(reader.isEndElement(NAMESPACE, "attributeGroup"), "Expected end of attributeGroup element");
        reader.nextTag();
        return result;
    }

    private XsdChoice readChoice()
    {
        check(reader.isStartElement(NAMESPACE, "choice"), "Expected start of choice element");
        XsdChoice result = readXsdObject(new XsdChoice());
        result.minOccurs = minOccurs();
        result.maxOccurs = maxOccurs();

        reader.nextTag();
        new ChildrenHandler<>(result, "choice")
                .whenElement("element", () -> result.items.add(readElement()))
                .whenElement("choice", () -> result.items.add(readChoice()))
                .whenElement("sequence", () -> result.items.add(readSequence()))
                .whenElement("group", () -> result.items.add(readGroup()))
                .whenElement("any", () -> result.items.add(readAny()))
                .process();

        check(reader.isEndElement(NAMESPACE, "choice"), "Expected end of choice element");
        reader.nextTag();
        return result;
    }

    private XsdComplexContent readComplexContent()
    {
        check(reader.isStartElement(NAMESPACE, "complexContent"), "Expected start of complexContent element");
        XsdComplexContent result = readXsdObject(new XsdComplexContent());
        result.mixed = xsdBoolean.parse(reader.getAttributeValueOrDefault("mixed", "false"));

        reader.nextTag();
        new ChildrenHandler<>(result, "complexContent")
                .whenElement("extension", () -> result.derivation = readExtension())
                .whenElement("restriction", () -> result.derivation = readRestriction())
                .process();

        check(reader.isEndElement(NAMESPACE, "complexContent"), "Expected end of complexContent element");
        reader.nextTag();
        return result;
    }

    private XsdComplexType readComplexType()
    {
        check(reader.isStartElement(NAMESPACE, "complexType"), "Expected start of complexType element");
        XsdComplexType result = readXsdObject(new XsdComplexType());
        result.name = reader.getAttributeValue("name");
        result.ref = xsdQName.parse(reader.getAttributeValue("ref"));
        result.block = xsdDerivationType("block", XsdDerivationType.EXTENSION_RESTRICTION_SUBSTITUTION);
        result._final = xsdDerivationType("final", XsdDerivationType.EXTENSION_RESTRICTION);
        result._abstract = xsdBoolean.parse(reader.getAttributeValueOrDefault("abstract", "false"));
        result.mixed = xsdBoolean.parse(reader.getAttributeValueOrDefault("mixed", "false"));

        reader.nextTag();
        new ChildrenHandler<>(result, "complexType")
                .whenElement("all", () -> result.particle = readAll())
                .whenElement("choice", () -> result.particle = readChoice())
                .whenElement("sequence", () -> result.particle = readSequence())
                .whenElement("group", () -> result.particle = readGroup())
                .whenElement("simpleContent", () -> result.contentModel = readSimpleContent())
                .whenElement("complexContent", () -> result.contentModel = readComplexContent())
                .whenElement("attribute", () -> result.attributeItems.add(readAttribute()))
                .whenElement("attributeGroup", () -> result.attributeItems.add(readAttributeGroup()))
                .whenElement("anyAttribute", () -> result.anyAttribute = readAnyAttribute())
                .process();

        check(reader.isEndElement(NAMESPACE, "complexType"), "Expected end of complexType element");
        reader.nextTag();
        return result;
    }

    private XsdDocumentation readDocumentation()
    {
        check(reader.isStartElement(NAMESPACE, "documentation"), "Expected start of documentation element");
        XsdDocumentation result = readXsdObject(new XsdDocumentation());
        result.language = xsdToken.parse(reader.getAttributeValue(XMLConstants.XML_NS_URI, "lang"));

        reader.next();
        result.value = xsdToken.parse(reader.getRawElementTextContents());

        check(reader.isEndElement(NAMESPACE, "documentation"), "Expected end of documentation element");
        reader.nextTag();
        return result;

    }

    private XsdElement readElement()
    {
        check(reader.isStartElement(NAMESPACE, "element"), "Expected start of element element");
        XsdElement result = readXsdObject(new XsdElement());
        result.name = reader.getAttributeValue("name");
        result.ref = xsdQName.parse(reader.getAttributeValue("ref"));
        result.minOccurs = minOccurs();
        result.maxOccurs = maxOccurs();
        result.defaultValue = reader.getAttributeValue("default");
        result.fixedValue = reader.getAttributeValue("fixed");
        result.form = xsdForm("form");
        result.block = xsdDerivationType("block", XsdDerivationType.EXTENSION_RESTRICTION_SUBSTITUTION);
        result._final = xsdDerivationType("final", XsdDerivationType.EXTENSION_RESTRICTION);
        result._abstract = xsdBoolean.parse(reader.getAttributeValueOrDefault("abstract", "false"));
        result.nilable = xsdBoolean.parse(reader.getAttributeValue("nilable", "false"));
        result.typeName = xsdQName.parse(reader.getAttributeValue("type"));
        result.substitutionGroup = xsdQName.parse(reader.getAttributeValue("substitutionGroup"));

        reader.nextTag();
        new ChildrenHandler<>(result, "element")
                .whenElement("simpleType", () -> result.type = readSimpleType())
                .whenElement("complexType", () -> result.type = readComplexType())
                .process();

        check(reader.isEndElement(NAMESPACE, "element"), "Expected end of element element");
        reader.nextTag();
        return result;
    }

    private XsdExtension readExtension()
    {
        check(reader.isStartElement(NAMESPACE, "extension"), "Expected start of extension element");
        XsdExtension result = readXsdObject(new XsdExtension());
        result.baseTypeName = xsdQName.parse(reader.getAttributeValue("base"));
        result.attributeItems = new ArrayList<>();

        reader.nextTag();
        new ChildrenHandler<>(result, "extension")
                .whenElement("all", () -> result.particle = readAll())
                .whenElement("choice", () -> result.particle = readChoice())
                .whenElement("sequence", () -> result.particle = readSequence())
                .whenElement("group", () -> result.particle = readGroup())
                .whenElement("attribute", () -> result.attributeItems.add(readAttribute()))
                .whenElement("attributeGroup", () -> result.attributeItems.add(readAttributeGroup()))
                .whenElement("anyAttribute", () -> result.anyAttribute = readAnyAttribute())
                .process();

        check(reader.isEndElement(NAMESPACE, "extension"), "Expected end of extension element");
        reader.nextTag();
        return result;
    }

    private XsdFacet readFacet(String facetName, Class<? extends XsdFacet> facetClass)
    {
        check(reader.isStartElement(NAMESPACE, facetName), "Expected start of " + facetName + " element");
        XsdFacet result;
        try
        {
            result = readXsdObject(facetClass.getConstructor().newInstance());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        result.value = reader.getAttributeValue("value");
        result.fixed = xsdBoolean.parse(reader.getAttributeValueOrDefault("fixed", "false"));

        reader.nextTag();
        new ChildrenHandler<>(result, facetName).process();

        check(reader.isEndElement(NAMESPACE, facetName), "Expected end of " + facetName + " element");
        reader.nextTag();
        return result;
    }

    private XsdGroup readGroup()
    {
        check(reader.isStartElement(NAMESPACE, "group"), "Expected start of group element");
        XsdGroup result = readXsdObject(new XsdGroup());
        result.name = reader.getAttributeValue("name");
        result.ref = xsdQName.parse(reader.getAttributeValue("ref"));
        result.minOccurs = minOccurs();
        result.maxOccurs = maxOccurs();

        reader.nextTag();
        new ChildrenHandler<>(result, "group")
                .whenElement("all", () -> result.particle = readAll())
                .whenElement("choice", () -> result.particle = readChoice())
                .whenElement("sequence", () -> result.particle = readSequence())
                .process();

        check(reader.isEndElement(NAMESPACE, "group"), "Expected end of group element");
        reader.nextTag();
        return result;
    }

    private XsdImport readImport()
    {
        check(reader.isStartElement(NAMESPACE, "import"), "Expected start of import element");
        XsdImport result = readXsdObject(new XsdImport());
        result.schemaLocation = reader.getAttributeValue("schemaLocation");
        result.namespace = reader.getAttributeValue("namespace");

        reader.nextTag();
        new ChildrenHandler<>(result, "import").process();

        check(reader.isEndElement(NAMESPACE, "import"), "Expected end of import element");
        reader.nextTag();
        return result;
    }

    private XsdInclude readInclude()
    {
        check(reader.isStartElement(NAMESPACE, "include"), "Expected start of include element");
        XsdInclude result = readXsdObject(new XsdInclude());
        result.schemaLocation = reader.getAttributeValue("schemaLocation");

        reader.nextTag();
        new ChildrenHandler<>(result, "include").process();

        check(reader.isEndElement(NAMESPACE, "include"), "Expected end of include element");
        reader.nextTag();
        return result;
    }

    private XsdList readList()
    {
        check(reader.isStartElement(NAMESPACE, "list"), "Expected start of list element");
        XsdList result = readXsdObject(new XsdList());
        result.itemTypeName = xsdQName.parse(reader.getAttributeValue("itemType"));

        reader.nextTag();
        new ChildrenHandler<>(result, "list")
                .whenElement("simpleType", () -> result.itemType = readSimpleType())
                .process();

        check(reader.isEndElement(NAMESPACE, "list"), "Expected end of list element");
        reader.nextTag();
        return result;
    }

    private XsdRedefine readRedefine()
    {
        check(reader.isStartElement(NAMESPACE, "redefine"), "Expected start of redefine element");
        XsdRedefine result = readXsdObject(new XsdRedefine());
        result.schemaLocation = reader.getAttributeValue("schemaLocation");

        reader.nextTag();
        new ChildrenHandler<>(result, "redefine")
                .whenElement("complexType", () -> result.items.add(readComplexType()))
                .whenElement("simpleType", () -> result.items.add(readSimpleType()))
                .whenElement("group", () -> result.items.add(readGroup()))
                .whenElement("attributeGroup", () -> result.items.add(readAttributeGroup()))
                .process();

        check(reader.isEndElement(NAMESPACE, "redefine"), "Expected end of redefine element");
        reader.nextTag();
        return result;
    }

    private XsdRestriction readRestriction()
    {
        check(reader.isStartElement(NAMESPACE, "restriction"), "Expected start of restriction element");
        XsdRestriction result = readXsdObject(new XsdRestriction());
        result.baseTypeName = xsdQName.parse(reader.getAttributeValue("base"));
        result.facets = new ArrayList<>();
        result.attributeItems = new ArrayList<>();

        reader.nextTag();
        new ChildrenHandler<>(result, "restriction")
                .whenElement("simpleType", () -> result.baseType = readSimpleType())
                .whenElement("minExclusive", () -> result.facets.add(readFacet("minExclusive", XsdMinExclusive.class)))
                .whenElement("minInclusive", () -> result.facets.add(readFacet("minInclusive", XsdMinInclusive.class)))
                .whenElement("maxExclusive", () -> result.facets.add(readFacet("maxExclusive", XsdMaxExclusive.class)))
                .whenElement("maxInclusive", () -> result.facets.add(readFacet("maxInclusive", XsdMaxInclusive.class)))
                .whenElement("totalDigits", () -> result.facets.add(readFacet("totalDigits", XsdTotalDigits.class)))
                .whenElement("fractionDigits", () -> result.facets.add(readFacet("fractionDigits", XsdFractionDigits.class)))
                .whenElement("length", () -> result.facets.add(readFacet("length", XsdLength.class)))
                .whenElement("minLength", () -> result.facets.add(readFacet("minLength", XsdMinLength.class)))
                .whenElement("maxLength", () -> result.facets.add(readFacet("maxLength", XsdMaxLength.class)))
                .whenElement("enumeration", () -> result.facets.add(readFacet("enumeration", XsdEnumeration.class)))
                .whenElement("whiteSpace", () -> result.facets.add(readFacet("whiteSpace", XsdWhiteSpace.class)))
                .whenElement("pattern", () -> result.facets.add(readFacet("pattern", XsdPattern.class)))
                .whenElement("all", () -> result.particle = readAll())
                .whenElement("choice", () -> result.particle = readChoice())
                .whenElement("sequence", () -> result.particle = readSequence())
                .whenElement("group", () -> result.particle = readGroup())
                .whenElement("attribute", () -> result.attributeItems.add(readAttribute()))
                .whenElement("attributeGroup", () -> result.attributeItems.add(readAttributeGroup()))
                .whenElement("anyAttribute", () -> result.anyAttribute = readAnyAttribute())
                .process();

        check(reader.isEndElement(NAMESPACE, "restriction"), "Expected end of restriction element");
        reader.nextTag();
        return result;
    }

    private XsdSchema readSchema()
    {
        check(reader.isStartElement(NAMESPACE, "schema"), "Expected start of schema element");
        String targetNamespace = reader.getAttributeValue("targetNamespace");

        XsdSchema result = readXsdObject(new XsdSchema());
        result.targetNamespace = targetNamespace;
        result.elementFormDefault = xsdForm("elementFormDefault");
        result.attributeFormDefault = xsdForm("attributeFormDefault");
        result.blockDefault = xsdDerivationType("blockDefault", XsdDerivationType.EXTENSION_RESTRICTION_SUBSTITUTION);
        result.finalDefault = xsdDerivationType("finalDefault", XsdDerivationType.EXTENSION_RESTRICTION);
        result.version = reader.getAttributeValue("version");
        result.items = new ArrayList<>();
        result.externals = new ArrayList<>();

        reader.nextTag();
        new ChildrenHandler<>(result, "schema")
                .whenElement("complexType", () -> result.items.add(readComplexType()))
                .whenElement("simpleType", () -> result.items.add(readSimpleType()))
                .whenElement("element", () -> result.items.add(readElement()))
                .whenElement("group", () -> result.items.add(readGroup()))
                .whenElement("attribute", () -> result.items.add(readAttribute()))
                .whenElement("attributeGroup", () -> result.items.add(readAttributeGroup()))
                .whenElement("import", () -> result.items.add(readImport()))
                .whenElement("include", () -> result.items.add(readInclude()))
                .whenElement("redefine", () -> result.items.add(readRedefine()))
                .process();

        check(reader.isEndElement(NAMESPACE, "schema"), "Expected end of schema element");
        reader.nextTag();
        return result;
    }

    private XsdSequence readSequence()
    {
        check(reader.isStartElement(NAMESPACE, "sequence"), "Expected start of sequence element");
        XsdSequence result = readXsdObject(new XsdSequence());
        result.minOccurs = minOccurs();
        result.maxOccurs = maxOccurs();

        reader.nextTag();
        new ChildrenHandler<>(result, "sequence")
                .whenElement("element", () -> result.items.add(readElement()))
                .whenElement("choice", () -> result.items.add(readChoice()))
                .whenElement("sequence", () -> result.items.add(readSequence()))
                .whenElement("group", () -> result.items.add(readGroup()))
                .whenElement("any", () -> result.items.add(readAny()))
                .process();

        check(reader.isEndElement(NAMESPACE, "sequence"), "Expected end of sequence element");
        reader.nextTag();
        return result;
    }

    private XsdSimpleContent readSimpleContent()
    {
        check(reader.isStartElement(NAMESPACE, "simpleContent"), "Expected start of simpleContent element");
        XsdSimpleContent result = readXsdObject(new XsdSimpleContent());

        reader.nextTag();
        new ChildrenHandler<>(result, "simpleContent")
                .whenElement("extension", () -> result.derivation = readExtension())
                .whenElement("restriction", () -> result.derivation = readRestriction())
                .process();

        check(reader.isEndElement(NAMESPACE, "simpleContent"), "Expected end of simpleContent element");
        reader.nextTag();
        return result;
    }

    private XsdSimpleType readSimpleType()
    {
        check(reader.isStartElement(NAMESPACE, "simpleType"), "Expected start of simpleType element");
        XsdSimpleType result = readXsdObject(new XsdSimpleType());
        result.name = reader.getAttributeValue("name");
        result.ref = xsdQName.parse(reader.getAttributeValue("ref"));
        result._final = xsdDerivationType("final", XsdDerivationType.LIST_UNION_RESTRICTION);

        reader.nextTag();
        new ChildrenHandler<>(result, "simpleType")
                .whenElement("restriction", () -> result.derivation = readRestriction())
                .whenElement("list", () -> result.derivation = readList())
                .whenElement("union", () -> result.derivation = readUnion())
                .process();

        check(reader.isEndElement(NAMESPACE, "simpleType"), "Expected end of simpleType element");
        reader.nextTag();
        return result;
    }

    private XsdUnion readUnion()
    {
        check(reader.isStartElement(NAMESPACE, "union"), "Expected start of union element");
        XsdUnion result = readXsdObject(new XsdUnion());
        result.memberTypeNames = xsdQNames.parse(reader.getAttributeValue("memberTypes"));
        result.memberTypes = new ArrayList<>();

        reader.nextTag();
        new ChildrenHandler<>(result, "union")
                .whenElement("simpleType", () -> result.memberTypes.add(readSimpleType()))
                .process();

        check(reader.isEndElement(NAMESPACE, "union"), "Expected end of union element");
        reader.nextTag();
        return result;
    }

    private <T extends XsdObject> T readXsdObject(T xsdObject)
    {
        xsdObject.id = reader.getAttributeValue("id");
        xsdObject.sourceLineNumber = reader.getLocation().getLineNumber();
        xsdObject.sourceColumnNumber = reader.getLocation().getColumnNumber();
        return xsdObject;
    }

    private long minOccurs()
    {
        return xsdNonNegativeInteger.parse(reader.getAttributeValueOrDefault("minOccurs", "1")).longValue();
    }

    private Long maxOccurs()
    {
        String text = xsdToken.parse(reader.getAttributeValueOrDefault("maxOccurs", "1"));
        return (text.equalsIgnoreCase("unbounded"))
               ? null
               : xsdNonNegativeInteger.parse(text).longValue();
    }

    private List<XsdDerivationType> xsdDerivationType(String attributeName, List<XsdDerivationType> all)
    {
        String value = reader.getAttributeValue(attributeName);
        if (value == null)
        {
            return Collections.emptyList();
        }
        else if (value.equals("#all"))
        {
            return all;
        }
        else
        {
            List<XsdDerivationType> result = new ArrayList<>();
            for (String item : value.split(" "))
            {
                XsdDerivationType type;
                try
                {
                    type = XsdDerivationType.valueOf(item.toUpperCase());
                }
                catch (IllegalArgumentException e)
                {
                    throw error("Invalid derivation type for " + attributeName + ": " + item);
                }
                result.add(type);
            }
            return result;
        }
    }

    private XsdForm xsdForm(String attributeName)
    {
        String value = reader.getAttributeValue(attributeName);
        try
        {
            return value == null ? null : XsdForm.valueOf(value.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            throw error("Invalid form for " + attributeName + ": " + value);
        }
    }

    private SimpleTypeHandler<String> stringEnum(String... values)
    {
        List<Facet> facets = Stream.of(values).map(FacetType.ENUMERATION::of).collect(Collectors.toList());
        return simpleTypesContext.defineType(BuiltInDataTypes.XS_TOKEN, facets);
    }

    private void check(boolean test, String message)
    {
        if (!test)
        {
            throw error(message);
        }
    }

    private ExternalFormatSchemaException error(String message)
    {
        Location location = reader.getLocation();
        return new ExternalFormatSchemaException(message + " [" + reader.describe() + "]", location.getLineNumber(), location.getColumnNumber());
    }

    private class ChildrenHandler<T extends XsdObject>
    {
        private final T parent;
        private final String parentElementName;
        private final Map<String, Runnable> childHandlers = new LinkedHashMap<>();

        ChildrenHandler(T parent, String parentElementName)
        {
            this.parent = parent;
            this.parentElementName = parentElementName;
        }

        ChildrenHandler<T> whenElement(String name, Runnable process)
        {
            this.childHandlers.put(name, process);
            return this;
        }

        void process()
        {
            while (reader.isStartElement())
            {
                if (!reader.getName().getNamespaceURI().equals(NAMESPACE))
                {
                    reader.skipElement();
                }
                else if (reader.isStartElement(NAMESPACE, "annotation"))
                {
                    XsdAnnotated annotated = (XsdAnnotated) parent;
                    XsdAnnotation annotation = readAnnotation();
                    if (annotated.annotation == null)
                    {
                        annotated.annotation = annotation;
                    }
                    else
                    {
                        annotated.annotation.items.addAll(annotation.items);
                    }
                }
                else
                {
                    String elementName = childHandlers.keySet().stream()
                                                      .filter(name -> reader.isStartElement(NAMESPACE, name))
                                                      .findFirst()
                                                      .orElseThrow(() -> error("Unknown child of " + parentElementName));
                    childHandlers.entrySet().stream()
                                 .filter(e -> e.getKey().equals(elementName))
                                 .findFirst()
                                 .orElseThrow(() -> error("Unknown child of " + parentElementName))
                                 .getValue()
                                 .run();
                }
            }
        }
    }
}
