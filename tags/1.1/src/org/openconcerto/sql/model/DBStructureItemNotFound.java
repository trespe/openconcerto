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

/**
 * An item of the database tree structure (server/base/schema/...) has not been found. For example
 * {@link DBStructureItem#getCheckedChild(String)} throws this, so instead of letting all methods
 * calling that method have another parameter to indicate whether they want null or an exn when an
 * item is not found, you simply catch or not this exception.
 * 
 * @author Sylvain
 */
public final class DBStructureItemNotFound extends RuntimeException {

    public DBStructureItemNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public DBStructureItemNotFound(String message) {
        super(message);
    }

}
