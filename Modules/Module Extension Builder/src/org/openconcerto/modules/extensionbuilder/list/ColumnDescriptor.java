package org.openconcerto.modules.extensionbuilder.list;

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.utils.StringUtils;

public class ColumnDescriptor {
    private List<String> fieldPaths = new ArrayList<String>();
    private String id;
    private String style = "concat";

    public ColumnDescriptor(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getFieldsPaths() {
        String r = "";
        final int size = fieldPaths.size();
        for (int i = 0; i < size; i++) {
            String fieldPath = fieldPaths.get(i);
            if (i != 0) {
                r += ",";
            }
            r += fieldPath;
        }
        return r;
    }

    public void setFieldsPaths(String paths) {
        final List<String> l = StringUtils.fastSplit(paths, ',');
        fieldPaths.clear();
        for (String string : l) {
            fieldPaths.add(string.trim());
        }
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

}
