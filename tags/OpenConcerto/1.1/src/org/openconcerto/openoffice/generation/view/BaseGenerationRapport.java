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
 
 package org.openconcerto.openoffice.generation.view;

import org.openconcerto.openoffice.Log;
import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.openconcerto.openoffice.OOUtils;
import org.openconcerto.openoffice.generation.GenerationTask;
import org.openconcerto.openoffice.generation.ReportGeneration;
import org.openconcerto.openoffice.generation.TaskStatus;
import org.openconcerto.openoffice.generation.desc.ReportType;
import org.openconcerto.openoffice.generation.desc.ReportTypes;
import org.jopendocument.link.Component;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.utils.EmailClient;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.model.ListComboBoxModel;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jdom.JDOMException;

/**
 * A panel to choose a ReportType, see the tasks of the generation, and optionnaly open the
 * document. NOTE: you have to call {@link #enableGeneration(boolean)} before being able to
 * generate, because it's set to false in the constructor.
 * 
 * @author ILM Informatique 27 déc. 2003
 * @param <R> type of generation
 */
public abstract class BaseGenerationRapport<R extends ReportGeneration<?>> extends JPanel {

    static public enum FileAction {
        DO_NOTHING("ne rien faire"), OPEN("ouvrir le fichier"), MAIL("envoyer par courriel");

        private final String label;

        private FileAction(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private JComboBox typeRapportComboSelection;

    private JButton genererButton;
    private JComboBox fileActionCombo;

    private JList tasksView;
    private JLabel status;

    // le groupe dans lequel doivent être toutes les thread de la génération
    private final ThreadGroup thg;

    public BaseGenerationRapport() throws JDOMException, IOException {
        this.thg = new ThreadGroup(this + " thread group");
        this.uiInit();
        this.setStatus("Inactif");
    }

    protected Map<String, JComponent> getEntries() {
        return new LinkedHashMap<String, JComponent>();
    }

    protected File getReportDir() {
        return null;
    }

    private void uiInit() throws JDOMException, IOException {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.ipadx = 40;
        c.insets = new Insets(2, 5, 0, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        final Map<String, JComponent> allEntries = new LinkedHashMap<String, JComponent>();
        this.typeRapportComboSelection = new JComboBox(new ListComboBoxModel(this.getTypes().getTypes()));
        allEntries.put("Type de rapport", this.typeRapportComboSelection);
        allEntries.putAll(this.getEntries());

        for (final Map.Entry<String, JComponent> e : allEntries.entrySet()) {
            c.gridx = 0;
            c.gridy++;
            this.add(new JLabel(e.getKey()), c);
            c.gridx++;
            c.weightx = 1;
            this.add(e.getValue(), c);
            c.weightx = 0;
        }
        final File reportDir = getReportDir();
        if (reportDir != null) {
            c.gridx = 1;
            c.gridy++;
            c.weightx = 1;
            this.add(new JButton(new AbstractAction("Ouvrir le dossier des rapports") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Desktop.getDesktop().browse(reportDir.toURI());
                    } catch (Exception e1) {
                        JOptionPane.showMessageDialog(BaseGenerationRapport.this, "Impossible d'ouvrir le dossier", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }), c);
            c.weightx = 0;
        }

        c.gridx = 0;
        c.gridy++;
        this.genererButton = new JButton("Générer le rapport et");
        this.add(this.genererButton, c);
        this.genererButton.setEnabled(false);
        this.genererButton.addActionListener(new GenerateAction());

        c.gridx++;
        this.fileActionCombo = new JComboBox(getAllowedActions());
        this.fileActionCombo.setSelectedItem(FileAction.OPEN);
        this.add(this.fileActionCombo, c);
        this.fileActionCombo.setEnabled(false);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.status = new JLabel();
        this.add(this.status, c);

        c.gridy++;
        this.tasksView = new JList(new TasksModel());
        this.tasksView.setCellRenderer(new GenerationTaskView());
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        JScrollPane scroll = new JScrollPane(this.tasksView);
        scroll.setPreferredSize(new Dimension(200, 400));
        this.add(scroll, c);
    }

    /**
     * Permet de modifier la liste des tâches.
     * 
     * @author Sylvain CUAZ
     */
    private static final class TasksModel extends AbstractListModel implements PropertyChangeListener {
        private List<GenerationTask> tasks;

        {
            this.tasks = new ArrayList<GenerationTask>(15);
        }

        public int getSize() {
            return this.tasks.size();
        }

        public Object getElementAt(int index) {
            return this.tasks.get(index);
        }

        void add(GenerationTask task) {
            this.tasks.add(task);
            task.addPropertyChangeListener(this);
            this.fireIntervalAdded(this, this.tasks.size(), this.tasks.size());
        }

        void clear() {
            for (final GenerationTask t : this.tasks) {
                t.removePropertyChangeListener(this);
            }
            this.fireIntervalRemoved(this, 0, this.tasks.size());
            this.tasks.clear();
        }

        /*
         * Une tache a changé.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            int index = this.tasks.indexOf(evt.getSource());
            this.fireContentsChanged(this, index, index);
        }
    }

    /**
     * Interrompt la génération.
     */
    public final void interrupt() {
        // pas de threads active, quand pas génération
        this.thg.interrupt();
    }

    class GenerateAction implements ActionListener {
        public GenerateAction() {
        }

        public void actionPerformed(ActionEvent e) {
            enableGeneration(false);
            final FileAction sel = (FileAction) BaseGenerationRapport.this.fileActionCombo.getSelectedItem();
            // "génération..."
            new Thread(BaseGenerationRapport.this.thg, new Runnable() {
                @Override
                public void run() {
                    generate(sel);
                    // toujours le faire, même si interrompu
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            enableGeneration(true);
                        }
                    });
                }
            }).start();
        }
    }

    protected final void enableGeneration(boolean b) {
        this.fileActionCombo.setEnabled(b);
        this.genererButton.setEnabled(b);
    }

    // doit s'exécuter dans this.thg
    protected final void generate(final FileAction sel) {
        final ReportType type = (ReportType) this.typeRapportComboSelection.getSelectedItem();
        final R rg = this.createGeneration(type);
        rg.addTaskListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final TaskStatus st = (TaskStatus) evt.getOldValue();
                if (st.getState().equals(TaskStatus.State.NOT_STARTED))
                    addTask((GenerationTask) evt.getSource());
            }

        });

        if (Thread.currentThread().isInterrupted())
            return;

        Map<String, ODSingleXMLDocument> report = null;
        try {
            report = this.generate(rg);
            if (report != null) {
                for (Entry<String, ODSingleXMLDocument> e : report.entrySet()) {
                    try {
                        final File reportFile = e.getValue().saveAs(this.getFile(rg, e.getKey()));
                        if (sel == FileAction.OPEN)
                            OOUtils.open(reportFile);
                        else if (sel == FileAction.MAIL) {
                            final File fileOutPDF = FileUtils.addSuffix(reportFile, ".pdf");
                            try {
                                final OOConnexion conn = OOConnexion.create();
                                if (conn == null)
                                    throw new IllegalStateException("OpenOffice n'a pu être trouvé");
                                final Component doc = conn.loadDocument(reportFile, true);
                                final Future<File> pdf = doc.saveToPDF(fileOutPDF, "writer_pdf_Export");
                                doc.close();
                                conn.closeConnexion();
                                // can wait for the pdf since we're not in the EDT
                                EmailClient.getPreferred().compose(getEmailRecipient(rg), "Rapport", null, pdf.get());
                            } catch (Exception exn) {
                                exn.printStackTrace();
                                ExceptionHandler.handle("Impossible de charger le document OpenOffice dans le logiciel de courriel", exn);
                            }
                        }
                    } catch (FileNotFoundException exn) {
                        // TODO tester pb de droits
                        ExceptionHandler.handle(BaseGenerationRapport.this, "Le fichier est déjà ouvert, veuillez le refermer avant de générer.", exn);
                    } catch (IOException exn) {
                        ExceptionHandler.handle(BaseGenerationRapport.this, "Impossible de sauver le rapport", exn);
                    }
                }
            }
        } catch (Throwable e) {
            ExceptionHandler.handle(BaseGenerationRapport.this, "Impossible de générer le rapport", e);
        }
    }

    private Map<String, ODSingleXMLDocument> generate(org.openconcerto.openoffice.generation.ReportGeneration<?> rg) throws Throwable {
        clearTasks();
        setStatus("Génération en cours...");

        if (Thread.currentThread().isInterrupted())
            return null;

        final long start = System.currentTimeMillis();
        Map<String, ODSingleXMLDocument> report = null;
        String s;
        Throwable t = null;
        try {
            report = rg.generateMulti();
            if (report == null) {
                s = "Génération interrompue.";
            } else {
                s = "Rapport généré en " + (System.currentTimeMillis() - start) / 1000 + " secondes.";
            }
        } catch (Throwable e) {
            s = "Erreur de génération.";
            t = e;
        }

        setStatus(s);
        if (t == null)
            return report;
        else
            throw t;
    }

    private final void addTask(final GenerationTask task) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ((TasksModel) BaseGenerationRapport.this.tasksView.getModel()).add(task);
            }
        });

    }

    private final void clearTasks() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ((TasksModel) BaseGenerationRapport.this.tasksView.getModel()).clear();
            }
        });
    }

    private final void setStatus(final String status) {
        Log.get().info(this + " status: " + status);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BaseGenerationRapport.this.status.setText(status);
            }
        });
    }

    public String toString() {
        return "Generation panel";
    }

    abstract protected ReportTypes getTypes() throws JDOMException, IOException;

    abstract protected R createGeneration(final ReportType type);

    abstract protected File getFile(final R rg, String docID);

    protected String getEmailRecipient(final R rg) {
        return null;
    }

    protected FileAction[] getAllowedActions() {
        return FileAction.values();
    }
}
