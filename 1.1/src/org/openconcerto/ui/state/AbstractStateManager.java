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
 
 package org.openconcerto.ui.state;

import java.io.File;
import java.io.IOException;

public abstract class AbstractStateManager<T> {

    private File configFile;
    private final T src;

    public AbstractStateManager(T src, File f) {
        this(src, f, true);
    }

    public AbstractStateManager(T src, File f, boolean autosave) {
        this.src = src;
        this.configFile = f;
        if (autosave) {
            beginAutoSave();
        }
    }

    public final File getConfigFile() {
        return this.configFile;
    }

    public final void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    protected final T getSrc() {
        return this.src;
    }

    public abstract void beginAutoSave();

    public abstract void endAutoSave();

    public final void deleteState() {
        check();
        this.configFile.delete();
    }

    private void check() {
        if (this.configFile == null)
            throw new IllegalStateException("configFile undefined.");
    }

    public final void saveState() throws IOException {
        check();
        this.saveState(this.configFile);
    }

    public final void saveState(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("null File specified");
        }
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        writeState(file);
    }

    /**
     * Should actually write the file.
     * 
     * @param file the file to save.
     * @throws IOException if an error occurs.
     */
    protected abstract void writeState(File file) throws IOException;

    public final boolean loadState() {
        check();
        return loadState(this.configFile);
    }

    /**
     * Loads the state from the specified file.
     * 
     * @param file the file from which to load.
     * @return if the state was restored.
     */
    public final boolean loadState(File file) {
        if (file.exists() && file.length() > 0) {
            this.configFile = file;
            return this.readState(file);
        } else {
            return false;
        }
    }

    /**
     * Should actually read the file.
     * 
     * @param file the file from which to load.
     * @return if the state was restored.
     */
    protected abstract boolean readState(File file);

}
