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
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataBooleanAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalData;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Person implements IReferencedObject, IExternalData
{

    public static final IExternalDataFactory FACTORY = new IExternalDataFactory()
    {
        public IExternalData newInstance()
        {
            return new Person();
        }

        public String getPureClassName()
        {
            return "meta::external::shared::testpack::simple::Person";
        }
    };

    private String firstName;

    private int firstNameSize;

    private String lastName;

    private int lastNameSize;

    private PureDate dateOfBirth;

    private int dateOfBirthSize;

    private boolean isAlive;

    private int isAliveSize;

    private double heightInMeters;

    private int heightInMetersSize;

    private Firm firm;

    private int firmSize;

    private List<AddressUse> addresses;

    public String getFirstName()
    {
        return this.firstNameSize == 0 ? null : this.firstName;
    }

    public void _firstNameAdd(String value)
    {
        if (this.firstNameSize == 0)
        {
            this.firstName = value;
        }
        this.firstNameSize++;
    }

    public String getLastName()
    {
        return this.lastNameSize == 0 ? null : this.lastName;
    }

    public void _lastNameAdd(String value)
    {
        if (this.lastNameSize == 0)
        {
            this.lastName = value;
        }
        this.lastNameSize++;
    }

    public PureDate getDateOfBirth()
    {
        return this.dateOfBirthSize == 0 ? null : this.dateOfBirth;
    }

    public void _dateOfBirthAdd(Temporal value)
    {
        if (this.dateOfBirthSize == 0)
        {
            this.dateOfBirth = PureDate.fromTemporal(value, Calendar.DAY_OF_MONTH);
        }
        this.dateOfBirthSize++;
    }

    public boolean getIsAlive()
    {
        return this.isAliveSize == 0 ? null : this.isAlive;
    }

    public void _isAliveAdd(boolean value)
    {
        if (this.isAliveSize == 0)
        {
            this.isAlive = value;
        }
        this.isAliveSize++;
    }

    public double getHeightInMeters()
    {
        return this.heightInMetersSize == 0 ? null : this.heightInMeters;
    }

    public void _heightInMetersAdd(double value)
    {
        if (this.heightInMetersSize == 0)
        {
            this.heightInMeters = value;
        }
        this.heightInMetersSize++;
    }

    public Firm getFirm()
    {
        return this.firmSize == 0 ? null : this.firm;
    }

    void _firmAddImpl(Firm value)
    {
        if (this.firmSize == 0)
        {
            this.firm = value;
        }
        this.firmSize++;
    }

    public void _firmAdd(Firm value)
    {
        this._firmAddImpl(value);
        value._employeesAddImpl(this);
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

    public List<IDefect> checkMultiplicities()
    {
        List<IDefect> defects = new ArrayList<IDefect>();
        if (this.firstNameSize < 1L || this.firstNameSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for firstName: expected [1] found [" + this.firstNameSize + "]", "meta::external::shared::testpack::simple::Person"));
        }
        if (this.lastNameSize < 1L || this.lastNameSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for lastName: expected [1] found [" + this.lastNameSize + "]", "meta::external::shared::testpack::simple::Person"));
        }
        if (this.dateOfBirthSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for dateOfBirth: expected [0..1] found [" + this.dateOfBirthSize + "]", "meta::external::shared::testpack::simple::Person"));
        }
        if (this.isAliveSize < 1L || this.isAliveSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for isAlive: expected [1] found [" + this.isAliveSize + "]", "meta::external::shared::testpack::simple::Person"));
        }
        if (this.heightInMetersSize < 1L || this.heightInMetersSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for heightInMeters: expected [1] found [" + this.heightInMetersSize + "]", "meta::external::shared::testpack::simple::Person"));
        }
        if (this.firmSize < 1L || this.firmSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for firm: expected [1] found [" + this.firmSize + "]", "meta::external::shared::testpack::simple::Person"));
        }
        return defects;
    }

    public static ExternalDataAdder<Person> _getAdderForProperty(String propertyName)
    {
        if (propertyName.equals("firstName"))
        {
            return new ExternalDataObjectAdder<Person, String>("firstName")
            {
                public void addTo(Person object, String value)
                {
                    object._firstNameAdd(value);
                }
            };
        }
        else if (propertyName.equals("lastName"))
        {
            return new ExternalDataObjectAdder<Person, String>("lastName")
            {
                public void addTo(Person object, String value)
                {
                    object._lastNameAdd(value);
                }
            };
        }
        else if (propertyName.equals("dateOfBirth"))
        {
            return new ExternalDataObjectAdder<Person, Temporal>("dateOfBirth")
            {
                public void addTo(Person object, Temporal value)
                {
                    object._dateOfBirthAdd(value);
                }
            };
        }
        else if (propertyName.equals("isAlive"))
        {
            return new ExternalDataBooleanAdder<Person>("isAlive")
            {
                public void addTo(Person object, boolean value)
                {
                    object._isAliveAdd(value);
                }
            };
        }
        else if (propertyName.equals("heightInMeters"))
        {
            return new ExternalDataDoubleAdder<Person>("heightInMeters")
            {
                public void addTo(Person object, double value)
                {
                    object._heightInMetersAdd(value);
                }
            };
        }
        else if (propertyName.equals("firm"))
        {
            return new ExternalDataObjectAdder<Person, Firm>("firm")
            {
                public void addTo(Person object, Firm value)
                {
                    object._firmAdd(value);
                }
            };
        }
        else if (propertyName.equals("addresses"))
        {
            return new ExternalDataObjectAdder<Person, AddressUse>("addresses")
            {
                public void addTo(Person object, AddressUse value)
                {
                    object._addressesAdd(value);
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
