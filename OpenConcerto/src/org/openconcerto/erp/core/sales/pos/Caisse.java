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
 
 package org.openconcerto.erp.core.sales.pos;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.sales.pos.io.ESCSerialPrinter;
import org.openconcerto.erp.core.sales.pos.io.JPOSTicketPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.erp.core.sales.pos.model.TicketLine;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.generationEcritures.GenerationMvtTicketCaisse;
import org.openconcerto.erp.generationEcritures.GenerationReglementVenteNG;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Caisse {
    private static Document document;

    private static File getConfigFile() {
        final File prefDir = new File("Configuration");
        final File file = new File(prefDir, "ConfigCaisse.xml");
        return file;
    }

    private static Document getDocument() {
        if (document != null) {
            return document;
        }

        final SAXBuilder constructeur = new SAXBuilder();
        // lecture du contenu d'un fichier XML avec JDOM
        File file = getConfigFile();
        if (!file.exists()) {
            System.err.println("Erreur le fichier ConfigCaisse.xml n'existe pas!");
            return new Document();
        }

        try {
            System.out.println("Loading:" + file.getAbsolutePath());
            document = constructeur.build(file);
        } catch (Exception e) {
            e.printStackTrace();
            return new Document();
        }
        return document;
    }

    public static void createConnexion() {
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
        conf.setupLogging("Logs");
        Configuration.setInstance(conf);
        try {
            conf.getBase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Document d = getDocument();
            final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
            comptaPropsConfiguration.setUpSocieteDataBaseConnexion(getSocieteID());
            UserManager.getInstance().setCurrentUser(getUserID());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), "Impossible de configurer la connexion à la base de donnée.\n ID société: " + getSocieteID() + " \n ID utilisateur: " + getUserID());
            e.printStackTrace();
        }
    }

    public static void commitAll(List<Ticket> tickets) {
        // createConnexion();
        SQLElement elt = Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE");
        SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        SQLElement eltEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
        SQLElement eltMode = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
        for (Ticket ticket : tickets) {
            SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
            sel.addSelect(elt.getTable().getField("NUMERO"));
            sel.setWhere(new Where(elt.getTable().getField("NUMERO"), "=", ticket.getCode()));
            List<?> l = Configuration.getInstance().getBase().getDataSource().executeCol(sel.asString());
            if (l != null && l.size() == 0) {

                SQLRowValues rowVals = new SQLRowValues(elt.getTable());
                rowVals.put("NUMERO", ticket.getCode());
                rowVals.put("DATE", ticket.getCreationDate());
                rowVals.put("ID_CAISSE", getID());
                long total = 0;
                long totalHT = 0;

                // Articles
                for (Pair<Article, Integer> item : ticket.getArticles()) {
                    SQLRowValues rowValsElt = new SQLRowValues(eltFact.getTable());

                    rowValsElt.put("QTE", item.getSecond());
                    rowValsElt.put("PV_HT", Long.valueOf(item.getFirst().getPriceInCents()));
                    final long value = Long.valueOf(item.getFirst().getPriceInCents()) * item.getSecond();
                    final long valueHT = Long.valueOf(item.getFirst().getPriceHTInCents()) * item.getSecond();
                    total += value;
                    totalHT += valueHT;
                    rowValsElt.put("T_PV_HT", valueHT);
                    rowValsElt.put("T_PV_TTC", value);
                    rowValsElt.put("CODE", item.getFirst().getCode());
                    rowValsElt.put("NOM", item.getFirst().getName());
                    rowValsElt.put("ID_TICKET_CAISSE", rowVals);
                }
                rowVals.put("TOTAL_HT", totalHT);

                rowVals.put("TOTAL_TTC", total);
                rowVals.put("TOTAL_TVA", total - totalHT);

                // Paiements
                for (Paiement paiement : ticket.getPaiements()) {
                    if (paiement.getMontantInCents() > 0) {
                        SQLRowValues rowValsElt = new SQLRowValues(eltEnc.getTable());
                        SQLRowValues rowValsEltMode = new SQLRowValues(eltMode.getTable());
                        if (paiement.getType() == Paiement.CB) {
                            rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CB);
                        } else if (paiement.getType() == Paiement.CHEQUE) {
                            rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CHEQUE);
                        } else if (paiement.getType() == Paiement.ESPECES) {
                            rowValsEltMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.ESPECE);
                        }

                        rowValsElt.put("ID_MODE_REGLEMENT", rowValsEltMode);
                        try {
                            rowValsElt.put("ID_CLIENT", getClientCaisse().getID());
                        } catch (SQLException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        long montant = Long.valueOf(paiement.getMontantInCents());
                        if (ticket.getPaiements().size() == 1 && paiement.getType() == Paiement.ESPECES) {
                            montant = total;
                        }
                        rowValsElt.put("MONTANT", montant);
                        rowValsElt.put("NOM", "Ticket " + ticket.getCode());
                        rowValsElt.put("DATE", ticket.getCreationDate());
                        rowValsElt.put("ID_TICKET_CAISSE", rowVals);
                    }
                }
                try {
                    SQLRow rowFinal = rowVals.insert();
                    Thread t = new Thread(new GenerationMvtTicketCaisse(rowFinal));
                    t.start();
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // msie à jour du mouvement
                    rowFinal = rowFinal.getTable().getRow(rowFinal.getID());
                    List<SQLRow> rowsEnc = rowFinal.getReferentRows(eltEnc.getTable());
                    for (SQLRow sqlRow : rowsEnc) {
                        long montant = sqlRow.getLong("MONTANT");
                        PrixTTC ttc = new PrixTTC(montant);
                        new GenerationReglementVenteNG("Règlement " + sqlRow.getForeignRow("ID_MODE_REGLEMENT").getForeignRow("ID_TYPE_REGLEMENT").getString("NOM") + " Ticket "
                                + rowFinal.getString("NUMERO"), getClientCaisse(), ttc, sqlRow.getDate("DATE").getTime(), sqlRow.getForeignRow("ID_MODE_REGLEMENT"), rowFinal,
                                rowFinal.getForeignRow("ID_MOUVEMENT"), false);
                    }

                    updateStock(rowFinal.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        JOptionPane.showMessageDialog(null, "Clôture de la caisse terminée.");
    }

    private static SQLRow rowClient = null;

    private static SQLRow getClientCaisse() throws SQLException {
        if (rowClient == null) {
            SQLElement elt = Configuration.getInstance().getDirectory().getElement("CLIENT");
            SQLSelect sel = new SQLSelect(elt.getTable().getBase());
            sel.addSelectStar(elt.getTable());
            sel.setWhere(new Where(elt.getTable().getField("NOM"), "=", "Caisse OpenConcerto"));
            List<SQLRow> l = (List<SQLRow>) elt.getTable().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
            if (l.size() > 0) {
                rowClient = l.get(0);
            } else {
                SQLRowValues rowValues = new SQLRowValues(elt.getTable());
                rowValues.put("NOM", "Caisse OpenConcerto");
                SQLRowValues rowValuesMode = new SQLRowValues(elt.getTable().getTable("MODE_REGLEMENT"));
                rowValuesMode.put("ID_TYPE_REGLEMENT", TypeReglementSQLElement.CB);
                rowValues.put("ID_MODE_REGLEMENT", rowValuesMode);

                // Select Compte client par defaut
                final SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
                final SQLTable tablePrefCompte = base.getTable("PREFS_COMPTE");
                final SQLRow rowPrefsCompte = tablePrefCompte.getRow(2);

                int idDefaultCompteClient = rowPrefsCompte.getInt("ID_COMPTE_PCE_CLIENT");
                if (idDefaultCompteClient <= 1) {
                    try {
                        idDefaultCompteClient = ComptePCESQLElement.getIdComptePceDefault("Clients");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                rowValues.put("ID_COMPTE_PCE", idDefaultCompteClient);
                rowClient = rowValues.insert();
            }
        }
        return rowClient;

    }

    private static void updateStock(int id) {

        final SQLElement eltArticleFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE");
        final SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement(sqlTableArticle);
        final SQLRow rowFacture = elt.getTable().getRow(id);

        // On récupére les articles qui composent la facture
        final SQLSelect selEltfact = new SQLSelect(eltArticleFact.getTable().getBase());
        selEltfact.addSelect(eltArticleFact.getTable().getField("ID"));
        selEltfact.setWhere(new Where(eltArticleFact.getTable().getField("ID_TICKET_CAISSE"), "=", id));

        final List lEltFact = (List) eltArticleFact.getTable().getBase().getDataSource().execute(selEltfact.asString(), new ArrayListHandler());

        if (lEltFact != null) {
            for (int i = 0; i < lEltFact.size(); i++) {

                // Elt qui compose facture
                final Object[] tmp = (Object[]) lEltFact.get(i);
                final int idEltFact = ((Number) tmp[0]).intValue();
                final SQLRow rowEltFact = eltArticleFact.getTable().getRow(idEltFact);

                // on récupére l'article qui lui correspond
                final SQLRowValues rowArticle = new SQLRowValues(eltArticle.getTable());
                for (SQLField field : eltArticle.getTable().getFields()) {
                    if (rowEltFact.getTable().getFieldsName().contains(field.getName())) {
                        rowArticle.put(field.getName(), rowEltFact.getObject(field.getName()));
                    }
                }
                // rowArticle.loadAllSafe(rowEltFact);
                final int idArticle = ReferenceArticleSQLElement.getIdForCN(rowArticle, true);

                // on crée un mouvement de stock pour chacun des articles
                final SQLElement eltMvtStock = Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
                final SQLRowValues rowVals = new SQLRowValues(eltMvtStock.getTable());
                rowVals.put("QTE", -(rowEltFact.getInt("QTE")));
                rowVals.put("NOM", "Ticket N°" + rowFacture.getString("NUMERO"));
                rowVals.put("IDSOURCE", id);
                rowVals.put("SOURCE", elt.getTable().getName());
                rowVals.put("ID_ARTICLE", idArticle);
                rowVals.put("DATE", rowFacture.getObject("DATE"));
                try {
                    final SQLRow row = rowVals.insert();
                    MouvementStockSQLElement.updateStock(row.getID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static int getID() {
        final Document d = getDocument();
        return Integer.valueOf(d.getRootElement().getAttributeValue("caisseID"));
    }

    public static void setID(int caisseId) {
        final Document d = getDocument();
        d.getRootElement().setAttribute("caisseID", String.valueOf(caisseId));
    }

    public static List<Ticket> allTickets() {
        final List<Ticket> l = new ArrayList<Ticket>();
        final Ticket t = new Ticket(Caisse.getID());
        final String[] names = t.getCompatibleFileNames();

        for (int i = 0; i < names.length; i++) {
            final String code = names[i].substring(0, 17);
            final Ticket ticket = Ticket.getTicketFromCode(code);
            l.add(ticket);
        }
        return l;
    }

    public static int getUserID() {
        final Document d = getDocument();
        return Integer.valueOf(d.getRootElement().getAttributeValue("userID"));
    }

    public static void setUserID(int userId) {
        final Document d = getDocument();
        d.getRootElement().setAttribute("userID", String.valueOf(userId));
    }

    public static int getSocieteID() {
        final Document d = getDocument();
        return Integer.valueOf(d.getRootElement().getAttributeValue("societeID"));
    }

    public static void setSocieteID(int societeId) {
        final Document d = getDocument();
        d.getRootElement().setAttribute("societeID", String.valueOf(societeId));
    }

    public static boolean isCopyActive() {
        final Document d = getDocument();
        return Boolean.valueOf(d.getRootElement().getAttributeValue("copyTicket"));
    }

    public static void setCopyActive(boolean b) {
        final Document d = getDocument();
        d.getRootElement().setAttribute("copyTicket", b ? "true" : "false");
    }

    public static TicketPrinter getTicketPrinter() {
        if (isUsingJPos()) {
            JPOSTicketPrinter prt = new JPOSTicketPrinter(getJPosPrinter());
            return prt;
        }
        return new ESCSerialPrinter(getESCPPort());
    }

    public static boolean isUsingJPos() {
        final Document d = getDocument();
        final Element child = d.getRootElement().getChild("printer");
        if (child == null) {
            return false;
        }
        final String type = child.getAttributeValue("type");
        return (type != null && type.equalsIgnoreCase("jpos"));
    }

    public static void setPrinterType(String type) {
        final Document d = getDocument();
        d.getRootElement().getChild("printer").setAttribute("type", type);
    }

    public static String getJPosPrinter() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("jpos");
        for (Element e : children) {
            if (e.getAttribute("printer") != null) {
                return e.getAttributeValue("printer");
            }
        }
        return "";
    }

    public static void setJPosPrinter(String printer) {
        final Document d = getDocument();
        final Element e = d.getRootElement().getChild("jpos");
        e.setAttribute("printer", printer);
    }

    public static String getESCPPort() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("escp");
        for (Element e : children) {
            if (e.getAttribute("port") != null) {
                return e.getAttributeValue("port");
            }
        }
        return "COM1:";
    }

    public static void setESCPPort(String port) {
        final Document d = getDocument();
        final Element e = d.getRootElement().getChild("escp");
        e.setAttribute("port", port);
    }

    public static String getJPosDirectory() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("jpos");
        for (Element e : children) {
            if (e.getValue() != null) {
                return e.getValue();
            }
        }
        return "";
    }

    public static void setJPosDirectory(String dir) {
        final Document d = getDocument();
        final Element e = d.getRootElement().getChild("jpos");
        e.setText(dir);
    }

    public static List<TicketLine> getHeaders() {
        final List<TicketLine> l = new ArrayList<TicketLine>();
        final Document d = getDocument();
        final List<Element> list = d.getRootElement().getChildren("header");
        for (Element element : list) {
            l.add(new TicketLine(element.getValue(), element.getAttributeValue("style")));
        }
        return l;
    }

    public static void setLines(String type, List<TicketLine> lines) {
        final Document d = getDocument();
        final Element rootElement = d.getRootElement();
        rootElement.removeChildren(type);
        for (TicketLine ticketLine : lines) {
            Element e = new Element(type);
            final String style = ticketLine.getStyle();
            if (style != null && !style.isEmpty()) {
                e.setAttribute("style", style);
            }
            e.setText(ticketLine.getText());
            rootElement.addContent(e);
        }

    }

    public static List<TicketLine> getFooters() {
        final List<TicketLine> l = new ArrayList<TicketLine>();
        final Document d = getDocument();
        final List<Element> list = d.getRootElement().getChildren("footer");
        for (Element element : list) {
            l.add(new TicketLine(element.getValue(), element.getAttributeValue("style")));
        }
        return l;
    }

    public static void setHeaders(List<TicketLine> lines) {
        setLines("header", lines);
    }

    public static void setFooters(List<TicketLine> lines) {
        setLines("footer", lines);
    }

    public static int getTicketWidth() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("printer");
        for (Element e : children) {
            if (e.getAttribute("printWidth") != null) {
                final String attributeValue = e.getAttributeValue("printWidth");
                return Integer.parseInt(attributeValue);
            }
        }
        return 20;
    }

    public static void setTicketWidth(String w) {
        final Document d = getDocument();
        final Element e = d.getRootElement().getChild("printer");
        e.setAttribute("printWidth", w);
    }

    public static void saveConfiguration() {
        final File file = getConfigFile();
        final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            System.out.println("Saving:" + file.getAbsolutePath());
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            outputter.output(getDocument(), fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(new JFrame(), "Erreur lors de la sauvegarde de la configuration de la caisse.\n" + file.getAbsolutePath());
        }

    }

}
