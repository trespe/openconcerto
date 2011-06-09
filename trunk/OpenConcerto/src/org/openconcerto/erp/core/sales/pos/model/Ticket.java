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
 
 package org.openconcerto.erp.core.sales.pos.model;

import org.openconcerto.erp.core.sales.pos.Caisse;
import org.openconcerto.erp.core.sales.pos.io.DefaultTicketPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.ui.TicketCellRenderer;
import org.openconcerto.utils.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Ticket {
    private static boolean inited = false;
    // Propre a ticket
    private List<Paiement> paiements = new ArrayList<Paiement>();
    private final List<Pair<Article, Integer>> items = new ArrayList<Pair<Article, Integer>>();
    private Date date;
    private int number;

    // Propre à la caisse
    private int caisseNumber;

    public static Ticket getTicketFromCode(String code) {
        // Code: 01_05042011_00002
        // filtre les chiffres
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (Character.isDigit(c)) {
                b.append(c);
            }
        }
        code = b.toString();
        // Code: 010504201100002
        // n°caisse sur 2 caracteres
        // date jour mois année JJMMAAAA
        // numero de ticket formaté sur 5 caractères

        final Ticket t = new Ticket(-1);
        Calendar c = Calendar.getInstance();
        try {
            int nCaisse = Integer.parseInt(code.substring(0, 2));
            int nJ = Integer.parseInt(code.substring(2, 4));
            int nM = Integer.parseInt(code.substring(4, 6));
            int nA = 2000 + Integer.parseInt(code.substring(6, 8));
            int nNumber = Integer.parseInt(code.substring(8, 13));

            c.setTimeInMillis(0);
            c.set(nA, nM - 1, nJ, 0, 0, 0);

            // Set fields
            t.caisseNumber = nCaisse;
            t.date.setTime(c.getTimeInMillis());
            t.number = nNumber;

            // Loading file
            File dir = t.getOutputDir();
            File file = new File(dir, code.replace(' ', '_') + ".xml");
            if (!file.exists()) {
                return null;
            }
            // XML Reading

            final SAXBuilder sxb = new SAXBuilder();
            final Document document = sxb.build(file);
            final Element root = document.getRootElement();
            final String h = root.getAttributeValue("hour");
            final String m = root.getAttributeValue("minute");
            c.set(nA, nM - 1, nJ, Integer.parseInt(h), Integer.parseInt(m), 0);
            t.date.setTime(c.getTimeInMillis());
            // article
            List<Element> children = root.getChildren("article");
            for (Element element : children) {
                int qte = Integer.parseInt(element.getAttributeValue("qte"));
                int prix_unitaire_cents_ht = Integer.parseInt(element.getAttributeValue("prixHT"));
                int idTaxe = Integer.parseInt(element.getAttributeValue("idTaxe"));
                int prix_unitaire_cents = Integer.parseInt(element.getAttributeValue("prix"));
                String categorie = element.getAttributeValue("categorie");
                String name = element.getValue();
                String codebarre = element.getAttributeValue("codebarre");
                Categorie cat = new Categorie(categorie);
                Article art = new Article(cat, name);
                art.priceInCents = prix_unitaire_cents;
                art.setPriceHTInCents(prix_unitaire_cents_ht);
                art.setIdTaxe(idTaxe);
                art.code = codebarre;
                Pair<Article, Integer> line = new Pair<Article, Integer>(art, qte);
                t.items.add(line);

            }
            // paiement
            children = root.getChildren("paiement");
            for (Element element : children) {

                String type = element.getAttributeValue("type");
                int montant_cents = Integer.parseInt(element.getAttributeValue("montant"));
                if (montant_cents > 0) {
                    int tp = Paiement.ESPECES;
                    if (type.equals("CB")) {
                        tp = Paiement.CB;
                    } else if (type.equals("CHEQUE")) {
                        tp = Paiement.CHEQUE;
                    } else if (type.equals("ESPECES")) {
                        tp = Paiement.ESPECES;
                    }
                    Paiement p = new Paiement(tp);
                    p.setMontantInCents(montant_cents);
                    t.paiements.add(p);
                }
            }

        } catch (Exception e) {
            System.err.println("Error with ticket code : " + code);
            e.printStackTrace();
            return null;
        }

        return t;

    }

    public Ticket(int caisse) {
        this.caisseNumber = caisse;
        this.date = Calendar.getInstance().getTime();
        initNumber();

    }

    public void setNumber(int i) {
        this.number = i;
    }

    public void setDate(Date d) {
        this.date = d;
    }

    private void initNumber() {
        if (!inited) {
            this.number = 1;

            String[] files = getCompatibleFileNames();
            for (int i = 0; i < files.length; i++) {
                String name = files[i];
                String nb = name.substring(8, 13);
                System.out.println("Found:" + nb);
                int n = Integer.parseInt(nb);
                if (n >= this.number) {
                    this.number = n + 1;
                }

            }

        }
    }

    public String[] getCompatibleFileNames() {
        File dir = getOutputDir();
        // Calendar cal = Calendar.getInstance();
        //
        // int j = cal.get(Calendar.DAY_OF_MONTH);
        // int m = cal.get(Calendar.MONTH) + 1;
        // int a = cal.get(Calendar.YEAR);
        // String code = "";
        // code += format(2, this.getCaisseNumber()) + "_";
        // code += format(2, j) + format(2, m) + format(4, a) + "_";
        // final String codeStart = code;
        final String codeStart = getPrefixCode();
        String[] files = dir.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {

                return name.startsWith(codeStart) && name.endsWith(".xml");
            }
        });
        return files;
    }

    String getPrefixCode() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.date);
        int j = cal.get(Calendar.DAY_OF_MONTH);
        int m = cal.get(Calendar.MONTH) + 1;
        int a = cal.get(Calendar.YEAR) - 2000;
        String code = "";
        code += format(2, this.getCaisseNumber());
        code += format(2, j) + format(2, m) + format(2, a);
        return code;
    }

    public String getCode() {
        // Calendar cal = Calendar.getInstance();
        // cal.setTime(this.date);
        // int j = cal.get(Calendar.DAY_OF_MONTH);
        // int m = cal.get(Calendar.MONTH) + 1;
        // int a = cal.get(Calendar.YEAR) - 2000;
        // String code = "";
        // code += format(2, this.getCaisseNumber());
        // code += format(2, j) + format(2, m) + format(2, a);
        // code += format(5, this.getNumber());
        String code = getPrefixCode();
        code += format(5, this.getNumber());
        return code;
    }

    /**
     * Numero du ticket fait ce jour, compteur remis à 1 chaque jour
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Numero de la caisse, de 1 à n
     */
    private int getCaisseNumber() {
        return this.caisseNumber;
    }

    public void save() {
        // Update Hour & Minute
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);

        // Hierarchie: 2010/04/05/01_05042010_00002.xml
        File dir = getOutputDir();
        File f = new File(dir, getCode().replace(' ', '_') + ".xml");
        Element topLevel = new Element("ticket");
        topLevel.setAttribute(new Attribute("code", this.getCode()));
        topLevel.setAttribute("hour", String.valueOf(hour));
        topLevel.setAttribute("minute", String.valueOf(minute));
        // Articles
        for (Pair<Article, Integer> item : this.items) {
            Element e = new Element("article");
            e.setAttribute("qte", String.valueOf(item.getSecond()));
            // Prix unitaire
            e.setAttribute("prix", String.valueOf(item.getFirst().getPriceInCents()));
            e.setAttribute("prixHT", String.valueOf(item.getFirst().getPriceHTInCents()));
            e.setAttribute("idTaxe", String.valueOf(item.getFirst().getIdTaxe()));
            e.setAttribute("categorie", item.getFirst().getCategorie().getName());
            e.setAttribute("codebarre", item.getFirst().getCode());
            e.setText(item.getFirst().getName());
            topLevel.addContent(e);
        }
        // Paiements
        for (Paiement paiement : this.paiements) {
            final int montantInCents = paiement.getMontantInCents();
            if (montantInCents > 0) {
                final Element e = new Element("paiement");
                String type = "";
                if (paiement.getType() == Paiement.CB) {
                    type = "CB";
                } else if (paiement.getType() == Paiement.CHEQUE) {
                    type = "CHEQUE";
                } else if (paiement.getType() == Paiement.ESPECES) {
                    type = "ESPECES";
                }
                e.setAttribute("type", type);
                e.setAttribute("montant", String.valueOf(montantInCents));
                topLevel.addContent(e);
            }

        }
        try {
            final XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(topLevel, new FileOutputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void print(TicketPrinter prt) {
        int maxWidth = Caisse.getTicketWidth();
        int MAX_PRICE_WIDTH = 8;
        int MAX_QTE_WIDTH = 5;

        List<TicketLine> headers = Caisse.getHeaders();
        for (TicketLine line : headers) {
            prt.addToBuffer(line);
        }

        // Date
        prt.addToBuffer("");
        SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm");
        prt.addToBuffer(DefaultTicketPrinter.formatRight(maxWidth, "Le " + df.format(getCreationDate())));
        prt.addToBuffer("");

        for (Pair<Article, Integer> item : this.items) {
            prt.addToBuffer(DefaultTicketPrinter.formatRight(MAX_QTE_WIDTH, String.valueOf(item.getSecond())) + " "
                    + DefaultTicketPrinter.formatLeft(maxWidth - 2 - MAX_PRICE_WIDTH - MAX_QTE_WIDTH, item.getFirst().getName()) + " "
                    + DefaultTicketPrinter.formatRight(MAX_PRICE_WIDTH, TicketCellRenderer.centsToString(item.getFirst().getPriceInCents())));
        }

        StringBuilder spacer = new StringBuilder();
        for (int i = 0; i <= MAX_QTE_WIDTH; i++) {
            spacer.append(' ');
        }
        for (int i = 0; i < maxWidth - MAX_QTE_WIDTH - 1; i++) {
            spacer.append('=');
        }
        prt.addToBuffer(spacer.toString());
        prt.addToBuffer(DefaultTicketPrinter.formatRight(maxWidth - 8, "Total") + DefaultTicketPrinter.formatRight(MAX_PRICE_WIDTH, TicketCellRenderer.centsToString(getTotal())),
                DefaultTicketPrinter.BOLD);
        prt.addToBuffer("");
        //
        for (Paiement paiement : this.paiements) {

            String type = "";
            if (paiement.getType() == Paiement.CB) {
                type = "Paiement CB";
            } else if (paiement.getType() == Paiement.CHEQUE) {
                type = "Paiment par chèque";
            } else if (paiement.getType() == Paiement.ESPECES) {
                type = "Paiment en espèces";
            }
            int montantInCents = paiement.getMontantInCents();
            if (montantInCents > 0) {
                type += " de " + TicketCellRenderer.centsToString(montantInCents);
                if (montantInCents > 100) {
                    type += " euros";
                } else {
                    type += " euro";
                }
                prt.addToBuffer(type);
            }
        }
        // Montant Rendu
        if (getTotal() < getPaidTotal()) {
            int montantInCents = getPaidTotal() - getTotal();
            String type = "Rendu : " + TicketCellRenderer.centsToString(montantInCents);
            if (montantInCents > 100) {
                type += " euros";
            } else {
                type += " euro";
            }
            prt.addToBuffer(type);
        }
        prt.addToBuffer("");
        // Footer
        List<TicketLine> footers = Caisse.getFooters();
        for (TicketLine line : footers) {
            prt.addToBuffer(line);
        }
        prt.addToBuffer("");
        prt.addToBuffer(getCode(), DefaultTicketPrinter.BARCODE);
        prt.addToBuffer("");
        prt.addToBuffer("Nous utilisons le logiciel OpenConcerto.");
        prt.addToBuffer("Logiciel libre, open source et gratuit!");
        try {
            prt.printBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getOutputDir() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.date);
        int j = cal.get(Calendar.DAY_OF_MONTH);
        int m = cal.get(Calendar.MONTH) + 1;
        int a = cal.get(Calendar.YEAR);

        File outputDir = new File("Tickets");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }
        File outputDirYear = new File(outputDir, format(4, a));
        if (!outputDirYear.exists()) {
            outputDirYear.mkdir();
        }
        File outputDirMonth = new File(outputDirYear, format(2, m));
        if (!outputDirMonth.exists()) {
            outputDirMonth.mkdir();
        }
        File outputDirDay = new File(outputDirMonth, format(2, j));
        if (!outputDirDay.exists()) {
            outputDirDay.mkdir();
        }
        return outputDirDay;
    }

    public Date getCreationDate() {
        return this.date;
    }

    private static String format(int l, int value) {
        return format(l, String.valueOf(value));
    }

    private static String format(int l, String string) {
        if (string.length() > l) {
            string = string.substring(0, l);
        }

        StringBuffer str = new StringBuffer(l);

        final int stop = l - string.length();
        for (int i = 0; i < stop; i++) {
            str.append('0');
        }
        str.append(string);
        return str.toString();
    }

    public void addPaiement(Paiement p1) {
        this.paiements.add(p1);

    }

    public void addArticle(Article a) {
        boolean alreadyExist = false;
        for (Pair<Article, Integer> line : this.items) {
            if (line.getFirst().equals(a)) {
                alreadyExist = true;
                break;
            }
        }
        if (!alreadyExist) {
            Pair<Article, Integer> line = new Pair<Article, Integer>(a, 1);
            this.items.add(line);
        }

    }

    public void incrementArticle(Article a) {
        boolean alreadyExist = false;
        for (Pair<Article, Integer> line : this.items) {
            if (line.getFirst().equals(a)) {
                alreadyExist = true;
                line.setSecond(line.getSecond() + 1);
                break;
            }
        }
        if (!alreadyExist) {
            Pair<Article, Integer> line = new Pair<Article, Integer>(a, 1);
            this.items.add(line);
        }

    }

    public List<Paiement> getPaiements() {

        return this.paiements;
    }

    public int getTotal() {
        int total = 0;
        for (Pair<Article, Integer> line : this.items) {

            final int count = line.getSecond();
            final int price = line.getFirst().priceInCents;
            total += (count * price);
        }
        return total;
    }

    public List<Pair<Article, Integer>> getArticles() {

        return this.items;
    }

    public void clearArticle(Article article) {
        Pair<Article, Integer> toRemove = null;

        for (Pair<Article, Integer> line : this.items) {
            if (line.getFirst().equals(article)) {
                toRemove = line;
                break;
            }
        }
        if (toRemove != null) {

            this.items.remove(toRemove);

        }

    }

    public void setArticleCount(Article article, int count) {
        if (count <= 0) {
            this.clearArticle(article);
            return;
        }
        Pair<Article, Integer> toModify = null;

        for (Pair<Article, Integer> line : this.items) {
            if (line.getFirst().equals(article)) {
                toModify = line;
                break;
            }
        }
        if (toModify != null) {
            toModify.setSecond(count);
        }

    }

    public int getItemCount(Article article) {
        for (Pair<Article, Integer> line : this.items) {
            if (line.getFirst().equals(article)) {
                return line.getSecond();
            }
        }
        return 0;
    }

    public int getPaidTotal() {
        int paid = 0;
        for (Paiement p : this.paiements) {
            paid += p.getMontantInCents();
        }
        return paid;
    }

    public void removeArticle(Article a) {
        Pair<Article, Integer> lineToDelete = null;
        for (Pair<Article, Integer> line : this.items) {
            if (line.getFirst().equals(a)) {

                final int count = line.getSecond() + 1;
                if (count <= 0) {
                    lineToDelete = line;
                }
                line.setSecond(count);
                break;
            }
        }
        if (lineToDelete != null) {
            this.items.remove(lineToDelete);
        }

    }

    @Override
    public String toString() {

        return "Ticket " + getCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Ticket) {
            Ticket t = (Ticket) obj;
            return t.getCode().equals(getCode());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
    }

    public void deleteTicket() {

        File dir = this.getOutputDir();
        String name = this.getCode().replace(' ', '_') + ".xml";
        File f = new File(dir, name);
        f.renameTo(new File(dir, name + "_deleted"));

    }
}
