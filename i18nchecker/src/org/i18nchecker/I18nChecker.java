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

import org.i18nchecker.impl.I18NUtils;
import org.i18nchecker.impl.ModuleScanner;
import org.i18nchecker.impl.TranslatedData;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * I18N tool class for verifying strings in Java sources and resource bundles.
 * Also serves for exporting all strings for translation and importing them back to resource bundles.
 * This task works per-module. TopDirs are those directories in repo which contains modules.
 * In our case (as it is on Jul/2010) there are modules, libraries and few others top-level directories which contain modules.
 * <p>
 * Currently this task work in a few different modes, depending on what properties are set (or what method is called). TopDirs and RootDir properties are mandatory
 * for all three modes.
 * <ul>
 * <li>Mode 1 - Print errors: prints all errors in I18N (no other extra property is required)</li>
 * <li>Mode 2 - Export to CSV for translation (language and exportToFile property must be set)</li>
 * <li>Mode 3 - Apply translated CSV into translated resource bundle files (language and importFromFile property must be set)</li>
 * <li>Mode 4 - Run as unit test verifying that there are no regressions in I18N. runAsTest method should be called and it takes all required parameters.</li>
 * </ul>
 *
 * @author Petr Hamernik
 */
public class I18nChecker extends Task {
    private static final MessageFormat TEST_ERROR = new MessageFormat("Module {0}: Found {1} errors in I18N (expected <= {2}).\n");

    private File rootDir;
    private List<String> topDirsToScan;

    private String language;
    private File exportToFile;
    private File importFromFile;
    private Boolean allProperties = false;
    private String logger = null;
    private List<String> loggerMethods = null;

    private String moduleFilter;

    /** Mandatory property - root of repository */
    public void setSrcDir(File f) {
        rootDir = f;
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
     * Scan all resource bundles, instead of just Bundle.properties?
     */
    public void setAllProperties(String allProperties) {
        this.allProperties = Boolean.parseBoolean(allProperties);
    }
    /**
     * Export resource bundles to a single csv file for given language
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
        System.out.println("Scanning modules...\n");
        try {
            if (language == null) {
                printErrors(rootDir, topDirsToScan, moduleFilter, allProperties);
            } else {
                if (exportToFile != null) {
                    exportToFile(rootDir, topDirsToScan, language, exportToFile, moduleFilter, allProperties);
                } else if (importFromFile != null) {
                    applyTranslation(rootDir, topDirsToScan, language, importFromFile, moduleFilter, allProperties);
                }
            }
        } catch (IOException exc) {
            throw new BuildException(exc);
        }
    }

    /** Mode 1 - print all I18N errors to console */
    private static void printErrors(File rootDir, List<String> topDirsToScan, String moduleFilter, Boolean allProperties) throws IOException {
        StringBuilder summary = new StringBuilder();
        int total = 0;
        List<ModuleScanner> modules = getModules(rootDir, topDirsToScan, moduleFilter);
        for (ModuleScanner moduleScanner: modules) {
            moduleScanner.scan(allProperties);
            moduleScanner.printResults(true);
            int problemsCount = moduleScanner.getProblemsCount();
            total += problemsCount;
            if (problemsCount > 0) {
                summary.append(moduleScanner.getModuleSimpleName()).append("=").append(moduleScanner.getProblemsCount()).append("\n");
            }
        }
        System.out.println("\n\nSummary:\n");
        System.out.println(summary.toString());
        System.out.println("total=" + total);
    }

    /** Mode 2 - prepare CSV for translation */
    private static void exportToFile(File rootDir, List<String> topDirsToScan, String language, File exportToFile, String moduleFilter, Boolean allProperties) throws IOException {
        List<String> exportedStrings = new LinkedList<String>();
        exportedStrings.add(TranslatedData.getCSVFileHeader());
        List<ModuleScanner> modules = getModules(rootDir, topDirsToScan, moduleFilter);
        for (ModuleScanner moduleScanner: modules) {
            moduleScanner.scan(allProperties);
            moduleScanner.printResults(false);
            moduleScanner.bundle2csv(language, exportedStrings);
        }
        System.out.println("\nExporting to: " + exportToFile);
        I18NUtils.storeToFile(exportToFile, exportedStrings);
    }

    /** Mode 3 - use translation from CSV and apply it into appropriate resource bundle files */
    private static void applyTranslation(File rootDir, List<String> topDirsToScan, String language, File importFromFile, String moduleFilter, Boolean allProperties) throws IOException {
        TranslatedData translatedData = new TranslatedData(importFromFile);
        List<String> header = I18NUtils.createTranslationFilesHeader(I18nChecker.class.getName(), rootDir, importFromFile);
        List<ModuleScanner> modules = getModules(rootDir, topDirsToScan, moduleFilter);
        for (ModuleScanner moduleScanner: modules) {
            moduleScanner.scan(allProperties);
            Map<String, Map<String,String>> translatedModule = translatedData.getTranslationsForModule(moduleScanner.getModuleSimpleName());
            if (translatedModule != null) {
                moduleScanner.csv2bundle(language, header, translatedModule);
            }
        }
    }


    /**
     * Mode 4 - This method is used from unit test I18NTest using introspection.
     *
     * @param rootDir root directory of repository
     * @param topDirs comma separated top level directories containing modules (e.g. "modules,libraries")
     * @param unfinishedModules contains map with counts of known problems in each module. Module names are in form e.g. "libraries/Jchem" or "modules/DIF_API", etc.
     * @throws IOException
     */
    public static String runAsTest(File rootDir, String topDirs, Map<String,Integer> unfinishedModules) throws IOException {
        return runAsTest(rootDir, topDirs, unfinishedModules, false);
    }
    
    /**
     * Mode 4 - This method is used from unit test I18NTest using introspection.
     *
     * @param rootDir root directory of repository
     * @param topDirs comma separated top level directories containing modules (e.g. "modules,libraries")
     * @param unfinishedModules contains map with counts of known problems in each module. Module names are in form e.g. "libraries/Jchem" or "modules/DIF_API", etc.
     * @param allProperties consider all .properties files in a package instead of just Bundle.properties
     * @throws IOException
     */
    public static String runAsTest(File rootDir, String topDirs, Map<String,Integer> unfinishedModules, boolean allProperties) throws IOException {
        StringBuilder result = new StringBuilder();
        List<ModuleScanner> modules = getModules(rootDir, Arrays.asList(topDirs.split(",")), null);
        for (ModuleScanner moduleScanner: modules) {
            moduleScanner.scan(allProperties);
            String moduleSimpleName = moduleScanner.getModuleSimpleName();
            int expectedMaximumProblems = unfinishedModules.containsKey(moduleSimpleName) ? unfinishedModules.get(moduleSimpleName) : 0;
            int actualProblems = moduleScanner.getProblemsCount();
            if (actualProblems > expectedMaximumProblems) {
                moduleScanner.printResults(true);
                result.append(TEST_ERROR.format(new Object[] { moduleSimpleName, actualProblems, expectedMaximumProblems }));
            }
        }
        return result.toString();
    }

    /** Get list of all modules
     *
     * @param root Repository root dir
     * @param topLevelDirs Dirs like "modules", "libraries", "commons", etc. to be scanned
     * @return
     */
    private static List<ModuleScanner> getModules(File root, List<String> topLevelDirs, String filter) throws IOException {
        List<ModuleScanner> modules = new ArrayList<ModuleScanner>();
        for (String name: topLevelDirs) {
            File topLevelDir = new File(root, name);
            if (!topLevelDir.exists() || !topLevelDir.isDirectory()) {
                throw new IllegalArgumentException("Invalid top level dir:" + topLevelDir);
            }
            File[] files = topLevelDir.listFiles();
            for (File f: files) {
                if (f.exists() && f.isDirectory()) {
                    File srcDir = new File(f, ModuleScanner.SRC_DIR);
                    if (srcDir.exists() && srcDir.isDirectory()) {
                        ModuleScanner module = new ModuleScanner(f);
                        if ((filter != null) && (filter.length() > 0)) {
                            if (!module.getModuleSimpleName().contains(filter)) {
                                continue;
                            }
                        }
                        modules.add(module);
                    }
                }
            }
        }
        Collections.sort(modules, new Comparator<ModuleScanner>() {
            public int compare(ModuleScanner o1, ModuleScanner o2) {
                try {
                    return o1.getModuleSimpleName().compareTo(o2.getModuleSimpleName());
                } catch (IOException ex) {
                    // should not happen
                    throw new RuntimeException(ex);
                }
            }
        });
        return modules;
    }

    /**
     * @param logger the logger to set
     */
    public void setLogger(String logger) {
        this.logger = logger;
    }

    /**
     * @param loggerMethods the loggerMethods to set
     */
    public void setLoggerMethods(String loggerMethods) {
        String[] methodArr = loggerMethods.split(",");
        this.loggerMethods = Arrays.asList(methodArr);
    }

}
