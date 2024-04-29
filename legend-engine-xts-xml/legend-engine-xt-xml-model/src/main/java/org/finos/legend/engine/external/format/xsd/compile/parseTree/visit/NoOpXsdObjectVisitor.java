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

public class NoOpXsdObjectVisitor implements XsdObjectVisitor
{
    @Override
    public void visitBefore(XsdAll all)
    {
    }

    @Override
    public void visitAfter(XsdAll all)
    {
    }

    @Override
    public void visitBefore(XsdAnnotation annotation)
    {
    }

    @Override
    public void visitAfter(XsdAnnotation annotation)
    {
    }

    @Override
    public void visitBefore(XsdAny any)
    {
    }

    @Override
    public void visitAfter(XsdAny any)
    {
    }

    @Override
    public void visitBefore(XsdAnyAttribute anyAttribute)
    {
    }

    @Override
    public void visitAfter(XsdAnyAttribute anyAttribute)
    {
    }

    @Override
    public void visit(XsdAppInfo appInfo)
    {
    }

    @Override
    public void visitBefore(XsdAttribute attribute)
    {
    }

    @Override
    public void visitAfter(XsdAttribute attribute)
    {
    }

    @Override
    public void visitBefore(XsdAttributeGroup attributeGroup)
    {
    }

    @Override
    public void visitAfter(XsdAttributeGroup attributeGroup)
    {
    }

    @Override
    public void visitBefore(XsdChoice choice)
    {
    }

    @Override
    public void visitAfter(XsdChoice choice)
    {
    }

    @Override
    public void visitBefore(XsdComplexContent complexContent)
    {
    }

    @Override
    public void visitAfter(XsdComplexContent complexContent)
    {
    }

    @Override
    public void visitBefore(XsdComplexType complexType)
    {
    }

    @Override
    public void visitAfter(XsdComplexType complexType)
    {
    }

    @Override
    public void visit(XsdDocumentation documentation)
    {
    }

    @Override
    public void visitBefore(XsdElement element)
    {
    }

    @Override
    public void visitAfter(XsdElement element)
    {
    }

    @Override
    public void visitBefore(XsdEnumeration enumeration)
    {
    }

    @Override
    public void visitAfter(XsdEnumeration enumeration)
    {
    }

    @Override
    public void visitBefore(XsdExtension extension)
    {
    }

    @Override
    public void visitAfter(XsdExtension extension)
    {
    }

    @Override
    public void visitBefore(XsdFractionDigits fractionDigits)
    {
    }

    @Override
    public void visitAfter(XsdFractionDigits fractionDigits)
    {
    }

    @Override
    public void visitBefore(XsdGroup group)
    {
    }

    @Override
    public void visitAfter(XsdGroup group)
    {
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
    }

    @Override
    public void visitAfter(XsdInclude include)
    {
    }

    @Override
    public void visitBefore(XsdLength length)
    {
    }

    @Override
    public void visitAfter(XsdLength length)
    {
    }

    @Override
    public void visitBefore(XsdList list)
    {
    }

    @Override
    public void visitAfter(XsdList list)
    {
    }

    @Override
    public void visitBefore(XsdMaxExclusive maxExclusive)
    {
    }

    @Override
    public void visitAfter(XsdMaxExclusive maxExclusive)
    {
    }

    @Override
    public void visitBefore(XsdMaxInclusive maxInclusive)
    {
    }

    @Override
    public void visitAfter(XsdMaxInclusive maxInclusive)
    {
    }

    @Override
    public void visitBefore(XsdMaxLength maxLength)
    {
    }

    @Override
    public void visitAfter(XsdMaxLength maxLength)
    {
    }

    @Override
    public void visitBefore(XsdMinExclusive minExclusive)
    {
    }

    @Override
    public void visitAfter(XsdMinExclusive minExclusive)
    {
    }

    @Override
    public void visitBefore(XsdMinInclusive minInclusive)
    {
    }

    @Override
    public void visitAfter(XsdMinInclusive minInclusive)
    {
    }

    @Override
    public void visitBefore(XsdMinLength minLength)
    {
    }

    @Override
    public void visitAfter(XsdMinLength minLength)
    {
    }

    @Override
    public void visitBefore(XsdPattern pattern)
    {
    }

    @Override
    public void visitAfter(XsdPattern pattern)
    {
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
    }

    @Override
    public void visitAfter(XsdRestriction restriction)
    {
    }

    @Override
    public void visitBefore(XsdSchema schema)
    {
    }

    @Override
    public void visitAfter(XsdSchema schema)
    {
    }

    @Override
    public void visitBefore(XsdSequence sequence)
    {
    }

    @Override
    public void visitAfter(XsdSequence sequence)
    {
    }

    @Override
    public void visitBefore(XsdSimpleContent simpleContent)
    {
    }

    @Override
    public void visitAfter(XsdSimpleContent simpleContent)
    {
    }

    @Override
    public void visitBefore(XsdSimpleType simpleType)
    {
    }

    @Override
    public void visitAfter(XsdSimpleType simpleType)
    {
    }

    @Override
    public void visitBefore(XsdTotalDigits totalDigits)
    {
    }

    @Override
    public void visitAfter(XsdTotalDigits totalDigits)
    {
    }

    @Override
    public void visitBefore(XsdUnion union)
    {
    }

    @Override
    public void visitAfter(XsdUnion union)
    {
    }

    @Override
    public void visitBefore(XsdWhiteSpace whiteSpace)
    {
    }

    @Override
    public void visitAfter(XsdWhiteSpace whiteSpace)
    {
    }
}
