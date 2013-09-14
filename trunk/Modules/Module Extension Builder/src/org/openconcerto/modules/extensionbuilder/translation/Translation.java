package org.openconcerto.modules.extensionbuilder.translation;

public class Translation {
    private String locale;

    public Translation(String lang) {
        this.locale = lang;
    }

    public String getLocale() {
        return this.locale;
    }
}
