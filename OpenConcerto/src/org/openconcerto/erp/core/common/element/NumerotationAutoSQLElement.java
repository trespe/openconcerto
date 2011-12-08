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
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

            private Icon iconWarning = ImageIconWarning.getInstance();

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                Set<Class<? extends SQLElement>> s = map.keySet();

                final ArrayList<Class<? extends SQLElement>> list = new ArrayList<Class<? extends SQLElement>>(s);
                Collections.sort(list, new Comparator<Class<? extends SQLElement>>() {
                    public int compare(Class<? extends SQLElement> o1, Class<? extends SQLElement> o2) {
                        return o1.toString().compareTo(o2.toString());
                    };
                });

                for (Class<? extends SQLElement> class1 : list) {
                    c.gridy++;
                    c.gridx = 0;
                    c.weightx = 0;
                    String prefix = map.get(class1);
                    SQLElement elt = Configuration.getInstance().getDirectory().getElement(class1);
                    // Avoir
                    JLabel labelAvoirFormat = new JLabel(StringUtils.firstUp(elt.getPluralName()) + " " + getLabelFor(prefix + FORMAT));
                    this.add(labelAvoirFormat, c);
                    c.gridx++;
                    c.weightx = 1;
                    final JTextField fieldFormat = new JTextField();
                    this.add(fieldFormat, c);

                    final JLabel labelAvoirStart = new JLabel(getLabelFor(prefix + START));
                    c.gridx++;
                    c.weightx = 0;
                    this.add(labelAvoirStart, c);
                    c.gridx++;
                    c.weightx = 1;
                    final JTextField fieldStart = new JTextField();
                    this.add(fieldStart, c);

                    final JLabel labelResult = new JLabel();
                    c.gridx++;
                    c.weightx = 0;
                    this.add(labelResult, c);

                    // Affichage dynamique du résultat
                    SimpleDocumentListener listener = new SimpleDocumentListener() {

                        @Override
                        public void update(DocumentEvent e) {
                            updateLabel(fieldStart, fieldFormat, labelResult);

                        }
                    };

                    fieldFormat.getDocument().addDocumentListener(listener);
                    fieldStart.getDocument().addDocumentListener(listener);

                    this.addRequiredSQLObject(fieldFormat, prefix + FORMAT);
                    this.addRequiredSQLObject(fieldStart, prefix + START);

                }

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

                // this.textCodeLettrage.getDocument().addDocumentListener(this.listenText);

            }

            // private void updateLabelNextCode() {
            // String s = getNextCodeLetrrage(this.textCodeLettrage.getText());
            // this.labelNextCodeLettrage.setText(donne + " " + s);
            // }

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

    // Format du type 'Fact'yyyy-MM-dd0000
    public static final String getNextNumero(Class<? extends SQLElement> clazz) {
        SQLRow rowNum = TABLE_NUM.getRow(2);
        String s = map.get(clazz);
        String format = rowNum.getString(s + FORMAT);
        int start = rowNum.getInt(s + START);
        return getNextNumero(format, start);
    }

    private static final Tuple2<String, String> getPrefixAndSuffix(String format, Date d) {

        String prefix = "";
        String suffix = "";
        int c = format.indexOf('0');
        if (format.trim().length() > 0) {
            if (c >= 0) {
                prefix = format.substring(0, c);
                suffix = format.substring(c, format.length());

                try {
                    DateFormat dateFormat = new SimpleDateFormat(prefix);
                    prefix = dateFormat.format(d);
                } catch (IllegalArgumentException e) {
                    System.err.println("pattern incorrect");
                }

            } else {

                try {
                    DateFormat dateFormat = new SimpleDateFormat(format);
                    prefix = dateFormat.format(d);
                } catch (IllegalArgumentException e) {
                    System.err.println("pattern incorrect");
                }

            }
        }

        return Tuple2.create(prefix, suffix);
    }

    protected static final String getNextNumero(String format, Integer start) {
        if (start != null && start < 0) {
            return null;
        }

        Tuple2<String, String> t = getPrefixAndSuffix(format, new Date());
        DecimalFormat decimalFormat = new DecimalFormat(t.get1());
        return t.get0() + decimalFormat.format(start);
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

    protected static final SQLTable TABLE_NUM = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("NUMEROTATION_AUTO");

    public static void main(String[] args) {

        List<String> l = Arrays.asList("2011/05/001", "2011/05/002", "2011/05/003", "2011/05/004");
        DecimalFormat format = new DecimalFormat("'2011/05/'000");
        for (String string : l) {
            Number n;
            try {
                n = format.parse(string);
                System.err.println(n);
            } catch (ParseException exn) {
                // TODO Bloc catch auto-généré
                exn.printStackTrace();
            }
        }

    }

    public static String getNextForMonth(Class<? extends SQLElement> clazz, SQLTable table, Date d) {

        SQLRow rowNum = TABLE_NUM.getRow(2);
        String s = map.get(clazz);
        String pattern = rowNum.getString(s + FORMAT);
        Tuple2<String, String> t = getPrefixAndSuffix(pattern, d);
        SQLSelect sel = new SQLSelect(table.getBase());
        sel.addSelect(table.getField("NUMERO"));
        sel.addSelect(table.getKey());
        sel.setWhere(new Where(table.getField("NUMERO"), "LIKE", "%" + t.get0() + "%"));
        List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        DecimalFormat format = new DecimalFormat("'" + t.get0() + "'" + t.get1());
        int value = 0;
        for (SQLRow sqlRow : l) {
            Number n;
            try {
                n = format.parse(sqlRow.getString("NUMERO"));
                value = Math.max(value, n.intValue());
            } catch (ParseException exn) {
                // TODO Bloc catch auto-généré
                exn.printStackTrace();
            }
        }
        final String format2 = format.format(value + 1);
        System.err.println(format2);
        return format2;
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

    /**
     * Permet d'ajouter la gestion de la numérotation automatique. Attention à bien créer les champs
     * name_FORMAT et name_START dans la table NUMEROTATION_AUTO
     * 
     * @param elt
     * @param name
     */
    public static void addClass(Class<? extends SQLElement> elt, String name) {
        map.put(elt, name);
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
