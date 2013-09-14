package org.openconcerto.modules.extensionbuilder.translation.action;

import org.openconcerto.modules.extensionbuilder.translation.Translation;

public class ActionTranslation extends Translation {

    private final String id;
    private String label;

    public ActionTranslation(String lang, String id) {
        super(lang);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
