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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.image.ImageIconWarning;
import org.openconcerto.erp.core.customerrelationship.customer.element.CourrierClientSQLElement;
import org.openconcerto.erp.core.customerrelationship.customer.element.RelanceSQLElement;
import org.openconcerto.erp.core.humanresources.payroll.element.SalarieSQLElement;
import org.openconcerto.erp.core.sales.credit.element.AvoirClientSQLElement;
import org.openconcerto.erp.core.sales.invoice.element.SaisieVenteFactureSQLElement;
import org.openconcerto.erp.core.sales.order.element.CommandeClientSQLElement;
import org.openconcerto.erp.core.sales.quote.element.DevisSQLElement;
import org.openconcerto.erp.core.sales.shipment.element.BonDeLivraisonSQLElement;
import org.openconcerto.erp.core.supplychain.order.element.CommandeSQLElement;
import org.openconcerto.erp.core.supplychain.receipt.element.BonReceptionSQLElement;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.model.Where;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

// FIXME bug JTextField for input bigInt

public class NumerotationAutoSQLElement extends ComptaSQLConfElement {

    private static final String FORMAT = "_FORMAT";
    private static final String START = "_START";

    public NumerotationAutoSQLElement() {
        super("NUMEROTATION_AUTO", "une numérotation automatique", "numérotations automatiques");
    }

    protected List<String> getListFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("DEVIS_FORMAT");
        list.add("DEVIS_START");
        return list;
    }

    protected List<String> getComboFields() {
        final List<String> list = new ArrayList<String>(2);
        list.add("DEVIS_FORMAT");
        list.add("DEVIS_START");
        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {

            private JTextField textDevisFormat = new JTextField(16);
            private JTextField textDevisStart = new JTextField(6);
            private JTextField textFactStart = new JTextField(6);
            private JTextField textFactFormat = new JTextField(16);
            private JTextField textBonFormat = new JTextField(16);
            private JTextField textBonStart = new JTextField(6);
            private JTextField textBonRFormat = new JTextField(16);
            private JTextField textBonRStart = new JTextField(6);
            private JTextField textSalarieFormat = new JTextField(16);
            private JTextField textSalarieStart = new JTextField(6);
            private JTextField textPropositionFormat = new JTextField(16);
            private JTextField textPropositionStart = new JTextField(6);
            private JTextField textRelanceFormat = new JTextField(16);
            private JTextField textRelanceStart = new JTextField(6);
            private JTextField textCmdCliFormat = new JTextField(16);
            private JTextField textCmdCliStart = new JTextField(6);
            private JTextField textCmdFormat = new JTextField(16);
            private JTextField textCmdStart = new JTextField(6);
            private JTextField textAffaireFormat = new JTextField(16);
            private JTextField textAffaireStart = new JTextField(6);
            private JTextField textAvoirFormat = new JTextField(16);
            private JTextField textAvoirStart = new JTextField(6);
            private JTextField textCourrierFormat = new JTextField(16);
            private JTextField textCourrierStart = new JTextField(6);

            private Icon iconWarning = ImageIconWarning.getInstance();

            private JLabel labelNumDevis, labelNumFact, labelNumBon, labelNumSalarie;
            private JLabel labelNumRelance, labelNumProposition, labelNumCmdCli;
            private JLabel labelNumCmd, labelNumBonR, labelNumAffaire, labelNumAvoir, labelNumCourrier;
            // private JLabel labelNextCodeLettrage;
            private DocumentListener listenText = new DocumentListener() {

                public void insertUpdate(DocumentEvent e) {
                    updateLabels();
                }

                public void removeUpdate(DocumentEvent e) {
                    updateLabels();
                }

                public void changedUpdate(DocumentEvent e) {
                    updateLabels();
                }
            };

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                // Avoir
                JLabel labelAvoirFormat = new JLabel("Avoir " + getLabelFor("AVOIR_FORMAT"));
                this.add(labelAvoirFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textAvoirFormat, c);

                JLabel labelAvoirStart = new JLabel(getLabelFor("AVOIR_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelAvoirStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textAvoirStart, c);

                this.labelNumAvoir = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumAvoir, c);

                // Devis
                JLabel labelDevisFormat = new JLabel("Devis " + getLabelFor("DEVIS_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelDevisFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textDevisFormat, c);

                JLabel labelDevisStart = new JLabel(getLabelFor("DEVIS_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelDevisStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textDevisStart, c);

                this.labelNumDevis = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumDevis, c);

                // Commande Client
                JLabel labelCmdCliFormat = new JLabel("Commande client " + getLabelFor("COMMANDE_CLIENT_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelCmdCliFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCmdCliFormat, c);

                JLabel labelCmdCliStart = new JLabel(getLabelFor("COMMANDE_CLIENT_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelCmdCliStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCmdCliStart, c);

                this.labelNumCmdCli = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumCmdCli, c);

                // Commande
                JLabel labelCmdFormat = new JLabel("Commande " + getLabelFor("COMMANDE_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelCmdFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCmdFormat, c);

                JLabel labelCmdStart = new JLabel(getLabelFor("COMMANDE_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelCmdStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCmdStart, c);

                this.labelNumCmd = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumCmd, c);

                // Bon
                JLabel labelBonFormat = new JLabel("Bon de livraison" + getLabelFor("BON_L_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelBonFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textBonFormat, c);

                JLabel labelBonStart = new JLabel(getLabelFor("BON_L_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelBonStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textBonStart, c);

                this.labelNumBon = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumBon, c);

                // Bon Reception
                JLabel labelBonRFormat = new JLabel("Bon de réception" + getLabelFor("BON_R_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelBonRFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textBonRFormat, c);

                JLabel labelBonRStart = new JLabel(getLabelFor("BON_R_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelBonRStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textBonRStart, c);

                this.labelNumBonR = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumBonR, c);

                // Facture
                JLabel labelFactFormat = new JLabel("Facture " + getLabelFor("FACT_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelFactFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textFactFormat, c);

                JLabel labelFactStart = new JLabel(getLabelFor("FACT_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelFactStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textFactStart, c);

                this.labelNumFact = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumFact, c);

                // Salarie
                JLabel labelSalarieFormat = new JLabel("Salarié " + getLabelFor("SALARIE_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelSalarieFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textSalarieFormat, c);

                JLabel labelSalarieStart = new JLabel(getLabelFor("SALARIE_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelSalarieStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textSalarieStart, c);

                this.labelNumSalarie = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumSalarie, c);

                // Affaire
                JLabel labelAffaireFormat = new JLabel("Affaire " + getLabelFor("AFFAIRE_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelAffaireFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textAffaireFormat, c);

                JLabel labelAffaireStart = new JLabel(getLabelFor("AFFAIRE_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelAffaireStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textAffaireStart, c);

                this.labelNumAffaire = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumAffaire, c);

                // Proposition
                JLabel labelPropositionFormat = new JLabel("Proposition " + getLabelFor("PROPOSITION_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelPropositionFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textPropositionFormat, c);

                JLabel labelPropositionStart = new JLabel(getLabelFor("PROPOSITION_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelPropositionStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textPropositionStart, c);

                this.labelNumProposition = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumProposition, c);

                // Proposition
                JLabel labelCourrierFormat = new JLabel("Courrier " + getLabelFor("COURRIER_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelCourrierFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCourrierFormat, c);

                JLabel labelCourrierStart = new JLabel(getLabelFor("COURRIER_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelCourrierStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textCourrierStart, c);

                this.labelNumCourrier = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumCourrier, c);

                // Relance
                JLabel labelRelanceFormat = new JLabel("Relance " + getLabelFor("RELANCE_FORMAT"));
                c.gridy++;
                c.gridx = 0;
                this.add(labelRelanceFormat, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textRelanceFormat, c);

                JLabel labelRelanceStart = new JLabel(getLabelFor("RELANCE_START"));
                c.gridx++;
                c.weightx = 0;
                this.add(labelRelanceStart, c);
                c.gridx++;
                c.weightx = 1;
                this.add(this.textRelanceStart, c);

                this.labelNumRelance = new JLabel();
                c.gridx++;
                c.weightx = 0;
                this.add(this.labelNumRelance, c);

                // JLabel labelCodeLettrage = new JLabel(getLabelFor("CODE_LETTRAGE"));
                // c.gridy++;
                // c.gridx = 0;
                // c.weightx = 0;
                // this.add(labelCodeLettrage, c);
                // c.gridx++;
                // c.weightx = 1;
                // this.add(this.textCodeLettrage, c);
                //
                // c.gridx++;
                // c.weightx = 0;
                // labelNextCodeLettrage = new JLabel();
                // this.add(labelNextCodeLettrage, c);

                JLabel labelExemple = new JLabel("Exemple de format : 'Fact'yyyy0000");
                c.gridy++;
                c.gridx = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weighty = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                this.add(labelExemple, c);

                this.textBonFormat.getDocument().addDocumentListener(this.listenText);
                this.textBonStart.getDocument().addDocumentListener(this.listenText);
                this.textDevisFormat.getDocument().addDocumentListener(this.listenText);
                this.textDevisStart.getDocument().addDocumentListener(this.listenText);
                this.textFactFormat.getDocument().addDocumentListener(this.listenText);
                this.textFactStart.getDocument().addDocumentListener(this.listenText);
                this.textSalarieFormat.getDocument().addDocumentListener(this.listenText);
                this.textSalarieStart.getDocument().addDocumentListener(this.listenText);
                this.textPropositionFormat.getDocument().addDocumentListener(this.listenText);
                this.textPropositionStart.getDocument().addDocumentListener(this.listenText);
                this.textRelanceFormat.getDocument().addDocumentListener(this.listenText);
                this.textRelanceStart.getDocument().addDocumentListener(this.listenText);
                this.textCmdCliFormat.getDocument().addDocumentListener(this.listenText);
                this.textCmdCliStart.getDocument().addDocumentListener(this.listenText);
                this.textCmdFormat.getDocument().addDocumentListener(this.listenText);
                this.textCmdStart.getDocument().addDocumentListener(this.listenText);
                this.textBonRFormat.getDocument().addDocumentListener(this.listenText);
                this.textBonRStart.getDocument().addDocumentListener(this.listenText);
                this.textAffaireFormat.getDocument().addDocumentListener(this.listenText);
                this.textAffaireStart.getDocument().addDocumentListener(this.listenText);
                this.textAvoirFormat.getDocument().addDocumentListener(this.listenText);
                this.textAvoirStart.getDocument().addDocumentListener(this.listenText);
                this.textCourrierFormat.getDocument().addDocumentListener(this.listenText);
                this.textCourrierStart.getDocument().addDocumentListener(this.listenText);
                // this.textCodeLettrage.getDocument().addDocumentListener(this.listenText);

                this.addRequiredSQLObject(this.textBonFormat, "BON_L_FORMAT");
                this.addRequiredSQLObject(this.textBonStart, "BON_L_START");
                this.addRequiredSQLObject(this.textDevisFormat, "DEVIS_FORMAT");
                this.addRequiredSQLObject(this.textDevisStart, "DEVIS_START");
                this.addRequiredSQLObject(this.textFactFormat, "FACT_FORMAT");
                this.addRequiredSQLObject(this.textFactStart, "FACT_START");
                this.addRequiredSQLObject(this.textSalarieFormat, "SALARIE_FORMAT");
                this.addRequiredSQLObject(this.textSalarieStart, "SALARIE_START");

                this.addRequiredSQLObject(this.textPropositionFormat, "PROPOSITION_FORMAT");
                this.addRequiredSQLObject(this.textPropositionStart, "PROPOSITION_START");
                this.addRequiredSQLObject(this.textRelanceFormat, "RELANCE_FORMAT");
                this.addRequiredSQLObject(this.textRelanceStart, "RELANCE_START");

                this.addRequiredSQLObject(this.textCmdCliFormat, "COMMANDE_CLIENT_FORMAT");
                this.addRequiredSQLObject(this.textCmdCliStart, "COMMANDE_CLIENT_START");

                this.addRequiredSQLObject(this.textCmdFormat, "COMMANDE_FORMAT");
                this.addRequiredSQLObject(this.textCmdStart, "COMMANDE_START");

                this.addRequiredSQLObject(this.textBonRFormat, "BON_R_FORMAT");
                this.addRequiredSQLObject(this.textBonRStart, "BON_R_START");

                this.addRequiredSQLObject(this.textAffaireFormat, "AFFAIRE_FORMAT");
                this.addRequiredSQLObject(this.textAffaireStart, "AFFAIRE_START");

                this.addRequiredSQLObject(this.textAvoirFormat, "AVOIR_FORMAT");
                this.addRequiredSQLObject(this.textAvoirStart, "AVOIR_START");

                this.addRequiredSQLObject(this.textCourrierFormat, "COURRIER_FORMAT");
                this.addRequiredSQLObject(this.textCourrierStart, "COURRIER_START");

                updateLabels();
            }

            // private void updateLabelNextCode() {
            // String s = getNextCodeLetrrage(this.textCodeLettrage.getText());
            // this.labelNextCodeLettrage.setText(donne + " " + s);
            // }

            private void updateLabels() {
                updateLabel(this.textDevisStart, this.textDevisFormat, this.labelNumDevis);
                updateLabel(this.textBonStart, this.textBonFormat, this.labelNumBon);
                updateLabel(this.textBonRStart, this.textBonRFormat, this.labelNumBonR);
                updateLabel(this.textFactStart, this.textFactFormat, this.labelNumFact);
                updateLabel(this.textSalarieStart, this.textSalarieFormat, this.labelNumSalarie);
                updateLabel(this.textPropositionStart, this.textPropositionFormat, this.labelNumProposition);
                updateLabel(this.textRelanceStart, this.textRelanceFormat, this.labelNumRelance);
                updateLabel(this.textCmdCliStart, this.textCmdCliFormat, this.labelNumCmdCli);
                updateLabel(this.textCmdStart, this.textCmdFormat, this.labelNumCmd);
                updateLabel(this.textAffaireStart, this.textAffaireFormat, this.labelNumAffaire);
                updateLabel(this.textAvoirStart, this.textAvoirFormat, this.labelNumAvoir);
                updateLabel(this.textCourrierStart, this.textCourrierFormat, this.labelNumCourrier);

            }

            private void updateLabel(JTextField textStart, JTextField textFormat, JLabel label) {
                if (textStart.getText().trim().length() > 0) {
                    String numProposition = getNextNumero(textFormat.getText(), Integer.parseInt(textStart.getText()));

                    if (numProposition != null) {
                        label.setText(" --> " + numProposition);
                        label.setIcon(null);
                    } else {
                        label.setIcon(this.iconWarning);
                        label.setText("");
                    }
                } else {
                    label.setIcon(this.iconWarning);
                    label.setText("");
                }
            }

        };
    }

    protected static final SQLTable TABLE_NUM = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("NUMEROTATION_AUTO");

    // Format du type 'Fact'yyyy-MM-dd0000
    public static final String getNextNumero(Class<? extends SQLElement> clazz) {
        SQLRow rowNum = TABLE_NUM.getRow(2);
        String s = map.get(clazz);
        String format = rowNum.getString(s + FORMAT);
        int start = rowNum.getInt(s + START);
        return getNextNumero(format, start);
    }

    protected static final String getNextNumero(String format, int start) {
        if (start < 0) {
            return null;
        }
        int c = format.indexOf('0');
        if (format.trim().length() > 0) {
            if (c >= 0) {
                String prefix = format.substring(0, c);
                String suffix = format.substring(c, format.length());
                String d = prefix;

                try {
                    DateFormat dateFormat = new SimpleDateFormat(prefix);
                    d = dateFormat.format(new Date());
                } catch (IllegalArgumentException e) {
                    System.err.println("pattern incorrect");
                }

                DecimalFormat numberFormat = new DecimalFormat(suffix);
                String n = numberFormat.format(start);

                return d + n;
            } else {

                String d = format;
                try {
                    DateFormat dateFormat = new SimpleDateFormat(format);
                    d = dateFormat.format(new Date());
                } catch (IllegalArgumentException e) {
                    System.err.println("pattern incorrect");
                }
                return d + String.valueOf(start);
            }
        } else {
            return String.valueOf(start);
        }
    }

    public static final String getNextCodeLettrage() {
        SQLRow rowNum = TABLE_NUM.getRow(2);
        final String string = rowNum.getString("CODE_LETTRAGE");
        String s = (string == null) ? "" : string.trim().toUpperCase();
        return getNextCodeLetrrage(s);
    }

    public static final String getNextCodeLetrrage(String code) {
        code = code.trim();
        if (code == null || code.length() == 0) {
            return "AAA";
        } else {
            char[] charArray = code.toCharArray();
            char c = 'A';
            int i = charArray.length - 1;
            while (i >= 0 && (c = charArray[i]) == 'Z') {
                i--;
            }
            if (i >= 0) {
                c++;
                charArray[i] = c;
                for (int j = i + 1; j < charArray.length; j++) {
                    charArray[j] = 'A';
                }
                code = String.valueOf(charArray);
            } else {
                // On ajoute une lettre
                final StringBuffer buf = new StringBuffer(code.length() + 1);
                final int nb = code.length() + 1;
                for (int j = 0; j < nb; j++) {
                    buf.append('A');
                }
                code = buf.toString();
            }

            return code;
        }

    }

    private static boolean isNumeroExist(SQLElement elt, int num) {
        if (num < 0) {
            return true;
        }
        SQLSelect sel = new SQLSelect(elt.getTable().getBase());
        sel.addSelect(elt.getTable().getKey());

        sel.setWhere(new Where(elt.getTable().getField("NUMERO"), "LIKE", getPattern(elt, num)));
        System.err.println("NumerotationAutoSQLElement.isNumeroExist() " + sel.asString());
        List<SQLRow> liste = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(elt.getTable(), true));
        return liste.size() > 0;
    }

    private static String getPattern(SQLElement elt, int num) {
        SQLRow rowNum = TABLE_NUM.getRow(2);
        String s = map.get(elt.getClass());
        String format = rowNum.getString(s + FORMAT);

        format = format.replaceAll("y|d|M", "_");
        format = format.replaceAll("'", "");
        int c = format.indexOf('0');

        String numero = "";
        if (format.trim().length() > 0) {
            if (c >= 0) {
                String prefix = format.substring(0, c);
                String suffix = format.substring(c, format.length());
                String d = prefix;

                DecimalFormat numberFormat = new DecimalFormat(suffix);
                String n = numberFormat.format(num);

                numero = d + n;
            } else {

                String d = format;

                numero = d + String.valueOf(num);
            }
        } else {
            numero = String.valueOf(num);
        }
        return numero;
    }

    /**
     * Vérifie et corrige la numérotation
     * 
     * @param elt
     */
    public static void fixNumerotation(SQLElement elt) {

        SQLRow rowNum = TABLE_NUM.getRow(2);
        String s = map.get(elt.getClass());
        int start = rowNum.getInt(s + START);

        // si le numero precedent n'existe pas
        if (!isNumeroExist(elt, start - 1)) {

            int i = 2;

            while (!isNumeroExist(elt, start - i)) {
                i++;
            }

            if (start - i >= 0) {
                SQLRowValues rowVals = rowNum.createEmptyUpdateRow();
                rowVals.put(s + START, start - i + 1);
                try {
                    rowVals.update();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Map<Class<? extends SQLElement>, String> map = new HashMap<Class<? extends SQLElement>, String>();

    static {
        map.put(AvoirClientSQLElement.class, "AVOIR");
        map.put(SaisieVenteFactureSQLElement.class, "FACT");
        map.put(AvoirClientSQLElement.class, "AVOIR");
        map.put(DevisSQLElement.class, "DEVIS");
        map.put(BonDeLivraisonSQLElement.class, "BON_L");
        map.put(BonReceptionSQLElement.class, "BON_R");
        map.put(CommandeClientSQLElement.class, "COMMANDE_CLIENT");
        map.put(CommandeSQLElement.class, "COMMANDE");
        map.put(CourrierClientSQLElement.class, "COURRIER");
        map.put(RelanceSQLElement.class, "RELANCE");
        map.put(SalarieSQLElement.class, "SALARIE");

    }

    public static void addListeners() {
        for (Class<? extends SQLElement> clazz : map.keySet()) {
            final SQLElement elt = Configuration.getInstance().getDirectory().getElement(clazz);
            if (elt != null) {
                elt.getTable().addTableModifiedListener(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        if (evt.getMode() == Mode.ROW_UPDATED) {
                            SQLRow row = evt.getRow();
                            if (row.isArchived()) {
                                fixNumerotation(elt);
                            }
                        }

                    }
                });
            } else {
                System.err.println(clazz);
                Thread.dumpStack();
            }

        }

    }

}
