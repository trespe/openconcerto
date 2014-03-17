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
 * SQLTableListener created on 15 mai 2004
 * 
 */
package org.openconcerto.sql.model;

/**
 * @author ILM Informatique 15 mai 2004
 * 
 */
public interface SQLTableListener {

    /**
     * Invoquée quand une SQLTable a des changements (ajout ou modif d'un element).
     * 
     * @param table la table modifiée.
     * @param id la ligne modifiée si >= {@link SQLRow#MIN_VALID_ID}, sinon toute la table.
     */
    public void rowModified(SQLTable table, int id);

    public void rowAdded(SQLTable table, int id);

    public void rowDeleted(SQLTable table, int id);

}
