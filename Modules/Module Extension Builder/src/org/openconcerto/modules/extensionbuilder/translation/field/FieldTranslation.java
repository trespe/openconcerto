package org.openconcerto.modules.extensionbuilder.translation.field;

import org.openconcerto.modules.extensionbuilder.translation.Translation;

public class FieldTranslation extends Translation {

    private final String tableName;
    private final String fieldName;
    private String label;
    private String documentation;

    public FieldTranslation(String lang, String tableName, String fieldName) {
        super(lang);
        this.tableName = tableName;
        this.fieldName = fieldName;

    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTableName() {
        return tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getLabel() {
        return label;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

}
