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

import java.sql.Connection;

/**
 * A simple {@link ConnectionHandler} with no setup.
 * 
 * @author Sylvain
 * 
 * @param <T> type of return.
 * @param <X> type of exception.
 */
public abstract class ConnectionHandlerNoSetup<T, X extends Exception> extends ConnectionHandler<T, X> {

    @Override
    public final void setup(Connection c) {
    }

    @Override
    public final boolean canRestoreState() {
        return true;
    }

    @Override
    public final void restoreState(Connection c) {
    }

}
