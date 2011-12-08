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
 
 package org.openconcerto.erp.generationDoc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TemplateManager {
    private static TemplateManager instance = new TemplateManager();
    private List<TemplateProvider> providers = new ArrayList<TemplateProvider>();
    private Map<String, TemplateProvider> defaultMap = new HashMap<String, TemplateProvider>();
    private TemplateProvider defautProvider;
    private List<String> knownTemplateIds = new ArrayList<String>();

    public static TemplateManager getInstance() {
        return instance;
    }

    public void add(TemplateProvider provider) {
        this.providers.add(provider);
    }

    public void remove(TemplateProvider provider) {
        this.providers.remove(provider);
        final Set<String> keys = defaultMap.keySet();
        for (String key : keys) {
            if (defaultMap.get(key).equals(provider)) {
                defaultMap.remove(key);
            }
        }
    }

    public void setDefaultProvider(String templateId, TemplateProvider provider) {
        defaultMap.put(templateId, provider);
        knownTemplateIds.add(templateId);
    }

    public void setDefaultProvider(TemplateProvider provider) {
        this.defautProvider = provider;
    }

    /**
     * Get the template using first the default template providers
     * 
     * 
     * @return the template file, IllegalStateException if no template is found
     * */
    public InputStream getTemplate(String templateId, String language, String type) {
        TemplateProvider provider = defaultMap.get(templateId);
        if (provider == null) {
            for (TemplateProvider pr : providers) {
                InputStream stream = pr.getTemplate(templateId, language, type);
                if (stream != null) {
                    return stream;
                }
            }
            if (defautProvider == null) {
                throw new IllegalStateException("Not default provider registered when using template id:" + templateId + " language:" + language + " type:" + type);
            }
            return defautProvider.getTemplate(templateId, language, type);
        }
        return provider.getTemplate(templateId, language, type);
    }

    /**
     * Get the template print configuration using first the default template providers
     * 
     * 
     * @return the template file, IllegalStateException if no template is found
     * */
    public InputStream getTemplatePrintConfiguration(String templateId, String language, String type) {
        TemplateProvider provider = defaultMap.get(templateId);
        if (provider == null) {
            for (TemplateProvider pr : providers) {
                InputStream stream = pr.getTemplatePrintConfiguration(templateId, language, type);
                if (stream != null) {
                    return stream;
                }
            }
            if (defautProvider == null) {
                throw new IllegalStateException("Not default provider registered when using template id:" + templateId + " langage:" + language + " type:" + type);
            }
            return defautProvider.getTemplatePrintConfiguration(templateId, language, type);
        }
        return provider.getTemplatePrintConfiguration(templateId, language, type);
    }

    /**
     * Get the template cofiguration using first the default template providers
     * 
     * 
     * @return the template file, IllegalStateException if no template is found
     * */
    public InputStream getTemplateConfiguration(String templateId, String language, String type) {
        TemplateProvider provider = defaultMap.get(templateId);
        if (provider == null) {
            for (TemplateProvider pr : providers) {
                InputStream stream = pr.getTemplateConfiguration(templateId, language, type);
                if (stream != null) {
                    return stream;
                }
            }
            if (defautProvider == null) {
                throw new IllegalStateException("Not default provider registered when using template id:" + templateId + " langage:" + language + " type:" + type);
            }
            return defautProvider.getTemplateConfiguration(templateId, language, type);
        }
        return provider.getTemplateConfiguration(templateId, language, type);
    }

    public InputStream getTemplate(String templateId) {
        return getTemplate(templateId, null, null);
    }

    public void register(String templateId) {
        knownTemplateIds.add(templateId);
    }

    public void dump() {
        System.out.println(this.getClass().getCanonicalName());
        System.out.println("Default provider: " + this.defautProvider);
        System.out.println("Templates:");
        for (String templateId : this.knownTemplateIds) {
            try {
                InputStream stream = this.getTemplate(templateId);
                if (stream == null) {
                    System.out.println(rightAlign("'" + templateId + "'") + " : stream missing");
                } else {
                    System.out.println(rightAlign("'" + templateId + "'") + " : ok");
                }
            } catch (Exception e) {
                System.out.println(rightAlign("'" + templateId + "'") + ": stream error");
            }
        }

    }

    private String rightAlign(String s) {
        String r = s;
        int n = 20 - s.length();
        for (int i = 0; i < n; i++) {
            r = ' ' + r;
        }
        return r;
    }

    public boolean isKnwonTemplate(String templateId) {
        return this.knownTemplateIds.contains(templateId);
    }
}
