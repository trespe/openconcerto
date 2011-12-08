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
 
 package org.openconcerto.erp.core.finance.accounting.ui;

import org.openconcerto.erp.core.finance.accounting.model.AnalytiqueModel;
import org.openconcerto.erp.element.objet.Poste;
import org.openconcerto.erp.element.objet.Repartition;
import org.openconcerto.erp.element.objet.RepartitionElement;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.table.CloseTableHeaderRenderer;
import org.openconcerto.ui.table.TablePopupMouseListener;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

public class RepartitionAxeAnalytiquePanel extends JPanel {

    private int idAxe;
    private AnalytiqueModel model;
    private JTable listeRepartitions;
    private RepartitionAxeAnalytiquePanel repPanel = this;
    private JWindow windowChangeNom;
    private JTextField text;
    private AjouterPosteAnalytiqueFrame ajoutPosteFrame = null;

    public RepartitionAxeAnalytiquePanel(int idAxe) {
        this.idAxe = idAxe;
        uiInit();
    }

    public List<Repartition> getRepartitions() {
        return this.model.getRepartition();
    }

    public List<RepartitionElement> getRepartitionElems() {
        return this.model.getRepartitionElem();
    }

    public List<Poste> getPostes() {
        return this.model.getPostes();
    }

    public int getIdAxe() {
        return this.idAxe;
    }

// TODO from UCDetector: Change visibility of Method "RepartitionAxeAnalytiquePanel.uiInit()" to private
    public void uiInit() { // NO_UCD
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        /*******************************************************************************************
         * * Ajouter une répartition
         ******************************************************************************************/
        JButton boutonAjoutRep = new JButton("Ajout Répartition");

        boutonAjoutRep.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {

                model.addElement();
                System.out.println("Ajout d'une répartiton");
            }
        });

        this.add(boutonAjoutRep, c);

        /*******************************************************************************************
         * Supprimer une répartition
         ******************************************************************************************/
        JButton boutonSupprRep = new JButton("Suppr Répartition");

        boutonSupprRep.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {

                model.removeElement(listeRepartitions.getSelectedRows());
            }
        });
        c.gridx++;
        this.add(boutonSupprRep, c);

        /*******************************************************************************************
         * * Ajouter un poste
         ******************************************************************************************/
        JButton boutonAjoutPoste = new JButton("Ajout Poste");

        boutonAjoutPoste.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {

                System.out.println("Ajout d'un poste");
                if (ajoutPosteFrame == null) {
                    ajoutPosteFrame = new AjouterPosteAnalytiqueFrame(model, repPanel);
                    ajoutPosteFrame.pack();
                }
                ajoutPosteFrame.setVisible(true);

            }
        });
        c.gridx++;
        this.add(boutonAjoutPoste, c);

        /*******************************************************************************************
         * Supprimer un poste
         ******************************************************************************************/
        JButton boutonSupprPoste = new JButton("Suppr Poste");

        boutonSupprPoste.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {

                disableAffichageColonne();
                model.removePoste(listeRepartitions.getSelectedColumn());
                setAffichageColonne();
                System.out.println("Suppression d'un poste");
            }
        });
        c.gridx++;
        this.add(boutonSupprPoste, c);

        /*******************************************************************************************
         * Liste des répartitions
         ******************************************************************************************/
        // on crée le modéle associé à l'axe
        System.out.println("Creation des repartitions de l'axe " + this.idAxe);
        this.model = new AnalytiqueModel(this.idAxe);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        this.listeRepartitions = new JTable(this.model) {
            // Pour remplir tout l'espace
            public boolean getScrollableTracksViewportHeight() {
                return getPreferredSize().height < getParent().getHeight();
            }
        };

        TablePopupMouseListener.add(this.listeRepartitions, new ITransformer<MouseEvent, JPopupMenu>() {
            @Override
            public JPopupMenu transformChecked(MouseEvent input) {
                final JPopupMenu res = new JPopupMenu();
                final JTable table = (JTable) input.getSource();
                for (final Action a : getActions(table, table.getSelectedRow())) {
                    res.add(a);
                }
                return res;
            }
        });

        JScrollPane scrollPane = new JScrollPane(this.listeRepartitions);

        this.add(scrollPane, c);

        this.listeRepartitions.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(AncestorEvent event) {
                if (windowChangeNom != null) {
                    windowChangeNom.setVisible(false);
                    windowChangeNom.dispose();
                }
            }

            public void ancestorRemoved(AncestorEvent event) {
                if (windowChangeNom != null) {
                    windowChangeNom.setVisible(false);
                    windowChangeNom.dispose();
                }
            }

            public void ancestorMoved(AncestorEvent event) {
                if (windowChangeNom != null) {
                    windowChangeNom.setVisible(false);
                    windowChangeNom.dispose();
                }
            }
        });

        setAffichageColonne();
    }

    /***********************************************************************************************
     * Action click droit sur les éléments de la liste
     **********************************************************************************************/
    public List<Action> getActions(final JTable table, final int row) {
        Action a = new AbstractAction("Supprimer ligne " + row) {

            public void actionPerformed(ActionEvent e) {

                model.removeElement(row);
                System.out.println("Suppression row:" + row);
            }
        };

        Action a2 = new AbstractAction("Ajouter une ligne") {

            public void actionPerformed(ActionEvent e) {

                model.addElement();
                System.out.println("Ajout d'une ligne");
            }
        };

        return Arrays.asList(a, a2);
    }

    /***********************************************************************************************
     * * Mise à jour des entètes de colonnes
     **********************************************************************************************/
    public void setAffichageColonne() {

        disableAffichageColonne();

        for (int i = 1; i < this.listeRepartitions.getColumnCount(); i++) {

            final int col = i;
            Vector actions = new Vector();

            // Action sur click droit
            Action a = new AbstractAction("Supprimer le poste") {

                public void actionPerformed(ActionEvent e) {
                    if (listeRepartitions.getColumnCount() > 2) {
                        System.out.println("Action efface colonne : " + col);

                        if (windowChangeNom != null) {
                            windowChangeNom.setVisible(false);
                            windowChangeNom.dispose();
                        }

                        disableAffichageColonne();
                        model.removePoste(col);
                        setAffichageColonne();
                    }
                }
            };
            actions.add(a);

            Action a1 = new AbstractAction("Ajouter un poste") {

                public void actionPerformed(ActionEvent e) {

                    if (ajoutPosteFrame == null) {
                        ajoutPosteFrame = new AjouterPosteAnalytiqueFrame(model, repPanel);
                        ajoutPosteFrame.pack();
                    }
                    ajoutPosteFrame.setVisible(true);

                    System.out.println("Action ajout");
                    if (windowChangeNom != null) {
                        windowChangeNom.setVisible(false);
                        windowChangeNom.dispose();
                    }
                }
            };

            Action a2 = new AbstractAction("Renommer le poste") {

                public void actionPerformed(ActionEvent e) {

                    actionModifierPoste(col);
                }
            };

            actions.add(a1);
            actions.add(a2);

            this.listeRepartitions.getColumnModel().getColumn(i).setHeaderRenderer(new CloseTableHeaderRenderer(a, actions));
        }
    }

    public void disableAffichageColonne() {

        for (int j = 1; j < this.listeRepartitions.getColumnCount(); j++) {
            System.out.println("Destroy : " + j);

            if ((this.listeRepartitions.getColumnModel().getColumn(j).getHeaderRenderer() != null)
                    && (this.listeRepartitions.getColumnModel().getColumn(j).getHeaderRenderer() instanceof CloseTableHeaderRenderer)) {
                System.out.println("Desactivation du listenner sur " + j);
                ((CloseTableHeaderRenderer) (this.listeRepartitions.getColumnModel().getColumn(j).getHeaderRenderer())).destroy();
                this.listeRepartitions.getColumnModel().getColumn(j).setHeaderRenderer(null);
            }
        }
    }

    /**
// TODO from UCDetector: Change visibility of Method "RepartitionAxeAnalytiquePanel.actionModifierPoste(int)" to private
     * Changer le nom de la colonne // NO_UCD
     * 
     * @param col : numero de la colonne à modifier
     */
    public void actionModifierPoste(final int col) {

        if (col == -1) {
            return;
        }

        KeyListener textKL = new KeyAdapter() {

            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    disableAffichageColonne();
                    model.modifierNomPoste(col, text.getText());
                    windowChangeNom.setVisible(false);
                    windowChangeNom.dispose();
                    setAffichageColonne();
                }
            }
        };

        FocusListener textFL = new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                // System.out.println("text focus lost");
                if (windowChangeNom != null) {

                    windowChangeNom.setVisible(false);
                    windowChangeNom.dispose();
                }
            }
        };

        JFrame frame = (JFrame) SwingUtilities.getRoot(this);
        this.windowChangeNom = new JWindow(frame);

        this.windowChangeNom.setLayout(new GridBagLayout());

        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;

        this.text = new JTextField(" " + this.listeRepartitions.getColumnName(col) + " ");

        this.text.setEditable(true);
        this.windowChangeNom.add(this.text, c);
        this.text.setBorder(null);
        // text.setBackground(getBackground());

        this.text.addFocusListener(textFL);
        this.text.addKeyListener(textKL);

        this.windowChangeNom.pack();

        int x = 0;
        for (int i = 0; i < col; i++) {
            x += this.listeRepartitions.getColumnModel().getColumn(i).getWidth();
        }

        int y = this.listeRepartitions.getTableHeader().getY();
        int centreY = (this.listeRepartitions.getTableHeader().getHeight() - this.windowChangeNom.getHeight()) / 2;

        x += 4;

        this.windowChangeNom.setLocation(this.listeRepartitions.getTableHeader().getLocationOnScreen().x + x, this.listeRepartitions.getTableHeader().getLocationOnScreen().y + y + centreY);
        this.windowChangeNom.setVisible(true);
    }
}
