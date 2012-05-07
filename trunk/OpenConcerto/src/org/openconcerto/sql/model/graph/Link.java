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
 * Link created on 13 mai 2004
 */
package org.openconcerto.sql.model.graph;

import static org.openconcerto.xml.JDOMUtils.OUTPUTTER;
import static java.util.Collections.singletonList;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLIdentifier;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;

import java.io.PrintWriter;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.ThreadSafe;

import org.jdom.Element;

/**
 * Un lien dans le graphe des tables. Par exemple, si la table ECLAIRAGE a un champ ID_LOCAL, alors
 * il existe un lien avec comme source ECLAIRAGE, comme destination LOCAL et comme label le champ
 * ECLAIRAGE.ID_LOCAL.
 * 
 * @author ILM Informatique 13 mai 2004
 */
@ThreadSafe
public class Link extends DirectedEdge<SQLTable> {

    public static enum Rule {

        SET_NULL("SET NULL"), SET_DEFAULT("SET DEFAULT"), CASCADE("CASCADE"), RESTRICT("RESTRICT"), NO_ACTION("NO ACTION");

        private final String sql;

        private Rule(final String sql) {
            this.sql = sql;
        }

        public static Rule fromShort(short s) {
            switch (s) {
            case DatabaseMetaData.importedKeyCascade:
                return CASCADE;
            case DatabaseMetaData.importedKeySetNull:
                return SET_NULL;
            case DatabaseMetaData.importedKeySetDefault:
                return SET_DEFAULT;
            case DatabaseMetaData.importedKeyRestrict:
                return RESTRICT;
            case DatabaseMetaData.importedKeyNoAction:
                return NO_ACTION;
            default:
                throw new IllegalArgumentException("Unknown rule " + s);
            }
        }

        public static Rule fromName(String n) {
            return n == null ? null : valueOf(n);
        }

        public String asString() {
            return this.sql;
        }
    }

    // ArrayList is thread-safe if not modified
    private final List<SQLField> cols;
    private final List<String> colsNames;
    private final List<SQLField> refCols;
    private final List<String> refColsNames;
    private final String name;
    private final Rule updateRule, deleteRule;

    /**
     * Creates a link between two tables.
     * 
     * @param keys foreign fields of the source table.
     * @param referredCols fields of the destination table.
     * @param foreignKeyName the name of the constraint, can be <code>null</code>.
     * @param updateRule what happens to a foreign key when the primary key is updated.
     * @param deleteRule what happens to the foreign key when primary is deleted.
     */
    public Link(List<SQLField> keys, List<SQLField> referredCols, String foreignKeyName, Rule updateRule, Rule deleteRule) {
        super(keys.get(0).getTable(), referredCols.get(0).getTable());
        if (keys.size() != referredCols.size())
            throw new IllegalArgumentException("size mismatch: " + keys + " != " + referredCols);
        this.cols = Collections.unmodifiableList(new ArrayList<SQLField>(keys));
        final ArrayList<String> tmpCols = new ArrayList<String>(this.cols.size());
        for (final SQLField f : this.cols) {
            tmpCols.add(f.getName());
        }
        this.colsNames = Collections.unmodifiableList(tmpCols);
        this.refCols = Collections.unmodifiableList(new ArrayList<SQLField>(referredCols));
        final ArrayList<String> tmpRefCols = new ArrayList<String>(this.refCols.size());
        for (final SQLField f : this.refCols) {
            tmpRefCols.add(f.getName());
        }
        this.refColsNames = Collections.unmodifiableList(tmpRefCols);
        this.name = foreignKeyName;
        this.updateRule = updateRule;
        this.deleteRule = deleteRule;
    }

    public final List<SQLField> getFields() {
        return this.cols;
    }

    public final SQLField getSingleField() {
        return CollectionUtils.getSole(this.cols);
    }

    public final List<String> getCols() {
        return this.colsNames;
    }

    public final SQLField getLabel() {
        if (this.cols.size() == 1)
            return this.cols.get(0);
        else
            throw new IllegalStateException(this + " has more than 1 foreign column: " + this.getFields());
    }

    public List<SQLField> getRefFields() {
        return this.refCols;
    }

    public final List<String> getRefCols() {
        return this.refColsNames;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * The contextual name of the target from the source. Eg if SCH.A points SCH2.B, this would
     * return "SCH2"."B".
     * 
     * @return the name of the target.
     * @see SQLIdentifier#getContextualSQLName(SQLIdentifier)
     */
    public final SQLName getContextualName() {
        return this.getTarget().getContextualSQLName(this.getSource());
    }

    public final Rule getUpdateRule() {
        return this.updateRule;
    }

    public final Rule getDeleteRule() {
        return this.deleteRule;
    }

    @Override
    public String toString() {
        return "<" + this.getFields() + " -> " + this.getTarget() + (this.getName() != null ? " '" + this.getName() + "'" : "") + ">";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Link)) {
            return false;
        }
        Link o = (Link) other;
        return this.getFields().equals(o.getFields()) && this.getRefFields().equals(o.getRefFields()) && this.getUpdateRule() == o.getUpdateRule() && this.getDeleteRule() == o.getDeleteRule();
    }

    @Override
    public int hashCode() {
        return this.getFields().hashCode() + this.getRefFields().hashCode();
    }

    void toXML(final PrintWriter pWriter) {
        pWriter.print("  <link to=\"" + OUTPUTTER.escapeAttributeEntities(this.getTarget().getSQLName().toString()) + "\" ");
        if (this.getName() != null)
            pWriter.print("name=\"" + OUTPUTTER.escapeAttributeEntities(this.getName()) + "\" ");
        if (this.getUpdateRule() != null)
            pWriter.print("updateRule=\"" + OUTPUTTER.escapeAttributeEntities(this.getUpdateRule().name()) + "\" ");
        if (this.getDeleteRule() != null)
            pWriter.print("deleteRule=\"" + OUTPUTTER.escapeAttributeEntities(this.getDeleteRule().name()) + "\" ");
        if (this.getFields().size() == 1) {
            toXML(pWriter, 0);
            pWriter.println("/>");
        } else {
            pWriter.println(">");
            for (int i = 0; i < this.getFields().size(); i++) {
                pWriter.print("    <l ");
                toXML(pWriter, i);
                pWriter.println("/>");
            }
            pWriter.println("  </link>");
        }
    }

    private void toXML(final PrintWriter pWriter, final int i) {
        pWriter.print("col=\"");
        pWriter.print(OUTPUTTER.escapeAttributeEntities(this.getFields().get(i).getName()));
        pWriter.print("\" refCol=\"");
        pWriter.print(OUTPUTTER.escapeAttributeEntities(this.getRefFields().get(i).getName()));
        pWriter.print("\"");
    }

    static Link fromXML(final SQLTable t, final Element linkElem) {
        final SQLName to = SQLName.parse(linkElem.getAttributeValue("to"));
        final SQLTable foreignTable = t.getDBSystemRoot().getDesc(to, SQLTable.class);
        final String linkName = linkElem.getAttributeValue("name");
        final Rule updateRule = Rule.fromName(linkElem.getAttributeValue("updateRule"));
        final Rule deleteRule = Rule.fromName(linkElem.getAttributeValue("deleteRule"));
        @SuppressWarnings("unchecked")
        final List<Element> lElems = linkElem.getAttribute("col") != null ? singletonList(linkElem) : linkElem.getChildren("l");
        final List<SQLField> cols = new ArrayList<SQLField>();
        final List<SQLField> refcols = new ArrayList<SQLField>();
        for (final Element l : lElems) {
            cols.add(t.getField(l.getAttributeValue("col")));
            refcols.add(foreignTable.getField(l.getAttributeValue("refCol")));
        }
        return new Link(cols, refcols, linkName, updateRule, deleteRule);
    }
}
