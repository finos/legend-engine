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

package org.finos.legend.engine.server.core.session;

import org.finos.legend.engine.plan.execution.stores.StoreExecutableManager;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;


public class StoreExecutableManagerSessionListener  implements HttpSessionListener
    {
    public StoreExecutableManagerSessionListener()
        {

        }

    @Override
    public void sessionCreated(HttpSessionEvent se)
        {
        StoreExecutableManager.INSTANCE.registerManager();
        }

    @Override
    public void sessionDestroyed(HttpSessionEvent se)
        {
        StoreExecutableManager.INSTANCE.cancelExecutablesOnSession(se.getSession().getId());
        }
    }

