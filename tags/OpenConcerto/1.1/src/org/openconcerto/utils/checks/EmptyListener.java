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
 * Créé le 3 févr. 2005
 *  
 */
package org.openconcerto.utils.checks;

/**
 * Pour être prévenu quand un objet devient vide ou non.
 * 
 * @author Sylvain CUAZ
 */
public interface EmptyListener {

    public void emptyChange(EmptyObj src, boolean newValue);

}
