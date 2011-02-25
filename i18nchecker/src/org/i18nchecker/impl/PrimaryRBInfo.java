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
 * Info about one string in primary resource bundle
 *
 * @author Petr Hamernik
 */
class PrimaryRBInfo extends AbstractRBInfo {

    /** Is this used in Java source */
    private int used;

    /** Was this string commented with # YESI18N? */
    private boolean yesI18N;

    public PrimaryRBInfo(String value, int line, boolean yesI18N) {
        super(value, line);
        this.used = 0;
        this.yesI18N = yesI18N;
    }

    public void markAsUsed() {
        used++;
    }

    public boolean isYesI18N() {
        return yesI18N;
    }

    public int getUsedCount() {
        return used;
    }
}
