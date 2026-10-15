// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.postgres.e2e.coverage.site;

/**
 * Minimal HTML-escaping / tag-building helpers used by {@link DocumentationSiteGenerator} in place
 * of a template engine (plan &sect;10.4 ? no new heavyweight deps).
 */
public final class Html
{
    private Html()
    {
    }

    public static String escape(String s)
    {
        if (s == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Escapes a string for safe inclusion inside a double-quoted HTML attribute value.
     */
    public static String attr(String s)
    {
        return escape(s);
    }

    public static String tag(String name, String attrs, String body)
    {
        return "<" + name + (attrs == null || attrs.isEmpty() ? "" : " " + attrs) + ">" + body + "</" + name + ">";
    }
}

