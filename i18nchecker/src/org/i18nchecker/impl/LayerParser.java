/*
 *   Copyright 2010-2011 Radim Kubacki
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple parser to find localizable entries in NetBeans XML layer file.
 *
 * @author radim
 */
public class LayerParser {

    public static class LayerData {

        /**
         * Full name of Bundle (using <code>File.separator</code> as delimiter).
         */
        public final String bundlePath;
        /**
         * Referenced key.
         */
        public final String bundleKey;
        /**
         * Info where this is referenced in the layer file.
         * Note that we do not know the offset or linenumber.
         */
        public final String info;

        /*
         * @VisibleForTesting
         */ LayerData(String bundlePath, String bundleKey, String info) {
            this.bundlePath = bundlePath;
            this.bundleKey = bundleKey;
            this.info = info;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LayerData other = (LayerData) obj;
            if ((this.bundlePath == null) ? (other.bundlePath != null) : !this.bundlePath.equals(other.bundlePath)) {
                return false;
            }
            if ((this.bundleKey == null) ? (other.bundleKey != null) : !this.bundleKey.equals(other.bundleKey)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 13 * hash + (this.bundlePath != null ? this.bundlePath.hashCode() : 0);
            hash = 13 * hash + (this.bundleKey != null ? this.bundleKey.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "LayerData{" + "bundlePath=" + bundlePath + ", bundleKey=" + bundleKey + ", info=" + info + '}';
        }
    }

    private List<LayerData> data = new ArrayList();

    public LayerParser() {
    }

    public Iterable<LayerData> parse(InputStream is) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            XPath xpath = XPathFactory.newInstance().newXPath();

            XPathExpression expr = xpath.compile("//@bundlevalue");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);

            NodeList nodeList = (NodeList) result;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String bundlevalue = node.getTextContent();
                int idx = bundlevalue.indexOf('#');
                String bundleFile = bundlevalue.substring(0, idx).replace('.', '/');
                // assume that last part is Bundle
                bundleFile = bundleFile.substring(0, bundleFile.lastIndexOf('/'));
                String bundleKey = bundlevalue.substring(idx + 1);
                Stack<String> stack = new Stack<String>();
                for (Node n = node;
                        n != null;
                        n = (n instanceof Attr) ? ((Attr) n).getOwnerElement() : n.getParentNode()) {
                    if (n instanceof Element) {
                        stack.push(((Element) n).getAttribute("name"));
                    } else if (n instanceof Attr) {
                        stack.push(((Attr) n).getName());
                    }
                }
                StringBuilder sb = new StringBuilder();
                while (!stack.empty()) {
                    sb.append('/').append(stack.pop());
                }
                data.add(new LayerData(
                        bundleFile, bundleKey, "bundle key referenced in " + sb.toString() + " not found"));
            }
            return data;
        } catch (Exception ex) {
            Logger.getLogger(LayerParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Collections.emptyList();
    }
}
