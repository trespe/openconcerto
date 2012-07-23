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

import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.accounting.ui.AjouterAxeAnalytiqueFrame;
import org.openconcerto.erp.core.finance.accounting.ui.RepartitionAxeAnalytiquePanel;
import org.openconcerto.erp.core.finance.accounting.ui.ValiderSuppressionAxeFrame;
import org.openconcerto.erp.element.objet.Axe;
import org.openconcerto.erp.element.objet.Poste;
import org.openconcerto.erp.element.objet.Repartition;
import org.openconcerto.erp.element.objet.RepartitionElement;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.DefaultGridBagConstraints;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class AnalytiqueSQLElement extends ComptaSQLConfElement {

    // association onglet - Axe
    private Vector vecteurTabAxe;

    private final List<Axe> axes = new ArrayList<Axe>();
    private List<List<Repartition>> repartitionsAxe;
    private List<List<RepartitionElement>> repartitionElemsAxe;
    private List<List<Poste>> postesAxe;

    private AnalytiqueSQLElement a;

    private JTabbedPane tabAxes;

    // Map utilisée pour les ID, association IDtmp/IDReal
    private Map<Integer, Integer> mapPostes;
    private Map<Integer, Integer> mapRepartitons;

    // Window utilisée pour changer le nom des onglets ou des colonnes
    private JWindow windowChangeNom;

    private AjouterAxeAnalytiqueFrame ajoutAxeFrame = null;
    private JTextField text;
    private int editedAxeIndex = -1;

    public AnalytiqueSQLElement() {
        super("AXE_ANALYTIQUE", "un axe analytique", "axes analytiques");
        this.a = this;
    }

    protected List getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    protected List getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {
        return new BaseSQLComponent(this) {
            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();
                c.anchor = GridBagConstraints.NORTHWEST;
                c.gridwidth = 4;
                c.gridheight = 8;

                vecteurTabAxe = new Vector();

                repartitionsAxe = new Vector();
                repartitionElemsAxe = new Vector();
                postesAxe = new Vector();

                tabAxes = new JTabbedPane();

                /***********************************************************************************
                 * * CREATION DES ONGLETS
                 **********************************************************************************/

                // on recupere les axes existant
                // SELECT ID, NOM FROM AXE
                SQLBase base = getTable().getBase();
                SQLSelect sel = new SQLSelect(base);
                sel.addSelect(getTable().getKey());
                sel.addSelect(getTable().getField("NOM"));
                sel.addRawOrder("AXE_ANALYTIQUE.NOM");
                String req = sel.asString();

                Object ob = getTable().getBase().getDataSource().execute(req, new ArrayListHandler());

                List myList = (List) ob;
                if (myList.size() != 0) {

                    // on crée les onglets et on stocke les axes
                    for (int i = 0; i < myList.size(); i++) {

                        // ID, nom
                        Object[] objTmp = (Object[]) myList.get(i);

                        axes.add(new Axe(Integer.parseInt(objTmp[0].toString()), objTmp[1].toString()));

                        // on recupere les repartitions et les élements associés à l'axe
                        RepartitionAxeAnalytiquePanel repAxeComp = new RepartitionAxeAnalytiquePanel(((Axe) axes.get(i)).getId());

                        repartitionsAxe.add(repAxeComp.getRepartitions());
                        repartitionElemsAxe.add(repAxeComp.getRepartitionElems());
                        postesAxe.add(repAxeComp.getPostes());
                        tabAxes.addTab(((Axe) axes.get(i)).getNom(), repAxeComp);

                        vecteurTabAxe.add(i, new String(String.valueOf(i)));
                    }

                    System.out.println("Size ----> " + axes.size());
                } else {
                    ajouterAxe("Nouvel Axe");
                }

                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1;
                c.weighty = 1;
                this.add(tabAxes, c);

                tabAxes.addMouseListener(new MouseAdapter() {

                    public void mousePressed(final MouseEvent e) {

                        final int index = tabAxes.indexAtLocation(e.getX(), e.getY());

                        validAxeText();

                        if (e.getClickCount() == 2) {

                            actionModifierAxe(e, index);
                        }

                        if (e.getButton() == MouseEvent.BUTTON3) {
                            actionDroitOnglet(index, e);
                        }
                    }
                });

                tabAxes.addAncestorListener(new AncestorListener() {

                    public void ancestorAdded(AncestorEvent event) {

                        validAxeText();

                    }

                    public void ancestorRemoved(AncestorEvent event) {

                        validAxeText();

                    }

                    public void ancestorMoved(AncestorEvent event) {

                        validAxeText();

                    }
                });

                /***********************************************************************************
                 * * AJOUT D'UN AXE
                 **********************************************************************************/
                JButton boutonAddAxe = new JButton("Ajouter un axe");
                boutonAddAxe.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {

                        if (ajoutAxeFrame == null) {
                            ajoutAxeFrame = new AjouterAxeAnalytiqueFrame(a);
                        }
                        ajoutAxeFrame.pack();
                        ajoutAxeFrame.setVisible(true);
                    }
                });

                c.gridx += 4;
                c.gridwidth = 1;
                c.gridheight = 1;
                c.weightx = 0;
                c.weighty = 0;
                c.fill = GridBagConstraints.HORIZONTAL;
                this.add(boutonAddAxe, c);

                /***********************************************************************************
                 * * SUPPRESSION D'UN AXE
                 **********************************************************************************/
                JButton boutonDelAxe = new JButton("Supprimer un axe");
                boutonDelAxe.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {

                        supprimerAxe(Integer.parseInt(vecteurTabAxe.get(tabAxes.getSelectedIndex()).toString()));

                    }
                });

                c.gridy++;
                this.add(boutonDelAxe, c);

                JButton boutonSet = new JButton("SetVal");
                boutonSet.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        setVal();
                    }
                });

                c.gridy++;
                this.add(boutonSet, c);

                JButton boutonCheck = new JButton("Check");
                boutonCheck.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent arg0) {
                        checkID();
                    }
                });

                c.gridy++;
                this.add(boutonCheck, c);
            }
        };
    }

    private void actionDroitOnglet(final int index, final MouseEvent e) {

        JPopupMenu pop = new JPopupMenu();
        pop.add(new AbstractAction("Ajouter un axe") {

            public void actionPerformed(ActionEvent e) {
                if (ajoutAxeFrame == null) {
                    ajoutAxeFrame = new AjouterAxeAnalytiqueFrame(a);
                }
                ajoutAxeFrame.pack();
                ajoutAxeFrame.setVisible(true);
            }
        });

        // si un onglet est selectionné
        if (index != -1) {
            pop.add(new AbstractAction("Supprimer l'axe") {

                public void actionPerformed(ActionEvent e) {

                    supprimerAxe(index);
                }
            });

            pop.add(new AbstractAction("Modifier le nom") {

                public void actionPerformed(ActionEvent aE) {

                    actionModifierAxe(e, index);
                }
            });
        }

        pop.show(e.getComponent(), e.getX(), e.getY());

        System.out.println("Click droit onglet");
    }

    private void actionModifierAxe(MouseEvent e, final int index) {

        if (index == -1)
            return;

        Component comp = (Component) e.getSource();
        JFrame frame = (JFrame) SwingUtilities.getRoot(comp);

        this.windowChangeNom = new JWindow(frame);
        Container container = this.windowChangeNom.getContentPane();
        container.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;

        this.editedAxeIndex = index;
        this.text = new JTextField(" " + this.tabAxes.getTitleAt(index) + " ");
        this.text.setEditable(true);
        container.add(this.text, c);
        this.text.setBorder(null);
        // text.setBackground(this.tabAxes.getBackground());
        this.text.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent event) {

                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    validAxeText();
                }
            }

        });

        this.windowChangeNom.pack();

        int ecartY = this.tabAxes.getBoundsAt(index).height - this.text.getBounds().height + 2;
        int ecartX = this.tabAxes.getBoundsAt(index).width - this.text.getBounds().width;

        this.windowChangeNom.setLocation(comp.getLocationOnScreen().x + this.tabAxes.getBoundsAt(index).getLocation().x + ecartX / 2, comp.getLocationOnScreen().y
                + this.tabAxes.getBoundsAt(index).getLocation().y + ecartY / 2);

        this.windowChangeNom.setVisible(true);
    }

    /**
     * @param index
     * @param text
     */
    private synchronized void validAxeText() {
        if (this.text == null || this.windowChangeNom == null)
            return;

        modifierAxe(this.editedAxeIndex, this.text.getText());
        this.windowChangeNom.setVisible(false);
        this.windowChangeNom.dispose();
        this.editedAxeIndex = -1;
        this.text = null;
    }

    /**
     * modifier le nom de l'onglet
     * 
     * @param axeIndex : numero de l'onglet
     * @param nom : nouveau nom
     */
    private void modifierAxe(int axeIndex, String nom) {
        if (axeIndex < 0)
            return;
        ((Axe) this.axes.get(Integer.parseInt(this.vecteurTabAxe.get(axeIndex).toString()))).setNom(nom);
        this.tabAxes.setTitleAt(axeIndex, nom);

    }

    /**
     * Ajouter un onglet
     * 
     * @param nom
     */
    public void ajouterAxe(String nom) {

        RepartitionAxeAnalytiquePanel repAxePanel = new RepartitionAxeAnalytiquePanel(1);

        if (nom.trim().length() == 0) {
            this.tabAxes.addTab("Nouvel axe", repAxePanel);
        } else {
            this.tabAxes.addTab(nom, repAxePanel);
        }

        this.repartitionsAxe.add(repAxePanel.getRepartitions());
        this.repartitionElemsAxe.add(repAxePanel.getRepartitionElems());
        this.postesAxe.add(repAxePanel.getPostes());

        this.vecteurTabAxe.add(this.tabAxes.getTabCount() - 1, new String(String.valueOf(this.axes.size())));
        this.axes.add(new Axe(1, this.tabAxes.getTitleAt(this.tabAxes.getTabCount() - 1), true));
    }

    /**
     * Supprimer un onglet
     * 
     * @param axeSelect : numero de l'axe
     */
    private void supprimerAxe(int axeSelect) {

        // int axeSelect =
        // Integer.parseInt(this.vecteurTabAxe.get(this.tabAxes.getSelectedIndex()).toString());

        // Test si l'axe contient une repartition deja associe à un compte
        // SELECT ID, ID_COMPTE FROM ASSOCIATION WHERE ID.AXE = id
        SQLTable assocTable = getTable().getBase().getTable("ASSOCIATION_COMPTE_ANALYTIQUE");

        SQLBase base = getTable().getBase();
        SQLSelect selAssoc = new SQLSelect(base);
        selAssoc.addSelect(assocTable.getField("ID"));
        selAssoc.addSelect(assocTable.getField("ID_COMPTE_PCE"));
        selAssoc.setWhere(assocTable.getField("ID_AXE_ANALYTIQUE"), "=", ((Axe) this.axes.get(axeSelect)).getId());

        String reqAssoc = selAssoc.asString();
        Object obAssoc = getTable().getBase().getDataSource().execute(reqAssoc, new ArrayListHandler());

        List myListAssoc = (List) obAssoc;

        if (myListAssoc.size() != 0) {
            System.out.println("La répartition est affectée à un compte.");
            ValiderSuppressionAxeFrame validFrame = new ValiderSuppressionAxeFrame(this.a, axeSelect);

            validFrame.pack();
            validFrame.setVisible(true);

        } else {
            deleteAxe(axeSelect);
        }
    }

    public void deleteAxe(int axeSelect) {
        int j;
        ((Axe) this.axes.get(axeSelect)).setSuppression(true);

        for (j = this.tabAxes.getSelectedIndex(); j < this.tabAxes.getTabCount() - 1; j++) {

            this.vecteurTabAxe.set(j, new String(this.vecteurTabAxe.get(j + 1).toString()));
        }

        // on recupere le vecteur de repartition associé à l'axe
        Vector repartitions = (Vector) this.repartitionsAxe.get(axeSelect);

        for (j = 0; j < repartitions.size(); j++) {
            ((Repartition) repartitions.get(j)).setSuppression(true);
        }

        // on recupere les éléments de répartitions de l'axe
        Vector repartitionElems = (Vector) this.repartitionElemsAxe.get(axeSelect);

        for (j = 0; j < repartitionElems.size(); j++) {
            ((RepartitionElement) repartitionElems.get(j)).setSuppression(true);
        }

        // on recupere les postes de l'axe
        Vector post = (Vector) this.postesAxe.get(axeSelect);

        for (j = 0; j < post.size(); j++) {
            ((Poste) post.get(j)).setSuppression(true);
        }

        this.tabAxes.remove(this.tabAxes.getSelectedIndex());
        // tabAxes.setEnabledAt(tabAxes.getSelectedIndex(), false);

        System.out.println("suppression de tab numero --> " + this.tabAxes.getSelectedIndex() + " numero Axe : " + axeSelect);
    }

    /***********************************************************************************************
     * * Validation des données dans la table
     **********************************************************************************************/
    private void supprimerAxeTable(Axe axe) {

        SQLRowValues vals = new SQLRowValues(this.getTable());
        vals.put(this.getTable().getArchiveField().getName(), new Integer(1));
        try {
            vals.update(axe.getId());
        } catch (SQLException e) {
            System.out.println("Erreur suppression Axe " + axe.getId());
        }

        System.out.println("Axe Supprimé  -->" + axe.toString());
    }

    private void creerAxeTable(Axe axe) {

        // axe.setId (INSERT INTO AXE_ANALYTIQUE (colonnes) VALUES ())

        Map m = new HashMap();
        m.put("NOM", axe.getNom());

        SQLRowValues val = new SQLRowValues(this.getTable(), m);

        try {

            SQLRow row = val.insert();
            System.out.println("Row ID -->" + row.getID());
            axe.setId(row.getInt("ID"));
        } catch (SQLException e) {
            System.out.println("Error insert row in " + this.getTable().toString());
        }

        // mise à jour du vecteur
        axe.setCreation(false);
        axe.setModif(false);

        System.out.println("Axe Nouveau  -->" + axe.toString());
    }

    private void modifierAxeTable(Axe axe) {

        // UPDATE AXE_ANALYTIQUE SET column = axe.get...(), ... WHERE id = axe.getId()
        SQLRowValues vals = new SQLRowValues(this.getTable());
        vals.put("NOM", axe.getNom());

        try {
            vals.update(axe.getId());
        } catch (SQLException e) {

            System.err.println("Erreur modification Axe " + axe.getId());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        axe.setCreation(false);
        axe.setModif(false);

        System.out.println("Axe Modifié  -->" + axe.toString());
    }

    private void supprimerRepartitionTable(Repartition rep) {

        SQLRowValues vals = new SQLRowValues(getTable().getBase().getTable("REPARTITION_ANALYTIQUE"));
        vals.put(getTable().getBase().getTable("REPARTITION_ANALYTIQUE").getArchiveField().getName(), new Integer(1));

        // Test si la repartition n'est pas deja associe à un compte
        // SELECT ID, ID_COMPTE FROM ASSOCIATION WHERE ID.REP = id
        SQLTable assocTable = getTable().getBase().getTable("ASSOCIATION_COMPTE_ANALYTIQUE");
        SQLBase base = assocTable.getBase();
        SQLSelect selAssoc = new SQLSelect(base);
        selAssoc.addSelect(assocTable.getField("ID"));
        selAssoc.addSelect(assocTable.getField("ID_COMPTE_PCE"));
        selAssoc.setWhere(assocTable.getField("ID_REPARTITION_ANALYTIQUE"), "=", rep.getId());

        String reqAssoc = selAssoc.asString();
        Object obAssoc = getTable().getBase().getDataSource().execute(reqAssoc, new ArrayListHandler());

        List myListAssoc = (List) obAssoc;

        if (myListAssoc.size() != 0) {
            SQLRowValues valsAssoc = new SQLRowValues(getTable().getBase().getTable("ASSOCIATION_COMPTE_ANALYTIQUE"));
            valsAssoc.put("ARCHIVE", 1);
            try {

                for (int i = 0; i < myListAssoc.size(); i++) {
                    Object[] objTmp = (Object[]) myListAssoc.get(i);
                    valsAssoc.update(Integer.parseInt(objTmp[0].toString()));
                }

            } catch (SQLException e) {
                System.err.println("Erreur suppression association ");
                e.printStackTrace();
            }
        }

        try {
            vals.update(rep.getId());
        } catch (SQLException e) {
            System.err.println("Erreur suppression Repartition " + rep.getId());
            e.printStackTrace();
        }

        System.out.println("Rep Supprimée  -->" + rep.toString());
    }

    private void creerRepartitionTable(Repartition rep) {
        // int new_id = INSERT INTO REPARTITION_ANALYTIQUE (column) VALUES
        // mettre à jour le vecteur
        // mapRepartitons.put(String.valueOf(rep.getId()), new_id);

        Map m = new HashMap();
        m.put("NOM", rep.getNom());

        SQLRowValues val = new SQLRowValues(getTable().getBase().getTable("REPARTITION_ANALYTIQUE"), m);

        try {

            SQLRow row = val.insert();
            System.out.println("Row ID -->" + row.getID());
            this.mapRepartitons.put(new Integer(rep.getId()), new Integer(row.getID()));
            rep.setId(row.getID());
        } catch (SQLException e) {
            System.err.println("Error insert row in " + val.getTable().getName());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        rep.setCreation(false);
        rep.setModif(false);

        System.out.println("Rep Crée  -->" + rep.toString());
    }

    private void modifierRepartitionTable(Repartition rep) {

        SQLRowValues vals = new SQLRowValues(getTable().getBase().getTable("REPARTITION_ANALYTIQUE"));
        vals.put("NOM", rep.getNom());
        try {
            vals.update(rep.getId());
        } catch (SQLException e) {
            System.err.println("Erreur modification Repartition " + rep.getId());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        rep.setCreation(false);
        rep.setModif(false);

        System.out.println("Répartition Modifié  -->" + rep.toString());
    }

    private void creerPosteTable(Poste p, int id_axe) {

        final Map<String, Object> m = new HashMap<String, Object>();
        m.put("NOM", p.getNom());
        m.put("ID_AXE_ANALYTIQUE", new Integer(id_axe));

        final SQLRowValues val = new SQLRowValues(getTable().getBase().getTable("POSTE_ANALYTIQUE"), m);

        try {

            final SQLRow row = val.insert();

            this.mapPostes.put(p.getId(), row.getID());

            p.setId(row.getID());
            p.setIdAxe(id_axe);
        } catch (SQLException e) {
            System.err.println("Error insert row in " + val.getTable().getName());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        p.setCreation(false);
        p.setModif(false);

        System.out.println("Poste Crée  -->" + p.toString());
    }

    private void supprimerPosteTable(Poste p) {

        SQLRowValues vals = new SQLRowValues(getTable().getBase().getTable("POSTE_ANALYTIQUE"));
        vals.put(getTable().getBase().getTable("POSTE_ANALYTIQUE").getArchiveField().getName(), new Integer(1));
        try {
            vals.update(p.getId());
        } catch (SQLException e) {
            System.err.println("Erreur suppression Poste " + p.getId());
            e.printStackTrace();
        }

        System.out.println("Poste Supprimée  -->" + p.toString());
    }

    private void modifierPosteTable(Poste p) {

        SQLRowValues vals = new SQLRowValues(getTable().getBase().getTable("POSTE_ANALYTIQUE"));
        vals.put("NOM", p.getNom());
        try {
            vals.update(p.getId());
        } catch (SQLException e) {
            System.err.println("Erreur modification Poste " + p.getId());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        p.setCreation(false);
        p.setModif(false);

        System.out.println("Poste Modifié  -->" + p.toString());
    }

    private void creerRepartitionElementTable(RepartitionElement repElem) {

        Map m = new HashMap();
        m.put("ID_REPARTITION_ANALYTIQUE", this.mapRepartitons.get(new Integer(repElem.getIdRep())));
        m.put("ID_POSTE_ANALYTIQUE", this.mapPostes.get(new Integer(repElem.getIdPoste())));
        m.put("TAUX", new Float(repElem.getTaux()));

        SQLRowValues val = new SQLRowValues(getTable().getBase().getTable("REPARTITION_ANALYTIQUE_ELEMENT"), m);

        try {

            if (val.getInvalid() == null) {
                SQLRow row = val.insert();
                System.out.println("Row ID -->" + row.getID());
                repElem.setId(row.getID());
                repElem.setIdRep(((Integer) this.mapRepartitons.get(new Integer(repElem.getIdRep()))).intValue());
                repElem.setIdPoste(((Integer) this.mapPostes.get(new Integer(repElem.getIdPoste()))).intValue());
            } else {
                System.out.println("--------------------------> Element supprimé");
                repElem.setDeleted(true);
            }

        } catch (SQLException e) {
            System.err.println("Error insert row in " + val.getTable().getName());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        repElem.setCreation(false);
        repElem.setModif(false);

        System.out.println("RepElement Crée  -->" + repElem.toString());
    }

    private void supprimerRepartitionElementTable(RepartitionElement repElem) {

        SQLRowValues vals = new SQLRowValues(getTable().getBase().getTable("REPARTITION_ANALYTIQUE_ELEMENT"));
        vals.put(getTable().getBase().getTable("REPARTITION_ANALYTIQUE_ELEMENT").getArchiveField().getName(), new Integer(1));
        try {
            vals.update(repElem.getId());
        } catch (SQLException e) {
            System.err.println("Erreur suppression Repartition Elem " + repElem.getId());
            e.printStackTrace();
        }

        System.out.println("RepElement Supprimée  -->" + repElem.toString());
    }

    private void modifierRepartitionElementTable(RepartitionElement repElem) {

        SQLRowValues vals = new SQLRowValues(getTable().getBase().getTable("REPARTITION_ANALYTIQUE_ELEMENT"));
        vals.put("TAUX", new Float(repElem.getTaux()));
        try {
            vals.update(repElem.getId());
        } catch (SQLException e) {
            System.err.println("Erreur suppression Repartition Elem " + repElem.getId());
            e.printStackTrace();
        }

        // mise à jour du vecteur
        repElem.setCreation(false);
        repElem.setModif(false);

        System.out.println("RepElement Modifié  -->" + repElem.toString());
    }

    private void setVal() {

        for (int i = 0; i < this.axes.size(); i++) {
            // on récupére un axe
            Axe axe = this.axes.get(i);
            this.mapPostes = new HashMap<Integer, Integer>();
            this.mapRepartitons = new HashMap<Integer, Integer>();

            if ((!(axe.getSuppression() && axe.getCreation())) || (!axe.getDeleted())) {
                if (axe.getSuppression()) {

                    supprimerAxeTable(axe);
                } else if (axe.getCreation()) {

                    creerAxeTable(axe);
                } else {

                    if (axe.getModif()) {

                        modifierAxeTable(axe);
                    }
                }
            }

            // on recupere le vecteur de repartition associé à l'axe
            List<Repartition> repartitions = this.repartitionsAxe.get(i);

            for (int j = 0; j < repartitions.size(); j++) {
                Repartition rep = repartitions.get(j);

                this.mapRepartitons.put(new Integer(rep.getId()), new Integer(rep.getId()));

                if ((!(rep.getSuppression() && rep.getCreation())) || (!rep.getDeleted())) {
                    if (rep.getCreation()) {

                        creerRepartitionTable(rep);
                    } else {
                        if (rep.getSuppression()) {

                            supprimerRepartitionTable(rep);
                        } else {
                            if (rep.getModif()) {

                                modifierRepartitionTable(rep);
                            }
                        }
                    }
                }

                // System.out.println("Map répartition : " + idtmp + " --> " +
                // this.mapRepartitons.get(Integer.valueOf(idtmp)));
            }

            // on recupere les postes de l'axe
            final List<Poste> postes = this.postesAxe.get(i);

            for (int j = 0; j < postes.size(); j++) {
                Poste p = postes.get(j);

                this.mapPostes.put(p.getId(), p.getId());

                if ((!(p.getSuppression() && p.getCreation())) || (!p.getDeleted())) {
                    if (p.getCreation()) {

                        creerPosteTable(p, axe.getId());
                    } else {
                        if (p.getSuppression()) {

                            supprimerPosteTable(p);
                        } else {
                            if (p.getModif()) {

                                modifierPosteTable(p);
                            }
                        }
                    }
                }

                // System.out.println("Map postes : " + idtmp + " --> " +
                // this.mapPostes.get(Integer.valueOf(idtmp)));
            }

            // on recupere les éléments de répartitions de l'axe
            final List<RepartitionElement> repartitionElems = this.repartitionElemsAxe.get(i);

            for (int j = 0; j < repartitionElems.size(); j++) {
                RepartitionElement repElem = repartitionElems.get(j);

                if ((!(repElem.getSuppression() && repElem.getCreation())) || (!repElem.getDeleted())) {
                    if (repElem.getCreation()) {

                        creerRepartitionElementTable(repElem);
                    } else {
                        if (repElem.getSuppression()) {

                            supprimerRepartitionElementTable(repElem);
                        } else {
                            if (repElem.getModif()) {

                                modifierRepartitionElementTable(repElem);
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkID() {

        final int size = this.axes.size();
        for (int i = 0; i < size; i++) {

            // on recupere les éléments de répartitions de l'axe
            final List<RepartitionElement> repartitionElems = this.repartitionElemsAxe.get(i);
            final int repartitionElemsCount = repartitionElems.size();
            final List<Poste> postes = this.postesAxe.get(i);
            final int postesCount = postes.size();
            final List<Repartition> repartitions = this.repartitionsAxe.get(i);
            final int repartitionsCount = repartitions.size();

            for (int j = 0; j < repartitionElemsCount; j++) {
                final RepartitionElement repElem = repartitionElems.get(j);
                if (!repElem.getSuppression() || !repElem.getDeleted()) {

                    for (int k = 0; k < postesCount; k++) {
                        final Poste poste = postes.get(k);
                        if (repElem.getIdPoste() == poste.getId() && poste.getSuppression() && poste.getDeleted()) {
                            System.out.println("Probleme repElem " + repElem.toString());
                        }
                    }

                    for (int k = 0; k < repartitionsCount; k++) {
                        final Repartition repartition = repartitions.get(k);
                        if (repElem.getIdRep() == repartition.getId() && repartition.getSuppression() && repartition.getDeleted()) {
                            System.out.println("Probleme repElem " + repElem.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".analytic";
    }
}
