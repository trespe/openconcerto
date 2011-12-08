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
 
 package org.openconcerto.openoffice.generation;

/**
 * Un générateur se servant d'un ReportGeneration.
 * 
 * @author ILM Informatique 16 nov. 2004
 * @param <R> type of generation
 */
public abstract class DocumentGenerator<R extends ReportGeneration> implements OOGenerator {

    private R rg;
    private StatusListener l;

    /**
     * Crée un nouveau générateur pour cette génération.
     * 
     * @param rg la génération.
     */
    public DocumentGenerator(R rg) {
        super();
        this.rg = rg;
    }

    public final R getRg() {
        return this.rg;
    }

    /**
     * Ce générateur doit-il être utilisé.
     * 
     * @return <code>true</code> si ce générateur doit être utilisé.
     * @throws Exception si problème.
     */
    public boolean willGenerate() throws Exception {
        return true;
    }

    public final void setStatusListener(StatusListener l) {
        this.l = l;
    }

    protected final void fireStatusChange(int percent) {
        if (this.l != null)
            this.l.statusChanged(percent);
    }
}
