package org.openconcerto.modules.extensionbuilder.translation.menu;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.modules.extensionbuilder.menu.mainmenu.MenuDescriptor;
import org.openconcerto.modules.extensionbuilder.translation.LocaleSelector;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;

public class MenuTranslationItemEditor extends JPanel {

    final Extension extension;
    private JTextField textId;
    private JTextField textTranslation1;
    private JTextField textTranslation2;

    public MenuTranslationItemEditor(final Item item, final Extension extension) {
        this.extension = extension;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();

        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Identifiant", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridwidth = 3;
        textId = new JTextField();
        this.add(textId, c);

        // Language selector
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.gridwidth = 1;
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
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        final JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        this.add(sep, c);

        c.gridheight = 1;
        c.weightx = 1;
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        final LocaleSelector comboLang2 = new LocaleSelector();

        comboLang2.setLocale(Locale.ENGLISH);
        this.add(comboLang2, c);
        // Traduction
        c.gridx = 0;
        c.gridy++;

        c.gridwidth = 1;

        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JLabel("Traduction", SwingConstants.RIGHT), c);
        c.gridx++;

        c.weightx = 1;

        textTranslation1 = new JTextField(20);

        this.add(textTranslation1, c);

        c.gridx += 2;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.weightx = 1;
        textTranslation2 = new JTextField(20);
        this.add(textTranslation2, c);

        c.gridy++;
        c.weighty = 1;
        this.add(new JPanel(), c);

        initUIFrom(item);

        textTranslation1.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                extension.setMenuTranslation(item.getId(), textTranslation1.getText(), comboLang1.getLocale());
                extension.setChanged();
            }
        });
        textTranslation2.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                extension.setMenuTranslation(item.getId(), textTranslation2.getText(), comboLang2.getLocale());
                extension.setChanged();
            }
        });

    }

    private void initUIFrom(Item item) {

        final LayoutHints localHint = item.getLocalHint();
        System.out.println("ItemEditor.initUIFrom:" + item + " " + localHint);
        textId.setEnabled(false);
        if (textId != null) {
            textId.setText(item.getId());
        }

    }
}
