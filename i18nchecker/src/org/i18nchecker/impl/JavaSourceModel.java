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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;

/**
 * Data holder for all String occurrences in a single Java source file.
 *
 * @author Petr Hamernik
 */
class JavaSourceModel {
    private static final boolean skipEmptyAndSingleCharStrings = true;

    private static final String NOI18N = "NOI18N";
    private static final String NB_BUNDLE = "NbBundle";

    private static final String FONT = "Font";
    private static final List<String> KNONW_FONTS = Arrays.asList(new String [] {
        "Tahoma", "Courier", "Arial", "Dialog"
    });

    private String fileName;
    private List<Info> strings;

    public JavaSourceModel(String fileName) {
        this.fileName = fileName;
    }

    /** Parse the source file and load strings into keys */
    public void parse() throws IOException {
        strings = new ArrayList<Info>();

        FileReader fr = new FileReader(fileName);
        try {
            CharStream stream = new ANTLRReaderStream(fr);
            JavaLexer lexer = new JavaLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            List<?> list = tokens.getTokens();
            int lineOfLastNbBundleOccurence = -1;
            int lineOfLastAnnotationOccurence = -1;
            int lineOfLastAssert = -1;
            int lineOfLastFont = -1;
            for (Object obj: list) {
                Token token = (Token) obj;
                int line = token.getLine();
                if (token.getType() == JavaLexer.StringLiteral) {
                    String str = token.getText();
                    str = str.substring(1, str.length() - 1);
                    if (skipEmptyAndSingleCharStrings && (str.trim().length() <= 1)) {
                        // skip short strings
                        continue;
                    }
                    if (line == lineOfLastAnnotationOccurence) {
                        // skip annotations
                        continue;
                    }
                    if (line == lineOfLastAssert) {
                        // skip asserts
                        continue;
                    }
                    if (line == lineOfLastFont) {
                        // NetBeans is not generating // NOI18N in locked parts of code like this:
                        // label.setFont(new java.awt.Font("Tahoma", 0, 11));
                        // So let's skip them:
                        if (KNONW_FONTS.contains(str)) {
                            continue;
                        }
                    }
                    boolean isCloseToNbBundle = (line == lineOfLastNbBundleOccurence);
                    strings.add(new Info(str, line, isCloseToNbBundle));
                } else if (token.getType() == JavaLexer.LINE_COMMENT) {
                    if (token.getText().indexOf(NOI18N) >= 0) {
                        for (Info info: strings) {
                            if (info.getLine() == line) {
                                info.setNoI18N();
                            }
                        }
                    }
                } else if (token.getType() == JavaLexer.Identifier) {
                    if (token.getText().equals(NB_BUNDLE)) {
                        lineOfLastNbBundleOccurence = line;
                    } else if (token.getText().equals(FONT)) {
                        lineOfLastFont = line;
                    }
                } else if (token.getType() == JavaLexer.T__73) { // @ character - annotation
                    lineOfLastAnnotationOccurence = line;
                } else if (token.getType() == JavaLexer.ASSERT) {
                    lineOfLastAssert = line;
                }
            }
        } finally {
            fr.close();
        }
    }

    /** Do verification of strings against the provided resource bundle */
    void verify(PrimaryResourceBundleModel bundle) {
        if (bundle != null) {
            for (Info info: strings) {
                if (bundle.markAsUsed(info.getStr())) {
                    info.markFoundInBundle();
                }
            }
        }
    }

    /** Report results into the provided ScanResults */
    void reportResults(ScanResults results) {
        results.incrementFileCounter(FileType.JAVA);
        Map<Integer,Integer> stringsPerLine = new HashMap<Integer,Integer>(); // [lineNo -> Count of strings at this line]
        for (Info info: strings) {
            int stringCount = stringsPerLine.containsKey(info.getLine()) ? stringsPerLine.get(info.getLine()).intValue() + 1 : 1;
            stringsPerLine.put(info.getLine(), stringCount);
            if (!info.isFoundInBundle() && !info.isNoI18N()) {
                if (info.isCloseToNbBundle()) {
                    results.add(ScanResults.Type.MISSING_KEY_IN_BUNDLE, fileName, info.getLine(), info.getStr());
                } else {
                    results.add(ScanResults.Type.MISSING_NOI18N_OR_KEY_IN_BUNDLE, fileName, info.getLine(), info.getStr());
                }
            }
        }
        
        // If string there is only string in the line in java source which exists in resource bundle and is marked with NOI18N then this comment is redundant
        // Ignore java sources which are forms.
        File form = new File(fileName.substring(0, fileName.length() - 5) + ".form");
        if (!form.exists()) {
            for (Info info: strings) {
                if (info.isFoundInBundle() && info.isNoI18N() && (stringsPerLine.get(info.getLine()).intValue() == 1)) {
                    results.add(ScanResults.Type.NOT_NECESSARY_TO_USE_NOI18N, fileName, info.getLine(), info.getStr());
                }
            }
        }
    }

    /** Info about one string in java source */
    private static class Info {
        /** The string found in source */
        private String str;
        /** Line number of this string */
        private int line;
        /** Is this string in the same line as identifier "NbBundle" in source? */
        private boolean closeToNbBundle;
        /** Is there // NOI18N line comment in the end of this line? */
        private boolean noi18n;
        /** Was this string found in package bundle? */
        private boolean foundInBundle;

        public Info(String str, int line, boolean closeToNbBundle) {
            this.str = str;
            this.line = line;
            this.closeToNbBundle = closeToNbBundle;
            this.noi18n = false;
        }

        public String getStr() {
            return str;
        }

        public void setNoI18N() {
            noi18n = true;
        }

        public int getLine() {
            return line;
        }

        private boolean isCloseToNbBundle() {
            return closeToNbBundle;
        }

        private boolean isNoI18N() {
            return noi18n;
        }

        private void markFoundInBundle() {
            foundInBundle = true;
        }

        public boolean isFoundInBundle() {
            return foundInBundle;
        }
    }
}
