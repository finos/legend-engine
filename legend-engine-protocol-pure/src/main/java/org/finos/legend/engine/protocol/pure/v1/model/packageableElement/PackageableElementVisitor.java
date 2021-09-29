// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.PackageableAuthorizer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;

public interface PackageableElementVisitor<T>
{
    // NOTE: a catch all until we remove all visitor methods
    T visit(PackageableElement element);

    T visit(Profile profile);

    T visit(Enumeration _enum);

    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class _class);

    T visit(Association association);

    T visit(Function function);

    T visit(Measure measure);

    T visit(SectionIndex sectionIndex);

    // dsl-mapping

    T visit(Mapping mapping);

    T visit(PackageableRuntime packageableRuntime);

    T visit(PackageableConnection packageableConnection);

    T visit(PackageableAuthorizer packageableAuthorizer);
}
