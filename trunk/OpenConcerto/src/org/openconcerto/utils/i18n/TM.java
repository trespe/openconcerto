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
 
 package org.openconcerto.utils.i18n;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.Log;
import org.openconcerto.utils.PropertiesUtils;
import org.openconcerto.utils.Tuple2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.MessagePattern;
import com.ibm.icu.text.MessagePattern.Part;
import com.ibm.icu.text.MessagePattern.Part.Type;

/**
 * Translation manager. The translations are provided by {@link Translator} instances, they are
 * created either from a class ending in a language tag that implements it, or by properties files
 * that must contain values that will be passed to {@link MessageFormat}. In the latter case,
 * messages can reference {@link #createValue(Map, String) virtual named arguments}.
 * 
 * @author Sylvain
 * @see LocalizedInstances
 */
public class TM {

    static private final TM INSTANCE = new TM();
    static private final Pattern splitPtrn = Pattern.compile("__", Pattern.LITERAL);
    static private boolean USE_DYNAMIC_MAP = true;

    public static void setUseDynamicMap(boolean b) {
        USE_DYNAMIC_MAP = b;
    }

    /**
     * Whether to use a {@link DynamicMap} or add all possible keys up front.
     * <code>DynamicMap</code> is the default since it's faster : only required keys are computed,
     * otherwise every key that may be used must be computed. So if your pattern has a lot of
     * plurals and choices it might make a difference. However <code>DynamicMap</code> is less
     * robust, since it twists a little the definition of {@link Map}.
     * 
     * @return <code>true</code> if <code>DynamicMap</code> should be used.
     */
    public static boolean useDynamicMap() {
        return USE_DYNAMIC_MAP;
    }

    static public TM getInstance() {
        return INSTANCE;
    }

    static public String tr(final String key, final Object... args) {
        return getInstance().translate(key, args);
    }

    private Locale locale;
    private TranslatorChain translations;
    private Locale translationsLocale;

    protected TM() {
        init();
    }

    protected void init() {
        setLocale(Locale.getDefault());
    }

    public final void setLocale(final Locale locale) {
        this.locale = locale;
        final LocalizedInstances<Translator> localizedInstances = new LocalizedInstances<Translator>(Translator.class, TranslationManager.getControl()) {
            @Override
            protected Translator createInstance(final String bundleName, final Locale l, final Class<?> cl) throws IOException {
                final Properties props = PropertiesUtils.createFromResource(cl, '/' + this.getControl().toResourceName(bundleName, "properties"));
                if (props == null) {
                    return null;
                } else {
                    return new Translator() {
                        @Override
                        public String translate(final String key, final MessageArgs args) {
                            final String msg = props.getProperty(key);
                            if (msg == null)
                                return null;
                            // replaceMap() handles virtual keys (e.g.
                            // element__pluralDefiniteArticle)
                            return new MessageFormat(msg, l).format(replaceMap(args, msg).getAll());
                        }
                    };
                }
            };
        };
        final Tuple2<Locale, List<Translator>> createInstances = localizedInstances.createInstances(getBaseName(), locale);
        this.translationsLocale = createInstances.get0();
        this.translations = new TranslatorChain(createInstances.get1());
    }

    /**
     * The requested locale.
     * 
     * @return the requested locale.
     * @see #setLocale(Locale)
     */
    public final Locale getLocale() {
        return this.locale;
    }

    /**
     * The actual locale of the loaded translations.
     * 
     * @return the actual locale.
     */
    public final Locale getTranslationsLocale() {
        return this.translationsLocale;
    }

    protected final String getBaseName() {
        return I18nUtils.getBaseName(this.getClass());
    }

    // translate array
    public final String trA(final String key, final Object... args) {
        return translate(key, args);
    }

    public final String translate(final String key, final Object... args) {
        return translate(true, key, args);
    }

    // translate map
    public final String trM(final String key, final String name1, final Object arg1) {
        return trM(key, Collections.singletonMap(name1, arg1));
    }

    public final String trM(final String key, final String name1, final Object arg1, final String name2, final Object arg2) {
        return trM(key, CollectionUtils.createMap(name1, arg1, name2, arg2));
    }

    public final String trM(final String key, final Map<String, ?> args) {
        return trM(true, key, args);
    }

    public final String trM(final boolean lenient, final String key, Map<String, ?> map) throws MissingResourceException {
        return translate(lenient, key, new MessageArgs(map));
    }

    public final String translate(final boolean lenient, final String key, final Object... args) throws MissingResourceException {
        return translate(lenient, key, new MessageArgs(args));
    }

    private final String translate(final boolean lenient, final String key, MessageArgs args) throws MissingResourceException {
        final String res = this.translations.translate(key, args);
        if (res == null) {
            if (lenient)
                return '!' + key + '!';
            else
                throw new MissingResourceException("Missing translation", this.getBaseName(), key);
        }
        return res;
    }

    protected MessageArgs replaceMap(final MessageArgs args, final String msg) {
        final MessageArgs res;
        if (args.getAll() instanceof Map) {
            final Map<String, ?> map = args.getMap();
            Map<String, Object> newMap;
            if (MessageArgs.isOrdered(map)) {
                newMap = new LinkedHashMap<String, Object>(map);
            } else {
                newMap = new HashMap<String, Object>(map);
            }
            if (useDynamicMap()) {
                newMap = new DynamicMap<Object>(newMap) {
                    @Override
                    protected Object createValueNonNull(String key) {
                        return TM.this.createValue(this, key);
                    }
                };
            } else {
                final MessagePattern messagePattern = new MessagePattern(msg);
                if (messagePattern.hasNamedArguments()) {
                    final int countParts = messagePattern.countParts();
                    String argName;
                    for (int i = 0; i < countParts; i++) {
                        final Part part = messagePattern.getPart(i);
                        if (part.getType() == Type.ARG_NAME && !newMap.containsKey(argName = messagePattern.getSubstring(part))) {
                            final Object createValue = this.createValue(newMap, argName);
                            if (createValue != null)
                                newMap.put(argName, createValue);
                        }
                    }
                }
            }
            res = new MessageArgs(newMap);
        } else {
            res = args;
        }
        return res;
    }

    /**
     * Try to create a value for a missing key. The syntax of keys must be phraseName(__name)+ and
     * if you need to have __ in a name it must be doubled (i.e. ____). <code>phraseName</code>, as
     * its name implies, must reference an existing phrase in <code>map</code>. Then this phrase and
     * the list of <code>name</code> are passed to {@link Grammar#eval(Phrase, Number, List)}. The
     * count is <code>phraseNameCount</code> if it exists and is a {@link Number}, then
     * <code>count</code> else <code>null</code>.
     * 
     * @param map the current map.
     * @param key the missing key.
     * @return its value, or <code>null</code> to leave the map unmodified.
     */
    protected Object createValue(final Map<String, Object> map, final String key) {
        final Matcher m = splitPtrn.matcher(key);
        final String pattern = splitPtrn.pattern();
        final int patternL = pattern.length();
        final StringBuffer sb = new StringBuffer(key.length());
        final List<String> l = new ArrayList<String>();
        int pos = 0;
        while (m.find(pos)) {
            // double to escape pattern
            if (key.length() >= m.end() + patternL && pattern.equals(key.substring(m.end(), m.end() + patternL))) {
                // go to the end to include one
                sb.append(key.substring(pos, m.end()));
                // and set pos after the second one
                pos = m.end() + patternL;
            } else {
                sb.append(key.substring(pos, m.start()));
                l.add(sb.toString());
                sb.setLength(0);
                pos = m.end();
            }
        }
        sb.append(key.substring(pos));
        l.add(sb.toString());

        final String first = CollectionUtils.getFirst(l);
        // at least the whole key
        assert first != null;
        final Object firstObj = handleGet(map, first);
        final Phrase phrase = firstObj instanceof Phrase ? (Phrase) firstObj : null;
        if (phrase != null && phrase.getGrammar() != null) {
            Object countObj = handleGet(map, first + "Count");
            if (!(countObj instanceof Number))
                countObj = handleGet(map, "count");
            final Number count = countObj instanceof Number ? (Number) countObj : null;
            return phrase.getGrammar().eval(phrase, count, l.subList(1, l.size()));
        } else if (phrase != null) {
            Log.get().warning("While splitting " + key + ", " + first + " is a Phrase without grammar : " + phrase);
            return phrase.getBase();
        } else {
            Log.get().warning("While splitting " + key + " : " + first + " isn't a Phrase");
            return null;
        }
    }

    private final Object handleGet(final Map<String, Object> map, final String key) {
        if (map instanceof DynamicMap)
            return ((DynamicMap<Object>) map).handleGet(key);
        else
            return map.get(key);
    }
}
