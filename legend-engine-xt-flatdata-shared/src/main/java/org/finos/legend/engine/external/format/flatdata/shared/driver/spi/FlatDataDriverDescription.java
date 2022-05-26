// Copyright 2022 Goldman Sachs
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

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * Providers should implement this interface to describe the configuration of a {@link FlatDataReadDriver}.
 */
public interface FlatDataDriverDescription
{
    static List<FlatDataDriverDescription> loadAll()
    {
        List<FlatDataDriverDescription> result = new ArrayList<>();
        for (FlatDataDriverDescription desc : ServiceLoader.load(FlatDataDriverDescription.class))
        {
            if (result.stream().anyMatch(d -> d.getId().equals(desc.getId())))
            {
                throw new Error("Duplicate FlatDataDrivers for id: " + desc.getId());
            }
            result.add(desc);
        }
        return result;
    }

    /**
     * Return the ID of the driver this describes.  This should be the same ID as is returned by the driver
     * and is the value that will be used to specify the driver to be used when defining a section
     * of a <tt>FlatData</tt> store.
     *
     * @return the driver ID
     */
    String getId();

    /**
     * Indicates whether this driver defines a self-describing data format or not:
     * <p>
     * * A self-describing data format is one in which the format includes labels as part of the data.
     * An example is a CSV with a headings row where each data row value is labeled with it's
     * corresponding heading.  Record types for self-describing data formats must not specify an
     * address.
     * <p>
     * * A non-self-describing data format does not include labels and record types for them must specify
     * the mapping between a label and an address.  An example is a CSV without a headings row.  The
     * addresses in this case will be the column number.
     * <p>
     * If the driver does not allow record types then this value is irrelevant.
     *
     * @return <tt>true</tt> if the driver processes self-describing data.
     */
    boolean isSelfDescribing();

    /**
     * Indicates the properties that may be defined when a FlatData store includes a section using the
     * described driver.  The values of these properties are captured in the {@link FlatDataSection}
     * and made available at instantiation time for a driver.
     *
     * @return the defined properties
     */
    List<PropertyDescription> getSectionProperties();

    /**
     * Indicates the variables that the described drive declares.  A driver may only access variables which
     * have been declared.  During execution, variables are part of the {@link FlatDataProcessingContext}.
     * If multiple drivers declare the same variable it must be declared with the same type and the value
     * assigned by any such prior section will be visible.
     * <p>
     * Drivers which declare the same variable as different types are incompatible and may not be combined in a
     * FlatData store.
     *
     * @return the declared variables.
     */
    List<FlatDataVariable> getDeclares();

    /**
     * Indicates the valid multiplicities of record formats that may be defined for sections using the described
     * driver.
     *
     * @return the valid multiplicity.
     */
    RecordTypeMultiplicity getRecordTypeMultiplicity();

    /**
     * Called to create an instance of the described driver for reading.
     *
     * @param section the FlatData section for which the driver is being instantiated.
     * @param context the {@link FlatDataProcessingContext} for the execution the driver will be used in.
     * @return an instance of the described driver.
     */
    <T> FlatDataReadDriver<T> newReadDriver(FlatDataSection section, FlatDataProcessingContext context);

    /**
     * Called to create an instance of the described driver for writing.
     *
     * @param section the FlatData section for which the driver is being instantiated.
     * @param context the {@link FlatDataProcessingContext} for the execution the driver will be used in.
     * @return an instance of the described driver.
     */
    default <T> FlatDataWriteDriver<T> newWriteDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        throw new UnsupportedOperationException("Write not supported for " + getId());
    }

    /**
     * Returns a function to create a {@link FlatDataProcessor.Builder} for this driver type.
     * The processors for all sections in a FlatData need to be compatible.
     * When processing a FlatData the <tt>FlatDataProcessor.Factory</tt> for the first section will be used to perform the reading/writing.
     *
     * @return the class of the processor for sections using this driver
     */
    <T> Function<FlatData, FlatDataProcessor.Builder<T>> getProcessorBuilderFactory();
}
