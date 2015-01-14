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
 * Créé le 23 avr. 2012
 */
package org.openconcerto.erp.core.finance.accounting.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jopenchart.Chart;

public class VentilationAnalytiquePanel extends JPanel {

    private final JDate dateDeb, dateEnd;

    public VentilationAnalytiquePanel() {
        super(new GridBagLayout());

        SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
        SQLRow rowExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON").getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));

        this.dateDeb = new JDate();
        this.dateEnd = new JDate();

        JLabel labelPoste = new JLabel("Poste Analytique");
        final ElementComboBox box = new ElementComboBox(false);
        SQLElement element = Configuration.getInstance().getDirectory().getElement("POSTE_ANALYTIQUE");
        ComboSQLRequest comboRequest = element.getComboRequest(true);
        box.init(element, comboRequest);

        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(labelPoste, c);
        c.gridx++;
        this.add(box, c);

        c.gridx++;
        this.add(new JLabel("Période du", SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateDeb, c);
        // Chargement des valeurs par défaut
        // String valueDateDeb = DefaultNXProps.getInstance().getStringProperty("JournauxDateDeb");
        // if (valueDateDeb.trim().length() > 0) {
        // Long l = new Long(valueDateDeb);
        // this.dateDeb.setValue(new Date(l.longValue()));
        // } else {
        this.dateDeb.setValue((Date) rowExercice.getObject("DATE_DEB"));
        // }

        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel("Au"), c);
        c.gridx++;
        c.weightx = 1;
        this.add(this.dateEnd, c);

        final JButton buttonValid = new JButton(new AbstractAction("Valider") {

            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread() {
                    public void run() {
                        SQLRow poste = box.getSelectedRow();
                        VentilationAnalytiqueSheetXML sheet = new VentilationAnalytiqueSheetXML(dateDeb.getDate(), dateEnd.getDate(), poste);
                        try {
                            sheet.createDocument();
                            sheet.showPrintAndExport(true, false, false);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    };
                }.start();

            }
        });
        c.gridx++;
        this.add(buttonValid, c);

        // Check validity
        buttonValid.setEnabled(false);
        final PropertyChangeListener listener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(box.getSelectedRow() != null && !box.getSelectedRow().isUndefined() && dateDeb.getValue() != null && dateEnd.getValue() != null);

            }
        };
        box.addValueListener(listener);
        dateEnd.addValueListener(listener);
        dateDeb.addValueListener(listener);
    }

}
