package org.openconcerto.modules.extensionbuilder.translation.menu;

import org.openconcerto.modules.extensionbuilder.translation.Translation;

public class MenuTranslation extends Translation {

    private final String id;
    private String label;

    public MenuTranslation(String lang, String id) {
        super(lang);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

}
