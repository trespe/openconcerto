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
 
 package org.openconcerto.erp.core.humanresources.payroll.ui;

import org.openconcerto.erp.model.EditionFichePayeModel;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.preferences.DefaultProps;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class EditionFichePayePanel extends JPanel implements MouseListener {

    private final DefaultProps props = DefaultNXProps.getInstance();
    private final JProgressBar bar = new JProgressBar();
    private final JLabel labelEtatEdition = new JLabel();
    private final EditionFichePayeModel model = new EditionFichePayeModel(bar, labelEtatEdition);
    private final JTable table;
    private EditFrame frameModifySal = null;

    public EditionFichePayePanel() {
        super(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 1, 2);

        // Période de travail
        JPanel panelPeriode = new JPanel();
        panelPeriode.setBorder(BorderFactory.createTitledBorder("Période de travail"));

        SQLElement eltMois = Configuration.getInstance().getDirectory().getElement("MOIS");
        JLabel labelMois = new JLabel("Mois");
        final ElementComboBox selMois = new ElementComboBox(false, 25);
        selMois.init(eltMois);
        selMois.setButtonsVisible(false);

        // on remet les anciennes valeurs
        int valMois = props.getIntProperty("MoisEditionPaye");
        if (valMois > 1) {
            selMois.setValue(valMois);
        }

        JLabel labelAnnee = new JLabel("Année");
        // final JTextField textAnnee = new JTextField(4);
        Calendar cal = Calendar.getInstance();
        SpinnerModel modelSpinner = new SpinnerNumberModel(cal.get(Calendar.YEAR), // initial value
                cal.get(Calendar.YEAR) - 100, // min
                cal.get(Calendar.YEAR) + 100, // max
                1);
        final JSpinner textAnnee = new JSpinner(modelSpinner);

        String valYear = props.getStringProperty("AnneeEditionPaye");
        if (valYear != null && valYear.trim().length() > 0) {
            textAnnee.setValue(Integer.valueOf(valYear));
        }
        panelPeriode.add(labelMois);
        panelPeriode.add(selMois);
        panelPeriode.add(labelAnnee);
        panelPeriode.add(textAnnee);

        JLabel periodeDeb = new JLabel("Correspondant à la période du");
        JLabel periodeFin = new JLabel("au");
        final JDate dateDeb = new JDate();
        final JDate dateFin = new JDate();

        panelPeriode.add(periodeDeb);
        panelPeriode.add(dateDeb);
        panelPeriode.add(periodeFin);
        panelPeriode.add(dateFin);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        this.add(panelPeriode, c);

        // Liste des salariés
        c.gridy++;
        this.table = new JTable(this.model);
        this.table.addMouseListener(this);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        this.add(new JScrollPane(this.table), c);

        // Label Etat
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        c.weightx = 1;
        this.labelEtatEdition.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(this.labelEtatEdition, c);

        // Progress Bar
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy++;
        c.weighty = 0;
        this.add(this.bar, c);

        // Button
        final JButton buttonValid = new JButton("Valider");
        final JButton buttonFermer = new JButton("Fermer");

        PropertyChangeListener dateListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                buttonValid.setEnabled(dateDeb.getValue() != null && dateFin.getValue() != null && dateDeb.getValue().before(dateFin.getValue()));
            }
        };
        dateDeb.addValueListener(dateListener);
        dateFin.addValueListener(dateListener);

        c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        this.add(buttonValid, c);
        c.weightx = 0;
        c.gridx++;
        this.add(buttonFermer, c);

        buttonFermer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JFrame) SwingUtilities.getRoot(EditionFichePayePanel.this)).dispose();
            }
        });

        buttonValid.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    // Sauvegarde des valeurs
                    props.setProperty("MoisEditionPaye", String.valueOf(selMois.getSelectedId()));
                    // props.setProperty("AnneeEditionPaye", textAnnee.getText());
                    props.store();

                    java.sql.Date du = new java.sql.Date(dateDeb.getDate().getTime());
                    java.sql.Date au = new java.sql.Date(dateFin.getDate().getTime());

                    // model.validationFiche(textAnnee.getText(), selMois.getSelectedId(), du, au);
                    model.validationFiche(textAnnee.getValue().toString(), selMois.getSelectedId(), du, au);
                } catch (Exception ex) {
                    ExceptionHandler.handle("Impossible de valider la paye", ex);
                }
            }
        });

        selMois.addValueListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (selMois.getSelectedId() > 1 && textAnnee.getValue() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.MONTH, selMois.getSelectedId() - 2);
                    cal.set(Calendar.YEAR, Integer.valueOf(textAnnee.getValue().toString()).intValue());
                    dateDeb.setValue(cal.getTime());
                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                    dateFin.setValue(cal.getTime());
                }
            }
        });

        textAnnee.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.MONTH, selMois.getSelectedId() - 2);
                cal.set(Calendar.YEAR, Integer.valueOf(textAnnee.getValue().toString()).intValue());
                dateDeb.setValue(cal.getTime());
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                dateFin.setValue(cal.getTime());
            }
        });

    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(final MouseEvent e) {

        int row = table.rowAtPoint(e.getPoint());
        final int idSal = model.getIdSalAtRow(row);

        if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menuDroit = new JPopupMenu();
            menuDroit.add(new AbstractAction("Modifier") {
                public void actionPerformed(ActionEvent e) {
                    if (frameModifySal == null) {
                        SQLElement elt = Configuration.getInstance().getDirectory().getElement("SALARIE");
                        frameModifySal = new EditFrame(elt, EditPanel.MODIFICATION);
                    }

                    frameModifySal.selectionId(idSal, 1);
                    frameModifySal.pack();
                    frameModifySal.setVisible(true);
                }
            });
            menuDroit.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }
}
