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

package org.finos.legend.engine.external.format.xsd.compile.parseTree.visit;

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
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdDocumentation;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdElement;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdEnumeration;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdExtension;
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
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdPattern;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdRedefine;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdRestriction;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSchema;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSequence;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSimpleContent;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSimpleType;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdTotalDigits;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdUnion;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdWhiteSpace;

public interface XsdObjectVisitor
{
    void visitBefore(XsdAll all);

    void visitAfter(XsdAll all);

    void visitBefore(XsdAnnotation annotation);

    void visitAfter(XsdAnnotation annotation);

    void visitBefore(XsdAny any);

    void visitAfter(XsdAny any);

    void visitBefore(XsdAnyAttribute anyAttribute);

    void visitAfter(XsdAnyAttribute anyAttribute);

    void visit(XsdAppInfo appInfo);

    void visitBefore(XsdAttribute attribute);

    void visitAfter(XsdAttribute attribute);

    void visitBefore(XsdAttributeGroup attributeGroup);

    void visitAfter(XsdAttributeGroup attributeGroup);

    void visitBefore(XsdChoice choice);

    void visitAfter(XsdChoice choice);

    void visitBefore(XsdComplexContent complexContent);

    void visitAfter(XsdComplexContent complexContent);

    void visitBefore(XsdComplexType complexType);

    void visitAfter(XsdComplexType complexType);

    void visit(XsdDocumentation documentation);

    void visitBefore(XsdElement element);

    void visitAfter(XsdElement element);

    void visitBefore(XsdEnumeration enumeration);

    void visitAfter(XsdEnumeration enumeration);

    void visitBefore(XsdExtension extension);

    void visitAfter(XsdExtension extension);

    void visitBefore(XsdFractionDigits fractionDigits);

    void visitAfter(XsdFractionDigits fractionDigits);

    void visitBefore(XsdGroup group);

    void visitAfter(XsdGroup group);

    void visitBefore(XsdImport _import);

    void visitAfter(XsdImport _import);

    void visitBefore(XsdInclude include);

    void visitAfter(XsdInclude include);

    void visitBefore(XsdLength length);

    void visitAfter(XsdLength length);

    void visitBefore(XsdList list);

    void visitAfter(XsdList list);

    void visitBefore(XsdMaxExclusive maxExclusive);

    void visitAfter(XsdMaxExclusive maxExclusive);

    void visitBefore(XsdMaxInclusive maxInclusive);

    void visitAfter(XsdMaxInclusive maxInclusive);

    void visitBefore(XsdMaxLength maxLength);

    void visitAfter(XsdMaxLength maxLength);

    void visitBefore(XsdMinExclusive minExclusive);

    void visitAfter(XsdMinExclusive minExclusive);

    void visitBefore(XsdMinInclusive minInclusive);

    void visitAfter(XsdMinInclusive minInclusive);

    void visitBefore(XsdMinLength minLength);

    void visitAfter(XsdMinLength minLength);

    void visitBefore(XsdPattern pattern);

    void visitAfter(XsdPattern pattern);

    void visitBefore(XsdRedefine redefine);

    void visitAfter(XsdRedefine redefine);

    void visitBefore(XsdRestriction restriction);

    void visitAfter(XsdRestriction restriction);

    void visitBefore(XsdSchema schema);

    void visitAfter(XsdSchema schema);

    void visitBefore(XsdSequence sequence);

    void visitAfter(XsdSequence sequence);

    void visitBefore(XsdSimpleContent simpleContent);

    void visitAfter(XsdSimpleContent simpleContent);

    void visitBefore(XsdSimpleType simpleType);

    void visitAfter(XsdSimpleType simpleType);

    void visitBefore(XsdTotalDigits totalDigits);

    void visitAfter(XsdTotalDigits totalDigits);

    void visitBefore(XsdUnion union);

    void visitAfter(XsdUnion union);

    void visitBefore(XsdWhiteSpace whiteSpace);

    void visitAfter(XsdWhiteSpace whiteSpace);
}
