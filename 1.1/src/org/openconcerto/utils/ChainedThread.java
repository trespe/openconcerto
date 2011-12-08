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
 
 package org.openconcerto.utils;

/**
 * Une thread qui appelle work(), puis attend une autre thread avant d'exécuter contribute().
 * Attention, cette classe peut lancer des RuntimeException lors de son exécution, il est donc
 * fortement recommandé d'implémenter uncaughtException().
 * 
 * @see java.lang.ThreadGroup#uncaughtException(java.lang.Thread, java.lang.Throwable)
 * @author ILM Informatique 25 juin 2004
 */
public abstract class ChainedThread extends Thread {

    private final Thread previous;

    /**
     * Crée une nouvelle instance sans nom.
     * 
     * @param group le groupe, utile pour implémenter uncaughtException().
     * @param previous le thread à attendre.
     */
    public ChainedThread(ThreadGroup group, Thread previous) {
        this(group, "Unnamed Thread " + System.currentTimeMillis(), previous);
    }

    /**
     * Crée une nouvelle instance.
     * 
     * @param group le groupe, utile pour implémenter uncaughtException().
     * @param name le nom de cette thread.
     * @param previous le thread à attendre.
     */
    public ChainedThread(ThreadGroup group, String name, Thread previous) {
        super(group, name);
        this.previous = previous;
    }

    private void append() {
        try {
            if (this.previous != null)
                this.previous.join();
            this.contribute();
        } catch (InterruptedException exn) {
            throw new RuntimeException("Problème lors de l'attente de la thread précédente.", exn);
        }
    }

    public void run() {
        this.work();
        this.append();
    }

    /**
     * Le travaille à fournir avant de pouvoir contribuer. Cette méthode est appelé avant que la
     * précédente thread n'ait fini.
     */
    abstract protected void work();

    /**
     * Cette méthode doit contribuer le travail fourni, si possible en un minimum de temps. Cette
     * méthode est appelé après que la précédente thread ait fini.
     */
    abstract protected void contribute();

}
