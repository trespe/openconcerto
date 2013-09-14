package org.openconcerto.modules.extensionbuilder.translation.field;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.translation.LocaleSelector;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JLabelBold;

public class TableTranslationEditorPanel extends JPanel {
    private final List<JComponent> hideableComponents = new ArrayList<JComponent>();
    final JTextField textSingular1 = new JTextField();
    final JTextField textSingular2 = new JTextField();
    final JTextField textPlural1 = new JTextField();
    final JTextField textPlural2 = new JTextField();
    private Map<String, JTextField> map1 = new HashMap<String, JTextField>();
    private Map<String, JTextField> map2 = new HashMap<String, JTextField>();

    TableTranslationEditorPanel(final Extension extension, final String tableName) {
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        this.add(new JLabelBold("Table " + tableName), c);
        c.gridy++;
        c.gridwidth = 1;

        // Languages
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Langue et pays", SwingConstants.RIGHT), c);
        final String[] isoLanguages = Locale.getISOLanguages();
        System.out.println(isoLanguages.length);

        final LocaleSelector comboLang1 = new LocaleSelector();

        c.weightx = 1;
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(comboLang1, c);
        c.weightx = 0;
        c.gridx++;
        c.gridheight = 3;
        c.fill = GridBagConstraints.BOTH;
        final JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        this.add(sep, c);
        hideableComponents.add(sep);
        c.gridheight = 1;
        c.weightx = 1;
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        final LocaleSelector comboLang2 = new LocaleSelector();
        hideableComponents.add(comboLang2);
        comboLang2.setLocale(Locale.ENGLISH);
        this.add(comboLang2, c);
        // Singular
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Singulier", SwingConstants.RIGHT), c);
        c.gridx++;

        this.add(textSingular1, c);
        c.gridx += 2;
        hideableComponents.add(textSingular2);
        this.add(textSingular2, c);

        // Plural
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        this.add(new JLabel("Pluriel", SwingConstants.RIGHT), c);
        c.gridx++;
        this.add(textPlural1, c);
        c.gridx += 2;
        hideableComponents.add(textPlural2);
        this.add(textPlural2, c);

        // Fields
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.NONE;
        this.add(new JLabelBold("Champs de la table"), c);
        c.gridwidth = 1;

        List<String> fieldsName = getFieldsName(extension, tableName);
        c.gridy++;
        c.gridheight = fieldsName.size();
        c.gridx = 2;
        c.fill = GridBagConstraints.BOTH;
        final JSeparator sep2 = new JSeparator(JSeparator.VERTICAL);
        hideableComponents.add(sep2);
        this.add(sep2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        for (String fName : fieldsName) {
            c.gridx = 0;
            c.weightx = 0;
            this.add(new JLabel(fName, SwingConstants.RIGHT), c);
            c.gridx++;
            final JTextField t1 = new JTextField();
            map1.put(fName, t1);
            this.add(t1, c);
            c.gridx += 2;
            final JTextField t2 = new JTextField();
            hideableComponents.add(t2);
            map2.put(fName, t2);
            this.add(t2, c);
            c.gridy++;
        }
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1;
        c.weightx = 0;
        this.add(new JPanel(), c);
        // TODO: ajouter la checkbox pour masquer
        // TODO: repercuter les modifs dans les traductions

        updateUIFrom(extension, tableName, comboLang1.getLocale(), comboLang2.getLocale());
        comboLang1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIFrom(extension, tableName, comboLang1.getLocale(), null);
            }
        });
        comboLang2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateUIFrom(extension, tableName, null, comboLang2.getLocale());
            }
        });
    }

    private void updateUIFrom(Extension extension, String tableName, Object l1, Object l2) {
        if (l1 != null) {
            final String lang = l1.toString();
            updateUI(extension, tableName, lang, textSingular1, textPlural1, map1);
        }
        if (l2 != null) {
            final String lang = l2.toString();
            updateUI(extension, tableName, lang, textSingular2, textPlural2, map2);
        }

    }

    private void updateUI(Extension extension, String tableName, final String lang, JTextField textSingular, JTextField textPlural, Map<String, JTextField> map) {
        final TableTranslation tTrans = extension.getTableTranslation(lang, tableName);
        if (tTrans != null && tTrans.getSingular() != null) {
            textSingular.setText(tTrans.getSingular());
        } else {
            textSingular.setText("");
        }
        if (tTrans != null && tTrans.getPlural() != null) {
            textPlural.setText(tTrans.getPlural());
        } else {
            textPlural.setText("");
        }
        for (String fName : map.keySet()) {
            String t = extension.getFieldTranslation(lang, tableName, fName);
            if (t != null) {
                map.get(fName).setText(t);
            } else {
                map.get(fName).setText("");
            }
        }
    }

    private List<String> getFieldsName(Extension extension, String tableName) {
        final Set<String> l = new HashSet<String>();
        l.addAll(extension.getAllKnownFieldName(tableName));

        // + champs traduit dans l'extension
        List<String> lT = extension.getTranslatedFieldOfTable(tableName);
        l.addAll(lT);

        l.remove("ARCHIVE");
        l.remove("ORDRE");
        l.remove("ID_USER_COMMON_CREATE");
        l.remove("ID_USER_COMMON_MODIFY");
        // Sort
        final ArrayList<String> result = new ArrayList<String>();
        result.addAll(l);
        Collections.sort(result);
        return result;
    }
}
