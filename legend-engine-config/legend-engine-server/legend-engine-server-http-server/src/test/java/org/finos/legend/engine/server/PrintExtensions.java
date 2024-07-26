// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.extension.Extensions;
import org.finos.legend.engine.shared.core.extension.LegendExtension;
import org.finos.legend.shared.stuctures.TreeNode;
import org.slf4j.LoggerFactory;

public class PrintExtensions
{
    public static void main(String[] args)
    {
        Logger l = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        l.setLevel(Level.INFO);

        MutableList<LegendExtension> extensionList = Extensions.get();
        TreeNode extensions = Extensions.buildTree(extensionList);
        System.out.println(extensions.print());
    }
}
