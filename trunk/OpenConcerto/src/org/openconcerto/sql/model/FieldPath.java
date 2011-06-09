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

import org.openconcerto.sql.model.graph.Path;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A path plus a name designating a field of the last table of the path, eg
 * [SITE,SITE.ID_CONTACT_CHEF]+"EMAIL".
 * 
 * @author Sylvain
 */
public class FieldPath implements IFieldPath {

    public static Set<FieldPath> create(final Path p, final Collection<String> fields) {
        final Set<FieldPath> res = new HashSet<FieldPath>();
        for (final String f : fields) {
            res.add(new FieldPath(p, f));
        }
        return res;
    }

    private final Path p;
    private final String fieldName;

    public FieldPath(final IFieldPath f) {
        this(f.getPath(), f.getField().getName());
    }

    public FieldPath(final Path p, final String fieldName) {
        super();
        this.p = p;
        if (!this.p.isSingleLink())
            throw new IllegalArgumentException(p + " is not single link");
        this.fieldName = fieldName;
        if (!p.getLast().contains(fieldName))
            throw new IllegalArgumentException(fieldName + " is not part of " + p);
    }

    @Override
    public final Path getPath() {
        return this.p;
    }

    public final String getFieldName() {
        return this.fieldName;
    }

    public Object getObject(final SQLRowAccessor r) {
        return this.getObject(r, true);
    }

    /**
     * Return the value of the field at the end of this path from <code>r</code>.
     * <code>getForeign</code> controls whether the safer but slower getForeign() is used.
     * 
     * @param r the row to use.
     * @param getForeign <code>true</code> to use {@link SQLRowAccessor#getForeign(String)},
     *        <code>false</code> for {@link SQLRowAccessor#getObject(String)}.
     * @return the value.
     */
    public Object getObject(final SQLRowValues r, final boolean getForeign) {
        return this.getObject((SQLRowAccessor) r, getForeign);
    }

    private Object getObject(final SQLRowAccessor r, final boolean getForeign) {
        SQLRowAccessor current = r;
        final Path p2 = this.p;
        final int length = p2.length();
        for (int i = 0; i < length; i++) {
            final SQLField step = p2.getSingleStep(i);
            current = getForeign ? current.getForeign(step.getName()) : (SQLRowAccessor) current.getObject(step.getName());
        }
        return current.getObject(this.fieldName);
    }

    @Override
    public SQLField getField() {
        return this.p.getLast().getField(this.fieldName);
    }

    @Override
    public SQLTable getTable() {
        return this.p.getLast();
    }

    @Override
    public FieldPath getFieldPath() {
        return this;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FieldPath) {
            final FieldPath o = (FieldPath) obj;
            return this.p.equals(o.p) && this.fieldName.equals(o.fieldName);
        } else
            return false;
    }

    @Override
    public int hashCode() {
        return this.p.hashCode() + this.fieldName.hashCode();
    }

    @Override
    public String toString() {
        String path = "";
        for (int i = 0; i < this.p.length(); i++) {
            final SQLField step = this.p.getSingleStep(i);
            path += step.getFullName() + ",";
        }
        return this.getClass().getSimpleName() + " " + path + this.fieldName;
    }

}
