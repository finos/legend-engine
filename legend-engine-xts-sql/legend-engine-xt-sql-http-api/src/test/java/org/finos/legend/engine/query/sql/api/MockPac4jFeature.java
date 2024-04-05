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

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.inject.Inject;
import javax.inject.Singleton;


public class MockPac4jFeature extends AbstractBinder
{

    @Override
    protected void configure()
    {
        bind(Pac4JProfileValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);

        bind(ProfileManagerInjectionResolver.class).to(new TypeLiteral<InjectionResolver<Pac4JProfileManager>>()
        {
        }).in(Singleton.class);
    }

    static class ProfileManagerInjectionResolver extends ParamInjectionResolver<Pac4JProfileManager>
    {
        public ProfileManagerInjectionResolver()
        {
            super(Pac4JProfileValueFactoryProvider.class);
        }
    }

    static class Pac4JProfileValueFactoryProvider extends AbstractValueFactoryProvider
    {
        @Inject
        protected Pac4JProfileValueFactoryProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator locator)
        {
            super(mpep, locator, Parameter.Source.UNKNOWN);
        }

        @Override
        protected Factory<?> createValueFactory(Parameter parameter)
        {
            return new Factory<ProfileManager<CommonProfile>>()
            {
                @Override
                public ProfileManager<CommonProfile> provide()
                {
                    return null;
                }

                @Override
                public void dispose(ProfileManager<CommonProfile> commonProfileProfileManager)
                {

                }
            };
        }
    }

}
