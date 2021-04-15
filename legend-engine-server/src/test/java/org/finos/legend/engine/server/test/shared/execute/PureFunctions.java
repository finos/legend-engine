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

package org.finos.legend.engine.server.test.shared.execute;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_scan_buildBasePureModel;
import org.finos.legend.pure.m3.execution.ExecutionSupport;

public class PureFunctions
{
    public static String alloy_metadataServer_pureModelFromMapping(String _package, String version, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, ExecutionSupport executionSupport)
    {
        return core_pure_protocol_vX_X_X_scan_buildBasePureModel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_buildBasePureModelFromMappingStr_String_1__RouterExtension_MANY__String_1_(_package, extensions, executionSupport);
    }

    public static String alloy_metadataServer_pureModelFromStore(String _package, String version, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, ExecutionSupport executionSupport)
    {
        return core_pure_protocol_vX_X_X_scan_buildBasePureModel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_buildBasePureModelFromStoreStr_String_1__RouterExtension_MANY__String_1_(_package, extensions, executionSupport);
    }
}
