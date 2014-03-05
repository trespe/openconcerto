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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.RemoteShell;
import org.openconcerto.utils.OSFamily;
import org.openconcerto.utils.prog.VMLauncher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GestionLauncher extends VMLauncher {
    /**
     * Launch Gestion.
     * 
     * @param args the one-time arguments.
     * @throws IOException if the launch failed.
     */
    public static void main(String[] args) throws IOException {
        new GestionLauncher().launch(Gestion.class.getName(), Arrays.asList(args));
    }

    @Override
    protected boolean remoteDebugDefault() {
        return true;
    }

    protected int getInitialMemory() {
        return 192;
    }

    protected int getMaxMemory() {
        return 1024;
    }

    @Override
    protected List<String> getVMArguments() {
        final boolean remoteShell = OSFamily.getInstance() != OSFamily.Windows;
        return Arrays.asList("-D" + RemoteShell.START_DEFAULT_SERVER + "=" + remoteShell, "-D" + PropsConfiguration.REDIRECT_TO_FILE + "=true", "-splash:Configuration/gestion.png",
                "-Dfile.encoding=UTF-8", "-Xms" + getInitialMemory() + "M", "-Xmx" + getMaxMemory() + "M", "-Djava.library.path=./lib");
    }
}
