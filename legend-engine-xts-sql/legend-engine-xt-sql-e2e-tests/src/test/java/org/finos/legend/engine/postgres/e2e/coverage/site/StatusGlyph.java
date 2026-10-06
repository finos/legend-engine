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
 * Single source of truth for the green/amber/red/grey status aggregation rule and its
 * paired glyph + aria-label (plan &sect;4.2 legend, &sect;8.4, &sect;10.3 status rule):
 *
 * <ul>
 *   <li>PASS ? every linked test passes on this path -&gt; green ?</li>
 *   <li>PARTIAL ? some pass, some don't -&gt; amber ?</li>
 *   <li>FAIL / ERROR ? none pass -&gt; red ?</li>
 *   <li>NOT_APPLICABLE ? unsupported/out-of-scope -&gt; grey ?</li>
 *   <li>UNTESTED ? no test references this catalog entry -&gt; grey ?</li>
 * </ul>
 *
 * Every glyph is paired with a text label so the site is usable in monochrome
 * (plan &sect;8.4 accessibility requirement).
 */
public final class StatusGlyph
{
    private StatusGlyph()
    {
    }

    public static final class Glyph
    {
        public final String symbol;
        public final String cssClass;
        public final String ariaLabel;

        Glyph(String symbol, String cssClass, String ariaLabel)
        {
            this.symbol = symbol;
            this.cssClass = cssClass;
            this.ariaLabel = ariaLabel;
        }
    }

    public static Glyph forStatus(String status)
    {
        if (status == null)
        {
            status = "UNTESTED";
        }
        switch (status)
        {
            case "PASS":
                return new Glyph("\u2705", "status-pass", "Pass");
            case "PARTIAL":
                return new Glyph("\u25D0", "status-partial", "Partial");
            case "FAIL":
                return new Glyph("\u274C", "status-fail", "Fail");
            case "ERROR":
                return new Glyph("\u274C", "status-fail", "Error");
            case "NOT_APPLICABLE":
            case "UNSUPPORTED":
                return new Glyph("\u26AA", "status-na", "Not applicable");
            case "SKIP":
                return new Glyph("\u26AA", "status-na", "Skipped");
            default:
                return new Glyph("\u25CB", "status-untested", "Untested");
        }
    }

    public static boolean isUntested(String status)
    {
        return status == null || "UNTESTED".equals(status);
    }

    public static String label(String status, int pass, int total)
    {
        if (status == null)
        {
            status = "UNTESTED";
        }
        switch (status)
        {
            case "PASS":
            case "PARTIAL":
            case "FAIL":
                return String.format("%s (%d/%d)", status, pass, total);
            case "ERROR":
                return String.format("ERROR (0/%d)", total);
            case "UNSUPPORTED":
                return String.format("UNSUPPORTED (%d/%d)", pass, total);
            case "NOT_APPLICABLE":
                return "UNSUPPORTED";
            default:
                return "UNTESTED";
        }
    }
}

