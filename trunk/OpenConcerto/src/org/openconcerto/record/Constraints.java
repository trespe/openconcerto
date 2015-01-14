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
 
 package org.openconcerto.record;

import org.openconcerto.utils.checks.EmptyObjFromVO;

public final class Constraints {

    static private final Constraint NONE = new Constraint() {
        @Override
        public final boolean check(final Object obj) {
            return true;
        }

        @Override
        public String getDeclarativeForm() {
            return "true";
        }

        @Override
        public String toString() {
            return "no constraint";
        }
    };

    public static final Constraint none() {
        return NONE;
    }

    static private final Constraint EMPTY = new Constraint() {
        @Override
        public final boolean check(final Object obj) {
            return EmptyObjFromVO.getDefaultPredicate().evaluateChecked(obj);
        }

        @Override
        public String getDeclarativeForm() {
            return ". = ''";
        }

        @Override
        public String toString() {
            return "default empty constraint";
        }
    };

    public static final Constraint getDefaultEmpty() {
        return EMPTY;
    }
}
