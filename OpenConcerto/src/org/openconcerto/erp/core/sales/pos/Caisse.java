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
import org.openconcerto.erp.config.MainFrame;
import org.openconcerto.erp.core.common.ui.TotalCalculator;
import org.openconcerto.erp.core.finance.accounting.element.ComptePCESQLElement;
import org.openconcerto.erp.core.finance.accounting.element.JournalSQLElement;
import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.pos.io.ESCSerialPrinter;
import org.openconcerto.erp.core.sales.pos.io.JPOSTicketPrinter;
import org.openconcerto.erp.core.sales.pos.io.TicketPrinter;
import org.openconcerto.erp.core.sales.pos.model.Article;
import org.openconcerto.erp.core.sales.pos.model.Paiement;
import org.openconcerto.erp.core.sales.pos.model.Ticket;
import org.openconcerto.erp.core.sales.pos.model.TicketLine;
import org.openconcerto.erp.core.supplychain.stock.element.MouvementStockSQLElement;
import org.openconcerto.erp.core.supplychain.stock.element.StockLabel;
import org.openconcerto.erp.generationEcritures.GenerationMvtTicketCaisse;
import org.openconcerto.erp.generationEcritures.GenerationMvtVirement;
import org.openconcerto.erp.generationEcritures.GenerationReglementVenteNG;
import org.openconcerto.erp.model.PrixTTC;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.DesktopEnvironment;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Pair;
import org.openconcerto.utils.i18n.TranslationManager;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Caisse {
    private static final String POS_CONFIGURATION_FILENAME = "pos.xml";
    private static Document document;

    public static File getConfigFile(final String appName, final File wd) {
        final File wdFile = new File(wd + "/Configuration", POS_CONFIGURATION_FILENAME);
        final File confFile;
        if (wdFile.isFile()) {
            confFile = wdFile;
        } else {
            final File preferencesFolder = DesktopEnvironment.getDE().getPreferencesFolder(appName);
            if (!preferencesFolder.exists()) {
                preferencesFolder.mkdir();
            }
            confFile = new File(preferencesFolder, POS_CONFIGURATION_FILENAME);
        }
        return confFile;
    }

    private static Document getDocument() {
        if (document != null) {
            return document;
        }

        final SAXBuilder constructeur = new SAXBuilder();
        // lecture du contenu d'un fichier XML avec JDOM
        File file = getConfigFile();
        document = new Document();

        if (!file.exists()) {
            System.err.println("Erreur le fichier " + file.getAbsolutePath() + " n'existe pas!");
            document.setRootElement(new Element("config"));
            return document;
        }

        try {
            System.out.println("Loading:" + file.getAbsolutePath());
            document = constructeur.build(file);
        } catch (Exception e) {
            document.setRootElement(new Element("config"));
        }
        return document;
    }

    public static File getConfigFile() {
        return getConfigFile(ComptaPropsConfiguration.APP_NAME, new File("."));
    }

    public static void createConnexion() {
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
        conf.setupLogging("logs");
        TranslationManager.getInstance().addTranslationStreamFromClass(MainFrame.class);
        TranslationManager.getInstance().setLocale(Locale.getDefault());

        Configuration.setInstance(conf);
        try {
            conf.getBase();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            UserManager.getInstance().setCurrentUser(getUserID());
            final ComptaPropsConfiguration comptaPropsConfiguration = ((ComptaPropsConfiguration) Configuration.getInstance());
            comptaPropsConfiguration.setUpSocieteDataBaseConnexion(getSocieteID());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(new JFrame(), "Impossible de configurer la connexion à la base de donnée.\n ID société: " + getSocieteID() + " \n ID utilisateur: " + getUserID());
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static void commitAll(final List<Ticket> tickets) {
        // createConnexion();
        try {
            SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLUtils.SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    SQLElement elt = Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE");
                    SQLElement eltFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
                    SQLElement eltEnc = Configuration.getInstance().getDirectory().getElement("ENCAISSER_MONTANT");
                    SQLElement eltMode = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT");
                    SQLElement eltArticle = Configuration.getInstance().getDirectory().getElement("ARTICLE");
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

                            TotalCalculator calc = new TotalCalculator("T_PA_HT", "T_PV_HT", null);

                            String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
                            Boolean bServiceActive = Boolean.valueOf(val);
                            calc.setServiceActive(bServiceActive != null && bServiceActive);

                            // Articles
                            for (Pair<Article, Integer> item : ticket.getArticles()) {
                                SQLRowValues rowValsElt = new SQLRowValues(eltFact.getTable());
                                final Article article = item.getFirst();
                                final Integer nb = item.getSecond();
                                rowValsElt.put("QTE", nb);
                                rowValsElt.put("PV_HT", article.getPriceHTInCents());
                                Float tauxFromId = TaxeCache.getCache().getTauxFromId(article.getIdTaxe());
                                BigDecimal tauxTVA = new BigDecimal(tauxFromId).movePointLeft(2).add(BigDecimal.ONE);

                                final BigDecimal valueHT = article.getPriceHTInCents().multiply(new BigDecimal(nb), MathContext.DECIMAL128);

                                rowValsElt.put("T_PV_HT", valueHT);
                                rowValsElt.put("T_PV_TTC", valueHT.multiply(tauxTVA, MathContext.DECIMAL128));
                                rowValsElt.put("ID_TAXE", article.getIdTaxe());
                                rowValsElt.put("CODE", article.getCode());
                                rowValsElt.put("NOM", article.getName());
                                rowValsElt.put("ID_TICKET_CAISSE", rowVals);
                                rowValsElt.put("ID_ARTICLE", article.getId());
                                calc.addLine(rowValsElt, eltArticle.getTable().getRow(article.getId()), 0, false);
                            }
                            calc.checkResult();
                            long longValueTotalHT = calc.getTotalHT().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                            rowVals.put("TOTAL_HT", longValueTotalHT);

                            long longValueTotal = calc.getTotalTTC().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                            rowVals.put("TOTAL_TTC", longValueTotal);
                            long longValueTotalTVA = calc.getTotalTVA().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
                            rowVals.put("TOTAL_TVA", longValueTotalTVA);

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
                                        e.printStackTrace();
                                    }
                                    long montant = Long.valueOf(paiement.getMontantInCents());
                                    if (ticket.getPaiements().size() == 1 && paiement.getType() == Paiement.ESPECES) {
                                        montant = longValueTotal;
                                    }
                                    rowValsElt.put("MONTANT", montant);
                                    rowValsElt.put("NOM", "Ticket " + ticket.getCode());
                                    rowValsElt.put("DATE", ticket.getCreationDate());
                                    rowValsElt.put("ID_TICKET_CAISSE", rowVals);
                                }
                            }

                            SQLRow rowFinal = rowVals.insert();
                            GenerationMvtTicketCaisse mvt = new GenerationMvtTicketCaisse(rowFinal);
                            final Integer idMvt;
                            try {
                                idMvt = mvt.genereMouvement().call();

                                SQLRowValues valTicket = rowFinal.asRowValues();
                                valTicket.put("ID_MOUVEMENT", Integer.valueOf(idMvt));
                                rowFinal = valTicket.update();

                                // msie à jour du mouvement
                                List<SQLRow> rowsEnc = rowFinal.getReferentRows(eltEnc.getTable());
                                long totalEnc = 0;
                                for (SQLRow sqlRow : rowsEnc) {
                                    long montant = sqlRow.getLong("MONTANT");
                                    PrixTTC ttc = new PrixTTC(montant);
                                    totalEnc += montant;
                                    new GenerationReglementVenteNG("Règlement " + sqlRow.getForeignRow("ID_MODE_REGLEMENT").getForeignRow("ID_TYPE_REGLEMENT").getString("NOM") + " Ticket "
                                            + rowFinal.getString("NUMERO"), getClientCaisse(), ttc, sqlRow.getDate("DATE").getTime(), sqlRow.getForeignRow("ID_MODE_REGLEMENT"), rowFinal, rowFinal
                                            .getForeignRow("ID_MOUVEMENT"), false);
                                }
                                if (totalEnc > longValueTotal) {
                                    final SQLTable table = Configuration.getInstance().getDirectory().getElement("TYPE_REGLEMENT").getTable();
                                    int idComptePceCaisse = table.getRow(TypeReglementSQLElement.ESPECE).getInt("ID_COMPTE_PCE_CLIENT");
                                    if (idComptePceCaisse == table.getUndefinedID()) {
                                        idComptePceCaisse = ComptePCESQLElement.getId(ComptePCESQLElement.getComptePceDefault("VenteEspece"));
                                    }
                                    new GenerationMvtVirement(idComptePceCaisse, getClientCaisse().getInt("ID_COMPTE_PCE"), 0, totalEnc - longValueTotal, "Rendu sur règlement " + " Ticket "
                                            + rowFinal.getString("NUMERO"), new Date(), JournalSQLElement.CAISSES, " Ticket " + rowFinal.getString("NUMERO")).genereMouvement();
                                }
                            } catch (Exception exn) {
                                exn.printStackTrace();
                                throw new SQLException(exn);
                            }
                            updateStock(rowFinal.getID());

                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            JOptionPane.showMessageDialog(null, "Clôture de la caisse terminée.");
                        }
                    });
                    return null;
                }
            });
        } catch (Exception exn) {
            ExceptionHandler.handle("Une erreur est survenue pendant la clôture.", exn);
        }

    }

    private static SQLRow rowClient = null;

    private static SQLRow getClientCaisse() throws SQLException {
        if (rowClient == null) {
            SQLElement elt = Configuration.getInstance().getDirectory().getElement("CLIENT");
            SQLSelect sel = new SQLSelect();
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

    private static void updateStock(int id) throws SQLException {

        final SQLElement elt = Configuration.getInstance().getDirectory().getElement("TICKET_CAISSE");
        final SQLElement eltArticleFact = Configuration.getInstance().getDirectory().getElement("SAISIE_VENTE_FACTURE_ELEMENT");
        MouvementStockSQLElement mvtStock = (MouvementStockSQLElement) Configuration.getInstance().getDirectory().getElement("MOUVEMENT_STOCK");
        mvtStock.createMouvement(elt.getTable().getRow(id), eltArticleFact.getTable(), new StockLabel() {
            @Override
            public String getLabel(SQLRow rowOrigin, SQLRow rowElt) {
                return "Ticket N°" + rowOrigin.getString("NUMERO");
            }
        }, false);
    }

    public static int getID() {
        final Document d = getDocument();
        return Integer.valueOf(d.getRootElement().getAttributeValue("caisseID", "-1"));
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
            String code = names[i];
            int indexExtension = code.indexOf(".xml");
            if (indexExtension > 0) {
                code = code.substring(0, indexExtension);
            }
            final Ticket ticket = Ticket.getTicketFromCode(code);

            if (ticket != null) {
                l.add(ticket);
            }
        }
        return l;
    }

    public static int getUserID() {
        final Document d = getDocument();
        return Integer.valueOf(d.getRootElement().getAttributeValue("userID", "-1"));
    }

    public static void setUserID(int userId) {
        final Document d = getDocument();
        d.getRootElement().setAttribute("userID", String.valueOf(userId));
    }

    public static int getSocieteID() {
        final Document d = getDocument();
        return Integer.valueOf(d.getRootElement().getAttributeValue("societeID", "-1"));
    }

    public static void setSocieteID(int societeId) {
        final Document d = getDocument();
        d.getRootElement().setAttribute("societeID", String.valueOf(societeId));
    }

    public static boolean isCopyActive() {
        final Document d = getDocument();
        return Boolean.valueOf(d.getRootElement().getAttributeValue("copyTicket", "true"));
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
        Element child = d.getRootElement().getChild("printer");
        if (child == null) {
            child = new Element("printer");
            d.getRootElement().addContent(child);
            return false;
        }
        final String type = child.getAttributeValue("type");
        return (type != null && type.equalsIgnoreCase("jpos"));
    }

    public static void setPrinterType(String type) {
        final Document d = getDocument();
        Element e = d.getRootElement().getChild("printer");
        if (e == null) {
            e = new Element("printer");
            d.getRootElement().addContent(e);
        }
        e.setAttribute("type", type);
    }

    public static String getJPosPrinter() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("jpos");
        if (children != null) {
            for (Element e : children) {
                if (e.getAttribute("printer") != null) {
                    return e.getAttributeValue("printer");
                }
            }
        }
        return "";
    }

    public static void setJPosPrinter(String printer) {
        final Document d = getDocument();
        Element e = d.getRootElement().getChild("jpos");
        if (e == null) {
            e = new Element("jpos");
            d.getRootElement().addContent(e);
        }
        e.setAttribute("printer", printer);
    }

    public static String getESCPPort() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("escp");
        if (children != null) {
            for (Element e : children) {
                if (e.getAttribute("port") != null) {
                    return e.getAttributeValue("port", "COM1:");
                }
            }
        }
        return "COM1:";
    }

    public static void setESCPPort(String port) {
        final Document d = getDocument();
        Element e = d.getRootElement().getChild("escp");
        if (e == null) {
            e = new Element("escp");
            d.getRootElement().addContent(e);
        }
        e.setAttribute("port", port);
    }

    public static String getJPosDirectory() {
        final Document d = getDocument();
        final List<Element> children = d.getRootElement().getChildren("jpos");
        if (children != null) {
            for (Element e : children) {
                if (e.getValue() != null) {
                    return e.getValue();
                }
            }
        }
        return "";
    }

    public static void setJPosDirectory(String dir) {
        final Document d = getDocument();
        Element e = d.getRootElement().getChild("jpos");
        if (e == null) {
            e = new Element("jpos");
            d.getRootElement().addContent(e);
        }
        e.setText(dir);
    }

    public static List<TicketLine> getHeaders() {
        final List<TicketLine> l = new ArrayList<TicketLine>();
        final Document d = getDocument();
        final List<Element> list = d.getRootElement().getChildren("header");
        if (list != null) {
            for (Element element : list) {
                l.add(new TicketLine(element.getValue(), element.getAttributeValue("style")));
            }
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
        if (list != null) {
            for (Element element : list) {
                l.add(new TicketLine(element.getValue(), element.getAttributeValue("style")));
            }
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
        if (children != null) {
            for (Element e : children) {
                if (e.getAttribute("printWidth") != null) {
                    final String attributeValue = e.getAttributeValue("printWidth");
                    return Integer.parseInt(attributeValue);
                }
            }
        }
        return 20;
    }

    public static void setTicketWidth(String w) {
        final Document d = getDocument();
        Element e = d.getRootElement().getChild("printer");
        if (e == null) {
            e = new Element("printer");
            d.getRootElement().addContent(e);
        }
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
            ExceptionHandler.handle("Erreur lors de la sauvegarde de la configuration de la caisse.\n" + file.getAbsolutePath());
        }

    }

}
