// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

public interface FlatDataDriver
{
    /**
     * Return the ID of this driver.  This should be the same ID as is returned by its description
     * and is the value that will be used to specify the driver to be used when defining a section
     * of a <tt>FlatData</tt> store.
     *
     * @return the driver ID
     */
    String getId();

    /**
     * Called to determine whether this driver can process the data from the given cursor position.
     */
    boolean canStartAt(Cursor cursor);
}
