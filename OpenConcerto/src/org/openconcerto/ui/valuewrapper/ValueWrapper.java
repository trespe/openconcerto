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
 
 package org.openconcerto.ui.valuewrapper;

import org.openconcerto.utils.checks.MutableValueObject;
import org.openconcerto.utils.checks.ValidObject;

import javax.swing.JComponent;

/**
 * A wrapper around a JComponent to expose its value as a certain type.
 * 
 * @author Sylvain
 * 
 * @param <T> the type of value the component has.
 */
public interface ValueWrapper<T> extends ValidObject, MutableValueObject<T> {

    public JComponent getComp();

}
