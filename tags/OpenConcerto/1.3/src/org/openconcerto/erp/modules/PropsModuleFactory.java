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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.utils.cc.IPredicate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcip.annotations.ThreadSafe;

/**
 * Parse module properties, and allow to create modules.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public abstract class PropsModuleFactory extends ModuleFactory {

    protected static Properties readAndClose(final InputStream ins) throws IOException {
        final Properties props = new Properties();
        try {
            props.load(ins);
        } finally {
            ins.close();
        }
        return props;
    }

    protected static final String getRequiredProp(Properties props, final String key) {
        final String res = props.getProperty(key);
        if (res == null)
            throw new IllegalStateException("Missing " + key);
        return res;
    }

    private static final int parseInt(Matcher m, int group) {
        final String s = m.group(group);
        return s == null ? 0 : Integer.parseInt(s);
    }

    private static final ModuleVersion getVersion(Matcher m, int offset) {
        return new ModuleVersion(parseInt(m, offset + 1), parseInt(m, offset + 2));
    }

    static private final ModuleVersion getVersion(final Properties props) {
        final String version = getRequiredProp(props, "version").trim();
        final Matcher versionMatcher = versionPatrn.matcher(version);
        if (!versionMatcher.matches())
            throw new IllegalArgumentException("Version doesn't match " + versionPatrn.pattern());
        return getVersion(versionMatcher, 0);
    }

    // \1 major version, \2 minor version
    private static final Pattern versionPatrn = Pattern.compile("(\\p{Digit}+)(?:\\.(\\p{Digit}+))?");
    private static final Pattern dependsSplitPatrn = Pattern.compile("\\p{Blank}*,\\p{Blank}+");
    // \1 id, \2 version
    private static final Pattern dependsPatrn = Pattern.compile("(" + ModuleReference.idPatrn.pattern() + ")(?:\\p{Blank}+\\( *(" + versionPatrn.pattern() + ") *\\))?");

    private final Map<Object, Dependency> depends;
    private final String mainClass;
    private ResourceBundle rsrcBundle;

    protected PropsModuleFactory(final Properties props) {
        super(new ModuleReference(getRequiredProp(props, "id"), getVersion(props)), getRequiredProp(props, "contact"));

        final String depends = props.getProperty("depends", "").trim();
        final String[] dependsArray = depends.length() == 0 ? new String[0] : dependsSplitPatrn.split(depends);
        // be predictable, keep order
        final List<Dependency> l = new ArrayList<Dependency>(dependsArray.length);
        for (final String depend : dependsArray) {
            final Matcher dependMatcher = dependsPatrn.matcher(depend);
            if (!dependMatcher.matches())
                throw new IllegalArgumentException("'" + depend + "' doesn't match " + dependsPatrn.pattern());
            final ModuleVersion depVersion = getVersion(dependMatcher, 3);
            l.add(new Dependency(dependMatcher.group(1), new IPredicate<ModuleFactory>() {
                @Override
                public boolean evaluateChecked(ModuleFactory input) {
                    return input.getVersion().compareTo(depVersion) >= 0;
                }
            }));
        }
        this.depends = Collections.unmodifiableMap(createMap(l));

        final String entryPoint = ModuleReference.checkMatch(ModuleReference.javaIdentifiedPatrn, props.getProperty("entryPoint", "Module"), "Entry point");
        this.mainClass = this.getID() + "." + entryPoint;

        this.rsrcBundle = null;
    }

    protected final String getMainClass() {
        return this.mainClass;
    }

    @Override
    protected final Map<Object, Dependency> getDependencies() {
        return this.depends;
    }

    // ResourceBundle is thread-safe
    @Override
    protected synchronized final ResourceBundle getResourceBundle() {
        if (this.rsrcBundle == null) {
            // don't allow classes to simplify class loaders
            this.rsrcBundle = ResourceBundle.getBundle(getID() + ".ModuleResources", Locale.getDefault(), getRsrcClassLoader(),
                    ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        }
        return this.rsrcBundle;
    }

    protected abstract ClassLoader getRsrcClassLoader();
}
