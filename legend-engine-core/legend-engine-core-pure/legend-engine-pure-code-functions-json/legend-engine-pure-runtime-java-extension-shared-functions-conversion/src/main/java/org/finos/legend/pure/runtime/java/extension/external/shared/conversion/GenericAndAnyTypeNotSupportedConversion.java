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

package org.finos.legend.pure.runtime.java.extension.external.shared.conversion;

import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.exception.PureExecutionException;

public class GenericAndAnyTypeNotSupportedConversion<T> implements Conversion<Object, T>
{
    public static final GenericAndAnyTypeNotSupportedConversion GENERIC_AND_ANY_TYPE_NOT_SUPPORTED_CONVERSION = new GenericAndAnyTypeNotSupportedConversion();

    @Override
    public T apply(Object value, ConversionContext context)
    {
        throw new PureExecutionException("Deserialization of generics currently not supported!", Stacks.mutable.empty());
    }

    @Override
    public String pureTypeAsString()
    {
        //TODO probably return something more indicative that this is generic type....
        return "T";
    }
}
