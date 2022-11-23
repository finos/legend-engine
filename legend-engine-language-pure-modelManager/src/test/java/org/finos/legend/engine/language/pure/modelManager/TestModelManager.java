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

package org.finos.legend.engine.language.pure.modelManager;

import io.opentracing.Span;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextCollection;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextText;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import org.pac4j.core.profile.CommonProfile;

public class TestModelManager
{
    @Test
    public void testPureModelCaching()
    {
        MockModelLoader mockModelLoader = new MockModelLoader();
        ModelManager manager = new ModelManager(DeploymentMode.TEST, mockModelLoader);
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
        manager.loadModel(pointer,null,null, null);
        Assert.assertEquals(1, mockModelLoader.loadCount);
        manager.loadModel(pointer,null,null, null);
        Assert.assertEquals("Should not invoke loader as this is cached",1, mockModelLoader.loadCount);
    }

    @Test
    public void testCompositeResolving()
    {
        MockModelLoader mockModelLoader = new MockModelLoader();
        ModelManager manager = new ModelManager(DeploymentMode.TEST, mockModelLoader);

        Class class1 = new Class();
        class1.name = "Class1";
        class1._package = "my::package";

        Class class2 = new Class();
        class2.name = "Class2";
        class2._package = "my::package";

        Class class3 = new Class();
        class3.name = "Class3";
        class3._package = "my::package";

        PureModelContextData data1 = PureModelContextData.newPureModelContextData(null, null, Lists.mutable.with(class1));
        PureModelContextData data2 = PureModelContextData.newPureModelContextData(null, null, Lists.mutable.with(class1));
        PureModelContextData data3 = PureModelContextData.newPureModelContextData(null, null, Lists.mutable.with(class2, class3));

        PureModelContextText text = new PureModelContextText();
        text.code = "Class my::package::Class4{}";

        PureModelContextCollection subComposite = new PureModelContextCollection();
        subComposite.contextCollection = Lists.mutable.with(
                data1,
                data2,
                data3
        );

        PureModelContextCollection composite = new PureModelContextCollection();
        composite.contextCollection = Lists.mutable.with(
            subComposite,
            text
        );

        PureModelContextData contextData = manager.loadData(composite, null, null);
        Assert.assertEquals(5, contextData.getElements().size());
        Assert.assertEquals("__internal__::SectionIndex", contextData.getElements().get(0).getPath());
        Assert.assertEquals("my::package::Class1", contextData.getElements().get(1).getPath());
        Assert.assertEquals("my::package::Class2", contextData.getElements().get(2).getPath());
        Assert.assertEquals("my::package::Class3", contextData.getElements().get(3).getPath());
        Assert.assertEquals("my::package::Class4", contextData.getElements().get(4).getPath());
    }

    private static class MockModelLoader implements ModelLoader
    {
        private int loadCount = 0;

        @Override
        public boolean supports(PureModelContext context)
        {
            return true;
        }

        @Override
        public PureModelContextData load(MutableList<CommonProfile> profiles, PureModelContext context, String clientVersion, Span parentSpan)
        {
            loadCount++;
            PureModelContextPointer pointer = (PureModelContextPointer) context;
            return PureModelContextData.newPureModelContextData(null, pointer, Lists.fixedSize.empty());
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
        public PureModelContext cacheKey(PureModelContext context, MutableList<CommonProfile> pm)
        {
            return context;
        }
    }
}
