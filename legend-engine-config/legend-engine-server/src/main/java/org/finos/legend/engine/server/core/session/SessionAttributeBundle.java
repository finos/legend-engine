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

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.Optional;

public class SessionAttributeBundle implements Bundle
{
    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
    }

    @Override
    public void run(Environment environment)
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
                WebContext context = new J2EContext((HttpServletRequest) request, (HttpServletResponse) response);
                ProfileManager<CommonProfile> manager = new ProfileManager<>(context);
                Optional<CommonProfile> profile = manager.get(true);
                profile.ifPresent(commonProfile -> session.setAttribute(SessionTracker.ATTR_USER_PROFILE, commonProfile));
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
