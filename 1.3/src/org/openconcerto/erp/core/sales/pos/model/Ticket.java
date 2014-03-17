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

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.pos.Caisse;
import org.openconcerto.erp.core.sales.pos.io.DefaultTicketPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.ui.TicketCellRenderer;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Ticket {
    static public Calendar getCalendar() {
        return Calendar.getInstance();
    }

    private static boolean inited = false;
    // Propre a ticket
    private List<Paiement> paiements = new ArrayList<Paiement>();
    private final List<Pair<Article, Integer>> items = new ArrayList<Pair<Article, Integer>>();
    private Calendar creationCal;
    private int number;

    // Propre à la caisse
    private final int caisseNumber;

    private static final SQLTable tableArticle = Configuration.getInstance().getRoot().findTable("ARTICLE");

    public static Ticket getTicketFromCode(String code) {
        try {
            // Loading file
            return parseFile(new ReceiptCode(code).getFile());
        } catch (Exception e) {
            System.err.println("Error with ticket code : " + code);
            e.printStackTrace();
            return null;
        }
    }

    public static Ticket parseFile(final File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            // XML Reading

            final SAXBuilder sxb = new SAXBuilder();
            final Document document = sxb.build(file);
            final Element root = document.getRootElement();
            final ReceiptCode receiptCode = new ReceiptCode(root.getAttributeValue("code"));
            final String h = root.getAttributeValue("hour");
            final String m = root.getAttributeValue("minute");
            final Calendar c = (Calendar) receiptCode.getDay().clone();
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(h));
            c.set(Calendar.MINUTE, Integer.parseInt(m));
            final Ticket t = new Ticket(receiptCode.getCaisseNb());
            t.setCreationCal(c);
            t.setNumber(receiptCode.getDayIndex());

            // article
            @SuppressWarnings("unchecked")
            final List<Element> children = root.getChildren("article");
            for (Element element : children) {
                int qte = Integer.parseInt(element.getAttributeValue("qte"));
                BigDecimal prix_unitaire_cents_ht = new BigDecimal(element.getAttributeValue("prixHT"));
                int idTaxe = Integer.parseInt(element.getAttributeValue("idTaxe"));
                BigDecimal prix_unitaire_cents = new BigDecimal(element.getAttributeValue("prix"));
                String categorie = element.getAttributeValue("categorie");
                String name = element.getValue();
                String codebarre = element.getAttributeValue("codebarre");
                String codeArt = element.getAttributeValue("code");
                Categorie cat = new Categorie(categorie);

                String valueID = element.getAttributeValue("id");

                int id = valueID == null || valueID.trim().length() == 0 ? tableArticle.getUndefinedID() : Integer.parseInt(valueID);
                Article art = new Article(cat, name, id);
                art.setPriceInCents(prix_unitaire_cents);
                art.setCode(codeArt);
                art.setPriceHTInCents(prix_unitaire_cents_ht);
                art.setIdTaxe(idTaxe);
                art.barCode = codebarre;
                Pair<Article, Integer> line = new Pair<Article, Integer>(art, qte);
                t.items.add(line);

            }
            // paiement
            @SuppressWarnings("unchecked")
            final List<Element> payChildren = root.getChildren("paiement");
            for (Element element : payChildren) {

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

            return t;
        } catch (Exception e) {
            System.err.println("Error with ticket : " + file);
            e.printStackTrace();
            return null;
        }
    }

    public Ticket(int caisse) {
        this.caisseNumber = caisse;
        this.creationCal = getCalendar();
        initNumber();
    }

    public void setNumber(int i) {
        this.number = i;
    }

    private void initNumber() {
        if (!inited) {
            int max = 0;
            for (final ReceiptCode c : this.getReceiptCode().getSameDayCodes(true)) {
                if (c.getDayIndex() > max)
                    max = c.getDayIndex();
            }
            this.setNumber(max + 1);
        }
    }

    public final ReceiptCode getReceiptCode() {
        // TODO replace our fields by one ReceiptCode
        return new ReceiptCode(this.getCaisseNumber(), this.getCreationCal(), this.getNumber());
    }

    public String getCode() {
        return getReceiptCode().getCode();
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
    final int getCaisseNumber() {
        return this.caisseNumber;
    }

    public void save() {
        // Update Hour & Minute
        int hour = getCalendar().get(Calendar.HOUR_OF_DAY);
        int minute = getCalendar().get(Calendar.MINUTE);

        // Hierarchie: 2010/04/05/01_05042010_00002.xml
        final File f = getFile();
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
            e.setAttribute("codebarre", item.getFirst().getBarCode());
            e.setAttribute("code", item.getFirst().getCode());
            e.setAttribute("id", String.valueOf(item.getFirst().getId()));
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
            final FileOutputStream fileOutputStream = new FileOutputStream(f);
            out.output(topLevel, fileOutputStream);
            fileOutputStream.close();
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
        SimpleDateFormat df = new SimpleDateFormat("EEEE d MMMM yyyy à HH:mm", Locale.FRENCH);
        prt.addToBuffer(DefaultTicketPrinter.formatRight(maxWidth, "Le " + df.format(getCreationDate())));
        prt.addToBuffer("");

        for (Pair<Article, Integer> item : this.items) {
            final Article article = item.getFirst();
            final Integer nb = item.getSecond();
            Float tauxFromId = TaxeCache.getCache().getTauxFromId(article.getIdTaxe());
            BigDecimal tauxTVA = new BigDecimal(tauxFromId).movePointLeft(2).add(BigDecimal.ONE);

            BigDecimal multiply = article.getPriceHTInCents().multiply(new BigDecimal(nb), MathContext.DECIMAL128).multiply(tauxTVA, MathContext.DECIMAL128);
            prt.addToBuffer(DefaultTicketPrinter.formatRight(MAX_QTE_WIDTH, String.valueOf(nb)) + " "
                    + DefaultTicketPrinter.formatLeft(maxWidth - 2 - MAX_PRICE_WIDTH - MAX_QTE_WIDTH, article.getName()) + " "
                    + DefaultTicketPrinter.formatRight(MAX_PRICE_WIDTH, TicketCellRenderer.centsToString(multiply.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue())));
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
                type = "Paiement par chèque";
            } else if (paiement.getType() == Paiement.ESPECES) {
                type = "Paiement en espèces";
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

    private File getFile() {
        return getReceiptCode().getFile();
    }

    public Date getCreationDate() {
        return this.getCreationCal().getTime();
    }

    public Calendar getCreationCal() {
        return this.creationCal;
    }

    public void setCreationCal(final Calendar cal) {
        this.creationCal = (Calendar) cal.clone();
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
        final SQLTable tableElt = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().findTable("SAISIE_VENTE_FACTURE_ELEMENT");
        final TotalCalculator calc = new TotalCalculator("T_PA_HT", "T_PV_HT", null);
        final String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        final Boolean bServiceActive = Boolean.valueOf(val);
        calc.setServiceActive(bServiceActive != null && bServiceActive);
        final int size = this.items.size();
        for (int i = 0; i < size; i++) {
            final Pair<Article, Integer> line = this.items.get(i);
            final int count = line.getSecond();
            final Article art = line.getFirst();
            final SQLRowValues rowVals = new SQLRowValues(tableElt);
            rowVals.put("T_PV_HT", art.getPriceHTInCents().multiply(new BigDecimal(count)));
            rowVals.put("QTE", count);
            rowVals.put("ID_TAXE", art.idTaxe);
            calc.addLine(rowVals, tableArticle.getRow(art.getId()), i, false);

        }
        calc.checkResult();
        return calc.getTotalTTC().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
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

    public void deleteTicket() throws IOException {
        getReceiptCode().markDeleted();
    }
}
