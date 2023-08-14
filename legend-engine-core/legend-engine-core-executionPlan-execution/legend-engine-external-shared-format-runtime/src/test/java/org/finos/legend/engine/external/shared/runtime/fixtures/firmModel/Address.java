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

public class Address implements IReferencedObject, IExternalData
{

    public static final IExternalDataFactory FACTORY = new IExternalDataFactory()
    {
        public IExternalData newInstance()
        {
            return new Address();
        }

        public String getPureClassName()
        {
            return "meta::external::shared::testpack::simple::Address";
        }
    };

    private String firstLine;

    private int firstLineSize;

    private String secondLine;

    private int secondLineSize;

    private String city;

    private int citySize;

    private String region;

    private int regionSize;

    private String country;

    private int countrySize;

    public String getFirstLine()
    {
        return this.firstLineSize == 0 ? null : this.firstLine;
    }

    public void _firstLineAdd(String value)
    {
        if (this.firstLineSize == 0)
        {
            this.firstLine = value;
        }
        this.firstLineSize++;
    }

    public String getSecondLine()
    {
        return this.secondLineSize == 0 ? null : this.secondLine;
    }

    public void _secondLineAdd(String value)
    {
        if (this.secondLineSize == 0)
        {
            this.secondLine = value;
        }
        this.secondLineSize++;
    }

    public String getCity()
    {
        return this.citySize == 0 ? null : this.city;
    }

    public void _cityAdd(String value)
    {
        if (this.citySize == 0)
        {
            this.city = value;
        }
        this.citySize++;
    }

    public String getRegion()
    {
        return this.regionSize == 0 ? null : this.region;
    }

    public void _regionAdd(String value)
    {
        if (this.regionSize == 0)
        {
            this.region = value;
        }
        this.regionSize++;
    }

    public String getCountry()
    {
        return this.countrySize == 0 ? null : this.country;
    }

    public void _countryAdd(String value)
    {
        if (this.countrySize == 0)
        {
            this.country = value;
        }
        this.countrySize++;
    }

    public static ExternalDataAdder<Address> _getAdderForProperty(String propertyName)
    {
        if (propertyName.equals("firstLine"))
        {
            return new ExternalDataObjectAdder<Address, String>("firstLine")
            {
                public void addTo(Address object, String value)
                {
                    object._firstLineAdd(value);
                }
            };
        }
        else if (propertyName.equals("secondLine"))
        {
            return new ExternalDataObjectAdder<Address, String>("secondLine")
            {
                public void addTo(Address object, String value)
                {
                    object._secondLineAdd(value);
                }
            };
        }
        else if (propertyName.equals("city"))
        {
            return new ExternalDataObjectAdder<Address, String>("city")
            {
                public void addTo(Address object, String value)
                {
                    object._cityAdd(value);
                }
            };
        }
        else if (propertyName.equals("region"))
        {
            return new ExternalDataObjectAdder<Address, String>("region")
            {
                public void addTo(Address object, String value)
                {
                    object._regionAdd(value);
                }
            };
        }
        else if (propertyName.equals("country"))
        {
            return new ExternalDataObjectAdder<Address, String>("country")
            {
                public void addTo(Address object, String value)
                {
                    object._countryAdd(value);
                }
            };
        }
        else
        {
            throw new IllegalArgumentException("Unknown property " + propertyName);
        }
    }

    public List<IDefect> checkMultiplicities()
    {
        List<IDefect> defects = new ArrayList<IDefect>();
        if (this.firstLineSize < 1L || this.firstLineSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for firstLine: expected [1] found [" + this.firstLineSize + "]", "meta::external::shared::testpack::simple::Address"));
        }
        if (this.secondLineSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for secondLine: expected [0..1] found [" + this.secondLineSize + "]", "meta::external::shared::testpack::simple::Address"));
        }
        if (this.citySize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for city: expected [0..1] found [" + this.citySize + "]", "meta::external::shared::testpack::simple::Address"));
        }
        if (this.regionSize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for region: expected [0..1] found [" + this.regionSize + "]", "meta::external::shared::testpack::simple::Address"));
        }
        if (this.countrySize < 1L || this.countrySize > 1L)
        {
            defects.add(BasicDefect.newClassStructureDefect("Invalid multiplicity for country: expected [1] found [" + this.countrySize + "]", "meta::external::shared::testpack::simple::Address"));
        }
        return defects;
    }

    public String getAlloyStoreObjectReference$()
    {
        return null;
    }
}
