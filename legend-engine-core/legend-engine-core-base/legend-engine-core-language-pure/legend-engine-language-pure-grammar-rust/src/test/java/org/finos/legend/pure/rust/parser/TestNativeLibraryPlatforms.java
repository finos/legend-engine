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

package org.finos.legend.pure.rust.parser;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The build and the loader name platforms alike: each of the pom's native-* profiles puts the library in the directory
 * NativeGrammarParser looks in on the JVMs that profile is active on, under the name those JVMs map the library to.
 */
public class TestNativeLibraryPlatforms
{
    /**
     * An os.name each profile's activation matches: a family stands for every version of it.
     */
    private static final Map<String, String> FAMILY_OS_NAMES = new LinkedHashMap<>();

    static
    {
        FAMILY_OS_NAMES.put("windows", "Windows 11");
    }

    @Test
    public void testProfilesAndLoaderAgree() throws Exception
    {
        Document pom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("pom.xml"));
        NodeList profiles = pom.getElementsByTagName("profile");
        int checked = 0;
        for (int i = 0; i < profiles.getLength(); i++)
        {
            Element profile = (Element) profiles.item(i);
            String platform = text(profile, "rust.native.platform");
            if (platform == null)
            {
                continue;
            }
            String osName = (text(profile, "name") != null) ? text(profile, "name") : FAMILY_OS_NAMES.get(text(profile, "family"));
            String osArch = text(profile, "arch");
            Assert.assertEquals(text(profile, "id"), platform, NativeGrammarParser.platform(osName, osArch));
            Assert.assertEquals(text(profile, "id"), libraryName(osName), text(profile, "rust.native.library"));
            checked++;
        }
        Assert.assertEquals(5, checked);
    }

    @Test
    public void testOtherPlatformsHaveNoDirectory()
    {
        Assert.assertNull(NativeGrammarParser.platform("SunOS", "amd64"));
        Assert.assertNull(NativeGrammarParser.platform("Linux", "ppc64le"));
    }

    /**
     * What System.mapLibraryName returns for the library on that operating system.
     */
    private static String libraryName(String osName)
    {
        if (osName.startsWith("Windows"))
        {
            return NativeGrammarParser.LIBRARY_NAME + ".dll";
        }
        return "lib" + NativeGrammarParser.LIBRARY_NAME + (osName.startsWith("Mac") ? ".dylib" : ".so");
    }

    private static String text(Element element, String tag)
    {
        NodeList nodes = element.getElementsByTagName(tag);
        return (nodes.getLength() == 0) ? null : nodes.item(0).getTextContent().trim();
    }
}
