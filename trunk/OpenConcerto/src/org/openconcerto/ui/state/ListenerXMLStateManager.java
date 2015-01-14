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

import org.openconcerto.ui.Log;
import org.openconcerto.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EventListener;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * A state manager who use a listener to know when to save an xml file.
 * 
 * @author Sylvain
 * 
 * @param <T> type of source.
 * @param <L> type of listener.
 */
public abstract class ListenerXMLStateManager<T, L extends EventListener> extends AbstractStateManager<T> {

    private L listener;

    public ListenerXMLStateManager(T src, File f) {
        this(src, f, true);
    }

    public ListenerXMLStateManager(T src, File f, boolean autosave) {
        super(src, f, autosave);
    }

    @Override
    public final void beginAutoSave() {
        if (this.listener == null) {
            this.listener = createListener();
            this.addListener(this.listener);
        }
    }

    @Override
    public final void endAutoSave() {
        if (this.listener != null) {
            this.rmListener(this.listener);
            this.listener = null;
        }
    }

    abstract protected L createListener();

    protected abstract void addListener(L l);

    protected abstract void rmListener(L l);

    @Override
    protected void writeState(File file) throws FileNotFoundException, IOException {
        final BufferedWriter out = FileUtils.createXMLWriter(file);
        try {
            writeState(out);
        } finally {
            out.close();
        }
    }

    protected void writeState(final BufferedWriter out) throws IOException {
        throw new UnsupportedOperationException("Override one of writeState() methods");
    }

    protected File getBadFile(File file) {
        return FileUtils.addSuffix(file, ".bad");
    }

    @Override
    protected final boolean readState(File file) {
        final DocumentBuilder docBuilder;
        try {
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        }
        final InputSource is = new InputSource();
        try {
            // don't use docBuilder.parse(File) : if an error occurs, the file isn't closed
            is.setByteStream(new BufferedInputStream(new FileInputStream(file)));
            is.setSystemId(file.toURI().toASCIIString());

            final Document doc = docBuilder.parse(is);
            // don't rely on parse() to close()
            is.getByteStream().close();

            // normalize text representation
            doc.getDocumentElement().normalize();

            return this.readState(doc);
        } catch (Exception t) {
            String msg = file + " is not valid";
            if (is.getByteStream() != null) {
                final File newFile = getBadFile(file);
                msg += ", trying to rename it to " + newFile + " : ";
                try {
                    is.getByteStream().close();
                    newFile.delete();
                    msg += file.renameTo(newFile);
                } catch (IOException e) {
                    msg += e.getMessage();
                }
            }
            Log.get().warning(msg);
            t.printStackTrace();
        }
        return false;
    }

    abstract protected boolean readState(Document doc);

}
