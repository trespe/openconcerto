package org.openconcerto.modules.badge;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

public class AssignationPanel extends JPanel {

    public AssignationPanel(final String cardNumber) {

        super(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(new JLabel("Assigner la carte numéro " + cardNumber + " à l'adhérent "), c);
        final ElementComboBox boxAdh = new ElementComboBox();
        final SQLElement elementAdh = Configuration.getInstance().getDirectory().getElement("ADHERENT");
        boxAdh.init(elementAdh);
        c.gridx++;
        this.add(boxAdh, c);

        final JPanel p = new JPanel();
        final JButton buttonValid = new JButton("Valider");
        p.add(buttonValid);
        final JButton buttonAnnuler = new JButton("Annuler");
        p.add(buttonAnnuler);
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.NONE;
        this.add(p, c);

        // Listeners
        buttonValid.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    SQLRow rowAdh = boxAdh.getSelectedRow();
                    if (rowAdh != null && !rowAdh.isUndefined()) {
                        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
                        sel.addSelect(elementAdh.getTable().getKey());
                        sel.addSelect(elementAdh.getTable().getField("NOM"));
                        sel.addSelect(elementAdh.getTable().getField("PRENOM"));
                        sel.setWhere(new Where(elementAdh.getTable().getField("NUMERO_CARTE"), "=", cardNumber));
                        List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
                        if (l != null && !l.isEmpty()) {
                            SQLRow rowAdhAlready = l.get(0);
                            JOptionPane.showMessageDialog(AssignationPanel.this, "Impossible d'assigner cette carte à " + rowAdh.getString("NOM") + " " + rowAdh.getString("PRENOM")
                                    + ". Elle est déjà assignée à l'adhérent " + rowAdhAlready.getString("NOM") + " " + rowAdhAlready.getString("PRENOM"));
                        } else {
                            SQLRowValues rowVals = rowAdh.asRowValues();
                            rowVals.put("NUMERO_CARTE", cardNumber);
                            rowVals.update();
                        }
                        ((JFrame) SwingUtilities.getRoot(AssignationPanel.this)).dispose();

                    }
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur", e);
                }
            }
        });

        buttonAnnuler.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                ((JFrame) SwingUtilities.getRoot(AssignationPanel.this)).dispose();
            }
        });
    }
}
