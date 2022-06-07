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
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataLongAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalData;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Firm implements IReferencedObject, IExternalData
{

    public static final IExternalDataFactory FACTORY = new IExternalDataFactory()
    {
        public IExternalData newInstance()
        {
            return new Firm();
        }

        public String getPureClassName()
        {
            return "meta::external::shared::testpack::simple::Firm";
        }
    };

    private String name;

    private int nameSize;

    private long ranking;

    private int rankingSize;

    private List<AddressUse> addresses;

    private List<Person> employees;

    public String getName()
    {
        return this.nameSize == 0 ? null : this.name;
    }

    public void _nameAdd(String value)
    {
        if (this.nameSize == 0)
        {
            this.name = value;
        }
        this.nameSize++;
    }

    public Long getRanking()
    {
        return this.rankingSize == 0 ? null : this.ranking;
    }

    public void _rankingAdd(long value)
    {
        if (this.rankingSize == 0)
        {
            this.ranking = value;
        }
        this.rankingSize++;
    }

    public List<AddressUse> getAddresses()
    {
        return this.addresses == null ? Collections.<AddressUse>emptyList() : this.addresses;
    }

    public void _addressesAdd(AddressUse value)
    {
        if (this.addresses == null)
        {
            this.addresses = new ArrayList<AddressUse>();
        }
        this.addresses.add(value);
    }

    public List<Person> getEmployees()
    {
        return this.employees == null ? Collections.<Person>emptyList() : this.employees;
    }

    void _employeesAddImpl(Person value)
    {
        if (this.employees == null)
        {
            this.employees = new ArrayList<Person>();
        }
        this.employees.add(value);
    }

    public void _employeesAdd(Person value)
    {
        this._employeesAddImpl(value);
        value._firmAddImpl(this);
    }

    public List<IDefect> checkMultiplicities()
    {
        List<IDefect> defects = new ArrayList<IDefect>();
        if (this.nameSize < 1L || this.nameSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for name: expected [1] found [" + this.nameSize + "]", "meta::external::shared::testpack::simple::Firm"));
        }
        if (this.rankingSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for ranking: expected [0..1] found [" + this.rankingSize + "]", "meta::external::shared::testpack::simple::Firm"));
        }
        if (this.addresses.size() < 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for addresses: expected [1..*] found [" + this.addresses.size() + "]", "meta::external::shared::testpack::simple::Firm"));
        }
        return defects;
    }

    public static ExternalDataAdder<Firm> _getAdderForProperty(String propertyName)
    {
        if (propertyName.equals("name"))
        {
            return new ExternalDataObjectAdder<Firm, String>("name")
            {
                public void addTo(Firm object, String value)
                {
                    object._nameAdd(value);
                }
            };
        }
        else if (propertyName.equals("ranking"))
        {
            return new ExternalDataLongAdder<Firm>("ranking")
            {
                public void addTo(Firm object, long value)
                {
                    object._rankingAdd(value);
                }
            };
        }
        else if (propertyName.equals("addresses"))
        {
            return new ExternalDataObjectAdder<Firm, AddressUse>("addresses")
            {
                public void addTo(Firm object, AddressUse value)
                {
                    object._addressesAdd(value);
                }
            };
        }
        else if (propertyName.equals("employees"))
        {
            return new ExternalDataObjectAdder<Firm, Person>("employees")
            {
                public void addTo(Firm object, Person value)
                {
                    object._employeesAdd(value);
                }
            };
        }
        else
        {
            throw new IllegalArgumentException("Unknown property " + propertyName);
        }
    }

    public String getAlloyStoreObjectReference$()
    {
        return null;
    }
}
