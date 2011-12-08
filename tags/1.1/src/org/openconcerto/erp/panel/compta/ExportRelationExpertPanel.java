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
 
 package org.openconcerto.erp.panel.compta;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.GestionDevise;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class ExportRelationExpertPanel extends JPanel implements ActionListener {

    private JDate du, au;
    private JButton buttonGen = new JButton("Exporter");
    private JButton buttonClose = new JButton("Fermer");
    private JFileChooser fileChooser = new JFileChooser();
    private JTextField textDestination = new JTextField();

    public ExportRelationExpertPanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        this.add(new JLabelBold("Export des écritures comptables"), c);

        // Fichier de destination
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0;
        this.add(new JLabel("Fichier de destination"), c);

        c.gridx++;
        c.weightx = 1;
        this.add(textDestination, c);

        final JButton buttonChoose = new JButton("...");
        c.gridx++;
        c.weightx = 0;
        this.add(buttonChoose, c);
        buttonGen.setEnabled(false);
        this.textDestination.setEditable(false);
        buttonChoose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int answer = fileChooser.showSaveDialog(ExportRelationExpertPanel.this);
                if (answer == JFileChooser.APPROVE_OPTION) {
                    textDestination.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }

                buttonGen.setEnabled(answer == JFileChooser.APPROVE_OPTION);
            }
        });
        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        this.add(new JLabel("du"), c);

        c.gridx++;
        this.du = new JDate(true);
        this.add(this.du, c);

        c.gridx++;
        this.add(new JLabel("au"), c);

        c.gridx++;
        this.au = new JDate(true);
        this.add(this.au, c);

        c.gridy++;
        c.gridx = 0;

        JPanel panelButton = new JPanel();
        panelButton.add(this.buttonGen);
        panelButton.add(this.buttonClose);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;
        this.add(panelButton, c);
        this.buttonGen.addActionListener(this);
        this.buttonClose.addActionListener(this);
    }

    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.buttonGen) {
            File fOut = fileChooser.getSelectedFile();
            try {
                BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(fOut.getAbsolutePath() + ".xls"));

                SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                SQLSelect sel = new SQLSelect(base);

                SQLTable tableEcriture = base.getTable("ECRITURE");
                SQLTable tableMouvement = base.getTable("MOUVEMENT");
                SQLTable tableCompte = base.getTable("COMPTE_PCE");
                SQLTable tableJrnl = base.getTable("JOURNAL");

                sel.addSelect(tableEcriture.getField("NOM"));
                sel.addJoin("LEFT", "ECRITURE.ID_COMPTE_PCE");
                sel.addJoin("LEFT", "ECRITURE.ID_MOUVEMENT");
                sel.addJoin("LEFT", "ECRITURE.ID_JOURNAL");
                sel.addSelect(tableMouvement.getField("NUMERO"));
                sel.addSelect(tableCompte.getField("NUMERO"));
                sel.addSelect(tableEcriture.getField("DATE"));
                sel.addSelect(tableEcriture.getField("DEBIT"));
                sel.addSelect(tableEcriture.getField("CREDIT"));
                sel.addSelect(tableJrnl.getField("CODE"));

                sel.setWhere(new Where(tableEcriture.getField("DATE"), this.du.getDate(), this.au.getDate()).and(new Where(tableEcriture.getField("ID_JOURNAL"), "=", JournalSQLElement.VENTES)));

                List l = (List) base.getDataSource().execute(sel.asString(), new ArrayListHandler());
                System.err.println(sel.asString());
                if (l != null) {
                    for (int i = 0; i < l.size(); i++) {

                        // Ligne à insérer dans le fichier
                        StringBuffer line = new StringBuffer();

                        Object[] tmp = (Object[]) l.get(i);

                        // Date
                        Date d = (Date) tmp[3];
                        line.append(dateFormat.format(d));
                        line.append('\t');
                        // Jrnl
                        line.append(tmp[6].toString().trim());
                        line.append('\t');
                        // N° Cpt
                        String cpt = tmp[2].toString().trim();
                        line.append(cpt);
                        line.append('\t');

                        // ?
                        line.append('\t');

                        // Libellé
                        line.append(tmp[0].toString().trim());
                        line.append('\t');

                        // Debit
                        Long debit = new Long(tmp[4].toString().trim());
                        line.append(GestionDevise.currencyToString(debit.longValue()));
                        line.append('\t');
                        // Credit
                        Long credit = new Long(tmp[5].toString().trim());
                        line.append(GestionDevise.currencyToString(credit.longValue()));
                        line.append('\t');
                        line.append('E');
                        line.append('\r');
                        line.append('\n');
                        bufOut.write(line.toString().getBytes());
                    }
                }

                bufOut.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        ((JFrame) SwingUtilities.getRoot(this)).dispose();
    }

}
