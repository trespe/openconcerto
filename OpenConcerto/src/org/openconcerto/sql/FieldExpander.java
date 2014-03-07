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
 
 package org.openconcerto.sql;

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.IFieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.Tuple2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class FieldExpander {

    static private final FieldExpander EMPTY = new FieldExpander() {
        @Override
        protected List<SQLField> expandOnce(SQLField field) {
            return Collections.emptyList();
        }
    };

    public static FieldExpander getEmpty() {
        return EMPTY;
    }

    // eg |TABLEAU.ID_OBSERVATION| -> [[DESIGNATION], []]
    private final Map<IFieldPath, List<FieldPath>> cache;
    private final Map<List<? extends IFieldPath>, List<Tuple2<Path, List<FieldPath>>>> cacheGroupBy;

    public FieldExpander() {
        super();
        this.cache = new HashMap<IFieldPath, List<FieldPath>>();
        this.cacheGroupBy = new HashMap<List<? extends IFieldPath>, List<Tuple2<Path, List<FieldPath>>>>();
    }

    /**
     * If for the same input expandOnce() will now return a different output, you have to call this
     * method.
     */
    protected void clearCache() {
        this.cache.clear();
        this.cacheGroupBy.clear();
    }

    // *** expand

    protected abstract List<SQLField> expandOnce(SQLField field);

    protected final List<FieldPath> expandOnce(IFieldPath field) {
        final List<SQLField> e = this.expandOnce(field.getField());
        if (e == null)
            return null;
        if (e.size() == 0)
            return Collections.emptyList();

        final List<FieldPath> res = new ArrayList<FieldPath>(e.size());
        final Path newPath = field.getPath().add(field.getField());
        for (final SQLField f : e)
            res.add(new FieldPath(newPath, f.getName()));
        return res;
    }

    /**
     * Expand le champ.
     * 
     * @param field le champ à expandre, eg |SITE.ID_ETABLISSEMENT|.
     * @return la liste des champs, eg [|ETABLISSEMENT.DESCRIPTION|, |ETABLISSEMENT.NUMERO|].
     */
    public final List<SQLField> simpleExpand(SQLField field) {
        final List<FieldPath> fieldPaths = this.expand(field);
        final List<SQLField> res = new ArrayList<SQLField>(fieldPaths.size());
        for (final FieldPath fp : fieldPaths)
            res.add(fp.getField());
        return res;
    }

    /**
     * Expand le champ.
     * 
     * @param field le nom du champ à expandre, eg "SITE.ID_ETABLISSEMENT".
     * @return les champs.
     */
    public final List<FieldPath> expand(IFieldPath field) {
        // eg field == BATIMENT.ID_SITE
        if (this.cache.containsKey(field)) {
            return this.cache.get(field);
        }

        final List<FieldPath> fields = new ArrayList<FieldPath>();

        if (!field.getTable().getForeignKeys().contains(field.getField())) {
            // si ce n'est pas une clef alors il n'y a pas à l'expandre
            fields.add(field.getFieldPath());
        } else {
            // eg [SITE.DESIGNATION]
            final List<FieldPath> tmp = expandOnce(field);
            if (tmp == null) {
                // on ne sait pas comment l'expandre
                // c'est une clef externe, donc elle pointe sur une table
                final SQLTable foreignTable = field.getTable().getBase().getGraph().getForeignTable(field.getField());
                throw new IllegalStateException(field + " cannot be expanded by " + this + "\nforeign table of " + field.getField().getSQLName() + ":" + foreignTable.getSQLName());
            }

            for (final FieldPath f : tmp) {
                fields.addAll(this.expand(f));
            }
        }

        final List<FieldPath> res = fields;
        this.cache.put(field, res);
        return res;
    }

    /**
     * Expand the passed SQLRowValues by replacing each foreign key with a SQLRowValues containing
     * the expanded fields.
     * 
     * @param vals the SQLRowValues to expand.
     */
    public final void expand(SQLRowValues vals) {
        final Set<SQLField> fks = vals.getTable().getForeignKeys();
        for (final String fName : vals.getFields()) {
            final SQLField ffield = vals.getTable().getField(fName);
            if (fks.contains(ffield)) {
                final List<SQLField> expandedFields = this.expandOnce(ffield);
                if (expandedFields.size() > 0) {
                    final SQLRowValues foreignVals = new SQLRowValues(expandedFields.get(0).getTable());
                    vals.put(fName, foreignVals);
                    for (final SQLField expandedField : expandedFields) {
                        foreignVals.put(expandedField.getName(), null);
                    }
                    this.expand(foreignVals);
                }
            }
        }
    }

    /**
     * Expand the passed list of fields as would {@link #simpleExpand(SQLField)} but group the
     * result by the parent foreign key. For each item of the result, the path to the ancestor is
     * also included, eg [ LOCAL, LOCAL.ID_BATIMENT, LOCAL.ID_BATIMENT.ID_SITE,
     * LOCAL.ID_BATIMENT.ID_SITE.ID_ETABLISSEMENT ].
     * 
     * @param fieldsOrig fields of a table, eg [ DESIGNATION, ID_BATIMENT].
     * @return the complete expansion, eg [ [LOCAL.DESIGNATION], [BAT.DES], [SITE.DES, ADRESSE.CP],
     *         [ETABLISSEMENT.DES] ].
     */
    public final List<Tuple2<Path, List<FieldPath>>> expandGroupBy(List<? extends IFieldPath> fieldsOrig) {
        if (this.cacheGroupBy.containsKey(fieldsOrig))
            return this.cacheGroupBy.get(fieldsOrig);

        if (fieldsOrig.size() == 0)
            return Collections.emptyList();
        // MAYBE check that all fields belong to the same path
        final Path fieldsPath = fieldsOrig.get(0).getPath();
        final SQLField parentFF = Configuration.getInstance().getDirectory().getElement(fieldsPath.getLast()).getParentForeignField();

        final List<Tuple2<Path, List<FieldPath>>> res = new ArrayList<Tuple2<Path, List<FieldPath>>>();
        final List<FieldPath> currentL = new ArrayList<FieldPath>();
        res.add(Tuple2.create(fieldsPath, currentL));
        IFieldPath parent = null;
        for (final IFieldPath f : fieldsOrig) {
            if (f.getField().equals(parentFF))
                parent = f;
            else
                currentL.addAll(this.expand(f));
        }
        if (parent != null)
            res.addAll(this.expandGroupBy(this.expandOnce(parent)));

        this.cacheGroupBy.put(fieldsOrig, res);

        return res;
    }
}
