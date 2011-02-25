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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Collects results per module.
 *
 * @author Petr Hamernik
 */
class ScanResults {
    private static final MessageFormat MSG = new MessageFormat("{0}:{1}: {2}");
    private static final MessageFormat SUMMARY = new MessageFormat("Scanned {0} Java sources, {1} primary and {2} translated resource bundles. Found {3} potential problems.");

    public enum Type {
        MISSING_KEY_IN_BUNDLE("Very likely missing key in resource bundle"),
        MISSING_NOI18N_OR_KEY_IN_BUNDLE("Probably missing key in resource bundle or string should be marked with // NOI18N"),
        MAYBE_UNUSED_KEY_IN_BUNDLE("Probably unused resource bundle"),
        MODULE_MANIFEST_BUNDLE("Module's resource bundle specified in manifest.mf"),
        NOT_NECESSARY_TO_USE_NOI18N("It is redundant to use NOI18N when String actually is in resource bundle");

        private String description;

        private Type(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private String name;
    private Map<Type,List<String>> results = new EnumMap<Type,List<String>>(Type.class);
    private int sourceCount;
    private int bundleCount;
    private int translatedBundleCount;

    /**
     * New ScanResults instance (typically for a whole module).
     *
     * @param name Display name (usually name of module) which is used when results are printed
     */
    public ScanResults(String name) {
        this.name = name;
    }

    /**
     * Add a warning/error message to this results
     *
     * @param type The type of problem
     * @param fileName Name of file where is the problem
     * @param line line number inside the file
     * @param message Some description message
     */
    public void add(Type type, String fileName, int line, String message) {
        List<String> list = results.get(type);
        if (list == null) {
            list = new ArrayList<String>();
            results.put(type, list);
        }
        list.add(MSG.format(new Object[] { fileName, Integer.toString(line), message }));
    }

    /** Just a primitive counter of Java/bundle files to be printed in summary
     */
    void incrementFileCounter(FileType type) {
        if (type == FileType.JAVA) {
            sourceCount++;
        } else if (type == FileType.PRIMARY_BUNDLE) {
            bundleCount++;
        } else if (type == FileType.TRANSLATED_BUNDLE) {
            translatedBundleCount++;
        }
    }

    public int getProblemsCount() {
        int count = 0;
        for (Type type: Type.values()) {
            List<String> list = results.get(type);
            if (list != null) {
                count += list.size();
            }
        }
        return count;
    }

    /** Print all results to System.out */
    public void printAll(boolean details) {
        I18NUtils.LOG.log(Level.INFO, "\nModule: {0}", name);
        I18NUtils.LOG.log(Level.INFO, 
                SUMMARY.format(new Object[] { sourceCount, bundleCount,
                translatedBundleCount, getProblemsCount() }));
        if (details) {
            for (Type type: Type.values()) {
                List<String> list = results.get(type);
                if (list != null) {
                    I18NUtils.LOG.log(Level.INFO, type.getDescription());
                    for (String val: list) {
                        I18NUtils.LOG.log(Level.INFO, val);
                    }
                }
            }
        }
    }
}
