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

package org.finos.legend.engine.protocol.hostedService.deployment.model.lineage;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.api.analytics.model.graph.Graph;
import org.finos.legend.engine.api.analytics.model.report.ColumnLineage;
import org.finos.legend.engine.api.analytics.model.tree.PropertyPathTreeNode;
import org.finos.legend.engine.api.analytics.model.tree.RelationalTreeNode;
import org.finos.legend.engine.protocol.Protocol;

import java.util.List;
import java.util.Objects;

public class SingleLineage extends Lineage
{
    public Protocol serializer;
    public Graph databaseLineage;
    public Graph classLineage;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<PropertyPathTreeNode> functionTree;
    public RelationalTreeNode relationTree;
    public List<ColumnLineage> reportLineage;

    public static SingleLineage emptyLineage(String lineageVersion)
    {
        Protocol protocol = new Protocol("lineage", lineageVersion);

        Graph dbLineage = new Graph();
        dbLineage.edges = Lists.mutable.empty();
        dbLineage.nodes = Lists.mutable.empty();

        Graph classLineage = new Graph();
        classLineage.edges = Lists.mutable.empty();
        classLineage.nodes = Lists.mutable.empty();

        RelationalTreeNode treenode = new RelationalTreeNode();
        treenode.children = Lists.mutable.empty();

        PropertyPathTreeNode functionTree = new PropertyPathTreeNode();
        functionTree.display = "root";
        functionTree.type = "root";
        functionTree.children = Lists.mutable.empty();

        SingleLineage result = new SingleLineage();
        result.serializer = protocol;
        result.databaseLineage  = dbLineage;
        result.classLineage = classLineage;
        result.functionTree = Lists.mutable.with(functionTree);
        result.reportLineage = Lists.mutable.empty();
        result.relationTree = treenode;
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SingleLineage))
        {
            return false;
        }
        SingleLineage that = (SingleLineage) o;
        return serializer.equals(that.serializer) && reportLineage.equals(that.reportLineage);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(serializer, databaseLineage, classLineage, functionTree, relationTree, reportLineage);
    }
}
