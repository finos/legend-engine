// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.platform;

import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_context_PersistencePlatform;

public interface PersistencePlatformActionsExtension
{
    Class<? extends Root_meta_pure_persistence_metamodel_context_PersistencePlatform> platformType();

    void validate(PersistencePlatformActionRequest request);

    void install(PersistencePlatformActionRequest request);

    void uninstall(PersistencePlatformActionRequest request);
}
