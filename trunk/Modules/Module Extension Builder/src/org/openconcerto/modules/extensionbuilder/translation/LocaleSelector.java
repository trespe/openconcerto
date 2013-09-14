package org.openconcerto.modules.extensionbuilder.translation;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class LocaleSelector extends JPanel {

    private String lang, country;
    private JComboBox comboLang;
    private JComboBox comboCountry;
    private List<ActionListener> listeners = new ArrayList<ActionListener>();
    private boolean interactive = true;

    public LocaleSelector() {
        this.setLayout(new FlowLayout(FlowLayout.LEFT));
        final Vector<String> langs = new Vector<String>();
        final Locale[] l = Locale.getAvailableLocales();

        for (int i = 0; i < l.length; i++) {
            final String language = ((Locale) l[i]).getLanguage();
            if (!langs.contains(language)) {
                langs.add(language);
            }
        }

        Collections.sort(langs);
        comboLang = new JComboBox(langs);
        this.add(comboLang);
        comboCountry = new JComboBox();
        this.add(comboCountry);

        try {
            this.setLocale(Locale.getDefault());
        } catch (Exception e) {
            System.err.println("LocaleSelector warning: unable to set current language");
        }
        comboLang.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (interactive) {
                    lang = comboLang.getSelectedItem().toString();
                    updateCountryFromLang();
                    country = comboCountry.getSelectedItem().toString();
                    fireActionPerformed();
                }
            }

        });
        comboCountry.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (interactive) {
                    country = comboCountry.getSelectedItem().toString();
                    fireActionPerformed();
                }
            }

        });
        final int minWidth = comboLang.getPreferredSize().width * 2;
        final int minHeight = comboLang.getPreferredSize().height;
        comboLang.setMinimumSize(new Dimension(minWidth, minHeight));
        comboLang.setPreferredSize(new Dimension(minWidth, minHeight));
        comboCountry.setMinimumSize(new Dimension(minWidth, minHeight));
        comboCountry.setPreferredSize(new Dimension(minWidth, minHeight));
    }

    private void updateCountryFromLang() {

        Vector<String> countries = new Vector<String>();
        Locale[] l = Locale.getAvailableLocales();
        for (int i = 0; i < l.length; i++) {
            Locale lo = (Locale) l[i];
            if (lo.getLanguage().equals(lang)) {
                countries.add(lo.getCountry());
            }
        }
        Collections.sort(countries);
        if (countries.isEmpty()) {
            countries.add("");
        }
        comboCountry.setModel(new DefaultComboBoxModel(countries));
    }

    public Locale getLocale() {
        Locale[] l = Locale.getAvailableLocales();
        if (country != null) {

            for (int i = 0; i < l.length; i++) {
                Locale lo = (Locale) l[i];
                if (lo.getLanguage().equals(lang) && lo.getCountry().equals(country)) {
                    return lo;
                }
            }
        }
        for (int i = 0; i < l.length; i++) {
            Locale lo = (Locale) l[i];
            if (lo.getLanguage().equals(lang)) {
                return lo;
            }
        }
        return null;
    }

    public void setLocale(Locale l) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Must be called in EDT");
        }
        if (l.getLanguage().equals(this.lang) && l.getCountry().equals(this.country)) {
            return;
        }
        this.interactive = false;
        this.lang = l.getLanguage();
        this.country = l.getCountry();
        System.err.println("LocaleSelector.setLocale() " + this.lang + " " + this.country);
        this.comboLang.setSelectedItem(lang);
        updateCountryFromLang();
        this.comboCountry.setSelectedItem(country);
        this.interactive = true;
    }

    public void addActionListener(ActionListener actionListener) {
        this.listeners.add(actionListener);
    }

    public void fireActionPerformed() {
        System.err.println("LocaleSelector.fireActionPerformed():" + this.lang + " " + this.country);
        System.err.println("LocaleSelector.fireActionPerformed(); locale: " + this.getLocale());
        for (ActionListener listener : this.listeners) {
            listener.actionPerformed(new ActionEvent(this, this.hashCode(), ""));
        }
    }

}
