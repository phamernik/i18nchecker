/*
 * Copyright (c) 2005-2010 ChemAxon Ltd. and Informatics Matters Ltd.
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * ChemAxon and Informatics Matters. You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the agreements
 * you entered into with ChemAxon or Informatics Matters.
 *
 * CopyrightVersion 1.2
 */

package org.i18nchecker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import junit.framework.TestCase;

/**
 * Unit test for keeping project sources/resource bundles clean and fully localized.
 *
 * @author Petr
 */
public class I18NTest extends TestCase {

    public I18NTest(java.lang.String testName) {
        super(testName);
    }

    public void testI18N() throws Exception {
        File suiteDir = new File(System.getProperty("user.dir")); // Should
        suiteDir = suiteDir.getCanonicalFile();
        File rootDir = suiteDir.getParentFile();
        URLClassLoader loader = new URLClassLoader(urlToLibs(suiteDir, new String[] {
            "/dist/i18nchecker.jar",
            "/lib/antlr-runtime-3.2.jar",
            "/lib/SuperCSV-1.52.jar" }));
        Class<?> c = loader.loadClass("org.i18nchecker.I18nChecker");
        Method runAsTestMethod = c.getMethod("runAsTest", File.class, String.class, Map.class);

        Map<String,Integer> unfinishedModules = getUnfinishedI18NModules();
        String result = (String) runAsTestMethod.invoke(null, rootDir, "i18nchecker/playground,i18nchecker/playground/PaintApp", unfinishedModules);
        if (!result.isEmpty()) {
            fail(result);
        }
    }

    private URL[] urlToLibs(File suiteDir, String[] paths) throws Exception {
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            File jar = new File(suiteDir.getAbsolutePath() + paths[i]);
            assertTrue("Library not found: "+jar, jar.exists());
            urls[i] = jar.toURI().toURL();
        }
        return urls;
    }

    private Map<String,Integer> getUnfinishedI18NModules() throws IOException {
        Properties props = new Properties();
        InputStream is = getClass().getResourceAsStream("i18n_known_errors.properties");
        try {
            props.load(is);
        } finally {
            is.close();
        }

        Map<String,Integer> result = new HashMap<String,Integer>();
        for (String key: props.stringPropertyNames()) {
            int val = Integer.parseInt(props.getProperty(key, "0"));
            result.put(key, val);
        }
        return result;
    }

}
