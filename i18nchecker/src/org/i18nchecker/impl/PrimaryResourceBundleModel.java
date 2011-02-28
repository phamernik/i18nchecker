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

import java.util.List;
import java.util.Map;

/**
 * Represents one resource bundle of primary language (English) strings for one package.
 *
 * @author Petr Hamernik
 */
class PrimaryResourceBundleModel extends AbstractResourceBundleModel<PrimaryRBInfo> {
    /** These keys are mandatory. All modules should have them */
    private static final String[] MODULE_BUNDLE_MANDATORY_KEYS = { "OpenIDE-Module-Display-Category", "OpenIDE-Module-Name" };
    /** These keys are optional, so it's not reported as warning if they are not present */
    private static final String[] MODULE_BUNDLE_OPTIONAL_KEYS = { "OpenIDE-Module-Long-Description", "OpenIDE-Module-Short-Description" };

    public PrimaryResourceBundleModel(String fileName) {
        super(fileName);
    }

    /** Verify Module's own Bundle.properties as some module specific keys are not used in sources  */
    void verifyNBModuleBundle(ScanResults results) {
        for (String key: MODULE_BUNDLE_MANDATORY_KEYS) {
            PrimaryRBInfo info = keys.get(key);
            if (info != null) {
                info.markAsUsed();
            } else {
                results.add(ScanResults.Type.MODULE_MANIFEST_BUNDLE, getFileName(), 1, "Missing "+key+" NetBeans module bundle");
            }
        }
        for (String key: MODULE_BUNDLE_OPTIONAL_KEYS) {
            PrimaryRBInfo info = keys.get(key);
            if (info != null) {
                info.markAsUsed();
            }
        }
    }

    /** Report results into the provided ScanResults */
    void reportResults(ScanResults results) {
        results.incrementFileCounter(FileType.PRIMARY_BUNDLE);
        for (Map.Entry<String,PrimaryRBInfo> entry: keys.entrySet()) {
            String key = entry.getKey();
            PrimaryRBInfo info = entry.getValue();
            if ((info.getUsedCount() == 0) && (!info.isYesI18N())) {
                results.add(ScanResults.Type.MAYBE_UNUSED_KEY_IN_BUNDLE, getFileName(), info.getLine(), key);
            }
        }
    }

    /**
     * Mark key as used in source.
     *
     * @param key The key to resource
     * @return true if key was found, otherwise false
     */
    public boolean markAsUsed(String key) {
        PrimaryRBInfo info = keys.get(key);
        if (info == null) {
            return false;
        }
        info.markAsUsed();
        return true;
    }

    @Override
    protected PrimaryRBInfo createInfo(String value, int lineCount, boolean yesI18N) {
        return new PrimaryRBInfo(value, lineCount, yesI18N);
    }

    /**
     * Export strings for translation for a single package.
     *
     * @param translated represents resource bundle with strings which were already translated last time
     * @param module module short name
     * @param pack package name
     * @param exportTo export strings into this list
     */
    void findStringsForTranslationTo(TranslatedResourceBundleModel translated, String module, String pack, List<String> exportTo) {
        assert TranslatedCSVColumns.values().length == 5;
        String[] cols = new String[TranslatedCSVColumns.values().length];
        cols[TranslatedCSVColumns.MODULE.getIndex()] = module;
        cols[TranslatedCSVColumns.PACKAGE.getIndex()] = pack;

        for (Map.Entry<String,PrimaryRBInfo> entry: keys.entrySet()) {
            String key = entry.getKey();
            PrimaryRBInfo primaryInfo = entry.getValue();

            cols[TranslatedCSVColumns.KEY.getIndex()] = key;
            cols[TranslatedCSVColumns.PRIMARY.getIndex()] = primaryInfo.getValue().replace("\"", "\"\"");
            cols[TranslatedCSVColumns.TRANSLATED.getIndex()] = "";
            if (translated != null) {
                TranslatedRBInfo translatedInfo = translated.keys.get(key);
                if (translatedInfo != null) {
                    String str = translatedInfo.getValue();
                    str = I18NUtils.getJapString(str);
                    cols[TranslatedCSVColumns.TRANSLATED.getIndex()] = str;
                }
            }
            exportTo.add(I18NUtils.convertArrayToLine(cols));
        }
    }
}
