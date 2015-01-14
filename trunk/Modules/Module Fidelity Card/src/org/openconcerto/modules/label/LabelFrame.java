package org.openconcerto.modules.label;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

public class LabelFrame extends JFrame {
    private static final boolean IGNORE_MARGINS = true;
    private static final int DEFAULT_LINES = 10;
    private static final int DEFAULT_COLS = 4;
    final LabelPanel labelPanel;

    public LabelFrame(List<? extends SQLRowAccessor> list, LabelRenderer labelRenderer) {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        p.add(createToolBar(), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(new JSeparator(JSeparator.HORIZONTAL), c);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        labelPanel = new LabelPanel(list, DEFAULT_LINES, DEFAULT_COLS, labelRenderer);
        labelPanel.setIgnoreMargins(IGNORE_MARGINS);
        p.add(labelPanel, c);
        this.setContentPane(p);
    }

    public JPanel createToolBar() {
        final JPanel toolbar = new JPanel();
        toolbar.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        toolbar.add(new JLabel("Lignes"), c);
        c.gridx++;
        final JSpinner sLines = new JSpinner(new SpinnerNumberModel(DEFAULT_LINES, 1, 100, 1));
        toolbar.add(sLines, c);
        c.gridx++;
        toolbar.add(new JLabel("Colonnes"), c);
        c.gridx++;
        final JSpinner sColums = new JSpinner(new SpinnerNumberModel(DEFAULT_COLS, 1, 50, 1));
        toolbar.add(sColums, c);
        c.gridx++;
        final JCheckBox ck = new JCheckBox("Ignorer les marges");
        ck.setSelected(IGNORE_MARGINS);

        toolbar.add(ck, c);

        c.gridx++;
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        final JButton printButton = new JButton("Imprimer");
        toolbar.add(printButton, c);
        c.gridx++;

        sLines.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Number n = (Number) sLines.getValue();
                if (n != null) {
                    labelPanel.setLineCount(n.intValue());
                }
            }
        });
        sColums.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Number n = (Number) sColums.getValue();
                if (n != null) {
                    labelPanel.setColumnCount(n.intValue());
                }
            }
        });
        printButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintable(labelPanel);
                boolean ok = job.printDialog();
                if (ok) {
                    try {
                        job.print();
                    } catch (PrinterException ex) {
                        ExceptionHandler.handle("Print error", ex);
                    }
                }

            }
        });
        ck.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                labelPanel.setIgnoreMargins(ck.isSelected());

            }
        });
        return toolbar;
    }

}
