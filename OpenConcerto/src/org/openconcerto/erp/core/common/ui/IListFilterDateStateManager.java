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
 
 package org.openconcerto.erp.core.common.ui;

import org.openconcerto.ui.state.ListenerXMLStateManager;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class IListFilterDateStateManager extends ListenerXMLStateManager<IListFilterDatePanel, PropertyChangeListener> {

    public IListFilterDateStateManager(IListFilterDatePanel p, File f) {
        this(p, f, true);
    }

    public IListFilterDateStateManager(IListFilterDatePanel p, File f, boolean autosave) {
        super(p, f, autosave);
    }

    @Override
    protected void addListener(PropertyChangeListener l) {
        this.getSrc().addValueListener(l);
    }

    @Override
    protected PropertyChangeListener createListener() {
        return new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                save();
            }
        };
    }

    @Override
    protected void rmListener(PropertyChangeListener l) {
        this.getSrc().rmValueListener(l);
    }

    protected final void save() {
        try {
            saveState();
        } catch (IOException exn) {
            ExceptionHandler.handle(this.getSrc(), "Impossible de sauvegarder l'état du filtre de période.", exn);
        }
    }

    DateFormat format = DateFormat.getDateInstance(DateFormat.FULL);

    @Override
    protected void writeState(BufferedWriter out) throws IOException {
        out.write("<filter>\n");
        // Taille
        out.write("<value");
        final Date fromValue = this.getSrc().getFromValue();
        out.write(" from=\"" + (fromValue == null ? "" : format.format(fromValue)) + "\"");
        final Date toValue = this.getSrc().getToValue();
        out.write(" to=\"" + (toValue == null ? "" : format.format(toValue)) + "\"");
        out.write("/>\n");

        out.write("</filter>\n");
    }

    @Override
    protected boolean readState(Document doc) {
        Tuple2<Date, Date> period;
        try {
            period = this.getPeriod(doc);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }

        this.getSrc().setPeriode(period);
        return true;
    }

    private Tuple2<Date, Date> getPeriod(Document doc) throws ParseException {
        Node filter = doc.getElementsByTagName("value").item(0);
        // from
        final String sDate1 = (filter.getAttributes().getNamedItem("from").getNodeValue());
        Date d1 = (sDate1 == null || sDate1.trim().length() == 0) ? null : format.parse(sDate1);

        // To
        final String sDate2 = (filter.getAttributes().getNamedItem("to").getNodeValue());
        Date d2 = (sDate2 == null || sDate2.trim().length() == 0) ? null : format.parse(sDate2);

        return Tuple2.create(d1, d2);
    }
}
