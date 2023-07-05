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

import org.eclipse.collections.impl.factory.Lists;

import java.util.List;

public enum XsdDerivationType
{
    EXTENSION,
    RESTRICTION,
    SUBSTITUTION,
    LIST,
    UNION;

    public static final List<XsdDerivationType> EXTENSION_RESTRICTION = Lists.mutable.with(XsdDerivationType.EXTENSION, XsdDerivationType.RESTRICTION);
    public static final List<XsdDerivationType> EXTENSION_RESTRICTION_SUBSTITUTION = Lists.mutable.with(XsdDerivationType.EXTENSION, XsdDerivationType.RESTRICTION, XsdDerivationType.SUBSTITUTION);
    public static final List<XsdDerivationType> LIST_UNION_RESTRICTION = Lists.mutable.with(XsdDerivationType.LIST, XsdDerivationType.UNION, XsdDerivationType.RESTRICTION);
}
