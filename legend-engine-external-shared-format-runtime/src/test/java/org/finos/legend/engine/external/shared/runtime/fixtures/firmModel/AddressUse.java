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

package org.finos.legend.engine.external.shared.runtime.fixtures.firmModel;

import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalData;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;

import java.util.ArrayList;
import java.util.List;

public class AddressUse implements IReferencedObject, IExternalData {

    public static final IExternalDataFactory FACTORY = new IExternalDataFactory()
    {
        public IExternalData newInstance()
        {
            return new AddressUse();
        }
        public String getPureClassName()
        {
            return "meta::external::shared::testpack::simple::AddressUse";
        }
    };

    private AddressType addressType;

    private int addressTypeSize;

    private Address address;

    private int addressSize;

    public AddressType getAddressType() {
        return this.addressTypeSize == 0 ? null : this.addressType;
    }

    public void _addressTypeAdd(AddressType value) {
        if (this.addressTypeSize == 0)
        {
            this.addressType = value;
        }
        this.addressTypeSize++;
    }

    public Address getAddress() {
        return this.addressSize == 0 ? null : this.address;
    }

    public void _addressAdd(Address value) {
        if (this.addressSize == 0)
        {
            this.address = value;
        }
        this.addressSize++;
    }

    public List<IDefect> checkMultiplicities() {
        List<IDefect> defects = new ArrayList<IDefect>();
        if (this.addressTypeSize < 1L || this.addressTypeSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for addressType: expected [1] found [" + this.addressTypeSize + "]", "meta::external::shared::testpack::simple::AddressUse"));
        }
        if (this.addressSize < 1L || this.addressSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for address: expected [1] found [" + this.addressSize + "]", "meta::external::shared::testpack::simple::AddressUse"));
        }
        return defects;
    }

    public static ExternalDataAdder<AddressUse> _getAdderForProperty(String propertyName) {
        if (propertyName.equals("addressType"))
        {
            return new ExternalDataObjectAdder<AddressUse, AddressType>("addressType")
            {
                public void addTo(AddressUse object, AddressType value)
                {
                    object._addressTypeAdd(value);
                }
            };
        }
        else if (propertyName.equals("address"))
        {
            return new ExternalDataObjectAdder<AddressUse, Address>("address")
            {
                public void addTo(AddressUse object, Address value)
                {
                    object._addressAdd(value);
                }
            };
        }
        else
        {
            throw new IllegalArgumentException("Unknown property " + propertyName);
        }
    }

    public String getAlloyStoreObjectReference$() {
        return null;
    }
}
