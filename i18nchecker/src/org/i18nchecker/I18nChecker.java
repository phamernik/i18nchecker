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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
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

    private Path modules;
    private Path sourceRoots;

    private File repoRoot;
    private String language;
    private File exportToFile;
    private File importFromFile;

    public Path createModules() {
        if (modules == null) {
            modules = new Path(getProject());
        }
        return modules;
    }

    public Path createSourceRoots() {
        if (sourceRoots == null) {
            sourceRoots = new Path(getProject());
        }
        return sourceRoots;
    }

    public void setRepoRoot(File repoRoot) {
        this.repoRoot = repoRoot;
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

    @Override
    public void execute() throws BuildException {
        try {
            log("Scanning modules...");
            List<ModuleScanner> scanners = new ArrayList<ModuleScanner>();
            if (modules != null) {
                collectScanners(scanners, Arrays.asList(modules.list()), true);
            }
            if (sourceRoots != null) {
                collectScanners(scanners, Arrays.asList(sourceRoots.list()), false);
            }

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
     * @param modulePaths A list of NetBeans module folders.
     *   The list should contain relative paths from the {@code repoRoot} folder.
     * @param sourceRootPaths A list of sources roots. These are the folders where the java source hierarchy starts.
     *   The list should contain relative paths from the {@code repoRoot} folder. The list is intended for
     *   non-NetBeans module projects such as simple class library projects, etc.
     * @param unfinishedModules The map with relative paths and error counts from a previous run of this method.
     *   The map serves as a baseline for the error checks. The keys are the relative paths
     *   from either {@code modulePaths} and {@code sourceRootPaths} lists.
     *
     * @return A list of errors.
     * @throws IOException If an I/O error occurs while scanning.
     */
    public static String runAsTest(
            File repoRoot,
            List<String> modulePaths,
            List<String> sourceRootPaths,
            Map<String, Integer> unfinishedModules
    ) throws IOException {

        List<String> resolvedModulePaths = resolvePaths(repoRoot, modulePaths);
        List<String> resolvedSourceRootPaths = resolvePaths(repoRoot, sourceRootPaths);

        List<ModuleScanner> scanners = new ArrayList<ModuleScanner>();
        collectScanners(scanners, resolvedModulePaths, true);
        collectScanners(scanners, resolvedSourceRootPaths, false);

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
            List<ModuleScanner> scanners, Iterable<String> dirs, boolean scanNbArtifacts
    ) throws IOException {
        for (String dir : dirs) {
            File d = new File(dir);
            if (!d.exists() || !d.isDirectory()) {
                throw new IOException("Directory does not exist:" + d.getCanonicalPath());
            }

            ModuleScanner scanner = new ModuleScanner(d, scanNbArtifacts);
            scanners.add(scanner);
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

    private static List<String> resolvePaths(File root, Iterable<String> relativePaths) throws IOException {
        List<String> resolved = new ArrayList<String>();
        for (String p : relativePaths) {
            resolved.add(new File(root, p).getCanonicalPath());
        }
        return resolved;
    }
}
