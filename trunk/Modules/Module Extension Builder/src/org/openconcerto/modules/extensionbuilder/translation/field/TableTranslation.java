package org.openconcerto.modules.extensionbuilder.translation.field;

import org.openconcerto.modules.extensionbuilder.translation.Translation;

public class TableTranslation extends Translation {

    private String tableName;
    private String singular;
    private String plural;

    public TableTranslation(String lang, String tableName) {
        super(lang);
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setSingular(String singular) {
        this.singular = singular;
    }

    public String getSingular() {
        return singular;
    }

    public void setPlural(String plural) {
        this.plural = plural;
    }

    public String getPlural() {
        return plural;
    }
}
