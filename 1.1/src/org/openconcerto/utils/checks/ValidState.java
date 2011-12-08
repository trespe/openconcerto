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
 
 package org.openconcerto.utils.checks;

import org.openconcerto.utils.CompareUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class ValidState {

    static private final ValidState TRUE = new ValidState(true, null);
    static private final ValidState FALSE = new ValidState(false, null);
    static private final Map<String, ValidState> cache = new LinkedHashMap<String, ValidState>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, ValidState> eldest) {
            return this.size() > 50;
        }
    };

    static public final ValidState getTrueInstance() {
        return TRUE;
    }

    static public final ValidState getNoReasonInstance(final boolean isValid) {
        return isValid ? TRUE : FALSE;
    }

    /**
     * Only create an instance if necessary. This method only caches instances without reason, so it
     * can be used if the reason is not a constant so as not to pollute the cache :
     * <code>ValidState.create(!clientExists, "Client '" + name + "' already exists")</code>
     * 
     * @param valid the validity.
     * @param invalidityReason the reason that valid is <code>false</code>.
     * @return a new instance if needed (e.g. if valid is <code>true</code> will return
     *         {@link #getTrueInstance()}).
     * @see #createCached(boolean, String)
     */
    static public final ValidState create(boolean valid, String reason) {
        return create(valid, reason, false);
    }

    /**
     * Only create an instance if necessary. This method caches all instances, so it should be used
     * if reason is a constant :
     * <code>ValidState.createCached(!clientExists, "Client already exists")</code>. Of course if
     * reason is a constant you can get better performance by creating a constant instead of relying
     * on a non-deterministic cache.
     * 
     * @param valid the validity.
     * @param invalidityReason the reason that valid is <code>false</code>.
     * @return a new instance if not already cached.
     * @see #create(boolean, String)
     */
    static public final ValidState createCached(boolean valid, String reason) {
        return create(valid, reason, true);
    }

    static public final ValidState create(boolean valid, String reason, final boolean cacheNonValid) {
        if (valid) {
            return TRUE;
        } else if (reason == null) {
            return FALSE;
        } else if (cacheNonValid) {
            ValidState res = cache.get(reason);
            if (res == null) {
                res = new ValidState(valid, reason);
                cache.put(reason, res);
            }
            return res;
        } else {
            return new ValidState(valid, reason);
        }
    }

    static public final ValidState createInvalid(String reason) {
        // to cache use createCached(false, reason)
        return new ValidState(false, reason);
    }

    private final boolean valid;
    private final String reason;

    /**
     * Create a new instance.
     * 
     * @param valid the validity.
     * @param invalidityReason the reason that valid is <code>false</code>, ignored if valid is
     *        <code>true</code>.
     * @see #create(boolean, String)
     */
    public ValidState(boolean valid, String invalidityReason) {
        this.valid = valid;
        this.reason = valid ? null : invalidityReason;
    }

    public final boolean isValid() {
        return this.valid;
    }

    /**
     * Why {@link #isValid()} is <code>false</code> (always <code>null</code> if <code>true</code>).
     * 
     * @return an explanation, e.g. "value is negative", can be <code>null</code>.
     */
    public final String getValidationText() {
        return this.reason;
    }

    private final boolean validationTextIsEmpty() {
        return this.reason == null || this.reason.trim().length() == 0;
    }

    public ValidState and(ValidState other) {
        return this.and(other, "\n");
    }

    public ValidState and(ValidState other, final String sep) {
        if (this.equals(other))
            return this;
        else if (this.equals(TRUE))
            return other;
        else if (other.equals(TRUE))
            return this;
        final String reason;
        if (CompareUtils.equals(this.getValidationText(), other.getValidationText()))
            reason = this.getValidationText();
        else if (this.validationTextIsEmpty())
            reason = other.getValidationText();
        else if (other.validationTextIsEmpty())
            reason = this.getValidationText();
        else
            reason = this.getValidationText() + sep + other.getValidationText();
        return new ValidState(this.isValid() && other.isValid(), reason);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.reason == null) ? 0 : this.reason.hashCode());
        result = prime * result + (this.valid ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ValidState other = (ValidState) obj;
        return this.valid == other.valid && CompareUtils.equals(this.reason, other.reason);
    }
}
