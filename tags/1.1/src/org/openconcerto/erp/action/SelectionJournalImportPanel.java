package org.openconcerto.erp.action;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.openconcerto.erp.core.finance.accounting.model.SelectJournauxModel;
import org.openconcerto.ui.DefaultGridBagConstraints;

public class SelectionJournalImportPanel extends JPanel {

    public SelectionJournalImportPanel(final String journalTitle, final Map<String, Integer> m, final Semaphore sema) {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();

        JLabel label = new JLabel("Le journal " + journalTitle + " n'existe pas. Quel est son type?");
        this.add(label, c);

        final SelectJournauxModel model = new SelectJournauxModel();

        final JTable tableJrnl = new JTable(model);
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        this.add(tableJrnl, c);

        final JButton button = new JButton("Continuer");
        button.setEnabled(false);
        tableJrnl.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {

                button.setEnabled(true);
            }
        });

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridy++;
        this.add(button, c);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int id = model.getIdForRow(tableJrnl.getSelectedRow());
                m.put(journalTitle, new Integer(id));
                // synchronized (t) {
                // System.err.println("Notify");
                // t.notify();
                // }
                //
                sema.release();
                ((JFrame) SwingUtilities.getRoot(SelectionJournalImportPanel.this)).dispose();

            }
        });

    }
}
