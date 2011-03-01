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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Superclass for classes representing one resource bundle (primary or translated).
 *
 * @author Petr Hamernik
 */
abstract class AbstractResourceBundleModel<T extends AbstractRBInfo> {
    private static final String YESI18N = "YESI18N";

    private String fileName;
    protected Map<String, T> keys;

    public AbstractResourceBundleModel(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    /** Parse resource bundle file and fill keys */
    public void parse() throws IOException {
        keys = new TreeMap<String, T>();

        FileInputStream fis = new FileInputStream(fileName);
        InputStreamReader in = new InputStreamReader(fis, "UTF-8"); // NOI18N

        FileReader fr = new FileReader(fileName);
        try {
            BufferedReader br = new BufferedReader(in);

            T lastInfo = null;
            int lineCount = 0;
            boolean insideMultiLineKey = false;
            boolean yesI18N = false;
            for (;;) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                lineCount++;
                if (insideMultiLineKey) {
                    if (!line.trim().endsWith("\\")) {
                        insideMultiLineKey = false;
                    }
                    if (lastInfo != null) {
                        lastInfo.appendNextLineToValue(line);
                    }
                    continue;
                }
                if (line.startsWith("#")) {
                    if (line.indexOf(YESI18N) > 0) {
                        yesI18N = true;
                    }
                    continue;
                }

                line = line.trim();
                if (line.length() > 0) {
                    int indexOfEqual = line.indexOf("=");
                    if (indexOfEqual <= 0) {
                        String msg = MessageFormat.format(
                                "{0}:{1}: WARNING: incorrect key: {2}",
                                new Object[] { fileName, lineCount, line } );
                        System.out.println(msg);
                    } else {
                        String key = line.substring(0, indexOfEqual).trim();
                        String value = line.substring(indexOfEqual + 1);
                        lastInfo = createInfo(value, lineCount, yesI18N);
                        keys.put(key, lastInfo);
                    }
                }

                yesI18N = false;
                if (line.trim().endsWith("\\")) {
                    insideMultiLineKey = true;
                } else {
                    lastInfo = null;
                }
            }
        } finally {
            fr.close();
        }
    }

    /** Create a own specific instance of Info object. One info object represents
     * one value in resource bundles.
     *
     * @param value text value of resource bundle string
     * @param lineCount where is it?
     * @param yesI18N was #YESI18N used?
     * @return
     */
    protected abstract T createInfo(String value, int lineCount, boolean yesI18N);

    /** Report results into the provided ScanResults */
    abstract void reportResults(ScanResults results);
}
