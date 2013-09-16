package org.openconcerto.modules.timetracking;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.modules.project.Module;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.utils.ExceptionHandler;

public class ReportingPanel extends JPanel {

    public ReportingPanel() {
        super(new GridBagLayout());

        final JLabel labelProject = new JLabel("Affaire", SwingConstants.RIGHT);
        final ElementComboBox boxProject = new ElementComboBox();
        boxProject.init(Configuration.getInstance().getDirectory().getElement(Module.PROJECT_TABLENAME));
        boxProject.setButtonsVisible(false);
        final GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(labelProject, c);
        c.gridx++;
        this.add(boxProject, c);

        final JLabel labelCom = new JLabel("Collaborateur", SwingConstants.RIGHT);
        final ElementComboBox boxUser = new ElementComboBox();
        boxUser.init(Configuration.getInstance().getDirectory().getElement("USER_COMMON"));
        boxUser.setButtonsVisible(false);
        c.gridx = 0;
        c.gridy++;
        this.add(labelCom, c);
        c.gridx++;
        this.add(boxUser, c);

        final ElementComboBox boxMois = new ElementComboBox();
        boxMois.init(Configuration.getInstance().getDirectory().getElement("MOIS"));
        boxMois.setButtonsVisible(false);
        c.gridx = 0;
        c.gridy++;
        this.add(new JLabel("Mois", SwingConstants.RIGHT), c);
        c.gridx++;
        c.fill = GridBagConstraints.NONE;
        this.add(boxMois, c);

        c.gridx = 0;
        c.gridy++;
        final JLabel labelYear = new JLabel("Année", SwingConstants.RIGHT);
        final JComboBox boxYear = new JComboBox();
        boxYear.setOpaque(false);
        final Calendar cal = Calendar.getInstance();
        final int year = cal.get(Calendar.YEAR);
        boxYear.addItem(year - 1);
        boxYear.addItem(year);
        boxYear.addItem(year + 1);
        boxYear.setSelectedItem(year);
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(labelYear, c);
        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        this.add(boxYear, c);

        final JButton buttonValid = new JButton(new AbstractAction("Créer le document") {

            @Override
            public void actionPerformed(ActionEvent e) {
                final SQLRow selectedProject = boxProject.getSelectedRow();
                final SQLRow selectedMonth = boxMois.getSelectedRow();
                final SQLRow selectedUser = boxUser.getSelectedRow();
                final Integer selectedYear = (Integer) boxYear.getSelectedItem();
                new Thread() {
                    public void run() {

                        ReportingSheetXml sheet = new ReportingSheetXml(selectedYear, selectedMonth, selectedUser, selectedProject);
                        try {
                            sheet.createDocument();
                        } catch (InterruptedException exn) {
                            exn.printStackTrace();
                        } catch (ExecutionException exn) {
                            exn.printStackTrace();
                        }
                        sheet.showPrintAndExport(false, false, true, true, true);
                        try {
                            Gestion.openPDF(sheet.getGeneratedPDFFile());
                        } catch (Exception exn) {
                            ExceptionHandler.handle("Impossible d'ouvrir le PDF", exn);
                        }

                    };
                }.start();

            }
        });
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.weightx = 1;
        this.add(buttonValid, c);
    }
}
