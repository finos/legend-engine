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

package org.finos.legend.engine.plan.dependencies.store.inMemory;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;

public interface IStoreStreamReader
{
    /**
     * Called before any reading to give the reading implementation an opportunity to perform resource setup
     */
    void initReading();

    /**
     * Called once no more reading will happen to give the reading implementation an opportunity to perform resource cleanup
     */
    void destroyReading();

    /**
     * Should return <tt>false</tt> if the store has more data available to deliver and <tt>true</tt> if it is exhausted.
     *
     * @return <tt>true</tt> if the store has reached the end of its data
     */
    boolean isFinished();

    /**
     * Should return one or more objects from the data source unless reading has finished in which case it should
     * return an empty collection. Each checked object should include its source along with any defects and,
     * if constructable, the value object.
     *
     * @return the next tranche of records from the store
     */
    <T> Collection<IChecked<T>> readCheckedObjects();
}
