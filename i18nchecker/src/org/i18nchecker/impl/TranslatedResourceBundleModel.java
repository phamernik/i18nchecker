/**
*   Copyright 2010-2011 Petr Hamernik
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package org.i18nchecker.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents one translated resource bundle (e.g Bundle_ja.properties)
 *
 * @author Petr Hamernik
 */
class TranslatedResourceBundleModel extends AbstractResourceBundleModel<TranslatedRBInfo> {
    private static final String PREFIX = "Bundle_";
    private static final String EXT = ".properties";

    public TranslatedResourceBundleModel(String fileName) {
        super(fileName);
    }

    /** Report results into the provided ScanResults */
    void reportResults(ScanResults results) {
        results.incrementFileCounter(FileType.TRANSLATED_BUNDLE);
    }

    @Override
    protected TranslatedRBInfo createInfo(String value, int lineCount, boolean yesI18N) {
        return new TranslatedRBInfo(value, lineCount);
    }

    /** Language code e.g. "ja"
     *
     * @return language code
     */
    public String getLanguage() {
        String name = getFileName();
        return name.substring(name.lastIndexOf(PREFIX) + PREFIX.length(), name.length() - EXT.length());
    }

    /** Generate this resource bundle.
     *
     * @param header File header
     * @param translatedPackage all translated strings for one package
     */
    void generateTranslatedResource(List<String> header, Map<String, String> translatedPackage) throws IOException {
        List<String> strings2Export = new LinkedList<String>();
        System.out.println("Generating resource bundle: " + getFileName());
        for (String str: header) {
            strings2Export.add(str);
        }
        List<String> translatedKeys = new ArrayList<String>(translatedPackage.keySet());
        Collections.sort(translatedKeys);
        for (String key: translatedKeys) {
            strings2Export.add(key + "=" + translatedPackage.get(key));
        }
        I18NUtils.storeToFile(new File(getFileName()), strings2Export);
    }
}
