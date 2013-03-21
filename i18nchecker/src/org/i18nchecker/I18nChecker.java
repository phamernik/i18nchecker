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

package org.i18nchecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.i18nchecker.impl.I18NUtils;
import org.i18nchecker.impl.ModuleScanner;
import org.i18nchecker.impl.TranslatedData;

/**
 * I18N tool class for verifying strings in Java sources and resource bundles.
 * Also serves for exporting all strings for translation and importing them back to resource bundles.
 * This task works per-module. TopDirs are those directories in repo which contains modules.
 * In our case (as it is on Jul/2010) there are modules,
 * libraries and few others top-level directories which contain modules.
 * <p>
 * Currently this task work in a few different modes, depending on what properties are set
 * (or what method is called). TopDirs and RootDir properties are mandatory for all three modes.
 * <ul>
 * <li>Mode 1 - Print errors: prints all errors in I18N (no other extra property is required)</li>
 * <li>Mode 2 - Export to CSV for translation (language and exportToFile property must be set)</li>
 * <li>Mode 3 - Apply translated CSV into translated resource bundle files
 *     (language and importFromFile property must be set)</li>
 * <li>Mode 4 - Run as unit test verifying that there are no regressions in I18N.
 *     runAsTest method should be called and it takes all required parameters.</li>
 * </ul>
 *
 * @author Petr Hamernik
 */
public final class I18nChecker extends Task {

    private static final MessageFormat TEST_ERROR =
            new MessageFormat("Module {0}: Found {1} errors in I18N (expected <= {2}).\n");

    private File repoRoot;
    private List<String> topDirsToScan;

    private String language;
    private File exportToFile;
    private File importFromFile;

    private String moduleFilter;

    /** Mandatory property - root of repository */
    public void setSrcDir(File f) {
        repoRoot = f;
    }

    /** Mandatory property - names of top-lever directories (under root dir) which should be scanned for modules. */
    public void setTopDirs(String dirs) {
        String[] dirsArr = dirs.split(",");
        topDirsToScan = Arrays.asList(dirsArr);
    }

    /**
     * Language code - e.g. "ja", "cs"
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Export resource bundles to a single csv file for given language.
     */
    public void setExportTo(File exportToFile) {
        this.exportToFile = exportToFile;
    }

    /**
     * Import from translated CSV file and apply translated strings into localized resource bundles.
     */
    public void setImportFrom(File importFromFile) {
        this.importFromFile = importFromFile;
    }

    public void setModuleFilter(String moduleFilter) {
        this.moduleFilter = moduleFilter;
    }

    @Override
    public void execute() throws BuildException {
        try {
            log("Scanning modules...");

            List<ModuleScanner> scanners = new ArrayList<ModuleScanner>();
            collectScanners(scanners, repoRoot, topDirsToScan, moduleFilter);

            log("Collected " + scanners.size() + " scanners.");

            for (ModuleScanner s : scanners) {
                log("Scanning " + s.getRoot().getCanonicalPath());
                s.scan();
            }

            if (language == null) {
                printErrors(scanners);
            } else {
                if (repoRoot == null) {
                    throw new BuildException("No 'repoRoot' specified.");
                }
                if (exportToFile != null) {
                    exportToFile(scanners, language, exportToFile);
                } else if (importFromFile != null) {
                    applyTranslation(scanners, language, importFromFile);
                }
            }
            log("Scanning modules finished successfully!");
        } catch (IOException exc) {
            throw new BuildException(exc);
        }
    }

    /** Mode 1 - print all I18N errors to console */
    private void printErrors(List<? extends ModuleScanner> scanners) throws IOException {
        StringBuilder summary = new StringBuilder();
        int total = 0;
        for (ModuleScanner moduleScanner: scanners) {
            moduleScanner.printResults(true);
            int problemsCount = moduleScanner.getProblemsCount();
            total += problemsCount;
            if (problemsCount > 0) {
                summary
                    .append(relativePath(repoRoot, moduleScanner.getRoot()))
                    .append(" = ")
                    .append(moduleScanner.getProblemsCount())
                    .append("\n");
            }
        }
        log("\n\nSummary:\n");
        log(summary.toString());
        log("total=" + total);
    }

    /** Mode 2 - prepare CSV for translation */
    private void exportToFile(
            List<? extends ModuleScanner> scanners, String language, File exportToFile
    ) throws IOException {
        List<String> exportedStrings = new LinkedList<String>();
        exportedStrings.add(TranslatedData.getCSVFileHeader());

        for (ModuleScanner moduleScanner : scanners) {
            moduleScanner.printResults(false);
            String relativePath = relativePath(repoRoot, moduleScanner.getRoot());
            moduleScanner.bundle2csv(language, exportedStrings, relativePath);
        }

        log("Exporting to: " + exportToFile);
        I18NUtils.storeToFile(exportToFile, exportedStrings);
    }

    /** Mode 3 - use translation from CSV and apply it into appropriate resource bundle files */
    private void applyTranslation(
            List<? extends ModuleScanner> scanners, String language, File importFromFile
    ) throws IOException {
        TranslatedData translatedData = new TranslatedData(importFromFile);
        List<String> header = I18NUtils.createTranslationFilesHeader(getClass().getName(), repoRoot, importFromFile);

        for (ModuleScanner moduleScanner : scanners) {
            String relativePath = relativePath(repoRoot, moduleScanner.getRoot());
            Map<String, Map<String, String>> translatedModule = translatedData.getTranslationsForModule(relativePath);
            if (translatedModule != null) {
                moduleScanner.csv2bundle(language, header, translatedModule);
            }
        }
    }


    /**
     * Mode 4 - This method is used from unit test I18NTest using introspection.
     *
     * @param repoRoot The root of the source code repository.
     * @param topDirs A list of folder to scan for NetBeans or Maven projects.
     *   The list should contain relative paths from the {@code repoRoot} folder.
     * @param unfinishedModules The map with relative paths and error counts from a previous run of this method.
     *   The map serves as a baseline for the error checks. The keys are the relative paths
     *   from either {@code modulePaths} and {@code sourceRootPaths} lists.
     *
     * @return A list of errors.
     * @throws IOException If an I/O error occurs while scanning.
     */
    public static String runAsTest(
            File repoRoot, String topDirs, Map<String, Integer> unfinishedModules
    ) throws IOException {

        List<ModuleScanner> scanners = new ArrayList<ModuleScanner>();
        collectScanners(scanners, repoRoot, Arrays.asList(topDirs.split(",")), null);

        StringBuilder result = new StringBuilder();
        for (ModuleScanner moduleScanner: scanners) {
            moduleScanner.scan();
            String relativePath = relativePath(repoRoot, moduleScanner.getRoot());
            int expectedMaximumProblems = unfinishedModules.containsKey(relativePath)
                    ? unfinishedModules.get(relativePath)
                    : 0;
            int actualProblems = moduleScanner.getProblemsCount();
            if (actualProblems > expectedMaximumProblems) {
                moduleScanner.printResults(true);
                result.append(TEST_ERROR.format(new Object[] {
                    relativePath, actualProblems, expectedMaximumProblems
                }));
            }
        }

        return result.toString();
    }

    private static void collectScanners(
            List<ModuleScanner> scanners, File repoRoot, Iterable<String> topDirsToScan, String moduleFilter
    ) throws IOException {
        for (String topDirToScan : topDirsToScan) {
            File topDir = new File(repoRoot, topDirToScan);
            if (!topDir.exists() || !topDir.isDirectory()
                || (moduleFilter != null && moduleFilter.length() > 0 && !topDirToScan.contains(moduleFilter))
            ) {
                return;
            }

            for (File f : topDir.listFiles()) {
                if (moduleFilter != null && moduleFilter.length() > 0 && !f.getName().contains(moduleFilter)) {
                    continue;
                }
                if (isNbmManifest(new File(f, "manifest.mf"))) {
                    scanners.add(new ModuleScanner(f, true));
                } else if (isMavenProject(new File(f, "pom.xml"))) {
                    scanners.add(new ModuleScanner(new File(f, "/src/main/java"), false));
                }
            }
        }
    }

    private static String relativePath(File ancestor, File file) throws IOException {
        if (ancestor == null) {
            return file.getCanonicalPath();
        } else {
            File f = file;
            Stack<String> stack = new Stack<String>();
            while (!ancestor.equals(f)) {
                stack.push(f.getName());
                f = f.getParentFile();
                if (f == null) {
                    throw new IOException(ancestor.getCanonicalPath() + " does not include " + file.getCanonicalPath());
                }
            }

            StringBuilder sb = new StringBuilder();
            while (!stack.empty()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(stack.pop());
            }

            return sb.toString();
        }
    }

    private static boolean isNbmManifest(File manifest) throws IOException {
        if (manifest.exists() && manifest.isFile()) {
            InputStream is = new FileInputStream(manifest);
            try {
                Manifest m = new Manifest(is);
                return m.getMainAttributes().containsKey(new Attributes.Name("OpenIDE-Module"));
            } finally {
                is.close();
            }
        }
        return false;
    }

    private static boolean isMavenProject(File pom) throws IOException {
        return pom.exists() && pom.isFile();
    }
}
