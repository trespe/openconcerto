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
 
 package org.openconcerto.utils.beans.list;

import org.openconcerto.utils.beans.Bean;
import org.openconcerto.utils.beans.PropertySheet;
import org.openconcerto.utils.model.DefaultIMutableListModel;
import org.openconcerto.utils.model.IMutableListModel;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Une frame affichant une liste et des boutons pour la manipuler.
 * 
 * @author ILM Informatique 11 juin 2004
 */
public final class CBeanListEdit extends JPanel implements ActionListener, ListSelectionListener {

    // static

    static public JFrame createFrame(List items) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new CBeanListEdit(items));
        setBounds(frame);
        return frame;
    }

    static final private void setBounds(JFrame frame) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        DisplayMode dm = ge.getDefaultScreenDevice().getDisplayMode();

        int topOffset = 50;
        if (dm.getWidth() <= 800 || dm.getHeight() <= 600) {
            frame.setLocation(0, topOffset);
            frame.setSize(dm.getWidth(), dm.getHeight() - topOffset);
        } else {
            frame.setLocation(10, topOffset);
            frame.setSize((int) (dm.getWidth() * 0.9), (int) (dm.getHeight() * 0.9));
        }
    }

    // instance

    private CBeanList liste;
    private ListSelectionListener selListener;

    private JButton cancelBtn;
    private JButton validateBtn;

    private PropertySheet edition;
    private Class beanClass;

    static private Class getClass(List items) {
        if (items.size() == 0)
            throw new IllegalArgumentException("items is empty, use the other constructor");
        return items.get(0).getClass();
    }

    public CBeanListEdit(List items) {
        this(getClass(items), new DefaultIMutableListModel(items));
    }

    public CBeanListEdit(Class clazz, IMutableListModel model) {
        // TODO vérifier que les items sont tous de la même classe
        if (!Bean.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("class is not a CBean");
        this.beanClass = clazz;
        this.liste = new CBeanList(this, model, this.beanClass);
        this.uiInit();
    }

    public List getSelectedBeans() {
        return this.liste.getSelectedBeans();
    }

    public void setSelection(Bean bean) {
        this.liste.setSelection(bean);
    }

    final private void uiInit() {
        this.createUI();

        Container container = this;
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;

        // partie d'edition
        JPanel editP = this.getEditPanel();
        JScrollPane p = new JScrollPane(editP);

        // on fixe la preferred size
        p.setPreferredSize(p.getPreferredSize());
        // ignoré par JSplitPane
        // TODO faire une sous classe de JSplitPane
        p.setMaximumSize(p.getPreferredSize());
        Dimension d = p.getPreferredSize();
        // w ignoré
        int w = 100;
        int h = Math.max((int) (d.height * 0.3), 80);
        p.setMinimumSize(new Dimension(w, h));

        c.weighty = 1;
        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, this.liste, p);
        // la liste prend le + d'espace
        sp.setResizeWeight(1.0);
        container.add(sp, c);
        c.gridy++;
    }

    private void createUI() {
        // TODO settable

        this.edition = new PropertySheet(this.beanClass);

        this.cancelBtn = new JButton("Annuler");
        this.cancelBtn.addActionListener(this);
        this.validateBtn = new JButton("Valider");
        this.validateBtn.addActionListener(this);

        this.setEditable(false);
    }

    final private JPanel getEditPanel() {
        JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        // c.insets = new Insets(0, 2, 0, 2);
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;

        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        container.add(this.edition, c);
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridy++;

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;

        container.add(this.cancelBtn, c);
        c.gridx++;

        c.weightx = 0;
        container.add(this.validateBtn, c);
        c.gridx++;

        return container;
    }

    public final void actionPerformed(ActionEvent e) {
        this.handleAction((JButton) e.getSource());
    }

    private void handleAction(JButton source) {
        if (source == this.validateBtn) {
            this.edition.commit();
        } else if (source == this.cancelBtn) {
            System.out.println("cancel");
        }
    }

    // appelé par CBeanList
    public void setCurrentBean(Bean bean) {
        // MAYBE change la selection de la liste (necessite que le tableModel
        // puisse convertir bean => row)
        this.setEditable(bean != null);
        this.edition.setTarget(bean);
    }

    private Bean getCurrentBean() {
        // a tout moment le panneau d'edition affiche le bean selectionné (cf setCurrentBean)
        return this.edition.getTarget();
    }

    /**
     * Est-ce qu'il y a un bean éditable.
     * 
     * @param b <code>true</code> si la liste est éditable.
     */
    private void setEditable(boolean b) {
        this.validateBtn.setEnabled(b);
        this.cancelBtn.setEnabled(b);
    }

    public Bean createNewBeanFromView() {
        // FIXME vérifier qu'il ya selection pour pouvoir remplir le nouveau
        Bean newBean = null;
        try {
            // FIXME utiliser un constructeur non vide
            newBean = (Bean) this.beanClass.newInstance();
        } catch (InstantiationException e) {
            // impossible (constructeur vide)
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // impossible
            e.printStackTrace();
        }
        // FIXME not setTarget mais create
        this.edition.setTarget_UpdateFromView(newBean);
        return newBean;
    }

    public void setSelectionListener(ListSelectionListener listener) {
        this.selListener = listener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
     */
    public void valueChanged(ListSelectionEvent e) {
        // appelé par CBeanList

        this.setCurrentBean(this.liste.getSelectedBean());
        if (this.selListener != null)
            this.selListener.valueChanged(e);

    }

}
