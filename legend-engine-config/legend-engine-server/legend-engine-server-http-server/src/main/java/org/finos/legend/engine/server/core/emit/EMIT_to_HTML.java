// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.server.core.emit;

import org.finos.legend.engine.test.emit.EMITModelDiscovery;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class EMIT_to_HTML
{
    private static final List<String> FULL_TAXONOMY = Arrays.asList(
            // grammar
            "grammar:association", "grammar:class-inheritance", "grammar:constraint",
            "grammar:derived-property", "grammar:enumeration", "grammar:function",
            "grammar:measure", "grammar:nested-association", "grammar:profile",
            "grammar:qualified-property",
            // mapping
            "mapping:aggregation-aware-mapping", "mapping:cross-store",
            "mapping:enumeration-mapping", "mapping:m2m-derived-source-property",
            "mapping:m2m-local-property", "mapping:m2m-transform", "mapping:mapping",
            "mapping:mapping-include", "mapping:operation-mapping",
            "mapping:operation-mapping-merge", "mapping:operation-mapping-merge-validation",
            "mapping:relational-association-implementation", "mapping:relational-distinct",
            "mapping:relational-embedded", "mapping:relational-group-by",
            "mapping:relational-inline-embedded", "mapping:relational-joined-table-inheritance",
            "mapping:relational-literal", "mapping:relational-literal-list",
            "mapping:relational-main-table-alias", "mapping:relational-otherwise-embedded",
            "mapping:relational-polymorphic-query", "mapping:relational-primary-key",
            "mapping:relational-single-table-inheritance", "mapping:relational-table-alias-column",
            "mapping:router-union", "mapping:store-union",
            // store
            "store:flat-data-store", "store:relational-cross-schema",
            "store:relational-cross-table-filter", "store:relational-dyna-function",
            "store:relational-filter", "store:relational-inline-view",
            "store:relational-inner-join", "store:relational-left-outer-join",
            "store:relational-multi-table", "store:relational-nested-join",
            "store:relational-outer-join", "store:relational-right-outer-join",
            "store:service-store",
            // milestoning
            "milestoning:all-versions-in-range-query", "milestoning:all-versions-query",
            "milestoning:bi-temporal", "milestoning:business-temporal",
            "milestoning:milestoning", "milestoning:point-in-time-query",
            "milestoning:processing-temporal",
            // execution
            "execution:bigquery-function", "execution:binding", "execution:data-element",
            "execution:external-format", "execution:external-format-binding",
            "execution:file-generation", "execution:hosted-service",
            "execution:model-generation", "execution:multi-execution-service",
            "execution:plan-generation", "execution:post-validation",
            "execution:schema-set", "execution:service", "execution:service-test",
            "execution:shared-test-data", "execution:snowflake-app", "execution:test-data",
            // scaffolding
            "scaffolding:class", "scaffolding:m2m-mapping", "scaffolding:model-connection",
            "scaffolding:relational-connection", "scaffolding:relational-mapping",
            "scaffolding:relational-store", "scaffolding:runtime"
    );

    private static final Map<String, String> DOMAIN_COLORS = new LinkedHashMap<>();

    static
    {
        DOMAIN_COLORS.put("grammar", "#4A90D9");
        DOMAIN_COLORS.put("mapping", "#E67E22");
        DOMAIN_COLORS.put("store", "#27AE60");
        DOMAIN_COLORS.put("milestoning", "#8E44AD");
        DOMAIN_COLORS.put("execution", "#C0392B");
        DOMAIN_COLORS.put("scaffolding", "#95A5A6");
    }

    public static void main(String[] args) throws Exception
    {
        List<EMITModelDescriptor> descriptors = EMITModelDiscovery.findDescriptorsViaSPI();
        String html = buildHTML(descriptors);
        Path output = Paths.get("./target/emit-coverage.html").toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        Files.write(output, html.getBytes(StandardCharsets.UTF_8));
        System.out.println("EMIT coverage report written to: " + output);
    }

    public static String buildHTML(List<EMITModelDescriptor> descriptors)
    {
        // Compute data structures
        Map<String, List<EMITModelDescriptor>> combinationMap = buildCombinationMap(descriptors);
        Map<String, Integer> featureHeatmap = buildFeatureHeatmap(descriptors);
        Set<String> allFeatures = new TreeSet<>();
        descriptors.forEach(d -> allFeatures.addAll(d.getFeatures()));
        Set<String> coveredFeatures = new TreeSet<>(featureHeatmap.keySet());
        List<String> uncoveredFeatures = FULL_TAXONOMY.stream()
                .filter(f -> !coveredFeatures.contains(f))
                .collect(Collectors.toList());

        // Group features by domain
        Map<String, List<String>> featuresByDomain = new LinkedHashMap<>();
        for (String feature : allFeatures)
        {
            String domain = feature.contains(":") ? feature.substring(0, feature.indexOf(':')) : "uncategorized";
            featuresByDomain.computeIfAbsent(domain, k -> new ArrayList<>()).add(feature);
        }

        // Build HTML
        StringBuilder sb = new StringBuilder();
        sb.append(HTML_HEAD);
        sb.append("<body>\n");
        sb.append("<div class=\"container\">\n");

        // Header
        sb.append("<header>\n");
        sb.append("  <h1>EMIT Coverage Report</h1>\n");
        sb.append("  <p class=\"subtitle\">Engine Model Integration Test — Feature Coverage Dashboard</p>\n");
        sb.append("  <div class=\"stats-bar\">\n");
        sb.append("    <div class=\"stat\"><span class=\"stat-value\">").append(descriptors.size()).append("</span><span class=\"stat-label\">Models</span></div>\n");
        sb.append("    <div class=\"stat\"><span class=\"stat-value\">").append(combinationMap.size()).append("</span><span class=\"stat-label\">Unique Combinations</span></div>\n");
        sb.append("    <div class=\"stat\"><span class=\"stat-value\">").append(coveredFeatures.size()).append("</span><span class=\"stat-label\">Features Covered</span></div>\n");
        sb.append("    <div class=\"stat\"><span class=\"stat-value\">").append(uncoveredFeatures.size()).append("</span><span class=\"stat-label\">Coverage Gaps</span></div>\n");
        sb.append("  </div>\n");
        sb.append("</header>\n\n");

        // Filter bar
        sb.append("<div class=\"filter-bar\">\n");
        sb.append("  <input type=\"text\" id=\"searchInput\" placeholder=\"Search features, models, tags...\" oninput=\"filterAll()\">\n");
        sb.append("  <select id=\"complexityFilter\" onchange=\"filterAll()\">\n");
        sb.append("    <option value=\"\">All Complexities</option>\n");
        sb.append("    <option value=\"basic\">Basic</option>\n");
        sb.append("    <option value=\"intermediate\">Intermediate</option>\n");
        sb.append("    <option value=\"advanced\">Advanced</option>\n");
        sb.append("  </select>\n");
        sb.append("  <select id=\"storeFilter\" onchange=\"filterAll()\">\n");
        sb.append("    <option value=\"\">All Stores</option>\n");
        Set<String> allStores = new TreeSet<>();
        descriptors.forEach(d -> allStores.addAll(d.getStores()));
        for (String store : allStores)
        {
            sb.append("    <option value=\"").append(escapeHtml(store)).append("\">").append(escapeHtml(store)).append("</option>\n");
        }
        sb.append("  </select>\n");
        sb.append("  <select id=\"domainFilter\" onchange=\"filterAll()\">\n");
        sb.append("    <option value=\"\">All Domains</option>\n");
        for (String domain : featuresByDomain.keySet())
        {
            sb.append("    <option value=\"").append(escapeHtml(domain)).append("\">").append(escapeHtml(domain)).append("</option>\n");
        }
        sb.append("  </select>\n");
        sb.append("</div>\n\n");

        // Tab navigation
        sb.append("<div class=\"tabs\">\n");
        sb.append("  <button class=\"tab active\" onclick=\"showTab('combinations')\">Feature Combinations</button>\n");
        sb.append("  <button class=\"tab\" onclick=\"showTab('heatmap')\">Feature Heatmap</button>\n");
        sb.append("  <button class=\"tab\" onclick=\"showTab('gaps')\">Coverage Gaps</button>\n");
        sb.append("  <button class=\"tab\" onclick=\"showTab('models')\">All Models</button>\n");
        sb.append("</div>\n\n");

        // Tab 1: Feature Combinations
        sb.append("<div id=\"combinations\" class=\"tab-content active\">\n");
        sb.append("<h2>Feature Combinations</h2>\n");
        sb.append("<p class=\"section-desc\">Each row represents a unique set of features covered by one or more EMIT models. Click a model name to view its YAML descriptor.</p>\n");
        sb.append("<table class=\"data-table\" id=\"combinationsTable\">\n");
        sb.append("<thead><tr><th>Features</th><th>Models</th><th>Complexity</th><th>Stores</th><th>Module</th></tr></thead>\n");
        sb.append("<tbody>\n");
        List<Map.Entry<String, List<EMITModelDescriptor>>> sortedCombos = new ArrayList<>(combinationMap.entrySet());
        sortedCombos.sort(Comparator.<Map.Entry<String, List<EMITModelDescriptor>>>comparingInt(e -> -e.getValue().size())
                .thenComparing(Map.Entry::getKey));
        for (Map.Entry<String, List<EMITModelDescriptor>> entry : sortedCombos)
        {
            List<EMITModelDescriptor> models = entry.getValue();
            EMITModelDescriptor first = models.get(0);
            String featuresDisplay = formatFeatureTags(first.getFeatures());
            String modelsDisplay = models.stream()
                    .map(m -> "<a href=\"#\" onclick=\"showModelDetail('" + escapeJs(m.getName()) + "'); return false;\" class=\"model-link\">" + escapeHtml(m.getName()) + "</a>")
                    .collect(Collectors.joining(", "));
            String complexity = first.getComplexity() != null ? first.getComplexity() : "—";
            String stores = first.getStores().isEmpty() ? "—" : String.join(", ", first.getStores());
            String module = models.stream().map(EMIT_to_HTML::displayModule).distinct().collect(Collectors.joining(", "));
            String dataFeatures = String.join(",", first.getFeatures());
            String dataTags = models.stream().flatMap(m -> m.getTags().stream()).distinct().collect(Collectors.joining(","));
            sb.append("<tr data-features=\"").append(escapeHtml(dataFeatures))
                    .append("\" data-complexity=\"").append(escapeHtml(complexity))
                    .append("\" data-stores=\"").append(escapeHtml(stores))
                    .append("\" data-tags=\"").append(escapeHtml(dataTags))
                    .append("\" data-models=\"").append(escapeHtml(models.stream().map(EMITModelDescriptor::getName).collect(Collectors.joining(","))))
                    .append("\">\n");
            sb.append("  <td class=\"features-cell\">").append(featuresDisplay).append("</td>\n");
            sb.append("  <td>").append(modelsDisplay).append("</td>\n");
            sb.append("  <td><span class=\"complexity-badge ").append(complexity).append("\">").append(escapeHtml(complexity)).append("</span></td>\n");
            sb.append("  <td>").append(escapeHtml(stores)).append("</td>\n");
            sb.append("  <td class=\"module-cell\">").append(escapeHtml(module)).append("</td>\n");
            sb.append("</tr>\n");
        }
        sb.append("</tbody></table>\n");
        sb.append("</div>\n\n");

        // Tab 2: Feature Heatmap
        sb.append("<div id=\"heatmap\" class=\"tab-content\">\n");
        sb.append("<h2>Feature Heatmap</h2>\n");
        sb.append("<p class=\"section-desc\">Each feature shown with the number of models covering it. Darker = more coverage.</p>\n");
        for (Map.Entry<String, List<String>> domainEntry : featuresByDomain.entrySet())
        {
            String domain = domainEntry.getKey();
            String color = DOMAIN_COLORS.getOrDefault(domain, "#7F8C8D");
            sb.append("<div class=\"domain-group\">\n");
            sb.append("  <h3 style=\"color: ").append(color).append("\">").append(escapeHtml(domain)).append("</h3>\n");
            sb.append("  <div class=\"heatmap-grid\">\n");
            for (String feature : domainEntry.getValue())
            {
                int count = featureHeatmap.getOrDefault(feature, 0);
                int maxCount = featureHeatmap.values().stream().mapToInt(Integer::intValue).max().orElse(1);
                double intensity = Math.min(1.0, (double) count / Math.max(maxCount, 1));
                String bgColor = interpolateColor(color, intensity);
                String capability = feature.contains(":") ? feature.substring(feature.indexOf(':') + 1) : feature;
                sb.append("    <div class=\"heatmap-cell\" style=\"background-color: ").append(bgColor)
                        .append("\" onclick=\"filterByFeature('").append(escapeJs(feature)).append("')\" title=\"")
                        .append(escapeHtml(feature)).append(" — ").append(count).append(" model(s)\">\n");
                sb.append("      <span class=\"heatmap-name\">").append(escapeHtml(capability)).append("</span>\n");
                sb.append("      <span class=\"heatmap-count\">").append(count).append("</span>\n");
                sb.append("    </div>\n");
            }
            sb.append("  </div>\n");
            sb.append("</div>\n");
        }
        sb.append("</div>\n\n");

        // Tab 3: Coverage Gaps
        sb.append("<div id=\"gaps\" class=\"tab-content\">\n");
        sb.append("<h2>Coverage Gaps</h2>\n");
        sb.append("<p class=\"section-desc\">Features from the full EMIT taxonomy (domain:capability format) with zero EMIT coverage. These represent untested pipeline paths.</p>\n");
        if (uncoveredFeatures.isEmpty())
        {
            sb.append("<p class=\"no-gaps\">All taxonomy features are covered!</p>\n");
        }
        else
        {
            Map<String, List<String>> gapsByDomain = new LinkedHashMap<>();
            for (String f : uncoveredFeatures)
            {
                String domain = f.contains(":") ? f.substring(0, f.indexOf(':')) : "uncategorized";
                gapsByDomain.computeIfAbsent(domain, k -> new ArrayList<>()).add(f);
            }
            for (Map.Entry<String, List<String>> gapEntry : gapsByDomain.entrySet())
            {
                String domain = gapEntry.getKey();
                String color = DOMAIN_COLORS.getOrDefault(domain, "#7F8C8D");
                sb.append("<div class=\"domain-group\">\n");
                sb.append("  <h3 style=\"color: ").append(color).append("\">").append(escapeHtml(domain))
                        .append(" <span class=\"gap-count\">(").append(gapEntry.getValue().size()).append(" uncovered)</span></h3>\n");
                sb.append("  <div class=\"gap-grid\">\n");
                for (String feature : gapEntry.getValue())
                {
                    String capability = feature.substring(feature.indexOf(':') + 1);
                    sb.append("    <div class=\"gap-cell\">").append(escapeHtml(capability)).append("</div>\n");
                }
                sb.append("  </div>\n");
                sb.append("</div>\n");
            }
        }
        sb.append("</div>\n\n");

        // Tab 4: All Models
        sb.append("<div id=\"models\" class=\"tab-content\">\n");
        sb.append("<h2>All Models</h2>\n");
        sb.append("<p class=\"section-desc\">Complete list of all EMIT models with their metadata.</p>\n");
        sb.append("<table class=\"data-table\" id=\"modelsTable\">\n");
        sb.append("<thead><tr><th>Name</th><th>Title</th><th>Features</th><th>Complexity</th><th>Stores</th><th>Tags</th><th>Module</th><th>YAML Path</th></tr></thead>\n");
        sb.append("<tbody>\n");
        for (EMITModelDescriptor d : descriptors)
        {
            String dataFeatures = String.join(",", d.getFeatures());
            String dataTags = String.join(",", d.getTags());
            String displayPath = displayPath(d);
            String module = displayModule(d);
            sb.append("<tr data-features=\"").append(escapeHtml(dataFeatures))
                    .append("\" data-complexity=\"").append(escapeHtml(d.getComplexity() != null ? d.getComplexity() : ""))
                    .append("\" data-stores=\"").append(escapeHtml(String.join(",", d.getStores())))
                    .append("\" data-tags=\"").append(escapeHtml(dataTags))
                    .append("\" data-models=\"").append(escapeHtml(d.getName()))
                    .append("\">\n");
            sb.append("  <td><strong>").append(escapeHtml(d.getName())).append("</strong></td>\n");
            sb.append("  <td>").append(escapeHtml(d.getTitle() != null ? d.getTitle() : "")).append("</td>\n");
            sb.append("  <td class=\"features-cell\">").append(formatFeatureTags(d.getFeatures())).append("</td>\n");
            sb.append("  <td><span class=\"complexity-badge ").append(d.getComplexity() != null ? d.getComplexity() : "").append("\">").append(escapeHtml(d.getComplexity() != null ? d.getComplexity() : "—")).append("</span></td>\n");
            sb.append("  <td>").append(d.getStores().isEmpty() ? "—" : escapeHtml(String.join(", ", d.getStores()))).append("</td>\n");
            sb.append("  <td class=\"tags-cell\">").append(formatTags(d.getTags())).append("</td>\n");
            sb.append("  <td class=\"module-cell\">").append(escapeHtml(module)).append("</td>\n");
            sb.append("  <td class=\"path-cell\"><code>").append(escapeHtml(displayPath.isEmpty() ? "—" : displayPath)).append("</code></td>\n");
            sb.append("</tr>\n");
        }
        sb.append("</tbody></table>\n");
        sb.append("</div>\n\n");

        // Model detail modal
        sb.append("<div id=\"modelModal\" class=\"modal\" onclick=\"if(event.target===this)closeModal()\">\n");
        sb.append("  <div class=\"modal-content\">\n");
        sb.append("    <span class=\"close\" onclick=\"closeModal()\">&times;</span>\n");
        sb.append("    <div id=\"modalBody\"></div>\n");
        sb.append("  </div>\n");
        sb.append("</div>\n\n");

        // Embed model data as JSON for client-side interactions
        sb.append("<script>\n");
        sb.append("const modelData = ");
        sb.append(buildModelJson(descriptors));
        sb.append(";\n");
        sb.append(CLIENT_JS);
        sb.append("</script>\n");

        sb.append("</div>\n"); // container
        sb.append("</body></html>\n");
        return sb.toString();
    }

    private static Map<String, List<EMITModelDescriptor>> buildCombinationMap(List<EMITModelDescriptor> descriptors)
    {
        Map<String, List<EMITModelDescriptor>> map = new LinkedHashMap<>();
        for (EMITModelDescriptor d : descriptors)
        {
            List<String> features = new ArrayList<>(d.getFeatures());
            features.sort(null);
            String key = String.join(" + ", features);
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }
        return map;
    }

    private static Map<String, Integer> buildFeatureHeatmap(List<EMITModelDescriptor> descriptors)
    {
        Map<String, Integer> heatmap = new TreeMap<>();
        for (EMITModelDescriptor d : descriptors)
        {
            for (String f : d.getFeatures())
            {
                heatmap.merge(f, 1, Integer::sum);
            }
        }
        return heatmap;
    }

    private static String formatFeatureTags(List<String> features)
    {
        if (features == null || features.isEmpty())
        {
            return "—";
        }
        return features.stream()
                .map(f ->
                {
                    String domain = f.contains(":") ? f.substring(0, f.indexOf(':')) : "uncategorized";
                    String capability = f.contains(":") ? f.substring(f.indexOf(':') + 1) : f;
                    String color = DOMAIN_COLORS.getOrDefault(domain, "#7F8C8D");
                    return "<span class=\"feature-tag\" style=\"border-color: " + color + "; color: " + color + "\" title=\"" + escapeHtml(f) + "\">" + escapeHtml(capability) + "</span>";
                })
                .collect(Collectors.joining(" "));
    }

    private static String formatTags(List<String> tags)
    {
        if (tags == null || tags.isEmpty())
        {
            return "—";
        }
        return tags.stream()
                .map(t -> "<span class=\"tag\">" + escapeHtml(t) + "</span>")
                .collect(Collectors.joining(" "));
    }

    private static String interpolateColor(String hexColor, double intensity)
    {
        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);
        double alpha = 0.1 + intensity * 0.5;
        return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, alpha);
    }

    private static String displayModule(EMITModelDescriptor d)
    {
        String module = d.getModule();
        return module != null ? module : "";
    }

    private static String displayPath(EMITModelDescriptor d)
    {
        String resourcePath = d.getResourcePath();
        return resourcePath != null ? resourcePath : "";
    }

    private static String buildModelJson(List<EMITModelDescriptor> descriptors)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (EMITModelDescriptor d : descriptors)
        {
            if (!first)
            {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJs(d.getName())).append("\":{");
            sb.append("\"name\":\"").append(escapeJs(d.getName())).append("\",");
            sb.append("\"title\":\"").append(escapeJs(d.getTitle() != null ? d.getTitle() : "")).append("\",");
            sb.append("\"description\":\"").append(escapeJs(d.getDescription() != null ? d.getDescription().replace("\n", "\\n") : "")).append("\",");
            sb.append("\"features\":").append(jsonList(d.getFeatures())).append(",");
            sb.append("\"stores\":").append(jsonList(d.getStores())).append(",");
            sb.append("\"complexity\":\"").append(escapeJs(d.getComplexity() != null ? d.getComplexity() : "")).append("\",");
            sb.append("\"tags\":").append(jsonList(d.getTags())).append(",");
            sb.append("\"module\":\"").append(escapeJs(displayModule(d))).append("\",");
            sb.append("\"path\":\"").append(escapeJs(displayPath(d))).append("\"");
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String jsonList(List<String> items)
    {
        if (items == null || items.isEmpty())
        {
            return "[]";
        }
        return "[" + items.stream().map(s -> "\"" + escapeJs(s) + "\"").collect(Collectors.joining(",")) + "]";
    }

    private static String escapeHtml(String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeJs(String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    private static final String HTML_HEAD = "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>EMIT Coverage Report</title>\n<style>\n" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, sans-serif; background: #f5f7fa; color: #2c3e50; line-height: 1.6; }\n" +
            ".container { max-width: 1400px; margin: 0 auto; padding: 24px; }\n" +
            "header { text-align: center; margin-bottom: 32px; }\n" +
            "header h1 { font-size: 2rem; color: #1a252f; margin-bottom: 4px; }\n" +
            ".subtitle { color: #7f8c8d; font-size: 0.95rem; margin-bottom: 16px; }\n" +
            ".stats-bar { display: flex; justify-content: center; gap: 32px; flex-wrap: wrap; }\n" +
            ".stat { text-align: center; }\n" +
            ".stat-value { display: block; font-size: 2rem; font-weight: 700; color: #2980b9; }\n" +
            ".stat-label { font-size: 0.8rem; color: #7f8c8d; text-transform: uppercase; letter-spacing: 0.5px; }\n" +
            ".filter-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }\n" +
            ".filter-bar input, .filter-bar select { padding: 8px 12px; border: 1px solid #dce1e6; border-radius: 6px; font-size: 0.9rem; background: #fff; }\n" +
            ".filter-bar input { flex: 1; min-width: 200px; }\n" +
            ".tabs { display: flex; gap: 4px; margin-bottom: 0; border-bottom: 2px solid #e1e8ed; }\n" +
            ".tab { padding: 10px 20px; border: none; background: none; cursor: pointer; font-size: 0.9rem; font-weight: 500; color: #7f8c8d; border-bottom: 2px solid transparent; margin-bottom: -2px; transition: all 0.2s; }\n" +
            ".tab:hover { color: #2980b9; }\n" +
            ".tab.active { color: #2980b9; border-bottom-color: #2980b9; }\n" +
            ".tab-content { display: none; padding: 24px 0; }\n" +
            ".tab-content.active { display: block; }\n" +
            ".section-desc { color: #7f8c8d; margin-bottom: 16px; font-size: 0.9rem; }\n" +
            ".data-table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }\n" +
            ".data-table thead { background: #f8fafc; }\n" +
            ".data-table th { padding: 12px 16px; text-align: left; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.5px; color: #5a6c7d; border-bottom: 2px solid #e1e8ed; }\n" +
            ".data-table td { padding: 10px 16px; border-bottom: 1px solid #f0f3f6; font-size: 0.85rem; vertical-align: top; }\n" +
            ".data-table tbody tr:hover { background: #f8fafc; }\n" +
            ".feature-tag { display: inline-block; padding: 2px 8px; margin: 2px; border: 1px solid; border-radius: 12px; font-size: 0.75rem; font-weight: 500; }\n" +
            ".tag { display: inline-block; padding: 2px 6px; margin: 1px; background: #ecf0f1; border-radius: 4px; font-size: 0.72rem; color: #5a6c7d; }\n" +
            ".complexity-badge { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; }\n" +
            ".complexity-badge.basic { background: #d5f5e3; color: #1e8449; }\n" +
            ".complexity-badge.intermediate { background: #fdebd0; color: #d35400; }\n" +
            ".complexity-badge.advanced { background: #fadbd8; color: #922b21; }\n" +
            ".model-link { color: #2980b9; text-decoration: none; font-weight: 500; }\n" +
            ".model-link:hover { text-decoration: underline; }\n" +
            ".module-cell { font-size: 0.78rem; color: #7f8c8d; }\n" +
            ".path-cell code { font-size: 0.75rem; color: #6c757d; background: #f8f9fa; padding: 2px 4px; border-radius: 3px; }\n" +
            ".domain-group { margin-bottom: 24px; }\n" +
            ".domain-group h3 { margin-bottom: 8px; font-size: 1rem; }\n" +
            ".heatmap-grid { display: flex; flex-wrap: wrap; gap: 8px; }\n" +
            ".heatmap-cell { padding: 8px 12px; border-radius: 6px; cursor: pointer; text-align: center; min-width: 100px; transition: transform 0.15s; }\n" +
            ".heatmap-cell:hover { transform: scale(1.05); box-shadow: 0 2px 8px rgba(0,0,0,0.12); }\n" +
            ".heatmap-name { display: block; font-size: 0.78rem; font-weight: 500; }\n" +
            ".heatmap-count { display: block; font-size: 1.2rem; font-weight: 700; }\n" +
            ".gap-grid { display: flex; flex-wrap: wrap; gap: 6px; }\n" +
            ".gap-cell { padding: 6px 12px; background: #fdf2f2; border: 1px dashed #e74c3c; border-radius: 6px; font-size: 0.8rem; color: #c0392b; }\n" +
            ".gap-count { font-size: 0.8rem; font-weight: normal; color: #e74c3c; }\n" +
            ".no-gaps { color: #27ae60; font-weight: 500; font-size: 1.1rem; }\n" +
            ".modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; justify-content: center; align-items: center; }\n" +
            ".modal.show { display: flex; }\n" +
            ".modal-content { background: #fff; border-radius: 12px; padding: 32px; max-width: 700px; width: 90%; max-height: 80vh; overflow-y: auto; position: relative; }\n" +
            ".close { position: absolute; top: 12px; right: 16px; font-size: 1.5rem; cursor: pointer; color: #7f8c8d; }\n" +
            ".close:hover { color: #2c3e50; }\n" +
            ".modal-content h3 { margin-bottom: 8px; }\n" +
            ".modal-content p { margin-bottom: 12px; color: #5a6c7d; font-size: 0.9rem; }\n" +
            ".modal-content .detail-row { margin-bottom: 12px; }\n" +
            ".modal-content .detail-label { font-weight: 600; font-size: 0.8rem; text-transform: uppercase; color: #7f8c8d; }\n" +
            ".features-cell { max-width: 400px; }\n" +
            ".tags-cell { max-width: 200px; }\n" +
            "@media (max-width: 768px) { .container { padding: 12px; } .filter-bar { flex-direction: column; } .data-table { font-size: 0.8rem; } }\n" +
            "</style>\n</head>\n";

    private static final String CLIENT_JS =
            "function showTab(tabId) {\n" +
            "  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));\n" +
            "  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));\n" +
            "  document.getElementById(tabId).classList.add('active');\n" +
            "  event.target.classList.add('active');\n" +
            "}\n\n" +
            "function filterAll() {\n" +
            "  const search = document.getElementById('searchInput').value.toLowerCase();\n" +
            "  const complexity = document.getElementById('complexityFilter').value;\n" +
            "  const store = document.getElementById('storeFilter').value;\n" +
            "  const domain = document.getElementById('domainFilter').value;\n" +
            "  document.querySelectorAll('.data-table tbody tr').forEach(row => {\n" +
            "    const features = (row.dataset.features || '').toLowerCase();\n" +
            "    const tags = (row.dataset.tags || '').toLowerCase();\n" +
            "    const models = (row.dataset.models || '').toLowerCase();\n" +
            "    const rowComplexity = row.dataset.complexity || '';\n" +
            "    const rowStores = (row.dataset.stores || '').toLowerCase();\n" +
            "    let show = true;\n" +
            "    if (search && !(features.includes(search) || tags.includes(search) || models.includes(search))) show = false;\n" +
            "    if (complexity && rowComplexity !== complexity) show = false;\n" +
            "    if (store && !rowStores.includes(store.toLowerCase())) show = false;\n" +
            "    if (domain && !features.split(',').some(f => f.startsWith(domain + ':'))) show = false;\n" +
            "    row.style.display = show ? '' : 'none';\n" +
            "  });\n" +
            "}\n\n" +
            "function filterByFeature(feature) {\n" +
            "  document.getElementById('searchInput').value = feature;\n" +
            "  showTab('combinations');\n" +
            "  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));\n" +
            "  document.querySelector('.tab').classList.add('active');\n" +
            "  filterAll();\n" +
            "}\n\n" +
            "function showModelDetail(name) {\n" +
            "  const m = modelData[name];\n" +
            "  if (!m) return;\n" +
            "  const body = document.getElementById('modalBody');\n" +
            "  body.innerHTML = '<h3>' + m.name + '</h3>' +\n" +
            "    '<p><em>' + m.title + '</em></p>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">Description</span><p>' + m.description.replace(/\\\\n/g, '<br>') + '</p></div>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">Features</span><p>' + m.features.map(f => '<span class=\"feature-tag\" style=\"border-color:#2980b9;color:#2980b9\">' + f + '</span>').join(' ') + '</p></div>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">Stores</span><p>' + (m.stores.length ? m.stores.join(', ') : 'None') + '</p></div>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">Complexity</span><p>' + m.complexity + '</p></div>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">Tags</span><p>' + m.tags.map(t => '<span class=\"tag\">' + t + '</span>').join(' ') + '</p></div>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">Module</span><p>' + m.module + '</p></div>' +\n" +
            "    '<div class=\"detail-row\"><span class=\"detail-label\">YAML Path</span><p><code>' + m.path + '</code></p></div>';\n" +
            "  document.getElementById('modelModal').classList.add('show');\n" +
            "}\n\n" +
            "function closeModal() {\n" +
            "  document.getElementById('modelModal').classList.remove('show');\n" +
            "}\n\n" +
            "document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });\n";
}
