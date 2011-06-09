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

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ISQLElementWithCodeSelector;
import org.openconcerto.ui.JMultiLineToolTip;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

/***************************************************************************************************
 * 
 * Loupe qui clignote quand le client a des informations complémentaires plus tooltip
 * 
 */
public class ISQLElementInfosSelector extends ISQLElementWithCodeSelector {

    private static String tooltipTextDefault = "Attention, infos complémentaires : ";
    private static String tooltipText = "";

    public ISQLElementInfosSelector(SQLElement e, SQLField optField) {
        super(e, optField);

        this.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent arg0) {

                int id = ISQLElementInfosSelector.this.getValue();
                if (id > 1) {
                    SQLElement elt = ISQLElementInfosSelector.this.getSQLElement();
                    SQLRow row = elt.getTable().getRow(id);
                    String infos = row.getString("INFOS");
                    if (infos.trim().length() > 0) {
                        tooltipText = tooltipTextDefault + infos;
                        runBlink();
                        return;
                    }
                }
                stopBlink();
            }
        });
    }

    private Thread blink;

    private void stopBlink() {
        if (blink != null && blink.isAlive()) {
            blink.interrupt();
        }
        this.getViewButton().setToolTipText(null);
        ISQLElementInfosSelector.this.setViewButtonDefaultIcon();
    }

    private void runBlink() {

        if (blink == null || !blink.isAlive()) {
            blink = new Thread() {

                @Override
                public void run() {

                    while (true) {
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            // e.printStackTrace();
                            break;
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (ISQLElementInfosSelector.this.getViewButton().getIcon() == null) {
                                    ISQLElementInfosSelector.this.setViewButtonDefaultIcon();
                                } else {
                                    ISQLElementInfosSelector.this.getViewButton().setIcon(null);
                                }
                            }
                        });
                    }
                }
            };
            blink.start();
        }

        // display tooltip
        getViewButton().setToolTipText(tooltipText);
        if (this.isShowing()) {
            JMultiLineToolTip toolTipMultiLine = new JMultiLineToolTip();
            toolTipMultiLine.setTipText(tooltipText);
            final Popup tooltip1 = PopupFactory.getSharedInstance().getPopup(this.getViewButton(), toolTipMultiLine, this.getViewButton().getLocationOnScreen().x,
                    this.getViewButton().getLocationOnScreen().y + 7);
            tooltip1.show();

            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    tooltip1.hide();
                    getViewButton().setToolTipText(tooltipText);
                }
            }.start();
        }

    }
}
