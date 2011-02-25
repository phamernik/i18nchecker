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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.tools.ant.DirectoryScanner;

/**
 * Scanner for a single module strings.
 *
 * @author Petr Hamernik
 */
public class ModuleScanner {
    public static final String SRC_DIR = "src";
    public static final String MANIFEST_FILE = "manifest.mf";
    private static final String MANIFEST_BUNDLE_LINK ="OpenIDE-Module-Localizing-Bundle: ";

    private File rootDir;
    private Map<String, PackageScanner> packages;
    private ScanResults results;
    private String simpleName;

    public ModuleScanner(File rootDir) throws IOException {
        this.rootDir = rootDir;
        this.packages = new TreeMap<String, PackageScanner>();
        this.results = new ScanResults(rootDir.toString());
        this.simpleName = createSimpleName(rootDir);
    }

    /** Scan module, verify I18N and collects results */
    public void scan() throws IOException {
        scanFiles(FileType.PRIMARY_BUNDLE);
        scanFiles(FileType.TRANSLATED_BUNDLE);
        scanFiles(FileType.JAVA);

        for (PackageScanner ps: packages.values()) {
            ps.parseFiles();
            ps.verify();
        }
        verifyManifest();

        for (PackageScanner ps: packages.values()) {
            ps.reportResults(results);
        }
    }

    /** Check the module manifest and verify module's own resource bundle */
    private void verifyManifest() throws IOException {
        String moduleBundlePack = findModuleBundlePackage();
        if (moduleBundlePack == null) {
            return;
        }

        for (String p: packages.keySet()) {
            if (p.equals(moduleBundlePack)) {
                packages.get(p).verifyNBModuleBundle(results);
                return;
            }
        }
        results.add(ScanResults.Type.MODULE_MANIFEST_BUNDLE, rootDir.getAbsolutePath() + File.separator + MANIFEST_FILE, 1, "Missing resource bundle specified in module manifest");
    }

    /** Print results to System.out
     * @param details Print details about each warning or just summary?
     */
    public void printResults(boolean details) {
        results.printAll(details);
    }

    public int getProblemsCount() {
        return results.getProblemsCount();
    }

    /** Return path to module's own resource bundle as written in manifest.mf
     */
    private String findModuleBundlePackage() throws IOException {
        File manifest = new File(rootDir, MANIFEST_FILE);
        if (manifest.exists() && manifest.isFile()) {
            FileReader fr = new FileReader(manifest);
            try {
                BufferedReader br = new BufferedReader(fr);
                for (;;) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.startsWith(MANIFEST_BUNDLE_LINK)) {
                        String pack = line.substring(MANIFEST_BUNDLE_LINK.length()).trim().replace("/", File.separator);
                        int index = pack.lastIndexOf(File.separator);
                        if (index >= 0) {
                            pack = pack.substring(0, index);
                        }
                        return pack;
                    }
                }
            } finally {
                fr.close();
            }
        }
        return null;
    }

    private void scanFiles(FileType type) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setCaseSensitive(true);
        ds.setBasedir(new File(rootDir, SRC_DIR));
        ds.setIncludes(type.getFilter());
        ds.scan();
        String[] files = ds.getIncludedFiles();
        for (String name: files) {
            String[] splitName = splitPackage(name);
            getPackage(splitName[0]).addFile(type, splitName[1]);
        }
    }

    private PackageScanner getPackage(String pack) throws IOException {
        PackageScanner ps = packages.get(pack);
        if (ps == null) {
            String moduleDirName = rootDir.getCanonicalPath() + File.separator + SRC_DIR + File.separator;
            ps = new PackageScanner(new File(rootDir + File.separator + SRC_DIR + File.separator + pack), moduleDirName);
            packages.put(pack, ps);
        }
        return ps;
    }

    private static String[] splitPackage(String file) {
        String[] ret = { "", file };
        int index = file.lastIndexOf(File.separator);
        if (index >= 0) {
            ret[0] = file.substring(0, index);
            ret[1] = file.substring(index + 1);
        }
        return ret;
    }

    /** Compute from e.g. "C:\Projects\IJC\Work\trunk\repo\modules\WorkBenchProject" => "modules/WorkBenchProject" string.
     */
    public String getModuleSimpleName() throws IOException {
        return simpleName;
    }

    private static String createSimpleName(File rootDir) throws IOException {
        String name = rootDir.getCanonicalPath();
        name = name.replace(File.separator, "/");
        while (name.indexOf("/") < name.lastIndexOf("/")) {
            name = name.substring(name.indexOf("/") + 1);
        }
        return name;
    }

    public void bundle2csv(String language, List<String> exportTo) throws IOException {
        String moduleSimpleName = getModuleSimpleName();
        for (PackageScanner ps: packages.values()) {
            ps.bundle2csv(language, exportTo, moduleSimpleName);
        }
    }

    public void csv2bundle(String language, List<String> header, Map<String, Map<String, String>> translatedModule) throws IOException {
        for (PackageScanner ps: packages.values()) {
            String packName = ps.getSimpleName();
            Map<String,String> translatedPackage = translatedModule.get(packName);
            if (translatedPackage != null) {
                ps.csv2bundle(language, header, translatedPackage);
            }
        }
    }
}
