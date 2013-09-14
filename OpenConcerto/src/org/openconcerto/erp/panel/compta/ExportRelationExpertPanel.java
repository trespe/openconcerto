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
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ComboSQLRequest;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.ExceptionHandler;
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
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
    private ElementComboBox boxJournal = new ElementComboBox(true);
    private JCheckBox boxExport = new JCheckBox("Seulement les nouvelles ecritures");

    public ExportRelationExpertPanel() {
        super(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;

        this.add(new JLabelBold("Export des écritures comptables"), c);

        // Fichier de destination
        JPanel panelFichier = new JPanel(new GridBagLayout());
        GridBagConstraints cFic = new DefaultGridBagConstraints();
        panelFichier.add(new JLabel("Dossier de destination"), cFic);
        cFic.gridx++;
        cFic.weightx = 1;
        panelFichier.add(textDestination, cFic);

        final JButton buttonChoose = new JButton("...");
        cFic.gridx++;
        cFic.weightx = 0;
        cFic.fill = GridBagConstraints.NONE;
        panelFichier.add(buttonChoose, cFic);

        buttonGen.setEnabled(false);
        this.textDestination.setEditable(false);

        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
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

        c.gridy++;
        c.weightx = 1;
        this.add(panelFichier, c);

        c.gridx = 0;
        c.gridwidth = 1;
        c.gridy++;
        c.anchor = GridBagConstraints.EAST;
        this.add(new JLabel("Période du"), c);

        c.gridx++;
        c.anchor = GridBagConstraints.WEST;
        this.du = new JDate(true);
        this.add(this.du, c);

        c.gridx++;
        this.add(new JLabel("au"), c);

        c.gridx++;
        this.au = new JDate(true);
        this.add(this.au, c);

        boxExport.setSelected(true);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(boxExport, c);

        SQLElement elt = Configuration.getInstance().getDirectory().getElement(JournalSQLElement.class);
        ComboSQLRequest comboRequest = elt.getComboRequest(true);
        comboRequest.setUndefLabel("Tous");
        boxJournal.init(elt, comboRequest);
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(new JLabel("Ecritures du journal"), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        boxJournal.setValue(JournalSQLElement.VENTES);
        this.add(boxJournal, c);

        c.gridy++;
        c.weightx = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        final JPanel panelButton = new JPanel();
        panelButton.add(this.buttonGen);
        panelButton.add(this.buttonClose);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.weightx = 0;
        c.weighty = 1;
        this.add(panelButton, c);
        this.buttonGen.addActionListener(this);
        this.buttonClose.addActionListener(this);
    }

    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == this.buttonGen) {

            try {

                SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                SQLSelect sel = new SQLSelect();

                SQLTable tableEcriture = base.getTable("ECRITURE");
                SQLTable tableMouvement = base.getTable("MOUVEMENT");
                SQLTable tableCompte = base.getTable("COMPTE_PCE");
                SQLTable tableJrnl = base.getTable("JOURNAL");
                SQLTable tablePiece = base.getTable("PIECE");

                sel.addSelect(tableEcriture.getField("NOM"));
                sel.addJoin("LEFT", tableEcriture.getField("ID_COMPTE_PCE"));
                sel.addJoin("LEFT", tableEcriture.getField("ID_MOUVEMENT"));
                sel.addJoin("LEFT", tableEcriture.getField("ID_JOURNAL"));
                sel.addJoin("LEFT", tableMouvement.getField("ID_PIECE"));
                sel.addSelect(tableMouvement.getField("NUMERO"));
                sel.addSelect(tableCompte.getField("NUMERO"));
                sel.addSelect(tableEcriture.getField("DATE"));
                sel.addSelect(tableEcriture.getField("DEBIT"));
                sel.addSelect(tableEcriture.getField("CREDIT"));
                sel.addSelect(tableJrnl.getField("CODE"));
                if (tableEcriture.contains("CODE_CLIENT")) {
                    final SQLField fieldCodeClient = tableEcriture.getField("CODE_CLIENT");
                    sel.addSelect(fieldCodeClient);
                }
                sel.addSelect(tablePiece.getField("NOM"));

                sel.addFieldOrder(tableEcriture.getField("ID_MOUVEMENT"));
                sel.addFieldOrder(tableCompte.getField("NUMERO"));
                Where w = new Where(tableEcriture.getField("DATE"), this.du.getDate(), this.au.getDate());

                if (boxJournal.getSelectedId() != SQLRow.NONEXISTANT_ID) {
                    Where wJ = new Where(tableEcriture.getField("ID_JOURNAL"), "=", boxJournal.getSelectedId());
                    w = w.and(wJ);
                }

                if (boxExport.isSelected()) {
                    Where wJ = new Where(tableEcriture.getField("DATE_EXPORT"), "IS", (Object) null);
                    w = w.and(wJ);
                }

                sel.setWhere(w);

                @SuppressWarnings("unchecked")
                final List<Object[]> l = (List<Object[]>) base.getDataSource().execute(sel.asString(), new ArrayListHandler());
                if (l != null && l.size() > 0) {
                    // Write file
                    final boolean containsCodeClient = tableEcriture.contains("CODE_CLIENT");
                    File f = writeFileToDisk(l, containsCodeClient);
                    if (f != null) {
                        // Store export date
                        final DBRoot root = Configuration.getInstance().getRoot();
                        final SQLTable tableEcr = root.findTable("ECRITURE");
                        UpdateBuilder update = new UpdateBuilder(tableEcr);
                        final SQLField field = tableEcr.getField("DATE_EXPORT");
                        update.set("DATE_EXPORT", field.getType().toString(new Date()));
                        if (!boxExport.isSelected()) {
                            Where wJ = new Where(tableEcriture.getField("DATE_EXPORT"), "IS", (Object) null);
                            w = w.and(wJ);
                        }
                        update.setWhere(w);
                        final String req2 = update.asString();
                        root.getDBSystemRoot().getDataSource().execute(req2);
                        // Notify
                        JOptionPane.showMessageDialog(this, "L'export des " + l.size() + " écritures est terminé.\nLe fichier créé est " + f.getAbsolutePath());

                        // Close frame
                        ((JFrame) SwingUtilities.getRoot(this)).dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Impossible de créer le fichier d'export");
                    }

                } else {
                    JOptionPane.showMessageDialog(this, "Aucune écriture trouvée. La période est-elle correcte?");
                }
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Création du fichier impossible : " + ex.getMessage());
            } catch (IOException ex) {
                ExceptionHandler.handle("Erreur d'export", ex);
            }
        }

    }

    private File writeFileToDisk(List<Object[]> l, boolean containsCodeClient) throws FileNotFoundException, IOException {
        final DateFormat format = new SimpleDateFormat("ddMMyyyy_HHMMSS");

        final File selectedFile = fileChooser.getSelectedFile();
        if (selectedFile == null) {
            throw new IllegalStateException("Dossier sélectionné incorrect");
        }
        if (!selectedFile.isDirectory()) {
            throw new IllegalStateException("Vous n'avez pas sélectionné un dossier");
        }
        if (!selectedFile.canWrite()) {
            throw new IllegalStateException("Vous n'avez pas les droits pour écrire dans le dossier " + selectedFile.getAbsolutePath());
        }
        final File fOut = new File(selectedFile, "ExportOpenConcerto_" + format.format(new Date()) + ".txt");
        final BufferedOutputStream bufOut = new BufferedOutputStream(new FileOutputStream(fOut.getAbsolutePath()));

        final int size = l.size();
        for (int i = 0; i < size; i++) {

            // Ligne à insérer dans le fichier
            StringBuffer line = new StringBuffer();

            Object[] tmp = l.get(i);

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

            int z = 7;

            if (containsCodeClient) {
                // Code Client
                String codeClient = "";
                if (tmp[z] != null) {
                    codeClient = tmp[z].toString().trim();
                }
                line.append('\t');
                line.append(codeClient);
                z++;
            }

            // Piece
            line.append('\t');
            line.append(tmp[z].toString().trim());

            line.append('\r');
            line.append('\n');
            bufOut.write(line.toString().getBytes());
        }

        bufOut.close();
        return fOut;
    }
}
