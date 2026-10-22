// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.AbstractValueParamProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.GenericType;
import java.util.function.Function;

public class MockPac4jFeature extends AbstractBinder
{

    @Override
    protected void configure()
    {
        bind(Pac4JProfileValueParamProvider.class).to(ValueParamProvider.class).in(Singleton.class);

        bind(ProfileManagerInjectionResolver.class).to(new GenericType<InjectionResolver<Pac4JProfileManager>>()
        {
        }).in(Singleton.class);
    }

    static class ProfileManagerInjectionResolver extends ParamInjectionResolver<Pac4JProfileManager>
    {
        @Inject
        public ProfileManagerInjectionResolver(Pac4JProfileValueParamProvider provider, Provider<ContainerRequest> request)
        {
            super(provider, Pac4JProfileManager.class, request);
        }
    }

    static class Pac4JProfileValueParamProvider extends AbstractValueParamProvider
    {
        @Inject
        protected Pac4JProfileValueParamProvider(Provider<MultivaluedParameterExtractorProvider> mpep)
        {
            super(mpep, Parameter.Source.UNKNOWN);
        }

        @Override
        protected Function<ContainerRequest, ?> createValueProvider(Parameter parameter)
        {
            return request -> null;
        }
    }

}
