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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.utils.cc.ITransformer;

import java.util.List;

public class TransfFieldExpander extends FieldExpander {

    private final ITransformer<SQLField, List<SQLField>> transf;

    /**
     * Construct a new instance.
     * 
     * @param transf to transform a foreign key.
     */
    public TransfFieldExpander(ITransformer<SQLField, List<SQLField>> transf) {
        super();
        this.transf = transf;
    }

    public TransfFieldExpander(TransfFieldExpander o) {
        // our superclass has no state apart from the cache which we don't want
        this(o.transf);
    }

    /**
     * If the transformer has changed (ie for the same input it will now return a different output),
     * you have to call this method.
     */
    public final void clearCache() {
        super.clearCache();
    }

    // *** expand

    protected List<SQLField> expandOnce(SQLField field) {
        return this.transf.transformChecked(field);
    }

}
