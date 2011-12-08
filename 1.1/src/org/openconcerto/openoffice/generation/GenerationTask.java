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
 * Créé le 12 nov. 2004
 */
package org.openconcerto.openoffice.generation;

import org.openconcerto.openoffice.Log;
import org.openconcerto.openoffice.ODSingleXMLDocument;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

/**
 * Une tache de génération.
 * 
 * @author Sylvain CUAZ
 */
public final class GenerationTask {

    private final String name;
    private final DocumentGenerator generator;
    private final PropertyChangeSupport changeSupport;
    private TaskStatus status;

    /**
     * Crée une nouvelle tache.
     * 
     * @param name le nom de cette tâche.
     * @param generator le generateur de document.
     */
    public GenerationTask(String name, DocumentGenerator generator) {
        this.name = name;
        this.generator = generator;
        this.status = TaskStatus.NOT_STARTED;
        this.changeSupport = new PropertyChangeSupport(this);
    }

    public ODSingleXMLDocument generate() throws IOException, InterruptedException {
        final boolean willGenerate;
        try {
            willGenerate = this.generator.willGenerate();
        } catch (Exception exn) {
            throw new IOException("Erreur willGenerate", exn);
        }
        if (willGenerate) {
            this.setStatus(TaskStatus.STARTED);
            this.generator.setStatusListener(new StatusListener() {
                public void statusChanged(int percent) {
                    setCompletion(percent / 100.0f);
                }
            });
            Log.get().fine(this + " begun");
            ODSingleXMLDocument res = this.generator.generate();
            Log.get().fine(this + " done");
            this.generator.setStatusListener(null);
            this.setStatus(TaskStatus.DONE);
            return res;
        } else
            return null;
    }

    private void setStatus(final TaskStatus status) {
        if (status != this.status) {
            final TaskStatus old = this.status;
            this.status = status;
            this.changeSupport.firePropertyChange("status", old, this.status);
        }
    }

    private void setCompletion(final float completion) {
        this.setStatus(TaskStatus.create(completion));
    }

    public final TaskStatus getStatus() {
        return this.status;
    }

    public String toString() {
        return this.name + " " + this.getStatus();
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        this.changeSupport.removePropertyChangeListener(listener);
    }

    protected final DocumentGenerator getGenerator() {
        return this.generator;
    }

    public final String getName() {
        return this.name;
    }
}
