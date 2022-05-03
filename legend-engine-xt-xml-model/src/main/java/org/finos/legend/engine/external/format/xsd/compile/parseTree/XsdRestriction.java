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

package org.finos.legend.engine.external.format.xsd.compile.parseTree;

import org.finos.legend.engine.external.format.xsd.compile.parseTree.visit.XsdObjectVisitor;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class XsdRestriction extends XsdAnnotated implements XsdSimpleTypeDerivation, XsdContentDerivation
{
    public QName baseTypeName;
    public XsdAnyType baseType;
    public List<XsdFacet> facets = new ArrayList<>();
    public List<XsdAttributeItem> attributeItems = new ArrayList<>();
    public XsdAnyAttribute anyAttribute;
    public XsdParticle particle;

    @Override
    public void accept(XsdObjectVisitor visitor)
    {
        visitor.visitBefore(this);
        Optional.ofNullable(annotation).ifPresent(a -> a.accept(visitor));
        Optional.ofNullable((XsdObject) baseType).ifPresent(bt -> bt.accept(visitor));
        facets.forEach(f -> f.accept(visitor));
        Optional.ofNullable((XsdObject) particle).ifPresent(p -> p.accept(visitor));
        attributeItems.forEach(ai -> ai.accept(visitor));
        Optional.ofNullable((XsdObject) anyAttribute).ifPresent(aa -> aa.accept(visitor));
        visitor.visitAfter(this);
    }
}
