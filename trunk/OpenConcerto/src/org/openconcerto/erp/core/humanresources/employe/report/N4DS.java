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
 
 package org.openconcerto.erp.core.humanresources.employe.report;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.ProductInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class N4DS {

    private static DateFormat format = new SimpleDateFormat("ddMMyyyy");
    private ComptaPropsConfiguration conf = ((ComptaPropsConfiguration) Configuration.getInstance());
    private double masseSalarialeBrute;
    private static final byte[] retour = "'\n".getBytes();
    private PrintStream stream = null;

    DecimalFormat decimalFormat = new DecimalFormat("0.00");

    // FIXME Salarie renvoye

    /**
     * Déclaration normale (type 51)
     * */
    public N4DS() {
        DecimalFormatSymbols symbol = new DecimalFormatSymbols();
        symbol.setDecimalSeparator('.');
        this.decimalFormat.setDecimalFormatSymbols(symbol);

    }

    public File createDocument() {
        this.masseSalarialeBrute = 0;
        File f = new File("N4DS_" + format.format(new Date()) + ".txt");

        try {

            this.stream = new PrintStream(f, "ISO-8859-1");

            SQLElement eltSalarie = this.conf.getDirectory().getElement("SALARIE");

            // Infos emetteur
            SQLRow rowSociete = this.conf.getRowSociete();

            writeS10(this.stream, rowSociete);

            writeS20(this.stream, rowSociete);

            SQLSelect sel = new SQLSelect(this.conf.getBase());
            sel.addSelect(eltSalarie.getTable().getKey());

            @SuppressWarnings("unchecked")
            List<SQLRow> l = (List<SQLRow>) this.conf.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltSalarie.getTable()));

            for (SQLRow row : l) {
                N4DSSalarie s = new N4DSSalarie(this);
                s.write(row, rowSociete);

            }
            writeS80(this.stream, rowSociete);
            writeS90(this.stream);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (this.stream != null) {
                this.stream.close();
            }
        }
        return f;
    }

    private void writeS80(PrintStream stream, SQLRow rowSociete) throws IOException {

        String siren = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(0, 9);
        String nic = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(9);

        // // SIREN
        // write("S80.G01.00.001.001", siren);

        // NIC
        write("S80.G01.00.001.002", nic);

        SQLRow rowAdr = rowSociete.getForeignRow("ID_ADRESSE_COMMON");
        String voie = rowAdr.getString("RUE");

        Ville ville = Ville.getVilleFromVilleEtCode(rowAdr.getString("VILLE"));

        // Complement adresse
        if (voie.contains("\n")) {
            String[] sVoies = voie.split("\n");
            if (sVoies.length > 0) {
                voie = sVoies[0];
                String complement = "";
                for (int i = 1; i < sVoies.length; i++) {
                    complement += sVoies[i].trim() + " ";
                }
                if (complement.length() > 0) {
                    complement = complement.substring(0, complement.length() - 1);
                }

                write("S80.G01.00.003.001", complement);
            }
        }

        // Voie
        write("S80.G01.00.003.006", voie);

        // TODO Code INSEE, facultatif
        // stream.write("S80.G01.00.003.007",voie);

        // FIXME Service de distribution
        // stream.write("S80.G01.00.003.009",ville.getName());

        // Code postal
        write("S80.G01.00.003.010", ville.getCodepostal());

        // Localité
        write("S80.G01.00.003.012", ville.getName().toUpperCase());

        // Code Pays, ne doit pas être renseigné pour une adresse en France
        // TODO support des autres pays
        // write("S80.G01.00.003.013", "");

        // FIXME effectif déclaré
        write("S80.G01.00.004.001", String.valueOf(getEffectifDeclare()));

        // TODO Code établissement sans salarié
        // write( "S80.G01.00.004.001", "2");

        // TODO Code assujettis taxe sur salaires
        write("S80.G01.00.005", "01");

        // Code NAF etablissement
        write("S80.G01.00.006", rowSociete.getString("NUM_APE"));

        // FIXME Code section prud'homale
        write("S80.G01.00.007.001", "04");

        // TODO stecion principale dérogatoire
        // write( "S80.G01.00.004.001", "2");

        // TODO Institution Prevoyance sans salarie
        // write( "S80.G01.01.001", "P0012");
        // write( "S80.G01.01.002", "0003");

        // TODO Institution retraite sans salarie
        // write( "S80.G01.02.001", "G022");

        // FIXME Code assujettissement taxe et contribution apprentissage
        write("S80.G62.05.001", "01");
        double totalApprentissage = this.masseSalarialeBrute * 0.0068;
        System.err.println(this.masseSalarialeBrute);
        write("S80.G62.05.002", this.decimalFormat.format(totalApprentissage));

        write("S80.G62.05.003", "02");

        // FIXME Code assujettissement formation professionnelle continue
        write("S80.G62.10.001", "01");
        double totalFormation = this.masseSalarialeBrute * 0.0055;
        write("S80.G62.10.003.001", this.decimalFormat.format(totalFormation));

    }

    private void writeS90(PrintStream stream) throws IOException {
        // Nombre total de rubrique + S90
        write("S90.G01.00.001", String.valueOf(this.nbRubrique + 2));

        // Nombre total de rubrique S20
        write("S90.G01.00.002", String.valueOf(1));
    }

    private int nbRubrique = 0;

    public void write(String rubriqueName, String value) throws IOException {
        String tmp = rubriqueName + ",'";
        this.stream.write(tmp.getBytes());
        this.stream.write(value.getBytes());
        this.stream.write(retour);

        // if (rubriqueName.startsWith("S20")) {
        // this.nbRubriqueS20++;
        // }
        this.nbRubrique++;
    }

    private int getEffectifDeclare() {
        // FIXME ne pas inclure les intérimaires
        SQLElement eltSalarie = this.conf.getDirectory().getElement("SALARIE");
        SQLElement eltInfos = this.conf.getDirectory().getElement("INFOS_SALARIE_PAYE");
        SQLSelect sel = new SQLSelect(eltSalarie.getTable().getBase());
        sel.addSelect(eltSalarie.getTable().getKey());
        Date d2 = new Date(111, 11, 31);
        Where w = new Where(eltSalarie.getTable().getField("ID_INFOS_SALARIE_PAYE"), "=", eltInfos.getTable().getKey());
        w = w.and(new Where(eltInfos.getTable().getField("DATE_SORTIE"), "=", (Date) null).or(new Where(eltInfos.getTable().getField("DATE_SORTIE"), ">", d2)));

        sel.setWhere(w);
        System.err.println(sel.asString());
        List<SQLRow> l = (List<SQLRow>) this.conf.getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(eltSalarie.getTable()));

        return (l == null ? 0 : l.size());
    }

    public static String normalizeString2(String s) {
        s = s.toUpperCase();
        String temp = Normalizer.normalize(s, Form.NFC);
        temp = temp.replaceAll("-", " ");
        return temp.replaceAll("[^\\p{ASCII}]", "");
    }

    private void writeS20(PrintStream stream, SQLRow rowSociete) throws IOException {

        // Siren

        String siren = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(0, 9);
        String nic = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(9);
        write("S20.G01.00.001", siren);

        // Raison sociale
        write("S20.G01.00.002", rowSociete.getString("NOM"));

        // FIXME Debut periode
        write("S20.G01.00.003.001", "01012011");

        // FIXME Fin periode
        write("S20.G01.00.003.002", "31122011");

        // Code nature
        write("S20.G01.00.004.001", "01");

        // FIXME Code type (complement, Rectificatif, ...)
        write("S20.G01.00.004.002", "51");

        // fraction
        write("S20.G01.00.005", "11");

        // TODO debut periode rattachement
        // stream.write("S20.G01.00.006.001,'","11");

        // fin periode rattachement
        // stream.write("S20.G01.00.006.002,'","11");

        // Code devise de la déclaration
        write("S20.G01.00.007", "01");

        // NIC
        write("S20.G01.00.008", nic);

        SQLRow rowAdr = rowSociete.getForeignRow("ID_ADRESSE_COMMON");
        String voie = rowAdr.getString("RUE");

        Ville ville = Ville.getVilleFromVilleEtCode(rowAdr.getString("VILLE"));

        // Complement adresse
        if (voie.contains("\n")) {
            String[] sVoies = voie.split("\n");
            if (sVoies.length > 0) {
                voie = sVoies[0];
                String complement = "";
                for (int i = 1; i < sVoies.length; i++) {
                    complement += sVoies[i].trim() + " ";
                }
                if (complement.length() > 0) {
                    complement = complement.substring(0, complement.length() - 1);
                }

                write("S20.G01.00.009.001", complement);
            }
        }

        // Voie
        write("S20.G01.00.009.006", voie);

        // Code postal
        write("S20.G01.00.009.010", ville.getCodepostal());

        // Localité
        write("S20.G01.00.009.012", ville.getName().toUpperCase());

        write("S20.G01.00.013.002", "1");

        // Code periodicite
        write("S20.G01.00.018", "A00");

        write("S20.G01.05.014.001", siren);
        write("S20.G01.05.014.002", nic);
        write("S20.G01.05.016.001", rowSociete.getString("MAIL"));

    }

    /**
     * Strucuture S10, N4DS
     * */
    private void writeS10(PrintStream stream, SQLRow rowSociete) throws IOException {

        // Siren

        String siren = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(0, 9);
        String nic = rowSociete.getString("NUM_SIRET").replaceAll(" ", "").substring(9);
        write("S10.G01.00.001.001", siren);

        // NIC
        write("S10.G01.00.001.002", nic);

        // Raison sociale
        write("S10.G01.00.002", rowSociete.getString("NOM"));

        SQLRow rowAdr = rowSociete.getForeignRow("ID_ADRESSE_COMMON");
        String voie = rowAdr.getString("RUE");

        Ville ville = Ville.getVilleFromVilleEtCode(rowAdr.getString("VILLE"));

        // Complement adresse
        if (voie.contains("\n")) {
            String[] sVoies = voie.split("\n");
            if (sVoies.length > 0) {
                voie = sVoies[0];
                String complement = "";
                for (int i = 1; i < sVoies.length; i++) {
                    complement += sVoies[i] + " ";
                }
                if (complement.length() > 0) {
                    complement = complement.substring(0, complement.length() - 1);
                }

                write("S10.G01.00.003.001", complement);
            }
        }

        // Voie
        write("S10.G01.00.003.006", voie);

        // TODO Code INSEE, facultatif
        // stream.write("S10.G01.00.003.007",voie);

        // TODO: Service de distribution
        write("S10.G01.00.003.009", ville.getName());

        // Code postal
        write("S10.G01.00.003.010", ville.getCodepostal());

        // Localité
        write("S10.G01.00.003.012", ville.getName().toUpperCase());

        // Code Pays, ne doit pas être renseigné pour une adresse en France
        // TODO support des autres pays
        // write("S10.G01.00.003.013", "");

        // FIXME Référence de l'envoi
        // Incrémenté le numéro
        write("S10.G01.00.004,'", "1");

        // Nom du logiciel
        write("S10.G01.00.005", "OpenConcerto");

        // Nom de l'éditeur
        write("S10.G01.00.006", "ILM Informatique");

        // Numéro version
        write("S10.G01.00.007", ProductInfo.getInstance().getVersion());

        // Code service choisi
        write("S10.G01.00.009", "40");

        // Code envoi du fichier essai ou réel
        write("S10.G01.00.010", "02");

        // Norme utilisée
        write("S10.G01.00.011", "V01X06");

        // Code table char
        write("S10.G01.00.012", "01");

        // TODO Contact pour DADS
        // Code civilite
        write("S10.G01.01.001.001", "01");

        // Nom Contact
        SQLTable table = Configuration.getInstance().getRoot().findTable("CONTACT_ADMINISTRATIF");
        SQLSelect selContact = new SQLSelect(table.getBase());
        selContact.addSelectStar(table);
        selContact.setWhere(new Where(table.getField("N4DS"), "=", Boolean.TRUE));
        List<SQLRow> l = SQLRowListRSH.execute(selContact);
        if (l.size() == 0) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Aucun contact administratif pour la N4DS. Veuillez en définir un.");
                }
            });
        } else {
            SQLRow rowContact = l.get(0);

            // TODO Contact pour DADS
            write("S10.G01.01.001.002", rowContact.getString("NOM") + " " + rowContact.getString("PRENOM"));

            // Code domaine
            write("S10.G01.01.002", "03");

            // Adresse mail
            // TODO Contact pour DADS
            write("S10.G01.01.005", rowContact.getString("EMAIL"));

            // Tel
            write("S10.G01.01.006", rowContact.getString("TEL_DIRECT"));

            // Fax
            write("S10.G01.01.007", rowContact.getString("FAX"));
        }
    }

    private String getNumeroVoie(String voie) {
        String numero = "";
        voie = voie.trim();
        for (int i = 0; i < voie.trim().length(); i++) {
            char c = voie.charAt(i);
            if (c >= '0' && c <= '9') {
                numero += c;
            } else {
                break;
            }
        }
        return numero;
    }

    private String getVoieWithoutNumber(String voie) {
        voie = voie.trim();
        String resultVoie = new String(voie);

        for (int i = 0; i < voie.trim().length(); i++) {
            char c = voie.charAt(i);
            if (c >= '0' && c <= '9') {
                resultVoie = resultVoie.substring(1);
            } else {
                break;
            }
        }
        return resultVoie.trim();
    }

    public void addMasseSalarialeBrute(double baseBrute) {
        this.masseSalarialeBrute += baseBrute;

    }

}
