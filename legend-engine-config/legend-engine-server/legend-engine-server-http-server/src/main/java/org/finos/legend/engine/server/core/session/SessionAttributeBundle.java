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

package org.finos.legend.engine.server.core.session;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.jee.context.session.JEESessionStore;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.profile.ProfileManager;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.Optional;

public class SessionAttributeBundle implements ConfiguredBundle<Configuration>
{
    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
    }

    @Override
    public void run(Configuration configuration, Environment environment)
    {
        environment.servlets().addFilter("SessionAttributeEnricher", Enricher.class)
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    public static class Enricher implements Filter
    {

        @Override
        public void init(FilterConfig filterConfig)
        {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException
        {
            HttpSession session = ((HttpServletRequest) request).getSession();
            Principal userPrincipal = ((HttpServletRequest) request).getUserPrincipal();
            if (userPrincipal != null)
            {
                session.setAttribute(SessionTracker.ATTR_USER_ID, userPrincipal.getName());
                WebContext context = new JEEContext((HttpServletRequest) request, (HttpServletResponse) response);
                ProfileManager manager = new ProfileManager(context, new JEESessionStore());
                Optional<UserProfile> profile = manager.getProfile();
                profile.ifPresent(userProfile -> session.setAttribute(SessionTracker.ATTR_USER_PROFILE, userProfile));
            }
            session.setAttribute(SessionTracker.ATTR_CALLS, (session.getAttribute(SessionTracker.ATTR_CALLS) == null ? 0 : (Integer) session.getAttribute(SessionTracker.ATTR_CALLS)) + 1);
            chain.doFilter(request, response);
        }

        @Override
        public void destroy()
        {
        }
    }
}
