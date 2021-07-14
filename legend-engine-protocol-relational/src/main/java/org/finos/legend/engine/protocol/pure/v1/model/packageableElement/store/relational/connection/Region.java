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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

public enum Region
{
    US_EAST_1, US_EAST_2, US_WEST_1, US_WEST_2, AF_SOUTH_1, AP_EAST_1 , AP_SOUTH_1, AP_NORTHEAST_1, AP_NORTHEAST_2,
    AP_NORTHEAST_3, AP_SOUTHEAST_1, AP_SOUTHEAST_2, CA_CENTRAL_1, EU_CENTRAL_1, EU_WEST_1, EU_WEST_2, EU_WEST_3,
    EU_SOUTH_1, EU_NORTH_1, ME_SOUTH_1, SA_EAST_1, US_GOV_EAST_1, US_GOV_WEST_1;

    @Override
    public  String toString(){
        return this.name().toLowerCase().replace("_","-");
    }
}
