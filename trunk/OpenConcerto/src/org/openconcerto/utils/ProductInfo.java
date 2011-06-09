/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Useful for defining product wide values, like version, from a property file. For example say you
 * have a common framework for 2 applications. You could put a property file with the same location
 * in each, then when building an app either you don't provide the file and the fwk one will be
 * used, either you provide one and assure that the fwk one won't overwrite it. That way the fwk
 * could use any key, knowing its value will be the right one.
 * 
 * @author Sylvain
 */
public class ProductInfo {

    private static final ProductInfo INSTANCE = new ProductInfo();

    public synchronized static final ProductInfo getInstance() {
        return INSTANCE;
    }

    private Properties props;

    private ProductInfo() {
        this.props = null;
    }

    /**
     * The properties.
     * 
     * @return the associated properties, or <code>null</code> if they couldn't be found.
     * @throws IllegalStateException if properties couldn't be loaded.
     */
    public final Properties getProps() {
        if (this.props == null) {
            try {
                final InputStream stream = this.getClass().getResourceAsStream("/product.properties");
                if (stream != null) {
                    final Properties loadingProps = new Properties();
                    loadingProps.load(stream);
                    this.props = loadingProps;
                }
            } catch (IOException e) {
                throw new IllegalStateException("unable to load product properties", e);
            }
        }
        return this.props;
    }

}
