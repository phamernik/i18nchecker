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

/**
 * Type of file - java source (*.java), primary properties resource bundle (Bundle.properties) or translations (Bundle_*.properties)
 *
 * @author Petr Hamernik
 */
enum FileType {
    PRIMARY_BUNDLE("**\\Bundle.properties"),
    TRANSLATED_BUNDLE("**\\*_??.properties"),
    OTHER_BUNDLE("**\\*.properties"),
    JAVA("**\\*.java");

    private String filter;

    private FileType(String filter) {
        this.filter = filter;
    }

    public String[] getFilter() {
        return new String[] { filter };
    }
}
