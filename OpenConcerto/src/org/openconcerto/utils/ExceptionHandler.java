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
 
 /*
 * ExceptionHandler created on 7 mai 2004
 */
package org.openconcerto.utils;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Allow to display an exception both on the GUI and on the console.
 * 
 * @author ILM Informatique 7 mai 2004
 */
public class ExceptionHandler extends RuntimeException {

    private static final String ILM_CONTACT = "http://www.ilm-informatique.fr/contact";
    private static String ForumURL = null;

    public static void setForumURL(String url) {
        ForumURL = url;
    }

    static private void copyToClipboard(final String s) {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection data = new StringSelection(s);
        clipboard.setContents(data, data);
    }

    /**
     * Display the passed message. Note: this method doesn't block.
     * 
     * @param comp the modal parent of the error window.
     * @param msg the message to display.
     * @param originalExn the cause, can be <code>null</code>.
     * @return an exception.
     */
    static public ExceptionHandler handle(Component comp, String msg, Throwable originalExn) {
        return new ExceptionHandler(comp, msg, originalExn, false);
    }

    static public RuntimeException handle(String msg, Throwable originalExn) {
        return handle(null, msg, originalExn);
    }

    static public RuntimeException handle(String msg) {
        return handle(msg, null);
    }

    /**
     * Display the passed message and quit. Note: this method blocks until the user closes the
     * window (then exits).
     * 
     * @param msg the message to display.
     * @param originalExn the cause, can be <code>null</code>.
     * @return an exception.
     */
    static public RuntimeException die(String msg, Throwable originalExn) {
        return new ExceptionHandler(null, msg, originalExn);
    }

    static public RuntimeException die(String msg) {
        return die(msg, null);
    }

    private static Logger getLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    // the comp on which to display the popup, may be null
    private final Component comp;
    private final Future<?> future;
    private static boolean forceUI;

    public static void setForceUI(boolean forceUI) {
        ExceptionHandler.forceUI = forceUI;
    }

    private Future<?> display(final boolean error) {
        final String msg = this.getMessage();
        if (error) {
            getLogger().log(Level.SEVERE, null, this);
        } else {
            if (this.getCause() != null) {
                getLogger().log(Level.INFO, null, this);
            }
        }
        if (!GraphicsEnvironment.isHeadless() || forceUI) {
            if (SwingUtilities.isEventDispatchThread()) {
                showMsg(msg, error);
            } else {
                final FutureTask<?> run = new FutureTask<Object>(new Runnable() {
                    public void run() {
                        showMsg(msg, error);
                    }
                }, null);
                if (error) {
                    try {
                        SwingUtilities.invokeAndWait(run);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                } else {
                    SwingUtilities.invokeLater(run);
                }
                return run;
            }
        }
        return null;
    }

    public final Future<?> getDialogFuture() {
        return this.future;
    }

    protected final void showMsg(final String msg, final boolean quit) {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        final JImage im = new JImage(new ImageIcon(this.getClass().getResource("error.png")));
        final JLabel l = new JLabel("Une erreur est survenue");
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        final JLabel lError = new JLabel(msg);

        final JTextArea textArea = new JTextArea();
        textArea.setFont(textArea.getFont().deriveFont(11f));

        c.gridheight = 3;
        p.add(im, c);
        c.insets = new Insets(2, 4, 2, 4);
        c.gridheight = 1;
        c.gridx++;
        c.weightx = 1;
        c.gridwidth = 2;
        p.add(l, c);
        c.gridy++;
        p.add(lError, c);

        c.gridy++;
        p.add(new JLabel("Il s'agit probablement d'une mauvaise configuration ou installation du logiciel."), c);

        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy++;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy++;

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        final boolean browseSupported = desktop != null && desktop.isSupported(Action.BROWSE);
        if (ForumURL != null) {
            final javax.swing.Action communityAction;
            if (browseSupported) {
                communityAction = new AbstractAction("Consulter le forum") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            desktop.browse(new URI(ForumURL));
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                };
            } else {
                communityAction = new AbstractAction("Copier l'adresse du forum") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        copyToClipboard(ForumURL);
                    }
                };
            }
            p.add(new JButton(communityAction), c);
        }
        c.weightx = 0;
        c.gridx++;

        final javax.swing.Action supportAction;
        if (browseSupported)
            supportAction = new AbstractAction("Contacter l'assistance") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        desktop.browse(URI.create(ILM_CONTACT));
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                }
            };
        else
            supportAction = new AbstractAction("Copier l'adresse de l'assistance") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyToClipboard(ILM_CONTACT);
                }
            };

        p.add(new JButton(supportAction), c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        p.add(new JSeparator(), c);

        c.gridx = 0;
        c.gridwidth = 3;
        c.gridy++;
        c.insets = new Insets(2, 4, 2, 4);
        p.add(new JLabel("Détails de l'erreur:"), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy++;
        String message = this.getCause() == null ? null : this.getCause().getMessage();
        if (message == null) {
            message = msg;
        } else {
            message = msg + "\n\n" + message;
        }
        message += "\n";
        message += getTrace();
        textArea.setText(message);
        textArea.setEditable(false);
        // Scroll
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.getViewport().setMinimumSize(new Dimension(200, 300));
        c.weighty = 1;
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy++;
        p.add(scroll, c);

        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weighty = 0;
        c.insets = new Insets(2, 4, 2, 4);
        final JButton buttonClose = new JButton("Fermer");
        p.add(buttonClose, c);

        final Window window = this.comp == null ? null : SwingUtilities.getWindowAncestor(this.comp);
        final JDialog f;
        if (window instanceof Frame) {
            f = new JDialog((Frame) window, "Erreur", true);
        } else {
            f = new JDialog((Dialog) window, "Erreur", true);
        }
        f.setContentPane(p);
        f.pack();
        f.setSize(580, 680);
        f.setMinimumSize(new Dimension(380, 380));
        f.setLocationRelativeTo(this.comp);
        final ActionListener al = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (quit) {
                    System.exit(1);
                } else {
                    f.dispose();
                }

            }
        };
        buttonClose.addActionListener(al);
        // cannot set EXIT_ON_CLOSE on JDialog
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                al.actionPerformed(null);
            }
        });
        f.setVisible(true);
    }

    private String getTrace() {
        return ExceptionUtils.getStackTrace(this);
    }

    /**
     * Affiche l'erreur et quitte.
     * 
     * @param comp the component upon which to display the popup.
     * @param msg le message d'erreur à afficher.
     * @param cause la cause de l'exception (peut être <code>null</code>).
     */
    private ExceptionHandler(Component comp, String msg, Throwable cause) {
        this(comp, msg, cause, true);
    }

    /**
     * Affiche l'erreur et quitte suivant l'option passée.
     * 
     * @param comp the component upon which to display the popup.
     * @param msg the error message to display.
     * @param cause the cause of the exception (maybe <code>null</code>).
     * @param quit if the VM must exit.
     */
    private ExceptionHandler(Component comp, String msg, Throwable cause, boolean quit) {
        super(msg, cause);
        this.comp = comp;
        this.future = this.display(quit);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        ExceptionHandler.handle("Fichier de configuration corrompu", new IllegalStateException("Id manquant"));
    }
}
