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

package org.finos.legend.engine.shared.core.identity.factory;

import org.finos.legend.engine.shared.core.extension.LegendExtension;
import org.finos.legend.engine.shared.core.identity.Identity;
import java.util.Optional;

public interface IdentityFactory extends LegendExtension
{
    Optional<Identity> makeIdentity(Object authenticationSource);
        /*
             This function is supposed to do following things
             1. return null if authenticationSource is empty/null or authenticationSource is not supported by respective IdentityFactory
             2. return Identity if authenticationSource is supported by IdentityFactory
        */
}