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
 
 package org.openconcerto.erp.modules;

import java.util.Set;

// not public since there must be coherence in between methods (getGraph() null <=>
// getReferencesToInstall() empty), and between methods and
// ModuleManager (e.g. getReferencesToInstall() depends on installed modules,
// getReferencesToRemove() must be ordered, canCurrentUserInstall())
interface ModulesStateChange {

    // null meaning this change is possible
    public String getError();

    // the state when this change was created
    public InstallationState getInstallState();

    // the references the user explicitly asked to install, not null
    public Set<ModuleReference> getUserReferencesToInstall();

    // can only be null if getReferencesToInstall() is empty
    public DepSolverGraph getGraph();

    // the references of getGraph() not already installed (order is not used, getGraph().flatten()
    // is)
    public Set<ModuleReference> getReferencesToInstall();

    // the references to remove in uninstallation order
    public Set<ModuleReference> getReferencesToRemove();

    public boolean forceRemove();
}
