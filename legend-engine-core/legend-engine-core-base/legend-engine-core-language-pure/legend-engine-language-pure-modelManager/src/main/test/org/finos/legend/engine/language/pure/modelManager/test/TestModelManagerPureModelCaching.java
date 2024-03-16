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

package org.finos.legend.engine.language.pure.modelManager.test;

import io.opentracing.Span;
import org.finos.legend.engine.language.pure.modelManager.ModelLoader;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.List;

public class TestModelManagerPureModelCaching
{
    @Test
    public void testPureModelCaching()
    {
        ModelManager manager = new ModelManager(DeploymentMode.TEST, new MockModelLoader());
        Protocol protocol = new Protocol("pure", "v1_17_0");
        PackageableElementPointer element0 = new PackageableElementPointer(PackageableElementType.MAPPING,"meta::relational::tests::milestoning::milestoningmap");
        PackageableElementPointer element1 = new PackageableElementPointer(PackageableElementType.STORE,"meta::relational::tests::dbInc");
        PackageableElementPointer element2 = new PackageableElementPointer(PackageableElementType.STORE,"meta::relational::tests::db");
        List<PackageableElementPointer> list = new ArrayList<>();
        list.add(element0);
        list.add(element1);
        list.add(element2);
        PureSDLC pureSDLC = new PureSDLC();
        pureSDLC.packageableElementPointers = list;
        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.serializer = protocol;
        pointer.sdlcInfo = pureSDLC;
        PureModelContext context = pointer;
        manager.loadModel(context,null,null, null);
        Assert.assertEquals(1, manager.pureModelCache.size());
    }


    private static class MockModelLoader implements ModelLoader
    {
        @Override
        public boolean supports(PureModelContext context)
        {
            return true;
        }

        @Override
        public PureModelContextData load(Subject callerSubject, PureModelContext context, String clientVersion, Span parentSpan)
        {
            PureModelContextPointer pointer = (PureModelContextPointer) context;
            PureModelContextData data = new PureModelContextData();
            data.origin = pointer;
            return data;
        }

        @Override
        public void setModelManager(ModelManager modelManager)
        {

        }

        @Override
        public boolean shouldCache(PureModelContext context)
        {
            return this.supports(context);
        }

        @Override
        public PureModelContext cacheKey(PureModelContext context, Subject subject)
        {
            return context;
        }
    }
}
