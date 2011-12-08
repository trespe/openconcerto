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
 
 /*
 * Created on 24 janv. 2005
 */
package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

public class ClassGenerator {

    /**
     * Genere dans out.java le squelette d'une classe 'classname' et la traduction a ajouter au
     * mapping xml
     * 
     * @param t le nom de la table a utiliser
     * @param classname le nom de la classe
     */
    public static void generate(SQLTable t, String classname) {
        try {
            FileOutputStream fop = new FileOutputStream("out.java");
            PrintStream out = new PrintStream(fop);
            List f = t.getOrderedFields();
            f.remove(t.getArchiveField());
            f.remove(t.getOrderField());
            f.remove(t.getKey());
            generateAutoLayoutedJComponent(t, f, classname, out, null);

            out.println("");
            generateMappingXML(t, f, out);

            out.close();
            fop.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param t
     * @param out
     * @param f
     */
    public static void generateMappingXML(SQLTable t, List f, PrintStream out) {
        out.println("<TABLE name=\"" + t.getName() + "\">");
        for (Iterator iter = f.iterator(); iter.hasNext();) {
            SQLField element = (SQLField) iter.next();
            final String fieldName = StringUtils.firstUpThenLow(element.getName()).replace('_', ' ');
            out.println("   <FIELD name=\"" + element.getName() + "\" label=\"" + fieldName + "\" titleLabel=\"" + fieldName + "\" />");

        }
        out.println("</TABLE>");
    }

    public static List generateAutoLayoutedJComponent(SQLTable t, List f, String classname, PrintStream out, String packageName) {

        if (packageName != null && packageName.length() > 0) {
            out.print("package ");
            out.print(packageName);
            out.println(";");
            out.println();
        }
        out.println("import org.openconcerto.sql.element.ConfSQLElement;");
        out.println("import org.openconcerto.sql.element.SQLComponent;");
        out.println("import org.openconcerto.sql.element.UISQLComponent;");
        out.println("import org.openconcerto.sql.model.SQLRow;");

        out.println();
        out.println("import java.util.ArrayList;");
        out.println("import java.util.HashSet;");
        out.println("import java.util.List;");
        out.println("import java.util.Set;");
        out.println();
        out.println("public class " + classname + " extends ConfSQLElement {");
        out.println();

        // Constructor
        out.println("    public " + classname + "() {");// static final String NAME = \"un ??\";");
        out.println("        super(\"" + t.getName() + "\", \"un " + t.getName().toLowerCase() + " \", \"" + t.getName().toLowerCase() + "s\");");
        out.println("    }");
        out.println();

        // List
        out.println("    protected List<String> getListFields() {");
        out.println("        final List<String> l = new ArrayList<String>();");
        for (Iterator iter = f.iterator(); iter.hasNext();) {
            SQLField element = (SQLField) iter.next();
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("        l.add(\"" + element.getName() + "\");");
            }
        }
        out.println("        return l;");
        out.println("    }");
        out.println();

        // Combo
        out.println("    protected List<String> getComboFields() {");
        out.println("        final List<String> l = new ArrayList<String>();");
        for (Iterator iter = f.iterator(); iter.hasNext();) {
            SQLField element = (SQLField) iter.next();
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("        l.add(\"" + element.getName() + "\");");
            }
        }
        out.println("        return l;");
        out.println("    }");
        out.println();

        // UI
        out.println("    public SQLComponent createComponent() {");
        out.println("        return new UISQLComponent(this) {");
        out.println();
        out.println("            @Override");
        out.println("            protected Set<String> createRequiredNames() {");
        out.println("                final Set<String> s = new HashSet<String>();");
        for (Iterator iter = f.iterator(); iter.hasNext();) {
            SQLField element = (SQLField) iter.next();
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("                // s.add(\"" + element.getName() + "\");");
            }
        }
        out.println("                return s;");
        out.println("            }");
        out.println();
        out.println("            public void addViews() {");
        SQLField first = null;
        for (Iterator iter = f.iterator(); iter.hasNext();) {
            SQLField element = (SQLField) iter.next();
            if (first == null) {
                first = element;
            }
            if (!element.isPrimaryKey() && !element.getName().equals("ORDRE")) {
                out.println("                this.addView(\"" + element.getName() + "\");");
            }
        }
        out.println("            }");
        out.println("        };");
        out.println("    }");
        out.println();
        out.println("    public String getDescription(SQLRow fromRow) {");
        out.println("        return fromRow.getString(\"" + first.getName() + "\");");
        out.println("    }");
        out.println();
        out.println("}");

        return f;
    }

    public static String generateAutoLayoutedJComponent(SQLTable table, String c, String packageName) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(b);
        final List<SQLField> f = getOrderedContentFields(table);
        generateAutoLayoutedJComponent(table, f, c, ps, packageName);
        ps.close();
        return b.toString();
    }

    public static String generateMappingXML(SQLTable table, String c) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(b);
        final List<SQLField> f = getOrderedContentFields(table);
        generateMappingXML(table, f, ps);
        ps.close();
        return b.toString();
    }

    private static List<SQLField> getOrderedContentFields(SQLTable table) {
        final List<SQLField> f = table.getOrderedFields();
        f.retainAll(table.getContentFields());
        return f;
    }

    public static String getStandardClassName(String n) {
        int nb = n.length();
        StringBuilder b = new StringBuilder(nb);
        if (n.toUpperCase().equals(n)) {
            n = n.toLowerCase();
        }
        n = StringUtils.firstUp(n);

        for (int i = 0; i < nb; i++) {
            char c = n.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                b.append(c);
            }

        }
        return b.toString();
    }
}
