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
 
 package org.openconcerto.utils.beans.list;

import org.openconcerto.utils.beans.Bean;
import org.openconcerto.utils.model.IMutableListModel;

import java.beans.PropertyDescriptor;

/**
 * A mutable adapter, non editable unless overloaded. You just have to define removeElementAt().
 * 
 * @author Sylvain CUAZ
 */
public abstract class BeanMutableListModelAdapter extends BeanListModelAdapter implements IMutableListModel {

    public BeanMutableListModelAdapter(Bean b, PropertyDescriptor desc) {
        super(b, desc);
    }

    // Adds an item at the end of the model.
    public void addElement(Object obj) {
    }

    // Adds an item at a specific index.
    public void insertElementAt(Object obj, int index) {
    }

}
