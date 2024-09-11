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

package org.finos.legend.pure.runtime.java.extension.external.relation.shared.window;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Window
{
    MutableList<? extends String> partition = Lists.mutable.empty();
    MutableList<SortInfo> sorts = Lists.mutable.empty();
    Frame frame;

    public Window(MutableList<? extends String> colSpec, RichIterable<Pair<Enum, String>> sorts, Frame frame)
    {
        this(colSpec, sorts.toList().collect(c -> new SortInfo(c.getTwo(), SortDirection.valueOf(c.getOne()._name()))).toList(), frame);
    }

    public Window(MutableList<? extends String> colSpec, MutableList<SortInfo> sorts, Frame frame)
    {
        this.partition = colSpec;
        this.sorts = sorts;
        this.frame = frame == null ? (this.sorts.isEmpty() ? new Frame(FrameType.rows, true, true) : new Frame(FrameType.rows, true, 0)) : frame;
    }

    public Window()
    {
        this.frame = new Frame(FrameType.rows, true, true);
    }

    public Window(Frame frame)
    {
        this.frame = frame == null ? (this.sorts.isEmpty() ? new Frame(FrameType.rows, true, true) : new Frame(FrameType.rows, true, 0)) : frame;
    }

    public MutableList<? extends String> getPartition()
    {
        return partition;
    }

    public MutableList<SortInfo> getSorts()
    {
        return sorts;
    }

    public Frame getFrame()
    {
        return this.frame;
    }

    public static Window build(CoreInstance window, ProcessorSupport processorSupport)
    {
        return new Window(
                window.getValueForMetaPropertyToMany("partition").collect(CoreInstance::getName).toList(),
                window.getValueForMetaPropertyToMany("sortInfo").collect(c -> new SortInfo(c.getValueForMetaPropertyToOne("column").getValueForMetaPropertyToOne("name").getName(), SortDirection.valueOf(c.getValueForMetaPropertyToOne("direction").getName()))).toList(),
                Frame.build(window.getValueForMetaPropertyToOne("frame"), processorSupport)
        );
    }


    public CoreInstance convert(ProcessorSupport ps, Frame.PrimitiveBuilder builder)
    {
        CoreInstance result = ps.newCoreInstance("", "meta::pure::functions::relation::_Window", null);
        result.setKeyValues(Lists.mutable.with("partition"), getPartition().collect(builder::build));
        result.setKeyValues(Lists.mutable.with("frame"), Lists.mutable.with(getFrame().convert(ps, builder)));
        result.setKeyValues(Lists.mutable.with("sortInfo"), getSorts().collect(x -> convert(x, ps, builder)));
        return result;
    }

    public CoreInstance convert(SortInfo sortInfo, ProcessorSupport ps, Frame.PrimitiveBuilder builder)
    {
        CoreInstance result = ps.newCoreInstance("", "meta::pure::functions::relation::SortInfo", null);
        CoreInstance colSpec = ps.newCoreInstance("", "meta::pure::metamodel::relation::ColSpec", null);
        colSpec.setKeyValues(Lists.mutable.with("name"), Lists.mutable.with(builder.build(sortInfo.getColumnName())));
        result.setKeyValues(Lists.mutable.with("column"), Lists.mutable.with(colSpec));
        CoreInstance sortTypeEnum = ps.package_getByUserPath("meta::pure::functions::relation::SortType");
        CoreInstance sortDir = sortTypeEnum.getValueForMetaPropertyToMany("values").detect(e -> sortInfo.getDirection().name().equals(e.getValueForMetaPropertyToOne("name").getName()));
        result.setKeyValues(Lists.mutable.with("direction"), Lists.mutable.with(sortDir));
        return result;
    }
}