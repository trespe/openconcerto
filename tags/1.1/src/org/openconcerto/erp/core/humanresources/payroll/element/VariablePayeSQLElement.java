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
 
 package org.openconcerto.erp.core.humanresources.payroll.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.SQLJavaEditor;
import org.openconcerto.erp.core.humanresources.payroll.component.FormuleTreeNode;
import org.openconcerto.erp.core.humanresources.payroll.component.VariableRowTreeNode;
import org.openconcerto.erp.core.humanresources.payroll.component.VariableTree;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.TreePath;

import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.jedit.CTokenMarker;

// FIXME retirer le scrolling de l'edit frame pour scroller uniquement sur l'arbre des variables
public class VariablePayeSQLElement extends ConfSQLElement {

    private final static ValidState VAR_ALREADY_EXIST = ValidState.createInvalid("Cette variable existe déjà !");
    private final static ValidState VAR_NAME_NOT_CORRECT = ValidState.createInvalid("Nom de variable incorrect !");
    private final static ValidState VAR_NO_NAME = ValidState.createInvalid("Aucun nom attribué !");

    private static SQLTable tableVarSal = null;

    private static SQLTable getTableVarSal() {
        if (tableVarSal == null)
            tableVarSal = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getTable("VARIABLE_SALARIE");
        return tableVarSal;
    }

    public VariablePayeSQLElement() {
        super("VARIABLE_PAYE", "une variable de paye", "variables de paye");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("CATEGORIE");
        l.add("VALEUR");
        l.add("FORMULE");
        return l;
    }

    @Override
    public Set<String> getInsertOnlyFields() {
        Set<String> s = new HashSet<String>();
        s.add("NOM");
        return s;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    static final boolean isForbidden(final String code) {
        final List<String> l = getForbiddenVarName();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).trim().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    // Liste des variables deja definit
    // L'utilisateur ne peut pas utiliser ces noms
    public static final List<String> getForbiddenVarName() {

        List<String> l = new ArrayList<String>();

        for (SQLField field : getTableVarSal().getContentFields()) {

            // String field = i.next().toString().trim();
            // field = field.substring(field.indexOf('.') + 1, field.length() - 1);
            l.add(field.getName());
        }

        l.add("PAT");
        l.add("SAL");
        l.add("BASE");
        l.add("MONTANT");
        l.add("TAUX");

        l.add("ACOMPTE");
        l.add("DUREE_HEBDO");
        l.add("DUREE_MOIS");
        l.add("SALAIRE_MOIS");
        l.add("TAUX_AT");
        l.add("CONGES_PAYES");
        l.add("NB_ENFANTS");
        l.add("NB_PERS_A_CHARGE");
        l.add("PAT");
        l.add("SAL");
        l.add("BASE");
        l.add("MONTANT");

        l.add("SAL_BRUT");
        l.add("COT_PAT");
        l.add("COT_SAL");
        l.add("NET_IMP");
        l.add("NET_A_PAYER");
        l.add("CSG");

        l.add("CONGES_ACQUIS");
        l.add("CSG_C");
        l.add("CONGES_PRIS");
        l.add("RESTANT");
        l.add("COT_PAT_C");
        l.add("COT_SAL_C");
        l.add("HEURE_ABS");
        l.add("HEURE_TRAV");
        l.add("HEURE_110");
        l.add("HEURE_125");
        l.add("HEURE_150");
        l.add("HEURE_200");
        l.add("SAL_BRUT_C");
        l.add("NET_A_PAYER_C");
        l.add("NET_IMP_C");
        return l;
    }

    // Map qui contient la structure de l'arbre
    public static final Map<String, List<?>> getMapTree() {

        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLBase baseCommon = Configuration.getInstance().getBase();
        SQLTable tableVar = baseCommon.getTable("VARIABLE_PAYE");
        SQLTable tableCumulsConges = base.getTable("CUMULS_CONGES");
        SQLTable tableCumulsPaye = base.getTable("CUMULS_PAYE");

        Map<String, List<?>> mapTree = new HashMap<String, List<?>>();

        // Variables propre au salarié
        List<SQLField> varSal = new ArrayList<SQLField>();

        for (SQLField field : getTableVarSal().getContentFields()) {

            varSal.add(field);
        }
        for (SQLField field : tableCumulsConges.getContentFields()) {

            varSal.add(field);
        }

        for (SQLField field : tableCumulsPaye.getContentFields()) {

            varSal.add(field);
        }

        mapTree.put("Infos période", varSal);

        List<SQLField> l = new ArrayList<SQLField>();
        SQLTable tableInfos = base.getTable("INFOS_SALARIE_PAYE");
        l.add(tableInfos.getField("DUREE_HEBDO"));
        l.add(tableInfos.getField("DUREE_MOIS"));
        l.add(tableInfos.getField("SALAIRE_MOIS"));
        l.add(tableInfos.getField("TAUX_AT"));
        l.add(tableInfos.getField("CONGES_PAYES"));
        mapTree.put("Contrat salarié", l);

        List<SQLField> l2 = new ArrayList<SQLField>();
        SQLTable tableFichePaye = base.getTable("FICHE_PAYE");
        l2.add(tableFichePaye.getField("CONGES_ACQUIS"));
        l2.add(tableFichePaye.getField("ACOMPTE"));
        l2.add(tableFichePaye.getField("SAL_BRUT"));
        l2.add(tableFichePaye.getField("COT_PAT"));
        l2.add(tableFichePaye.getField("COT_SAL"));
        l2.add(tableFichePaye.getField("NET_IMP"));
        l2.add(tableFichePaye.getField("NET_A_PAYER"));
        l2.add(tableFichePaye.getField("CSG"));
        mapTree.put("Contenu paye", l2);

        /*
         * List lEtat = new ArrayList(); SQLTable tableEtat =
         * Configuration.getInstance().getBase().getTable("ETAT_CIVIL");
         * lEtat.add(tableEtat.getField("NB_ENFANTS"));
         * lEtat.add(tableEtat.getField("NB_PERS_A_CHARGE")); mapTree.put("Salarie.etat_civil",
         * lEtat);
         */

        SQLSelect selAllVarID = new SQLSelect(tableVar.getBase());

        selAllVarID.addSelect(tableVar.getField("ID"));
        selAllVarID.addRawOrder("LENGTH(\"VARIABLE_PAYE\".\"NOM\") DESC");

        String reqAllVarID = selAllVarID.asString();
        Object[] objKeysRowVar = ((List) tableVar.getBase().getDataSource().execute(reqAllVarID, new ArrayListHandler())).toArray();

        List<SQLRow> lVar = new ArrayList<SQLRow>();
        for (int i = 0; i < objKeysRowVar.length; i++) {
            Object[] tmp = (Object[]) objKeysRowVar[i];
            lVar.add(tableVar.getRow(Integer.parseInt(tmp[0].toString())));
        }

        if (lVar.size() > 0) {
            mapTree.put("Variables", lVar);
        }

        List<Map<String, String>> fonction = new ArrayList<Map<String, String>>();
        Map<String, String> listFonction = new HashMap<String, String>();
        listFonction.put("Minimum", "Math.min()");
        listFonction.put("Maximum", "Math.max()");
        listFonction.put("Valeur absolue", "Math.abs()");
        listFonction.put("Arrondi", "Math.round()");

        fonction.add(listFonction);

        mapTree.put("Fonctions", fonction);
        // Chargement des variables dans l'éditeur
        // System.out.println(mapTree);

        return mapTree;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new BaseSQLComponent(this) {

            private ValidState validVarName;
            private JRadioButton radioVal = new JRadioButton("Valeur");
            private JRadioButton radioFormule = new JRadioButton("Formule");

            private final JTextField textValeur = new JTextField();
            // private final ITextArea textFormule = new ITextArea();
            private final VariableTree treeVariable = new VariableTree();
            private final JTextField textNom = new JTextField();
            private final JLabel labelWarningBadVar = new JLabelWarning();
            private ElementComboBox comboSelSal;
            private EditFrame edit = null;
            private final SQLJavaEditor textFormule = new SQLJavaEditor(getMapTree());

            public void addViews() {
                this.setLayout(new GridBagLayout());
                final GridBagConstraints c = new DefaultGridBagConstraints();

                this.validVarName = null;
                this.textFormule.setEditable(false);

                // Arbre des variables
                JScrollPane sc = new JScrollPane(this.treeVariable);
                sc.setPreferredSize(new Dimension(150, sc.getPreferredSize().height));

                this.treeVariable.addMouseListener(new MouseAdapter() {
                    public void mousePressed(final MouseEvent mE) {
                        if (mE.getButton() == MouseEvent.BUTTON3) {
                            JPopupMenu menuDroit = new JPopupMenu();

                            TreePath path = treeVariable.getClosestPathForLocation(mE.getPoint().x, mE.getPoint().y);

                            final Object obj = path.getLastPathComponent();

                            if ((obj == null) || !(obj instanceof VariableRowTreeNode)) {
                                return;
                            }

                            menuDroit.add(new AbstractAction("Editer") {
                                public void actionPerformed(ActionEvent e) {
                                    if (edit == null) {
                                        edit = new EditFrame(new VariablePayeSQLElement(), EditFrame.MODIFICATION);
                                    }

                                    System.err.println("Action performed");

                                    if (obj != null) {
                                        System.err.println("Object not null --> " + obj.toString());
                                        if (obj instanceof VariableRowTreeNode) {
                                            System.err.println("Object VariableRowTreeNode");
                                            VariableRowTreeNode varNode = (VariableRowTreeNode) obj;

                                            edit.selectionId(varNode.getID(), 1);
                                            edit.setVisible(true);
                                        }
                                    }
                                }
                            });
                            menuDroit.show((Component) mE.getSource(), mE.getPoint().x, mE.getPoint().y);
                        } else {
                            if (mE.getClickCount() == 2) {
                                TreePath path = treeVariable.getClosestPathForLocation(mE.getPoint().x, mE.getPoint().y);
                                Object obj = path.getLastPathComponent();

                                if (obj != null) {
                                    if (obj instanceof FormuleTreeNode) {
                                        final FormuleTreeNode n = (FormuleTreeNode) obj;

                                        int start = textFormule.getSelectionStart();
                                        String tmp = textFormule.getText();
                                        textFormule.setText(tmp.substring(0, start) + n.getTextValue() + tmp.substring(start, tmp.length()));
                                    }
                                }
                            }
                        }
                    }
                });

                JPanel panelDroite = new JPanel();
                panelDroite.setLayout(new GridBagLayout());

                // Categorie
                JTextField textCategorie = new JTextField();
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridheight = 1;
                c.gridx = 1;
                c.gridy = 0;
                JLabel labelCategorie = new JLabel("Catégorie");
                panelDroite.add(labelCategorie, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                panelDroite.add(textCategorie, c);
                c.gridwidth = 1;

                // Nom
                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridheight = 1;
                c.gridx = 1;
                c.gridy++;
                JLabel labelNom = new JLabel("Nom");
                panelDroite.add(labelNom, c);

                c.gridx++;
                c.weightx = 1;
                panelDroite.add(this.textNom, c);

                this.textNom.getDocument().addDocumentListener(new SimpleDocumentListener() {
                    @Override
                    public void update(DocumentEvent e) {
                        updateValidVarName();
                    }
                });

                c.gridx++;
                c.weightx = 0;
                panelDroite.add(this.labelWarningBadVar, c);

                // Description
                JLabel labelInfos = new JLabel(getLabelFor("INFOS"));
                ITextArea textInfos = new ITextArea();
                c.gridy++;
                c.gridx = 1;
                c.gridwidth = 1;
                c.weightx = 0;
                panelDroite.add(labelInfos, c);
                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1;
                c.weighty = 0;
                panelDroite.add(textInfos, c);

                // Valeur
                c.gridx = 1;
                c.gridy++;
                c.gridwidth = 1;
                c.weightx = 0;
                panelDroite.add(this.radioVal, c);

                c.gridx++;
                c.weightx = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                panelDroite.add(this.textValeur, c);

                c.gridwidth = 1;
                c.gridx = 1;
                c.gridy++;
                panelDroite.add(this.radioFormule, c);

                c.gridx++;
                c.weightx = 1;
                c.weighty = 1;
                c.fill = GridBagConstraints.BOTH;
                c.gridwidth = GridBagConstraints.REMAINDER;
                panelDroite.add(this.textFormule, c);
                c.gridwidth = 1;

                ButtonGroup group = new ButtonGroup();
                group.add(this.radioVal);
                group.add(this.radioFormule);

                this.radioVal.setSelected(true);
                setFormuleEnabled(false);

                this.radioVal.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        setFormuleEnabled(false);
                    }
                });
                this.radioFormule.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {

                        setFormuleEnabled(true);
                    }
                });

                c.gridy++;
                c.gridx = 1;
                c.weighty = 0;
                c.weightx = 0;
                c.fill = GridBagConstraints.HORIZONTAL;

                this.comboSelSal = new ElementComboBox(false);
                this.comboSelSal.init(new SalarieSQLElement());

                c.gridx++;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 0;
                panelDroite.add(this.comboSelSal, c);
                c.gridwidth = 1;

                JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sc, panelDroite);

                c.fill = GridBagConstraints.BOTH;
                c.gridx = 0;
                c.gridy = 0;
                c.weightx = 1;
                c.weighty = 1;
                this.add(split, c);

                this.addRequiredSQLObject(this.textNom, "NOM");
                this.addSQLObject(this.textValeur, "VALEUR");
                this.addSQLObject(this.textFormule, "FORMULE");
                this.addSQLObject(textCategorie, "CATEGORIE");
                this.addSQLObject(textInfos, "INFOS");

                this.comboSelSal.addValueListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        // TODO Auto-generated method stub
                        textFormule.setSalarieID(comboSelSal.getSelectedId());
                    }
                });
            }

            @Override
            public synchronized ValidState getValidState() {
                return super.getValidState().and(this.validVarName);
            }

            private void setFormuleEnabled(boolean b) {

                if (b) {
                    this.textValeur.setText("");
                } else {
                    this.textFormule.setText("");
                }

                this.textValeur.setEditable(!b);
                this.textValeur.setEnabled(!b);
                this.textFormule.setEditable(b);
                this.textFormule.setEnabled(b);
                this.treeVariable.setEnabled(b);
                this.treeVariable.setEditable(b);
            }

            private void setValidVarName(ValidState s) {
                if (!s.equals(this.validVarName)) {
                    this.validVarName = s;
                    final boolean warningVisible = !s.isValid();
                    if (warningVisible)
                        this.labelWarningBadVar.setText(s.getValidationText());
                    this.labelWarningBadVar.setVisible(warningVisible);
                    this.fireValidChange();
                }
            }

            private void updateValidVarName() {
                this.setValidVarName(this.computeValidVarName());
            }

            private ValidState computeValidVarName() {
                // on vérifie si la syntaxe de la variable est correct (chiffre lettre et _)
                final String varName = this.textNom.getText().trim();

                System.err.println("Verification de la validité du nom de la variable.");

                if (varName.length() == 0) {
                    return VAR_NO_NAME;
                }

                // ne contient que des chiffre lettre et _ et ne commence pas par un chiffre
                if (!isJavaVar(varName)) {
                    return VAR_NAME_NOT_CORRECT;
                }

                // on vérifie que la variable n'existe pas déja
                SQLSelect selAllVarName = new SQLSelect(getTable().getBase());

                selAllVarName.addSelect(VariablePayeSQLElement.this.getTable().getField("ID"));
                Where w = new Where(VariablePayeSQLElement.this.getTable().getField("NOM"), "=", varName);
                w = w.and(new Where(VariablePayeSQLElement.this.getTable().getKey(), "!=", getSelectedID()));
                selAllVarName.setWhere(w);

                String reqAllVarName = selAllVarName.asString();// + " AND '" + varName.trim() + "'
                // REGEXP VARIABLE_PAYE.NOM";
                Object[] objKeysRowName = ((List) getTable().getBase().getDataSource().execute(reqAllVarName, new ArrayListHandler())).toArray();

                if (objKeysRowName.length > 0) {
                    return VAR_ALREADY_EXIST;
                } else {

                    // Impossible de créer une variable du meme nom qu'un champ du salarie
                    if (isForbidden(varName))
                        return VAR_ALREADY_EXIST;

                    this.textFormule.setVarAssign(varName);
                    return ValidState.getTrueInstance();
                }
            }

            private boolean isJavaVar(String s) {
                if ((s.charAt(0) >= '0') && ((s.charAt(0) <= '9'))) {
                    System.err.println("Erreur la variable commence par un chiffre!!");
                    return false;
                } else {
                    for (int i = 0; i < s.length(); i++) {

                        if (!(((s.charAt(i) >= '0') && (s.charAt(i) <= '9')) || (s.charAt(i) >= 'a') && (s.charAt(i) <= 'z') || (s.charAt(i) >= 'A') && (s.charAt(i) <= 'Z') || (s.charAt(i) == '_'))) {
                            System.err.println("Erreur la variable contient un caractere incorrect!!");
                            return false;
                        }
                    }

                    return (!CTokenMarker.getKeywords().isExisting(s));
                }
            }

            @Override
            public void select(SQLRowAccessor r) {

                super.select(r);
                // System.err.println("Select RowAccess -------> " + r.getID() + " For Object " +
                // this.hashCode());
                if (r != null) {
                    if (r.getString("FORMULE").trim().length() == 0) {
                        this.radioVal.setSelected(true);
                        setFormuleEnabled(false);
                    } else {
                        this.radioFormule.setSelected(true);
                        setFormuleEnabled(true);
                    }

                    this.textFormule.setVarAssign(r.getString("NOM"));
                }

                this.updateValidVarName();
            }
        };
    }

    @Override
    protected void archive(SQLRow row, boolean cutLinks) throws SQLException {
        secureArchiveVariable(row.getID());
    }

    // Archive la variable si elle n'est pas utilisé dans une formule
    private void secureArchiveVariable(int id) throws SQLException {
        SQLRow row = getTable().getRow(id);

        // FIXME verifier que la variable n'est pas utilisée dans une rubrique
        if (row != null) {
            SQLSelect sel = new SQLSelect(getTable().getBase());
            sel.addSelect(getTable().getField("ID"));
            System.err.println("Check variable");
            sel.setWhere("VARIABLE_PAYE.FORMULE", "LIKE", "%" + row.getString("NOM") + "%");
            sel.andWhere(new Where(getTable().getField("ID"), "!=", id));

            String req = sel.asString();
            List l = (List) getTable().getBase().getDataSource().execute(req, new ArrayListHandler());
            if (l.size() == 0) {
                super.archive(getTable().getRow(id));
            } else {
                System.err.println("Suppression impossible, cette variable est référencée par une autre.");
                ExceptionHandler.handle("Suppression impossible, cette variable est référencée par une autre.");
            }

        } else {
            super.archive(getTable().getRow(id), true);
        }
    }
}
