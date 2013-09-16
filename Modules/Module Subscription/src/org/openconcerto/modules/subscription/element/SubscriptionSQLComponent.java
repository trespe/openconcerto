/*
 * Créé le 1 juin 2012
 */
package org.openconcerto.modules.subscription.element;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ITextArea;

public class SubscriptionSQLComponent extends BaseSQLComponent {

    private static final long serialVersionUID = 4274010869219769289L;

    public SubscriptionSQLComponent(SQLElement element) {
        super(element);
    }

    @Override
    public void addViews() {

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        // Numéro
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("NUMERO"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        final JTextField textNumero = new JTextField(8);
        this.add(textNumero, c);
        // Date
        c.gridx++;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("DATE"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        final JDate date = new JDate(true);
        this.add(date, c);
        // Libellé
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("NOM"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        final JTextField textNom = new JTextField();
        this.add(textNom, c);

        // Libellé
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        c.gridwidth = 1;
        this.add(new JLabel(getLabelFor("INTITULE_FACTURE"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        final JTextField intitule = new JTextField();
        this.add(intitule, c);
        this.addView(intitule, "INTITULE_FACTURE");

        // Description
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("DESCRIPTION"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        final ITextArea textDescription = new ITextArea();
        this.add(textDescription, c);

        // Client
        c.gridwidth = 1;
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        this.add(new JLabel(getLabelFor("ID_CLIENT"), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 3;
        final ElementComboBox client = new ElementComboBox();
        this.add(client, c);

        //
        c.gridwidth = 4;
        c.gridx = 0;
        c.gridy++;

        c.weightx = 1;
        c.weighty = 1;

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Facture", createTypeAboComponent("FACTURE", "ID_SAISIE_VENTE_FACTURE"));
        tabbedPane.add("Devis", createTypeAboComponent("DEVIS", "ID_DEVIS"));
        tabbedPane.add("Bon de commande", createTypeAboComponent("COMMANDE", "ID_COMMANDE_CLIENT"));

        this.add(tabbedPane, c);

        this.addView(textNumero, "NUMERO", REQ);
        this.addView(date, "DATE");
        this.addView(textNom, "NOM");
        this.addView(textDescription, "DESCRIPTION");
        this.addView(client, "ID_CLIENT");

        // Codé mais jamais lancé.. a verifier: nom des champs
    }

    private Component createTypeAboComponent(String type, String idName) {
        final String fieldCreate = "CREATE_" + type;
        final String fieldStart = "DATE_DEBUT_" + type;
        final String fieldStop = "DATE_FIN_" + type;
        final String fieldPeriodicity = "NB_MOIS_" + type;

        final JPanel panel = new JPanel();
        panel.setOpaque(false);

        panel.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        // Checkbox
        c.gridx = 1;
        c.weightx = 1;
        final JCheckBox check = new JCheckBox(getLabelFor(fieldCreate));
        check.setOpaque(false);
        panel.add(check, c);

        // Item
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        panel.add(new JLabel(getLabelFor(idName), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        final ElementComboBox item = new ElementComboBox();
        // SQLElement elt =
        // Configuration.getInstance().getDirectory().getElement(getTable().getForeignTable(idName));
        // ComboSQLRequest req = new ComboSQLRequest(elt.getComboRequest(true),
        // Arrays.asList(elt.getTable().getField("NUMERO"), elt.getTable().getField("ID_CLIENT")));
        // item.init(elt, req);
        panel.add(item, c);

        // Start
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(getLabelFor(fieldStart), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        final JDate startDate = new JDate(true);
        panel.add(startDate, c);

        // Stop
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(getLabelFor(fieldStop), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        final JDate stopDate = new JDate(true);
        c.fill = GridBagConstraints.NONE;
        panel.add(stopDate, c);

        // Periodicity
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(getLabelFor(fieldPeriodicity), SwingConstants.RIGHT), c);
        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        final JTextField textPeriod = new JTextField(5);
        panel.add(textPeriod, c);

        check.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setFieldEnabled(item, startDate, stopDate, textPeriod, check.isSelected());
            }
        });

        this.addView(check, fieldCreate);
        this.addView(item, idName);
        this.addView(startDate, fieldStart);
        this.addView(stopDate, fieldStop);
        this.addView(textPeriod, fieldPeriodicity);

        setFieldEnabled(item, startDate, stopDate, textPeriod, false);
        return panel;
    }

    // @Override
    // protected SQLRowValues createDefaults() {
    // SQLRowValues rowVals = new SQLRowValues(getTable());
    // return super.createDefaults();
    // }

    public void setFieldEnabled(JComponent item, JComponent startDate, JComponent stopDate, JComponent textPeriod, boolean b) {
        System.err.println(b);
        Thread.dumpStack();
        item.setEnabled(b);
        startDate.setEnabled(b);
        stopDate.setEnabled(b);
        textPeriod.setEnabled(b);
    }
}
