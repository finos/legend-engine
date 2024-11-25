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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.type.GenericType;
import org.finos.legend.engine.protocol.pure.v1.model.type.PackageableType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.packageableElement.PackageableElementPtr;

@JsonDeserialize(converter = AppliedFunction.AppliedFunctionConverter.class)
public class AppliedFunction extends AbstractAppliedFunction
{
    public String function;
    public String fControl;
    public List<ValueSpecification> parameters = Collections.emptyList();

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public static class AppliedFunctionConverter extends StdConverter<AppliedFunction, AppliedFunction>
    {
        @Override
        public AppliedFunction convert(AppliedFunction appliedFunction)
        {
            if (appliedFunction.function.equals("new"))
            {
                // Backward compatibility - old protocol -------------------------------------------------------------------
                if (appliedFunction.parameters.get(0) instanceof PackageableElementPtr)
                {
                    PackageableElementPtr packageableElementPtr = (PackageableElementPtr) appliedFunction.parameters.get(0);

                    Set<String> classesThatNeedTypeFixing = Sets.fixedSize.of(
                            "meta::pure::tds::BasicColumnSpecification",
                            "BasicColumnSpecification",
                            "meta::pure::tds::TdsOlapRank",
                            "TdsOlapRank"
                    );
                    if (classesThatNeedTypeFixing.contains(packageableElementPtr.fullPath))
                    {
                        Collection collection = (Collection) appliedFunction.parameters.get(2);
                        Optional<Lambda> func = ListIterate.detectOptional(collection.values, x -> ((CString) ((KeyExpression) x).key).value.equals("func"))
                                .map(KeyExpression.class::cast)
                                .map(x -> x.expression)
                                .filter(Lambda.class::isInstance)
                                .map(Lambda.class::cast)
                                .filter(x -> x.parameters.size() == 1);

                        if (func.isPresent())
                        {
                            Lambda l = func.get();
                            PackageableType rawType = new PackageableType(packageableElementPtr.fullPath);
                            rawType.sourceInformation = packageableElementPtr.sourceInformation;
                            List<GenericType> classType = Lists.mutable.of(new GenericType(rawType, Lists.mutable.with(l.parameters.get(0).genericType)));
                            GenericTypeInstance generic = new GenericTypeInstance(new GenericType(new PackageableType("meta::pure::metamodel::type::Class"), classType));
                            appliedFunction.parameters.set(0, generic);
                        }
                    }
                }
                // Backward compatibility -------------------------------------------------------------------
            }
            return appliedFunction;
        }
    }
}
