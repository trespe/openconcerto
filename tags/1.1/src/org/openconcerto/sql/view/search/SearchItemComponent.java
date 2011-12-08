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
 
 package org.openconcerto.sql.view.search;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.view.search.TextSearchSpec.Mode;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.model.ListComboBoxModel;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;

public class SearchItemComponent extends JPanel {
    private static final String TOUT = "Tout";
    private static final Mode[] MODES = { Mode.CONTAINS, Mode.CONTAINS_STRICT, Mode.LESS_THAN, Mode.EQUALS, Mode.EQUALS_STRICT, Mode.GREATER_THAN };

    private JTextField textFieldRecherche = new JTextField(10);
    private final JComboBox comboColonnePourRecherche = new JComboBox(new ListComboBoxModel());
    private final JComboBox searchMode;
    private JCheckBox invertSearch = new JCheckBox("inverser");
    private JButton buttonAdd = new JButton("+");
    private JButton buttonRemove = new JButton();
    final SearchListComponent list;
    private String text = "";

    public SearchItemComponent(final SearchListComponent list) {
        super();
        this.list = list;
        this.setOpaque(false);
        // Initialisation de l'interface graphique
        this.searchMode = new JComboBox(new String[] { "Contient", "Contient exactement", "Est inférieur à", "Est égal à", "Est exactement égal à", "Est supérieur à", "Est vide" });
        uiInit();
    }

    private void uiInit() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 2, 0, 2);
        c.fill = GridBagConstraints.HORIZONTAL;

        // designation
        // don't just use DefaultListCellRenderer, it fails on some l&f
        final ListCellRenderer old = this.comboColonnePourRecherche.getRenderer();
        this.comboColonnePourRecherche.setRenderer(new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return old.getListCellRendererComponent(list, ((Tuple2<?, ?>) value).get0(), index, isSelected, cellHasFocus);
            }
        });
        // hand tuned for a IListPanel width of 1024px
        this.comboColonnePourRecherche.setMinimumSize(new Dimension(150, 20));
        add(this.comboColonnePourRecherche, c);
        c.gridx++;
        // contient
        this.searchMode.setMinimumSize(new Dimension(40, 20));
        add(this.searchMode, c);
        c.gridx++;
        // Texte de recherche
        c.weightx = 1;
        // about 10 characters
        this.textFieldRecherche.setMinimumSize(new Dimension(50, 20));
        add(this.textFieldRecherche, c);
        c.weightx = 0;
        c.gridx++;
        // inversion de la recherche
        this.invertSearch.setOpaque(false);
        if (!Boolean.getBoolean("org.openconcerto.ui.removeSwapSearchCheckBox")) {
            add(this.invertSearch, c);
        }
        // ajout d'un element de recherche
        c.gridx++;
        add(this.buttonAdd, c);
        // supprime un element de recherche
        c.gridx++;
        this.buttonRemove.setIcon(new ImageIcon(BaseSQLComponent.class.getResource("delete.png")));
        this.buttonRemove.setBorder(null);
        this.buttonRemove.setOpaque(false);
        this.buttonRemove.setBorderPainted(false);
        this.buttonRemove.setFocusPainted(false);
        this.buttonRemove.setContentAreaFilled(false);
        add(this.buttonRemove, c);

        initCombo();
        initSearchText();
        initInvertSearch();

        this.buttonAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchItemComponent.this.list.addNewSearchItem();
            }
        });
        this.buttonRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SearchItemComponent.this.list.removeSearchItem(SearchItemComponent.this);
            }
        });
    }

    private void initInvertSearch() {
        this.invertSearch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateSearchList();
            }
        });
    }

    private void initSearchText() {
        this.textFieldRecherche.getDocument().addDocumentListener(new SimpleDocumentListener() {
            public void update(DocumentEvent e) {
                try {
                    // One ne peut pas appeler chercher() car le texte n'est pas encore a jour
                    SearchItemComponent.this.text = e.getDocument().getText(0, e.getDocument().getLength()).trim();
                    updateSearchList();
                } catch (BadLocationException exn) {
                    // impossible
                    exn.printStackTrace();
                }
            }
        });
    }

    private void initCombo() {
        // sort column names alphabetically
        final int columnCount = this.list.getTableModel().getColumnCount();
        final String[][] names = new String[columnCount][];
        final int[] indexes = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            names[i] = this.list.getColumnNames(i);
            indexes[i] = 0;
        }
        // use column index as columns names are not unique
        final SortedMap<String, Integer> map = solve(names, indexes);
        final List<Tuple2<String, Integer>> cols = new ArrayList<Tuple2<String, Integer>>(columnCount);
        cols.add(Tuple2.create(TOUT, -1));
        for (final Entry<String, Integer> e : map.entrySet())
            cols.add(Tuple2.create(e.getKey(), e.getValue()));

        ((ListComboBoxModel) this.comboColonnePourRecherche.getModel()).addAll(cols);

        final ItemListener listener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateSearchList();
            }
        };
        this.comboColonnePourRecherche.addItemListener(listener);
        this.searchMode.addItemListener(listener);
    }

    /**
     * Return a sorted map of column index by name.
     * 
     * @param names all possible names for each column.
     * @param indexes index of the current name for each column.
     * @return a sorted map.
     */
    private SortedMap<String, Integer> solve(final String[][] names, final int[] indexes) {
        final int columnCount = names.length;
        // columns' index by name
        final CollectionMap<String, Integer> collisions = new CollectionMap<String, Integer>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            final int index = indexes[i];
            if (index >= names[i].length)
                throw new IllegalStateException("Ran out of names for " + i + " : " + Arrays.asList(names[i]));
            final String columnName = names[i][index];
            collisions.put(columnName, i);
        }
        final SortedMap<String, Integer> res = new TreeMap<String, Integer>();
        for (Entry<String, Collection<Integer>> e : collisions.entrySet()) {
            final Collection<Integer> indexesWithCollision = e.getValue();
            if (indexesWithCollision.size() > 1) {
                // increment only the minimum indexes to try to solve the conflict with the lowest
                // possible indexes
                int minIndex = Integer.MAX_VALUE;
                for (final Integer i : indexesWithCollision) {
                    if (indexes[i] < minIndex)
                        minIndex = indexes[i];
                }
                // now increment all indexes equal to minimum
                for (final Integer i : indexesWithCollision) {
                    if (indexes[i] == minIndex)
                        indexes[i]++;
                }
            } else {
                res.put(e.getKey(), indexesWithCollision.iterator().next());
            }
        }
        if (res.size() == columnCount)
            return res;
        else
            return solve(names, indexes);
    }

    void updateSearchList() {
        SearchItemComponent.this.list.updateSearch();
    }

    public SearchSpec getSearchItem() {
        final SearchSpec res;
        if (this.searchMode.getSelectedIndex() < MODES.length) {
            final TextSearchSpec textSpec = new TextSearchSpec(this.getText(), MODES[this.searchMode.getSelectedIndex()]);
            textSpec.setFormats(this.list.getFormats());
            res = textSpec;
        } else {
            res = new EmptySearchSpec();
        }
        return new ColumnSearchSpec(this.isExcluded(), res, this.getColIndex());
    }

    // *** state

    private final boolean isExcluded() {
        return this.invertSearch.isSelected();
    }

    private final int getColIndex() {
        return (Integer) ((Tuple2<?, ?>) this.comboColonnePourRecherche.getSelectedItem()).get1();
    }

    private final String getText() {
        return this.text;
    }

    public final void setText(String s) {
        this.textFieldRecherche.setText(s);
    }

    /**
     * Reinitialise le composant de recherche
     */
    public void resetState() {
        this.setText("");
        this.comboColonnePourRecherche.setSelectedIndex(0);
        this.searchMode.setSelectedIndex(0);
        this.invertSearch.setSelected(false);
    }

    public void setSearchFullMode(boolean b) {
        this.invertSearch.setVisible(b);
        this.buttonAdd.setVisible(b);
        this.buttonRemove.setVisible(b);
        this.comboColonnePourRecherche.setVisible(b);
        this.searchMode.setVisible(b);
    }

}
