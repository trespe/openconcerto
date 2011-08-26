package org.openconcerto.erp.action;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.utils.GestionDevise;

public class ImportExportUtils {

    private static void appendAt(int index, String value, StringBuffer buf) {
        if (buf.length() < index) {
            int size = index - buf.length() - 1;
            for (int i = 0; i < size; i++) {
                buf.append(' ');
            }
        }
        buf.append(value);
    }

    public static void exportData(ImportExportPanel panel) {

        File file = new File(panel.getPathFile());
        BufferedOutputStream bufOut;
        try {
            bufOut = new BufferedOutputStream(new FileOutputStream(file));

            String s = panel.getFormatDate();
            DateFormat dateFormat = new SimpleDateFormat(s);

            SQLBase base = panel.getTable2Import().getBase();

            SQLSelect sel = new SQLSelect(base);

            for (int i = 0; i < panel.getModel().getRowCount(); i++) {
                SQLField f = panel.getModel().getFieldForIndex(i);
                sel.addSelect(f);
            }
            // Where w = new Where();

            List l = (List) base.getDataSource().execute(sel.asString(), new ArrayListHandler());
            System.err.println(sel.asString());
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {

                    // Ligne à insérer dans le fichier
                    StringBuffer line = new StringBuffer(0);

                    Object[] tmp = (Object[]) l.get(i);

                    // Libellé
                    appendAt(0, tmp[0].toString().trim(), line);

                    // Mouvement
                    appendAt(100, tmp[1].toString().trim(), line);

                    // N° Cpt
                    appendAt(105, tmp[2].toString().trim(), line);

                    // Date
                    Date d = (Date) tmp[3];
                    appendAt(117, dateFormat.format(d), line);

                    // Debit
                    Long debit = new Long(tmp[4].toString().trim());
                    appendAt(127, GestionDevise.currencyToStandardStringExport(debit.longValue(), 15), line);

                    // Credit
                    Long credit = new Long(tmp[5].toString().trim());
                    appendAt(142, GestionDevise.currencyToStandardStringExport(credit.longValue(), 15), line);

                    // Jrnl
                    appendAt(157, tmp[6].toString().trim(), line);

                    appendAt(162, "\n", line);

                    try {
                        bufOut.write(line.toString().getBytes());
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            try {
                bufOut.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Parse une ligne contenant un liste de champs délimité par le caractère delim et englobé par
     * le caractère englob. Exemple : line = Field1;Field2; Field 3 ; " Field ; 4 " delim = ; englob
     * = " return = [ Field1, Field2, Field 3 , Field ; 4 ]
     * 
     * @param line
     * @param delim
     * @param englob
     * @return la liste des fields contenus dans la ligne
     */
    public static Vector parseLine(String line, char delim, char englob) {

        Vector result = new Vector();

        boolean englobed = false;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < line.length(); i++) {

            if (line.charAt(i) == '\\' && (i + 1) < line.length()) {
                buffer.append(line.charAt(i + 1));
                i++;
            } else {

                // si on tombe sur un charactere englob
                if (!englobed && line.charAt(i) == englob) {
                    englobed = true;
                } else {

                    // si on tombe sur un charactere englon
                    if (englobed && line.charAt(i) == englob) {
                        englobed = false;
                    } else {

                        if (!englobed && line.charAt(i) == delim) {
                            result.add(buffer.toString());
                            buffer = new StringBuffer();
                        } else {
                            buffer.append(line.charAt(i));
                        }
                    }
                }
            }
        }
        result.add(buffer.toString());

        return result;
    }

    public static Vector parseLine(String line, int[] delim) {
        Vector result = new Vector();

        int left = 0;
        for (int i = 0; i < delim.length; i++) {
            result.add(line.substring(left, left + delim[i]));
            left += delim[i];
        }

        return result;
    }

    public static void importData(final ImportExportPanel panel, final JProgressBar bar) {
        new Thread() {
            public void run() {
                File f = new File(panel.getPathFile());
                int nbLine = 0;
                int lineNumber = 0;
                bar.setStringPainted(false);

                // Importation des éléments
                try {

                    // On calcule le nombre de ligne du fichier à importer pour la taille de la
                    // barre de progression
                    BufferedReader br = new BufferedReader(new FileReader(f));
                    while (br.readLine() != null) {
                        nbLine++;
                    }
                    bar.setMaximum(nbLine);
                    br.close();

                    // On parse le fichier
                    br = new BufferedReader(new FileReader(f));
                    String line = br.readLine();
                    Vector listRowVals = new Vector();

                    // On importe chacune des lignes du fichier
                    while (line != null) {

                        SQLRowValues rowVals = new SQLRowValues(panel.getTable2Import());

                        // incrémentation de la barre de progression
                        final int lineNumberTmp = lineNumber;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                bar.setValue(lineNumberTmp);
                            }
                        });

                        // on parse la ligne pour récuperer la liste des valeurs
                        importValues(line, rowVals, panel);

                        listRowVals.add(rowVals);

                        line = br.readLine();
                        lineNumber++;
                    }

                    br.close();

                    System.err.println("Insertion des rowValues");
                    for (Iterator i = listRowVals.iterator(); i.hasNext();) {
                        SQLRowValues r = (SQLRowValues) i.next();
                        try {
                            r.commit();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (FileNotFoundException fnotFnd) {
                    // TODO PopUp pour prevenir l'utilisateur, Faire un Check permanent à partir du
                    // JTEXTFIELD???
                    System.err.println("Le chemin du fichier est incorrect!");
                    fnotFnd.printStackTrace();
                }

                catch (IOException e) {

                    System.err.println("Erreur lors de l'importation du fichier " + f.getName() + " à la ligne " + lineNumber);
                    e.printStackTrace();
                }

                bar.setValue(nbLine);
                bar.setStringPainted(true);
                bar.setString("Importation terminée.");

                System.err.println("Fin de l'importation.");
            }
        }.start();
    }

    /**
     * Chargement des valeurs dans la rowValues
     * 
     * @param line
     * @param vals
     * @param panel
     */
    private static void importValues(String line, SQLRowValues vals, ImportExportPanel panel) {

        // on parse la ligne
        Vector v;
        if (panel.isLongueurFixeSelected()) {
            v = ImportExportUtils.parseLine(line, panel.getModel().getDelimiteur());
        } else {
            v = ImportExportUtils.parseLine(line, panel.getSeparator().charAt(0), '"');
        }

        // on récupere les données
        System.err.println("Value to import -->  " + v);
        for (int i = 0; i < v.size(); i++) {
            String value = v.get(i).toString().trim();
            Class c = panel.getModel().getClassForIndex(i);
            String fieldName = panel.getModel().getFieldNameForIndex(i);

            // si le champ à une valeur definie dans le fichier
            if (value != null && value.length() > 0) {

                // si c'est un champ propre à la table que l'on veut importer
                if (panel.getModel().getTableNameForIndex(i).equalsIgnoreCase(vals.getTable().getName())) {
                    if (c == Float.class) {
                        System.err.println("Put Float " + value);
                        value = value.replace(',', '.');
                        vals.put(fieldName, new Float(value));
                    } else {

                        if (c == BigInteger.class || c == Long.class) {
                            System.err.println("Put Long " + value);
                            // value = value.replace(',', '.');
                            vals.put(fieldName, new Long(GestionDevise.parseLongCurrency(value)));
                        } else {
                            vals.put(fieldName, value);
                        }
                    }

                }
                // si c'est un champ étranger
                else {

                    SQLField f = panel.getModel().getFieldForIndex(i);
                    Map m = addFillMapForeignField(f);

                    //
                    Object valueObject;
                    if (c == Float.class) {
                        valueObject = new Float(value.replace(',', '.'));
                    } else {
                        if (c == BigInteger.class) {
                            valueObject = new Long(GestionDevise.parseLongCurrency(value));
                        } else {
                            if (c == Integer.class) {
                                valueObject = new Integer(value);
                            } else {
                                valueObject = new String(value);
                            }
                        }
                    }

                    if (m.get(valueObject) == null) {

                        SQLRowValues nouvRowForeign = new SQLRowValues(f.getTable());
                        nouvRowForeign.put(f.getName(), valueObject);

                        try {
                            SQLRow rowForeign = nouvRowForeign.insert();
                            m.put(valueObject, rowForeign.getObject(f.getTable().getKey().getName()));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    Set s = vals.getTable().getForeignKeys(f.getTable().getName());
                    for (Iterator iter = s.iterator(); iter.hasNext();) {
                        SQLField field = (SQLField) iter.next();
                        vals.put(field.getName(), m.get(valueObject));
                    }
                    // }
                }
            }
        }
    }

    /**
     * Stocke l'ensemble des id d'une table etrangére
     * 
     * @param f
     * @return Map contenant la liste des valeurs de la table du field passé en paramètre associé à
     *         l'id de la ligne
     */
    private static Map addFillMapForeignField(SQLField f) {
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();

        Map m = new HashMap();
        // On recupere toutes les taxes définies
        SQLSelect selAllTaxe = new SQLSelect(base);
        selAllTaxe.addSelect(f.getTable().getKey());
        selAllTaxe.addSelect(f);
        String reqAllTaxe = selAllTaxe.asString();

        List l = (List) base.getDataSource().execute(reqAllTaxe, new ArrayListHandler());
        for (Iterator i = l.iterator(); i.hasNext();) {
            Object[] tmp = (Object[]) i.next();
            m.put(tmp[1], tmp[0]);
        }
        return m;
    }

    public static void main(String[] args) {
        // String s = " Code;libellé;\" N°\\\";\\\"de \\\"compte\";Debit;Credit";

        File f = new File("C:\\Documents and Settings\\Administrateur\\Mes documents\\ExportDelim.txt");
        try {
            BufferedReader buf = new BufferedReader(new FileReader(f));

            String s;
            while ((s = buf.readLine()) != null) {
                System.err.println(parseLine(s, ',', '\''));
            }

        } catch (FileNotFoundException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }

    }
}
