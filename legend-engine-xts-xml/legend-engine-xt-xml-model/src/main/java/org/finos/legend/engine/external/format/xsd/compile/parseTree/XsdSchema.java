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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class XsdSchema extends XsdAnnotated
{
    public String targetNamespace;
    public List<XsdObject> items = new ArrayList<>();
    public List<XsdExternalSchema> externals = new ArrayList<>();
    public XsdForm attributeFormDefault;
    public XsdForm elementFormDefault;
    public List<XsdDerivationType> blockDefault = new ArrayList<>();
    public List<XsdDerivationType> finalDefault = new ArrayList<>();
    public String version;
    public List<Namespace> additionalNamespaces = new ArrayList<>();

    @Override
    public void accept(XsdObjectVisitor visitor)
    {
        visitor.visitBefore(this);
        Optional.ofNullable(annotation).ifPresent(a -> a.accept(visitor));
        externals.forEach(x -> x.accept(visitor));
        items.forEach(x -> x.accept(visitor));
        visitor.visitAfter(this);
    }
}
