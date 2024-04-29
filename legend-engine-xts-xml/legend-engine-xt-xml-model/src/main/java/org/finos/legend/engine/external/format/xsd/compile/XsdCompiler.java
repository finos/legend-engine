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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdAll;
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
import org.finos.legend.engine.external.format.xsd.compile.parseTree.visit.XsdObjectVisitor;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xml_QName;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xml_QName_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAll;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAll_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnnotated;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnnotation;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnnotationItem;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnnotation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAny;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAnyType;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAny_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAppInfo_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAttribute;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAttributeGroup;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAttributeGroup_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdAttribute_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdChoice;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdChoice_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdComplexContent;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdComplexContent_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdComplexType;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdComplexType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdContentDerivation;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdContentModel;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdDocumentation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdElement;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdElement_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdEnumeration;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdEnumeration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdExtension;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdExtension_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdExternalSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdFacet;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdFractionDigits;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdFractionDigits_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdGroup;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdGroupParticle;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdGroup_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdLength;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdLength_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdList;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdList_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMaxExclusive;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMaxExclusive_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMaxInclusive;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMaxInclusive_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMaxLength;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMaxLength_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMinExclusive;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMinExclusive_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMinInclusive;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMinInclusive_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMinLength;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdMinLength_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdObject;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdParticle;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdPattern;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdPattern_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdRestriction;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdRestriction_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSchema_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSequence;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSequence_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSimpleContent;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSimpleContent_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSimpleTypeDerivation;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdTotalDigits;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdTotalDigits_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdUnion;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdUnion_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdWhiteSpace;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdWhiteSpace_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Consumer;

public class XsdCompiler
{
    private static final String SCHEMA_LOCATION_TAG = "schemaLocation";
    private static final String METAMODEL_PACKAGE = "meta::external::format::xml::metamodel::xsd::";

    private final XsdSchema schema;
    private final ExternalSchemaCompileContext context;
    private final Map<String, XsdSchema> externalSchemas = new HashMap<>();
    private final String targetNamespace;
    private final Map<String, String> namespaceToPrefix = new TreeMap<>();

    private String mainSchemaLocation;
    private XsdForm attributeFormDefault;
    private XsdForm elementFormDefault;
    private List<XsdDerivationType> blockDefault;
    private List<XsdDerivationType> finalDefault;

    public XsdCompiler(ExternalSchemaCompileContext context)
    {
        this.schema = new XsdParser(context.getContent()).parse();
        this.targetNamespace = schema.targetNamespace == null
                ? XMLConstants.NULL_NS_URI
                : schema.targetNamespace;
        this.namespaceToPrefix.put(XMLConstants.W3C_XML_SCHEMA_NS_URI, "xs");
        this.namespaceToPrefix.put(XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX);
        this.namespaceToPrefix.put(this.targetNamespace, XMLConstants.DEFAULT_NS_PREFIX);
        this.context = context;
    }

    public Root_meta_external_format_xml_metamodel_xsd_XsdSchema compile()
    {
        mainSchemaLocation = context.getLocation();
        if (mainSchemaLocation == null)
        {
            throw new ExternalFormatSchemaException("Location must be specified for XSD schemas");
        }

        MainVisitor mainVisitor = new MainVisitor(context);
        schema.accept(mainVisitor);
        return mainVisitor.compiledSchema;
    }

    private void ensureNamespace(String uri, String suggestedPrefix)
    {
        if (!namespaceToPrefix.containsKey(uri))
        {
            if (uri.equals(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI))
            {
                ensureNamespace(uri, "xsi");
            }
            else if (suggestedPrefix == null)
            {
                int num = 1;
                while (namespaceToPrefix.containsValue("ns" + num))
                {
                    num++;
                }
                namespaceToPrefix.put("ns" + num, suggestedPrefix);
            }
            else if (!namespaceToPrefix.containsValue(suggestedPrefix))
            {
                namespaceToPrefix.put(uri, suggestedPrefix);
            }
            else
            {
                int num = 1;
                while (namespaceToPrefix.containsValue(suggestedPrefix + num))
                {
                    num++;
                }
                namespaceToPrefix.put(suggestedPrefix + num, suggestedPrefix);
            }
        }
    }

    private String prefixFor(String uri)
    {
        return prefixFor(uri, null);
    }

    private String prefixFor(String uri, String suggestedPrefix)
    {
        ensureNamespace(uri, suggestedPrefix);
        return namespaceToPrefix.get(uri);
    }

    private class MainVisitor implements XsdObjectVisitor
    {
        private Stack<Consumer<Root_meta_external_format_xml_metamodel_xsd_XsdObject>> objectConsumer = new Stack<>();
        private Stack<Consumer<Root_meta_external_format_xml_metamodel_xsd_XsdAnnotation>> annotationConsumer = new Stack<>();
        private Root_meta_external_format_xml_metamodel_xsd_XsdSchema compiledSchema;
        private List<String> schemaLocationsIncluded = new ArrayList<>();
        private Stack<XsdInclude> including = new Stack<>();
        private ExternalSchemaCompileContext context;

        public MainVisitor(ExternalSchemaCompileContext context)
        {
            this.context = context;
        }

        @Override
        public void visitBefore(XsdAll all)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdAll compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdAll_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAll"))
                    ._minOccurs(all.minOccurs)
                    ._maxOccurs(all.maxOccurs);
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._itemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdParticle) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdAll all)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdAnnotation annotation)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdAnnotation compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdAnnotation_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAnnotation"));
            annotationConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._itemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdAnnotationItem) o));
        }

        @Override
        public void visitAfter(XsdAnnotation annotation)
        {
            objectConsumer.pop();
        }

        @Override
        public void visitBefore(XsdAny any)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdAny compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdAny_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAny"))
                    ._minOccurs(any.minOccurs)
                    ._maxOccurs(any.maxOccurs)
                    ._namespace(any.namespace == null
                            ? Lists.mutable.empty()
                            : ListIterate.collect(any.namespace, s -> s))
                    ._processContents(contentProceesing(any.processContents));
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdAny any)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdAnyAttribute anyAttribute)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAnyAttribute"))
                    ._namespace(anyAttribute.namespace == null
                            ? Lists.mutable.empty()
                            : ListIterate.collect(anyAttribute.namespace, s -> s))
                    ._processContents(contentProceesing(anyAttribute.processContents));
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdAnyAttribute anyAttribute)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visit(XsdAppInfo appInfo)
        {
            objectConsumer.peek().accept(new Root_meta_external_format_xml_metamodel_xsd_XsdAppInfo_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAppInfo")));
        }

        @Override
        public void visitBefore(XsdAttribute attribute)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdAttribute compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdAttribute_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAttribute"))
                    ._name(qName(attribute.name))
                    ._ref(qName(attribute.ref)) // TODO Resolve reference
                    ._defaultValue(attribute.defaultValue)
                    ._fixedValue(attribute.fixedValue)
                    ._form(form(attribute.form == null ? attributeFormDefault : attribute.form))
                    ._typeName(qName(attribute.typeName)) // TODO Resolve reference
                    ._use(attribute.use == null
                            ? use("optional")
                            : use(attribute.use));
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._type((Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType) o));
            pushAnnotationConsumer(compiled);
            // TODO Resolve type from ref/child?
        }

        @Override
        public void visitAfter(XsdAttribute attribute)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdAttributeGroup attributeGroup)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdAttributeGroup compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdAttributeGroup_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdAttributeGroup"))
                    ._name(qName(attributeGroup.name))
                    ._ref(qName(attributeGroup.ref)); // TODO Resolve reference
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o ->
            {
                if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute)
                {
                    compiled._anyAttribute((Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute) o);
                }
                else
                {
                    compiled._itemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem) o);
                }
            });
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdAttributeGroup attributeGroup)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdChoice choice)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdChoice compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdChoice_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdChoice"))
                    ._minOccurs(choice.minOccurs)
                    ._maxOccurs(choice.maxOccurs);
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._itemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdParticle) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdChoice choice)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdComplexContent complexContent)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdComplexContent compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdComplexContent_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdComplexContent"))
                    ._mixed(complexContent.mixed);
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._derivation((Root_meta_external_format_xml_metamodel_xsd_XsdContentDerivation) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdComplexContent complexContent)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdComplexType complexType)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdComplexType compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdComplexType_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdComplexType"))
                    ._name(qName(complexType.name))
                    ._ref(qName(complexType.ref)) // TODO Resolve reference
                    ._abstract(complexType._abstract)
                    ._mixed(complexType.mixed)
                    ._block(derivationTypes(complexType.block, blockDefault, XsdDerivationType.EXTENSION_RESTRICTION, complexType))
                    ._final(derivationTypes(complexType._final, finalDefault, XsdDerivationType.EXTENSION_RESTRICTION, complexType));
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o ->
            {
                if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute)
                {
                    compiled._anyAttribute((Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute) o);
                }
                else if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem)
                {
                    compiled._attributeItemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem) o);
                }
                else if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdParticle)
                {
                    compiled._particle((Root_meta_external_format_xml_metamodel_xsd_XsdParticle) o);
                }
                else
                {
                    compiled._contentModel((Root_meta_external_format_xml_metamodel_xsd_XsdContentModel) o);
                }
            });
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdComplexType complexType)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visit(XsdDocumentation documentation)
        {
            objectConsumer.peek().accept(
                    new Root_meta_external_format_xml_metamodel_xsd_XsdDocumentation_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdDocumentation"))
                            ._value(documentation.value)
                            ._language(documentation.language)
            );
        }

        @Override
        public void visitBefore(XsdElement element)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdElement compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdElement_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdElement"))
                    ._name(qName(element.name))
                    ._ref(qName(element.ref)) // TODO Resolve reference
                    ._minOccurs(element.minOccurs)
                    ._maxOccurs(element.maxOccurs)
                    ._defaultValue(element.defaultValue)
                    ._fixedValue(element.fixedValue)
                    ._form(form(element.form == null ? elementFormDefault : element.form))
                    ._block(derivationTypes(element.block, blockDefault, XsdDerivationType.EXTENSION_RESTRICTION_SUBSTITUTION, element))
                    ._final(derivationTypes(element._final, finalDefault, XsdDerivationType.EXTENSION_RESTRICTION, element))
                    ._abstract(element._abstract)
                    ._nilable(element.nilable)
                    ._typeName(qName(element.typeName)) // TODO Resolve reference
                    ._substitutionGroup(qName(element.substitutionGroup));
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._type((Root_meta_external_format_xml_metamodel_xsd_XsdAnyType) o));
            pushAnnotationConsumer(compiled);
            // TODO Resolve type from ref/child?
        }

        @Override
        public void visitAfter(XsdElement element)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdEnumeration enumeration)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdEnumeration compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdEnumeration_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdEnumeration"))
                    ._value(enumeration.value)
                    ._fixed(enumeration.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdEnumeration enumeration)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdExtension extension)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdExtension compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdExtension_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdExtension"))
                    ._baseTypeName(qName(extension.baseTypeName)); // TODO Resolve reference
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o ->
            {
                if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute)
                {
                    compiled._anyAttribute((Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute) o);
                }
                else if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem)
                {
                    compiled._attributeItemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem) o);
                }
                else
                {
                    compiled._particle((Root_meta_external_format_xml_metamodel_xsd_XsdParticle) o);
                }
            });
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdExtension extension)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdFractionDigits fractionDigits)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdFractionDigits compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdFractionDigits_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdFractionDigits"))
                    ._value(fractionDigits.value)
                    ._fixed(fractionDigits.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdFractionDigits fractionDigits)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdGroup group)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdGroup compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdGroup_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdGroup"))
                    ._name(qName(group.name))
                    ._ref(qName(group.ref)) // TODO Resolve reference
                    ._minOccurs(group.minOccurs)
                    ._maxOccurs(group.maxOccurs);
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._particle((Root_meta_external_format_xml_metamodel_xsd_XsdGroupParticle) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdGroup group)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdImport _import)
        {

        }

        @Override
        public void visitAfter(XsdImport _import)
        {

        }

        @Override
        public void visitBefore(XsdInclude include)
        {
            if (!schemaLocationsIncluded.contains(include.schemaLocation) && !mainSchemaLocation.equals(include.schemaLocation))
            {
                XsdSchema included = externalSchemas.computeIfAbsent(include.schemaLocation, loc -> new XsdParser(context.getContent(loc)).parse());
                if (included == null)
                {
                    throw new ExternalFormatSchemaException("No schema found for location: " + include.schemaLocation, include.sourceLineNumber, include.sourceColumnNumber);
                }
                if (included.targetNamespace != null && !included.targetNamespace.equals(targetNamespace))
                {
                    throw new ExternalFormatSchemaException("Included schema for location: " + include.schemaLocation + " has a different targetNamespace", include.sourceLineNumber, include.sourceColumnNumber);
                }
                schemaLocationsIncluded.add(include.schemaLocation);
                including.push(include);
                included.accept(this);
            }
            annotationConsumer.push(a ->
            {
            });
        }

        @Override
        public void visitAfter(XsdInclude include)
        {
            if (!including.isEmpty() && include.equals(including.peek()))
            {
                including.pop();
            }
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdLength length)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdLength compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdLength_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdLength"))
                    ._value(length.value)
                    ._fixed(length.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdLength length)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdList list)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdList compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdList_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdList"))
                    ._itemTypeName(qName(list.itemTypeName)); // TODO Resolve reference
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._itemType((Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType) o));
            pushAnnotationConsumer(compiled);
            // TODO Resolve type from ref/child?
        }

        @Override
        public void visitAfter(XsdList list)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdMaxExclusive maxExclusive)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdMaxExclusive compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdMaxExclusive_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdMaxExclusive"))
                    ._value(maxExclusive.value)
                    ._fixed(maxExclusive.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdMaxExclusive maxExclusive)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdMaxInclusive maxInclusive)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdMaxInclusive compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdMaxInclusive_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdMaxInclusive"))
                    ._value(maxInclusive.value)
                    ._fixed(maxInclusive.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdMaxInclusive maxInclusive)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdMaxLength maxLength)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdMaxLength compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdMaxLength_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdMaxLength"))
                    ._value(maxLength.value)
                    ._fixed(maxLength.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdMaxLength maxLength)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdMinExclusive minExclusive)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdMinExclusive compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdMinExclusive_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdMinExclusive"))
                    ._value(minExclusive.value)
                    ._fixed(minExclusive.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdMinExclusive minExclusive)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdMinInclusive minInclusive)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdMinInclusive compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdMinInclusive_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdMinInclusive"))
                    ._value(minInclusive.value)
                    ._fixed(minInclusive.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdMinInclusive minInclusive)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdMinLength minLength)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdMinLength compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdMinLength_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdMinLength"))
                    ._value(minLength.value)
                    ._fixed(minLength.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdMinLength minLength)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdPattern pattern)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdPattern compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdPattern_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdPattern"))
                    ._value(pattern.value)
                    ._fixed(pattern.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdPattern pattern)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdRedefine redefine)
        {

        }

        @Override
        public void visitAfter(XsdRedefine redefine)
        {

        }

        @Override
        public void visitBefore(XsdRestriction restriction)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdRestriction compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdRestriction_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdRestriction"))
                    ._baseTypeName(qName(restriction.baseTypeName)); // TODO Resolve reference
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o ->
            {
                if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute)
                {
                    compiled._anyAttribute((Root_meta_external_format_xml_metamodel_xsd_XsdAnyAttribute) o);
                }
                else if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem)
                {
                    compiled._attributeItemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdAttributeItem) o);
                }
                else if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdFacet)
                {
                    compiled._facetsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdFacet) o);
                }
                else if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdParticle)
                {
                    compiled._particle((Root_meta_external_format_xml_metamodel_xsd_XsdParticle) o);
                }
                else
                {
                    compiled._baseType((Root_meta_external_format_xml_metamodel_xsd_XsdAnyType) o);
                }
            });
            pushAnnotationConsumer(compiled);
            // TODO Resolve type from ref/child
        }

        @Override
        public void visitAfter(XsdRestriction restriction)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdSchema schema)
        {
            if (including.isEmpty())
            {
                blockDefault = schema.blockDefault;
                finalDefault = schema.finalDefault;
                elementFormDefault = schema.elementFormDefault == null ? XsdForm.UNQUALIFIED : schema.elementFormDefault;
                attributeFormDefault = schema.attributeFormDefault == null ? XsdForm.UNQUALIFIED : schema.attributeFormDefault;

                compiledSchema = new Root_meta_external_format_xml_metamodel_xsd_XsdSchema_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdSchema"))
                        ._targetNamespace(targetNamespace)
                        ._elementFormDefault(form(elementFormDefault))
                        ._attributeFormDefault(form(attributeFormDefault))
                        ._blockDefault(derivationTypes(schema.blockDefault, Collections.emptyList(), XsdDerivationType.EXTENSION_RESTRICTION_SUBSTITUTION, schema))
                        ._finalDefault(derivationTypes(schema.finalDefault, Collections.emptyList(), XsdDerivationType.EXTENSION_RESTRICTION, schema))
                        ._version(schema.version);
                objectConsumer.push(o ->
                {
                    if (o instanceof Root_meta_external_format_xml_metamodel_xsd_XsdExternalSchema)
                    {
                        compiledSchema._externalsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdExternalSchema) o);
                    }
                    else
                    {
                        compiledSchema._itemsAdd(o);
                    }
                });
                pushAnnotationConsumer(compiledSchema);
            }
        }

        @Override
        public void visitAfter(XsdSchema schema)
        {
            if (including.isEmpty())
            {
                objectConsumer.pop();
                popAnnotationConsumer();
            }
        }

        @Override
        public void visitBefore(XsdSequence sequence)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdSequence compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdSequence_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdSequence"))
                    ._minOccurs(sequence.minOccurs)
                    ._maxOccurs(sequence.maxOccurs);
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._itemsAdd((Root_meta_external_format_xml_metamodel_xsd_XsdParticle) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdSequence sequence)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdSimpleContent simpleContent)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdSimpleContent compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdSimpleContent_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdSimpleContent"));
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._derivation((Root_meta_external_format_xml_metamodel_xsd_XsdContentDerivation) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdSimpleContent simpleContent)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdSimpleType simpleType)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdSimpleType"))
                    ._name(qName(simpleType.name))
                    ._ref(qName(simpleType.ref)) // TODO Resolve reference
                    ._final(derivationTypes(simpleType._final, Collections.emptyList(), XsdDerivationType.LIST_UNION_RESTRICTION, simpleType));
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._derivation((Root_meta_external_format_xml_metamodel_xsd_XsdSimpleTypeDerivation) o));
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdSimpleType simpleType)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdTotalDigits totalDigits)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdTotalDigits compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdTotalDigits_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdTotalDigits"))
                    ._value(totalDigits.value)
                    ._fixed(totalDigits.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdTotalDigits totalDigits)
        {
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdUnion union)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdUnion compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdUnion_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdUnion"));
            union.memberTypeNames.forEach(m -> compiled._memberTypeNamesAdd(qName(m))); // TODO Resolve reference
            objectConsumer.peek().accept(compiled);
            objectConsumer.push(o -> compiled._memberTypesAdd((Root_meta_external_format_xml_metamodel_xsd_XsdSimpleType) o));
            pushAnnotationConsumer(compiled);
            // TODO Resolve type from refs/children?
        }

        @Override
        public void visitAfter(XsdUnion union)
        {
            objectConsumer.pop();
            popAnnotationConsumer();
        }

        @Override
        public void visitBefore(XsdWhiteSpace whiteSpace)
        {
            Root_meta_external_format_xml_metamodel_xsd_XsdWhiteSpace compiled = new Root_meta_external_format_xml_metamodel_xsd_XsdWhiteSpace_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xsd::XsdWhiteSpace"))
                    ._value(whiteSpace.value)
                    ._fixed(whiteSpace.fixed);
            objectConsumer.peek().accept(compiled);
            pushAnnotationConsumer(compiled);
        }

        @Override
        public void visitAfter(XsdWhiteSpace whiteSpace)
        {
            popAnnotationConsumer();
        }

        private void pushAnnotationConsumer(Root_meta_external_format_xml_metamodel_xsd_XsdAnnotated annotated)
        {
            annotationConsumer.push(annotated::_annotation);
        }

        private void popAnnotationConsumer()
        {
            annotationConsumer.pop();
        }

        private Root_meta_external_format_xml_metamodel_xml_QName qName(QName qName)
        {
            if (qName == null)
            {
                return null;
            }
            else if (XMLConstants.NULL_NS_URI.equals(qName.getNamespaceURI()))
            {
                return new Root_meta_external_format_xml_metamodel_xml_QName_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xml::QName"))._localPart(qName.getLocalPart())
                        ._namespace(targetNamespace)
                        ._prefix(prefixFor(targetNamespace, qName.getPrefix()));
            }
            else
            {
                return new Root_meta_external_format_xml_metamodel_xml_QName_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xml::QName"))._localPart(qName.getLocalPart())
                        ._namespace(qName.getNamespaceURI())
                        ._prefix(prefixFor(qName.getNamespaceURI(), qName.getPrefix()));
            }
        }

        private Root_meta_external_format_xml_metamodel_xml_QName qName(String name)
        {
            return name == null
                    ? null
                    : new Root_meta_external_format_xml_metamodel_xml_QName_Impl("", null, context.getPureModel().getClass("meta::external::format::xml::metamodel::xml::QName"))._localPart(name)
                    ._namespace(targetNamespace)
                    ._prefix(prefixFor(targetNamespace));
        }

        private Enum contentProceesing(XsdContentProcessing contentProcessing)
        {
            return contentProcessing == null
                    ? null
                    : context.getPureModel().getEnumValue(METAMODEL_PACKAGE + "XsdContentProcessing", contentProcessing.toString());
        }

        private Enum use(XsdUse use)
        {
            return use == null
                    ? null
                    : use(use.toString());
        }

        private Enum use(String use)
        {
            return context.getPureModel().getEnumValue(METAMODEL_PACKAGE + "XsdUse", use.toUpperCase());
        }

        private Enum form(XsdForm form)
        {
            return form == null
                    ? null
                    : context.getPureModel().getEnumValue(METAMODEL_PACKAGE + "XsdForm", form.toString());
        }

        private RichIterable<? extends Enum> derivationTypes(List<XsdDerivationType> derivationTypes, List<XsdDerivationType> dflt, List<XsdDerivationType> allowed, XsdObject object)
        {
            List<XsdDerivationType> resolved;

            if (derivationTypes.isEmpty())
            {
                resolved = ListIterate.select(dflt, allowed::contains);
            }
            else
            {
                MutableList<XsdDerivationType> invalid = ListIterate.select(derivationTypes, r -> !allowed.contains(r));
                if (!invalid.isEmpty())
                {
                    throw new ExternalFormatSchemaException("Invalid derivation types: " + invalid.makeString(","), object.sourceLineNumber, object.sourceColumnNumber);
                }
                resolved = derivationTypes;
            }

            return ListIterate.collect(resolved, this::derivationType);
        }

        private Enum derivationType(XsdDerivationType derivationType)
        {
            return derivationType == null
                    ? null
                    : context.getPureModel().getEnumValue(METAMODEL_PACKAGE + "XsdDerivationType", derivationType.toString());
        }
    }
}
