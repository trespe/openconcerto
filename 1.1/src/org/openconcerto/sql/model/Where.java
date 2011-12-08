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
 
 package org.openconcerto.sql.model;

import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.functors.InstanceofPredicate;

/**
 * Une clause WHERE dans une requete SQL. Une clause peut être facilement combinée avec d'autre,
 * exemple : prenomPasVide.and(pasIndéfini).and(age_sup_3.or(assez_grand)).
 * 
 * @author ILM Informatique 27 sept. 2004
 */
public class Where {

    static public final Where FALSE = Where.createRaw("1=0");
    static public final Where TRUE = Where.createRaw("1=1");
    static public final String NULL_IS_DATA_EQ = new String("===");
    static public final String NULL_IS_DATA_NEQ = new String("IS DISTINCT FROM");

    private static abstract class Combiner {
        public final Where combine(Where w1, Where w2) {
            if (w1 == null)
                return w2;
            else
                return this.combineNotNull(w1, w2);
        }

        protected abstract Where combineNotNull(Where w1, Where w2);
    }

    private static Combiner AndCombiner = new Combiner() {
        protected Where combineNotNull(Where where1, Where where2) {
            return where1.and(where2);
        }
    };

    private static Combiner OrCombiner = new Combiner() {
        protected Where combineNotNull(Where where1, Where where2) {
            return where1.or(where2);
        }
    };

    static private Where combine(Collection<Where> wheres, Combiner c) {
        Where res = null;
        for (final Where w : wheres) {
            res = c.combine(res, w);
        }
        return res;
    }

    static public Where and(Collection<Where> wheres) {
        return combine(wheres, AndCombiner);
    }

    static public Where or(Collection<Where> wheres) {
        return combine(wheres, OrCombiner);
    }

    /**
     * Permet de faire un ET entre 2 where.
     * 
     * @param where1 le 1er, peut être <code>null</code>.
     * @param where2 le 2ème, peut être <code>null</code>.
     * @return le ET, peut être <code>null</code>.
     */
    static public Where and(Where where1, Where where2) {
        return AndCombiner.combine(where1, where2);
    }

    static public Where isNull(FieldRef ref) {
        return new Where(ref, "is", (Object) null);
    }

    static public Where isNotNull(FieldRef ref) {
        return new Where(ref, "is not", (Object) null);
    }

    static public Where createRaw(String clause, FieldRef... refs) {
        return new Where(clause, Arrays.asList(refs));
    }

    static public Where createRaw(String clause, Collection<? extends FieldRef> refs) {
        return new Where(clause, refs);
    }

    /**
     * To create complex Where not possible with constructors.
     * 
     * @param pattern a pattern to be passed to {@link SQLSelect#quote(String, Object...)}, eg
     *        "EXTRACT(YEAR FROM %n) = 3007".
     * @param params the params to be passed to <code>quote()</code>, eg [|MISSION.DATE_DBT|].
     * @return a new Where with the result from <code>quote()</code> as its clause, and all
     *         <code>FieldRef</code> in params as its fields, eg {EXTRACT(YEAR FROM "DATE_DBT") =
     *         3007 , |MISSION.DATE_DBT|}.
     */
    @SuppressWarnings("unchecked")
    static public Where quote(String pattern, Object... params) {
        return new Where(SQLSelect.quote(pattern, params), CollectionUtils.select(Arrays.asList(params), new InstanceofPredicate(FieldRef.class)));
    }

    static private final String comparison(FieldRef ref, String op, String y) {
        if (op == NULL_IS_DATA_EQ || op == NULL_IS_DATA_NEQ) {
            return ref.getField().getServer().getSQLSystem().getSyntax().getNullIsDataComparison(ref.getFieldRef(), op == NULL_IS_DATA_EQ, y);
        } else {
            return ref.getFieldRef() + " " + op + " " + y;
        }
    }

    private final List<FieldRef> fields;
    private String clause;

    {
        this.fields = new ArrayList<FieldRef>();
        this.clause = "";
    }

    public Where(FieldRef field1, String op, FieldRef field2) {
        this.fields.add(field1);
        this.fields.add(field2);
        this.clause = comparison(field1, op, field2.getFieldRef());
    }

    public Where(FieldRef field1, String op, int scalar) {
        this(field1, op, (Integer) scalar);
    }

    /**
     * Construct a clause like "field = 'hi'". Note: this method will try to rewrite "= null" and
     * "<> null" to "is null" and "is not null", treating null as a Java <code>null</code> (ie null
     * == null) and not as a SQL NULL (NULL != NULL), see PostgreSQL documentation section 9.2.
     * Comparison Operators. ATTN new Where(f, "=", null) will call
     * {@link #Where(FieldRef, String, FieldRef)}, you have to cast to Object.
     * 
     * @param ref a field.
     * @param op an arbitrary operator.
     * @param o the object to compare <code>ref</code> to.
     */
    public Where(FieldRef ref, String op, Object o) {
        this.fields.add(ref);
        if (o == null) {
            if (op.trim().equals("="))
                op = "is";
            else if (op.trim().equals("<>"))
                op = "is not";
        }
        this.clause = comparison(ref, op, ref.getField().getType().toString(o));
    }

    /**
     * Crée une clause "field1 in (values)". Some databases won't accept empty values (impossible
     * where clause), so we return false.
     * 
     * @param field1 le champs à tester.
     * @param values les valeurs.
     */
    public Where(final FieldRef field1, Collection<?> values) {
        this(field1, true, values);
    }

    /**
     * Construct a clause like "field1 not in (value, ...)".
     * 
     * @param field1 le champs à tester.
     * @param in <code>true</code> for "in", <code>false</code> for "not in".
     * @param values les valeurs.
     */
    public Where(final FieldRef field1, final boolean in, Collection<?> values) {
        if (values.isEmpty()) {
            this.clause = in ? FALSE.getClause() : TRUE.getClause();
        } else {
            this.fields.add(field1);
            final String op = in ? " in (" : " not in (";
            this.clause = field1.getFieldRef() + op + CollectionUtils.join(values, ",", new ITransformer<Object, String>() {
                @Override
                public String transformChecked(Object input) {
                    return field1.getField().getType().toString(input);
                }
            }) + ")";
        }
    }

    /**
     * Crée une clause "field BETWEEN borneInf AND borneSup".
     * 
     * @param ref le champs à tester.
     * @param borneInf la valeur minimum.
     * @param borneSup la valeur maximum.
     */
    public Where(FieldRef ref, Object borneInf, Object borneSup) {
        final SQLField field1 = ref.getField();
        this.fields.add(ref);
        this.clause = ref.getFieldRef() + " BETWEEN " + field1.getType().toString(borneInf) + " AND " + field1.getType().toString(borneSup);
    }

    /**
     * Crée une clause pour que <code>ref</code> soit compris entre <code>bornInf</code> et
     * <code>bornSup</code>.
     * 
     * @param ref a field, eg NAME.
     * @param borneInf the lower bound, eg "DOE".
     * @param infInclusive <code>true</code> if the lower bound should be included, eg
     *        <code>false</code> if "DOE" shouldn't match.
     * @param borneSup the upper bound, eg "SMITH".
     * @param supInclusive <code>true</code> if the upper bound should be included.
     */
    public Where(FieldRef ref, Object borneInf, boolean infInclusive, Object borneSup, boolean supInclusive) {
        this.fields.add(ref);
        final String infClause = new Where(ref, infInclusive ? ">=" : ">", borneInf).getClause();
        final String supClause = new Where(ref, supInclusive ? "<=" : "<", borneSup).getClause();
        this.clause = infClause + " AND " + supClause;
    }

    // raw ctor, see static methods
    private Where(String clause, Collection<? extends FieldRef> refs) {
        this.fields.addAll(refs);
        this.clause = clause;
    }

    private Where() {
        /* Pour combine() */
    }

    /**
     * Clone un Where.
     * 
     * @param orig l'instance à cloner.
     */
    public Where(Where orig) {
        this(orig.clause, orig.fields);
    }

    public Where or(Where w) {
        return this.combine(w, "OR");
    }

    public Where and(Where w) {
        return this.combine(w, "AND");
    }

    public Where not() {
        final Where res = new Where(this);
        res.clause = "NOT (" + this.clause + ")";
        return res;
    }

    private Where combine(Where w, String op) {
        if (w == null)
            return this;

        Where res = new Where();
        res.fields.addAll(this.fields);
        res.fields.addAll(w.fields);

        res.clause = "(" + this.clause + ") " + op + " (" + w.clause + ")";
        return res;
    }

    /**
     * La clause.
     * 
     * @return la clause.
     */
    public String getClause() {
        return this.clause;
    }

    /**
     * Les champs utilisés dans cette clause.
     * 
     * @return a list of FieldRef.
     */
    public List<FieldRef> getFields() {
        return this.fields;
    }

    public String toString() {
        return this.getClause();
    }

    public boolean equals(Object obj) {
        if (obj instanceof Where) {
            Where o = ((Where) obj);
            return this.getClause().equals(o.getClause()) && this.getFields().equals(o.getFields());
        } else
            return false;
    }

    public int hashCode() {
        return this.getClause().hashCode() + this.getFields().hashCode();
    }
}
