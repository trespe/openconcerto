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

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.utils.cc.IPredicate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcip.annotations.ThreadSafe;

/**
 * Parse module properties, and allow to create modules.
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public abstract class ModuleFactory {

    public static final String NAME_KEY = "name";
    public static final String DESC_KEY = "description";

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

    private static String checkMatch(final Pattern p, final String s, final String name) {
        if (!p.matcher(s).matches())
            throw new IllegalArgumentException(name + " doesn't match " + p.pattern());
        return s;
    }

    private static final int parseInt(Matcher m, int group) {
        final String s = m.group(group);
        return s == null ? 0 : Integer.parseInt(s);
    }

    private static final ModuleVersion getVersion(Matcher m, int offset) {
        return new ModuleVersion(parseInt(m, offset + 1), parseInt(m, offset + 2));
    }

    private static final Pattern javaIdentifiedPatrn = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
    private static final Pattern qualifiedPatrn = Pattern.compile(javaIdentifiedPatrn.pattern() + "(\\." + javaIdentifiedPatrn.pattern() + ")*");

    private static final Pattern idPatrn = qualifiedPatrn;
    // \1 major version, \2 minor version
    private static final Pattern versionPatrn = Pattern.compile("(\\p{Digit}+)(?:\\.(\\p{Digit}+))?");
    private static final Pattern dependsSplitPatrn = Pattern.compile("\\p{Blank}*,\\p{Blank}+");
    // \1 id, \2 version
    private static final Pattern dependsPatrn = Pattern.compile("(" + idPatrn.pattern() + ")(?:\\p{Blank}+\\( *(" + versionPatrn.pattern() + ") *\\))?");

    private final String id;
    private final ModuleVersion version;
    // TODO add moduleAPIVersion;
    private final String contact;
    private final Map<String, IPredicate<ModuleFactory>> dependsPredicates;
    private final String mainClass;
    private ResourceBundle rsrcBundle;

    protected ModuleFactory(final Properties props) throws IOException {
        this.id = checkMatch(idPatrn, getRequiredProp(props, "id").trim(), "ID");

        final String version = getRequiredProp(props, "version").trim();
        final Matcher versionMatcher = versionPatrn.matcher(version);
        if (!versionMatcher.matches())
            throw new IllegalArgumentException("Version doesn't match " + versionPatrn.pattern());
        this.version = getVersion(versionMatcher, 0);

        this.contact = getRequiredProp(props, "contact");
        final String depends = props.getProperty("depends", "").trim();
        final String[] dependsArray = depends.length() == 0 ? new String[0] : dependsSplitPatrn.split(depends);
        final HashMap<String, IPredicate<ModuleFactory>> map = new HashMap<String, IPredicate<ModuleFactory>>(dependsArray.length);
        for (final String depend : dependsArray) {
            final Matcher dependMatcher = dependsPatrn.matcher(depend);
            if (!dependMatcher.matches())
                throw new IllegalArgumentException("'" + depend + "' doesn't match " + dependsPatrn.pattern());
            final ModuleVersion depVersion = getVersion(dependMatcher, 2);
            map.put(dependMatcher.group(1), new IPredicate<ModuleFactory>() {
                @Override
                public boolean evaluateChecked(ModuleFactory input) {
                    return input.getVersion().compareTo(depVersion) >= 0;
                }
            });
        }
        this.dependsPredicates = Collections.unmodifiableMap(map);

        final String entryPoint = checkMatch(javaIdentifiedPatrn, props.getProperty("entryPoint", "Module"), "Entry point");
        this.mainClass = this.id + "." + entryPoint;

        this.rsrcBundle = null;
    }

    public final String getID() {
        return this.id;
    }

    public final String getContact() {
        return this.contact;
    }

    public final ModuleVersion getVersion() {
        return this.version;
    }

    public final int getMajorVersion() {
        return this.version.getMajor();
    }

    public final int getMinorVersion() {
        return this.version.getMinor();
    }

    protected final String getMainClass() {
        return this.mainClass;
    }

    public final Collection<String> getRequiredIDs() {
        return this.dependsPredicates.keySet();
    }

    public final boolean isRequiredFactoryOK(ModuleFactory f) {
        return this.dependsPredicates.get(f.getID()).evaluateChecked(f);
    }

    // ResourceBundle is thread-safe
    protected synchronized final ResourceBundle getResourceBundle() {
        if (this.rsrcBundle == null) {
            // don't allow classes to simplify class loaders
            this.rsrcBundle = ResourceBundle.getBundle(getID() + ".ModuleResources", Locale.getDefault(), getRsrcClassLoader(),
                    ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        }
        return this.rsrcBundle;
    }

    protected abstract ClassLoader getRsrcClassLoader();

    public final String getName() {
        return this.getResourceBundle().getString(NAME_KEY);
    }

    public final String getDescription() {
        return this.getResourceBundle().getString(DESC_KEY);
    }

    public abstract AbstractModule createModule(Map<String, AbstractModule> alreadyCreated) throws Exception;

    // not sure if Class or Constructor are thread-safe
    protected synchronized final AbstractModule createModule(final Class<?> c) throws Exception {
        return (AbstractModule) c.getConstructor(ModuleFactory.class).newInstance(this);
    }

    public final Preferences getLocalPreferences() {
        return this.getPreferences(true, null);
    }

    public final Preferences getSQLPreferences(final DBRoot root) {
        return this.getPreferences(false, root);
    }

    public final Preferences getPreferences(final boolean local, final DBRoot root) {
        final Preferences rootPrefs = local ? Preferences.userRoot() : new SQLPreferences(root);
        // ID is a package name, transform to path to avoid bumping into the size limit
        return rootPrefs.node(ModulePreferencePanel.getAppPrefPath() + this.getID().replace('.', '/'));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + getID() + " (" + getMajorVersion() + "." + getMinorVersion() + ")";
    }
}
