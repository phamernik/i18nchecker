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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Petr Hamernik
 */
class PackageScanner {
    private File packageDir;

    /** One primary resource bundle per package. */
    private PrimaryResourceBundleModel primaryBundle;

    /** Default bundles other than the primary bundle. */
    private Set<PrimaryResourceBundleModel> otherBundles;
    
    /** Translations */
    private Set<TranslatedResourceBundleModel> translatedBundles;

    /** Java sources in this package */
    private Map<String, JavaSourceModel> sources;

    /** Simple package name e.g. "com/im/df/api" */
    private String simpleName;

    public PackageScanner(File packageDir, String moduleDirName) throws IOException {
        this.packageDir = packageDir;
        this.sources = new TreeMap<String, JavaSourceModel>();
        this.translatedBundles = new HashSet<TranslatedResourceBundleModel>();
        this.otherBundles = new HashSet<PrimaryResourceBundleModel>();
        this.simpleName = packageDir.getCanonicalPath().substring(moduleDirName.length()).replace(File.separator, "/");
    }

    /**
     * Add file to model.
     *
     * @param type is it java or resource bundle?
     * @param name Name of the file
     */
    public void addFile(FileType type, String name) {
        switch (type) {
            case PRIMARY_BUNDLE:
                if (primaryBundle != null) {
                    throw new IllegalStateException("Not implemented yet: more resource bundles in one package: "+packageDir);
                }
                primaryBundle = new PrimaryResourceBundleModel(packageDir + File.separator + name);
                break;
            case TRANSLATED_BUNDLE:
                translatedBundles.add(new TranslatedResourceBundleModel(packageDir + File.separator + name));
                break;
            case OTHER_BUNDLE:
                otherBundles.add(new PrimaryResourceBundleModel(packageDir + File.separator + name));
                break;
            case JAVA:
                sources.put(name, new JavaSourceModel(packageDir + File.separator + name));
                break;
        }
    }

    /** Parse all files
     *
     * @throws IOException
     */
    public void parseFiles() throws IOException {
        if (primaryBundle != null) {
            primaryBundle.parse();
        }
        for (PrimaryResourceBundleModel other: otherBundles) {
            other.parse();
        }
        for (TranslatedResourceBundleModel translated: translatedBundles) {
            translated.parse();
        }
        for (JavaSourceModel source: sources.values()) {
            source.parse();
        }
    }

    /** Verify whole package.
     */
    public void verify() {
        for (JavaSourceModel source: sources.values()) {
            source.verify(primaryBundle);
            for (PrimaryResourceBundleModel other: otherBundles) {
                source.verify(other);
            }
        }
    }

    /** Verify module's resource bundle.
     *
     * @param results
     */
    public void verifyNBModuleBundle(ScanResults results) {
        primaryBundle.verifyNBModuleBundle(results);
    }

    /**
     * Checks if the primary bundle contains given key and marks it as used.
     *
     * @param layerFileName name of layer XML file
     */
    public boolean markAsUsed(String key) {
        return primaryBundle.markAsUsed(key);
    }

    /** Report results of verification */
    public void reportResults(ScanResults results) {
        for (JavaSourceModel source: sources.values()) {
            source.reportResults(results);
        }
        if (primaryBundle != null) {
            primaryBundle.reportResults(results);
        }
        for (PrimaryResourceBundleModel other: otherBundles) {
            other.reportResults(results);
        }
        for (TranslatedResourceBundleModel trb: translatedBundles) {
            trb.reportResults(results);
        }
    }

    public void bundle2csv(String languageCode, List<String> exportTo, String moduleName) throws IOException {
        if (primaryBundle != null) {
            TranslatedResourceBundleModel translated = findLanguage(languageCode);
            primaryBundle.findStringsForTranslationTo(translated, moduleName, simpleName, exportTo);
        }
    }

    void csv2bundle(String language, List<String> header, Map<String, String> translatedPackage) throws IOException {
        TranslatedResourceBundleModel translationRB = findLanguage(language);
        if (translationRB == null) {
            translationRB = new TranslatedResourceBundleModel(packageDir + File.separator + "Bundle_" + language + ".properties");
        }
        translationRB.generateTranslatedResource(header, translatedPackage);

    }

    private TranslatedResourceBundleModel findLanguage(String language) {
        for (TranslatedResourceBundleModel trb: translatedBundles) {
            if (language.equals(trb.getLanguage())) {
                return trb;
            }
        }
        return null;
    }

    public String getSimpleName() {
        return simpleName;
    }
}
