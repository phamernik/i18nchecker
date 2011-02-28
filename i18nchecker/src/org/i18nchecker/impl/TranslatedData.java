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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Parser and provider of complete translation to a single language.
 *
 * @author Petr
 */
public class TranslatedData {

    public static String getCSVFileHeader() {
        String[] cols = new String[TranslatedCSVColumns.values().length];
        for (TranslatedCSVColumns col : TranslatedCSVColumns.values()) {
            cols[col.getIndex()] = col.getHeaderName();
        }
        return I18NUtils.convertArrayToLine(cols);
    }
    /**
     * Module name (in simple format e.g. "modules/UIWidgets")
     * -> Package name (in format e.g. "/com/im/df/api")
     * -> [Message key, Message translation]
     */
    private Map<String, Map<String, Map<String, String>>> data;

    public TranslatedData(File sourceFile) throws IOException {
        System.out.println("Parsing translation: " + sourceFile);
        FileInputStream fis = new FileInputStream(sourceFile);
        InputStreamReader in = new InputStreamReader(fis, "UTF-8"); // NOI18N
        CsvListReader reader = new CsvListReader(in, CsvPreference.EXCEL_PREFERENCE);
        data = new HashMap<String, Map<String, Map<String, String>>>();
        try {
            for (;;) {
                List<String> line = reader.read();
                if (line == null) {
                    break;
                }
                int expectedColsCount = TranslatedCSVColumns.values().length;
                if (line.size() != expectedColsCount) {
                    throw new IOException("Line has wrong number of values (" + line.size() + " instead of " + expectedColsCount + "): " + line);
                }

                String translated = line.get(TranslatedCSVColumns.TRANSLATED.getIndex());
                if (translated.trim().length() == 0) {
                    continue;
                }
                translated = I18NUtils.getUTFString(translated);

                // get module
                String moduleName = line.get(TranslatedCSVColumns.MODULE.getIndex());
                Map<String, Map<String, String>> module = data.get(moduleName);
                if (module == null) {
                    module = new HashMap<String, Map<String, String>>();
                    data.put(moduleName, module);
                }

                // get package
                String packageName = line.get(TranslatedCSVColumns.PACKAGE.getIndex());
                Map<String, String> pack = module.get(packageName);
                if (pack == null) {
                    pack = new HashMap<String, String>();
                    module.put(packageName, pack);
                }

                pack.put(line.get(TranslatedCSVColumns.KEY.getIndex()), translated);
            }
        } finally {
            reader.close();
        }
    }

    public Map<String, Map<String, String>> getTranslationsForModule(String moduleSimpleName) {
        return data.get(moduleSimpleName);
    }
}
