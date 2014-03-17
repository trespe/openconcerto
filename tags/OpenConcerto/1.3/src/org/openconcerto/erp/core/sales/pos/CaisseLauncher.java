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
 
 package org.openconcerto.erp.core.sales.pos;

import org.openconcerto.erp.config.GestionLauncher;
import org.openconcerto.erp.core.sales.pos.ui.CaisseFrame;

import java.io.IOException;
import java.util.Arrays;

public class CaisseLauncher extends GestionLauncher {
    /**
     * Launch Caisse.
     * 
     * @param args the one-time arguments.
     * @throws IOException if the launch failed.
     */
    public static void main(String[] args) throws IOException {
        new CaisseLauncher().launch(CaisseFrame.class.getName(), Arrays.asList(args));
    }

    @Override
    protected int getInitialMemory() {
        return 64;
    }

    @Override
    protected int getMaxMemory() {
        return 512;
    }
}
