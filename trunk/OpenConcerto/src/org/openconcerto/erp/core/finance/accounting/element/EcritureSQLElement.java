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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.common.ui.DeviseField;
import org.openconcerto.erp.core.common.ui.PanelFrame;
import org.openconcerto.erp.core.finance.accounting.ui.SuppressionEcrituresPanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class EcritureSQLElement extends ComptaSQLConfElement {

    public EcritureSQLElement() {
        super("ECRITURE", "une écriture", "écritures");
    }

    // Impossible de modifier si validée
    // FIXME impossible de saisir une écriture avant la date de debut d'exercice --> de saisir de
    // document de gest comm

    public List<String> getListFields() {
        final List<String> l = new ArrayList<String>();

        l.add("ID");
        l.add("ID_MOUVEMENT");
        // l.add("ID_COMPTE_PCE");
        l.add("COMPTE_NUMERO");
        l.add("COMPTE_NOM");
        // l.add("ID_JOURNAL");
        l.add("JOURNAL_NOM");
        l.add("NOM");
        l.add("DATE");

        l.add("DEBIT");
        l.add("CREDIT");

        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("DATE");
        l.add("DEBIT");
        l.add("CREDIT");
        l.add("ID_JOURNAL");
        l.add("ID_MOUVEMENT");
        return l;
    }

    @Override
    public synchronized ListSQLRequest getListRequest() {
        return new ListSQLRequest(this.getTable(), this.getListFields()) {
            @Override
            protected void customizeToFetch(SQLRowValues graphToFetch) {
                super.customizeToFetch(graphToFetch);
                graphToFetch.put("VALIDE", null);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JTextField nom;
            private DeviseField debit;
            private DeviseField credit;
            private JDate date;
            private ElementComboBox journal;

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.nom = new JTextField();
                this.debit = new DeviseField();
                this.credit = new DeviseField();
                this.date = new JDate();
                this.journal = new ElementComboBox();
                // Mouvement
                /*
                 * JLabel labelMouvement = new JLabel("Mouvement"); this.add(labelMouvement, c);
                 * 
                 * c.gridx ++; c.weightx = 1; this.add(idMouvement, c);
                 */

                // Journal
                JLabel labelJournal = new JLabel("Journal");
                c.gridx = 0;
                this.add(labelJournal, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.journal, c);

                // Date
                JLabel labelDate = new JLabel("Date");
                c.gridx++;
                this.add(labelDate, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.date, c);

                // libellé
                JLabel labelNom = new JLabel("Libellé");
                c.gridy++;
                c.gridx = 0;
                this.add(labelNom, c);

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                this.add(this.nom, c);

                // debit
                c.gridwidth = 1;
                JLabel labelDebit = new JLabel("Debit");
                c.gridy++;
                c.gridx = 0;
                this.add(labelDebit, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.debit, c);
                this.debit.addKeyListener(new KeyAdapter() {
                    public void keyReleased(KeyEvent e) {
                        if ((credit.getText().trim().length() != 0) && (debit.getText().trim().length() != 0)) {
                            credit.setText("");
                        }
                    }
                });

                // Credit
                JLabel labelCredit = new JLabel("Credit");
                c.gridx++;
                this.add(labelCredit, c);

                c.gridx++;
                c.weightx = 1;
                this.add(this.credit, c);

                this.credit.addKeyListener(new KeyAdapter() {
                    public void keyReleased(KeyEvent e) {
                        if ((debit.getText().trim().length() != 0) && (credit.getText().trim().length() != 0)) {
                            debit.setText("");
                        }
                    }
                });

                this.addSQLObject(this.nom, "NOM");
                this.addSQLObject(this.debit, "DEBIT");
                this.addSQLObject(this.credit, "CREDIT");
                this.addRequiredSQLObject(this.date, "DATE");
                this.addRequiredSQLObject(this.journal, "ID_JOURNAL");
            }

            @Override
            public void select(SQLRowAccessor r) {
                super.select(r);

                if (r != null && r.getBoolean("VALIDE")) {
                    this.nom.setEnabled(false);
                    this.debit.setEnabled(false);
                    this.credit.setEnabled(false);
                    this.date.setEnabled(false);
                    this.date.setEditable(false);
                    this.journal.setEnabled(false);
                }

                /*
                 * System.out.println("Impossible de modifier une ecriture validée");
                 * SaisieKmSQLElement elt = new SaisieKmSQLElement(); EditFrame edit = new
                 * EditFrame(elt); edit.selectionId(row.getInt("ID_MOUVEMENT"), 0); edit.pack();
                 * edit.setVisible(true);
                 */
            }

            public void update() {
                SQLRow row = EcritureSQLElement.this.getTable().getRow(getSelectedID());
                if (row.getBoolean("VALIDE")) {
                    System.out.println("Impossible de modifier une ecriture validée");
                } else {
                    super.update();
                }
            }

        };
    }

    /**
     * Validation d'un mouvement, implique la validation de l'ensemble de la piece
     * 
     * @param idMvt
     */
    private static void validerMouvement(int idMvt) {

        if (idMvt == 1) {
            return;
        }

        SQLTable tableMvt = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("MOUVEMENT");
        SQLRow rowMvt = tableMvt.getRow(idMvt);

        // On parcourt en profondeur
        if (rowMvt.getInt("ID_MOUVEMENT_PERE") > 1) {
            validerMouvement(rowMvt.getInt("ID_MOUVEMENT_PERE"));
        } else {
            validerMouvementProfondeur(idMvt);
        }
    }

    /**
     * Valider l'ensemble des mouvements formés par le sous arbre de du mouvement d'id idMvtPere
     * 
     * @param idMvtPere
     */
    private static void validerMouvementProfondeur(int idMvtPere) {
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableMvt = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("MOUVEMENT");

        SQLSelect selectFils = new SQLSelect(base);
        selectFils.addSelect(tableMvt.getField("ID"));
        selectFils.setWhere("MOUVEMENT.ID_MOUVEMENT_PERE", "=", idMvtPere);

        List l = (List) base.getDataSource().execute(selectFils.asString(), new ArrayListHandler());

        // valide mouvements fils
        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            validerMouvementProfondeur(Integer.parseInt(tmp[0].toString()));
        }

        // valide mouvement
        validationEcritures(idMvtPere);
    }

    /**
     * Valide l'ensemble des ecritures du mouvement
     * 
     * @param idMvt Id du mouvement à valider
     */
    public static final void validationEcritures(int idMvt) {
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableEcriture = base.getTable("ECRITURE");

        SQLSelect selEcriture = new SQLSelect(base);
        selEcriture.addSelect(tableEcriture.getField("ID"));

        Where w = new Where(tableEcriture.getField("ID_MOUVEMENT"), "=", idMvt);

        selEcriture.setWhere(w);

        String reqEcriture = selEcriture.asString();

        Object obEcriture = base.getDataSource().execute(reqEcriture, new ArrayListHandler());

        List myListEcriture = (List) obEcriture;

        if (myListEcriture.size() != 0) {

            for (int i = 0; i < myListEcriture.size(); i++) {
                Object[] objTmp = (Object[]) myListEcriture.get(i);
                valideEcriture(Integer.parseInt(objTmp[0].toString()));
            }
        }
    }

    /**
     * Validation des ecritures avant la date d
     * 
     * @param d date
     * @param cloture
     */
    public static final void validationEcrituresBefore(Date d, boolean cloture) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable tableEcriture = base.getTable("ECRITURE");

        // on recupere l'ensemble des mouvements à valider
        SQLSelect selEcriture = new SQLSelect(base);
        selEcriture.addSelect(tableEcriture.getField("ID_MOUVEMENT"));
        selEcriture.setDistinct(true);
        Where w1 = new Where(tableEcriture.getField("DATE"), "<=", d);
        Where w2 = new Where(tableEcriture.getField("VALIDE"), "=", Boolean.FALSE);
        selEcriture.setWhere(w1.and(w2));
        List l = (List) base.getDataSource().execute(selEcriture.asString(), new ArrayListHandler());

        // validation de tous les mouvements
        for (int i = 0; i < l.size(); i++) {
            Object[] tmp = (Object[]) l.get(i);
            System.err.println("Validation du mouvement " + tmp[0]);
            validationEcritures(Integer.parseInt(tmp[0].toString()));
        }

        if (cloture) {

            SQLTable tableExercice = Configuration.getInstance().getBase().getTable("EXERCICE_COMMON");

            SQLRow rowSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRowSociete();
            SQLRow rowExercice = tableExercice.getRow(rowSociete.getInt("ID_EXERCICE_COMMON"));
            Date dateCloture = (Date) rowExercice.getObject("DATE_CLOTURE");

            if (dateCloture == null || dateCloture.before(d)) {

                SQLRowValues rowVals = new SQLRowValues(tableExercice);
                rowVals.put("DATE_CLOTURE", new java.sql.Date(d.getTime()));
                try {
                    rowVals.update(rowExercice.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final void valideEcriture(int id) {
        SQLTable tableEcriture = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("ECRITURE");
        SQLRowValues rowVals = new SQLRowValues(tableEcriture);
        rowVals.put("VALIDE", Boolean.TRUE);
        rowVals.put("DATE_VALIDE", new java.sql.Date(new Date().getTime()));
        rowVals.put("IDUSER_VALIDE", UserManager.getInstance().getCurrentUser().getId());

        try {
            rowVals.update(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void archiveMouvement(int idMvt) {
        SQLTable tableMouvement = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("MOUVEMENT");
        SQLRow rowMvt = tableMouvement.getRow(idMvt);

        if (rowMvt.getInt("ID_MOUVEMENT_PERE") > 1) {
            archiveMouvement(rowMvt.getInt("ID_MOUVEMENT_PERE"));
        } else {
            archiveMouvementProfondeur(idMvt, true);
        }
    }

    /**
     * Archivage de l'ensemble des opérations liés au mouvement passé en parametre
     * 
     * @param idMvtPere mouvement racine
     * @param dropPere suppression du mouvement pere
     */
    public void archiveMouvementProfondeur(int idMvtPere, boolean dropPere) {
        if (idMvtPere > 1) {
            SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
            SQLTable tableMvt = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("MOUVEMENT");

            SQLSelect selectFils = new SQLSelect(base);
            selectFils.addSelect(tableMvt.getField("ID"));
            selectFils.setWhere("MOUVEMENT.ID_MOUVEMENT_PERE", "=", idMvtPere);

            List l = (List) base.getDataSource().execute(selectFils.asString(), new ArrayListHandler());

            // archive mouvements fils
            for (int i = 0; i < l.size(); i++) {
                Object[] tmp = (Object[]) l.get(i);
                archiveMouvementProfondeur(Integer.parseInt(tmp[0].toString()), true);
            }

            // archive mouvement
            archiveEcritures(idMvtPere, dropPere);

        } else {

            System.err.println("Suppression du mouvement d'id 1 impossible.");
        }
    }

    // Suppression des ecritures associées à un mouvement
    private synchronized void archiveEcritures(final int idMvt, final boolean dropMvt) {
        final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        final SQLTable tableMvt = base.getTable("MOUVEMENT");
        final SQLTable tableEcriture = base.getTable("ECRITURE");
        final SQLRow rowMvt = tableMvt.getRow(idMvt);

        // on verifie que le mouvement n'est pas validé
        if (MouvementSQLElement.isEditable(idMvt)) {

            // on archive le mouvement
            if (dropMvt) {
                SQLElement elt = Configuration.getInstance().getDirectory().getElement(tableMvt);
                try {
                    elt.archive(idMvt);
                } catch (SQLException e) {
                    ExceptionHandler.handle("Erreur lors de la suppression du mouvement d'id [" + idMvt + "]", e);
                    e.printStackTrace();
                }
            } else {

                SQLFactory<Object> sqlFactory = new SQLFactory<Object>() {
                    @Override
                    public Object create() throws SQLException {
                        // on recupere l'ensemble des ecritures associées au mouvement
                        SQLSelect selEcritures = new SQLSelect(base);
                        selEcritures.addSelect(tableEcriture.getField("ID"));
                        selEcritures.setWhere("ECRITURE.ID_MOUVEMENT", "=", idMvt);

                        List l = (List) base.getDataSource().execute(selEcritures.asString(), new ArrayListHandler());
                        for (int i = 0; i < l.size(); i++) {
                            Object[] tmp = (Object[]) l.get(i);
                            try {
                                archiveEcriture(tableEcriture.getRow(Integer.parseInt(tmp[0].toString())));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            } catch (SQLException e) {
                                e.printStackTrace();
                                ExceptionHandler.handle("Une erreur est survenue lors de la suppression de l'écritures [" + tmp[0] + "].", e);
                            }
                        }

                        return null;
                    }
                };

                try {
                    SQLUtils.executeAtomic(base.getDataSource(), sqlFactory);
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } else {
            ExceptionHandler.handle("Impossible de supprimer le mouvement n°" + rowMvt.getInt("NUMERO") + " car il est validé.");
        }
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {
        // Si on supprime une ecriture on doit supprimer toutes les ecritures du mouvement associé
        System.err.println("Archivage des écritures");
        // archiveMouvement(row.getInt("ID_MOUVEMENT"));
        JFrame frame = new PanelFrame(new SuppressionEcrituresPanel(row.getInt("ID_MOUVEMENT")), "Suppression d'ecritures");
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Permet d'archiver une ecriture seule attention méthode à utiliser en vérifiant que la balance
     * est respectée
     * 
     * @param row
     * @throws SQLException
     */
    public void archiveEcriture(SQLRow row) throws SQLException {

        if (!row.getBoolean("VALIDE")) {

            SQLRowValues rowVals = new SQLRowValues(this.getTable());
            rowVals.put("IDUSER_DELETE", UserManager.getInstance().getCurrentUser().getId());
            rowVals.update(row.getID());

            super.archive(row, true);
        } else {
            System.err.println("Impossible de supprimer une ecriture validée");
            JOptionPane.showMessageDialog(null, "Impossible de supprimer une ecriture validée. \n" + row);
        }
    }

    private static EditFrame frameSaisieKm = null;

    /**
     * Contrepassation d'un piece comptable à partir de l'id d'une écriture. Chargement d'un panel
     * Saisie au Km avec les ecritures inversées
     * 
     * @param id
     */
    public static void contrePassationPiece(int id) {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

        SQLTable tableEcriture = base.getTable("ECRITURE");
        SQLRow rowEcr = tableEcriture.getRow(id);
        final int idMvt = rowEcr.getInt("ID_MOUVEMENT");
        System.err.println("ID MOUVEMENT --> " + idMvt);
        if (frameSaisieKm == null) {
            frameSaisieKm = new EditFrame(Configuration.getInstance().getDirectory().getElement("SAISIE_KM"));
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SaisieKmSQLElement.loadContrePassation(frameSaisieKm.getSQLComponent(), idMvt);
            }
        });
        frameSaisieKm.setVisible(true);
    }
}
