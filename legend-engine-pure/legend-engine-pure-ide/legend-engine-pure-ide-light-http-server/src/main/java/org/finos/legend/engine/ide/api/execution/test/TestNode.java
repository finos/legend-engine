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

package org.finos.legend.engine.ide.api.execution.test;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.ide.helpers.response.ExceptionTranslation;
import org.finos.legend.pure.m3.navigation.*;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.tools.tree.TreeNode;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Objects;

public class TestNode implements TreeNode<TestNode>
{
    private final CoreInstance coreInstance;
    private final String testParameterizationId;
    private final MutableList<TestNode> children = Lists.mutable.empty();

    public TestNode(CoreInstance coreInstance, String testParameterizationId)
    {
        this.coreInstance = coreInstance;
        this.testParameterizationId = testParameterizationId;
    }

    public TestNode(CoreInstance coreInstance)
    {
        this(coreInstance, null);
    }

    public TestNode getOrCreateChild(CoreInstance instance, String testParameterizationId)
    {
        for (TestNode child : this.children)
        {
            if (instance == child.coreInstance && Objects.equals(testParameterizationId, child.testParameterizationId))
            {
                return child;
            }
        }
        TestNode newChild = new TestNode(instance, testParameterizationId);
        this.children.add(newChild);
        return newChild;
    }

    @Override
    public MutableList<TestNode> getChildren()
    {
        return this.children;
    }

    @Override
    public TestNode getChildAt(int index)
    {
        return this.children.get(index);
    }

    @Override
    public boolean isLeaf()
    {
        return this.children.isEmpty();
    }

    @Override
    public int indexOf(TestNode node)
    {
        return this.children.indexOf(node);
    }

    @Override
    public String toString()
    {
        return this.coreInstance.getName();
    }

    public String toJson(final int testId)
    {
        CoreInstance coreInstance = this.coreInstance;
        ProcessorSupport processorSupport = new M3ProcessorSupport(coreInstance.getRepository());
        SourceInformation si = coreInstance.getSourceInformation() != null ? coreInstance.getSourceInformation() : ExceptionTranslation.DUMMY_SOURCE_INFORMATION;
        String parameterizationSuffix = this.testParameterizationId == null ? "" : "[" + this.testParameterizationId + "]";
        return "{\"text\":\"" + (Instance.instanceOf(coreInstance, M3Paths.Function, processorSupport) ? Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.functionName, processorSupport).getName() : Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.name, processorSupport).getName()) + parameterizationSuffix + "\"" +
                (this.children.isEmpty() ? ", \"type\":\"notRan\"" : "") +
                ",\"li_attr\" : {\"file\":\"" + si.getSourceId() + "\",\"line\":\"" + si.getLine() + "\",\"column\":\"" + si.getColumn() + "\",\"parentId\":\"" + PackageableElement.getUserPathForPackageableElement(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties._package, new M3ProcessorSupport(coreInstance.getRepository())), "_") + "\", \"id\":\"test" + testId + "_" + PackageableElement.getUserPathForPackageableElement(coreInstance, "_") + parameterizationSuffix + "\"}" +
                (this.getChildren().isEmpty() ? "" : ",\"children\":[" + this.getChildren().collect(testNode -> testNode.toJson(testId)).makeString() + "]") + "}";
    }
}
