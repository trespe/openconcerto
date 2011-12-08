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
 
 package org.openconcerto.laf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ComboBoxEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.plaf.metal.MetalComboBoxEditor;
import javax.swing.plaf.metal.MetalComboBoxUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

import javax.swing.plaf.metal.MetalComboBoxUI.MetalComboBoxLayoutManager;

public class IComboBoxUI extends MetalComboBoxUI {

	static int comboBoxButtonSize = 19;

	public static ComponentUI createUI(JComponent c) {
		return new IComboBoxUI();
	}

	/*public void paint(Graphics g, JComponent c) {
	}*/

	/*protected ComboBoxEditor createEditor() {
		return new MetalComboBoxEditor.UIResource();
	}

	protected ComboPopup createPopup() {
		return new BasicComboPopup(comboBox);
	}*/
 // This is here because of a bug in the compiler.  
    // When a protected-inner-class-savvy compiler comes out we
    // should move this into MetalComboBoxLayoutManager.
    public void layoutComboBox( Container parent, MetalComboBoxLayoutManager manager ) {
      
        if (arrowButton != null) {
                 Icon icon = arrowButton.getIcon();
                Insets buttonInsets = arrowButton.getInsets();
                Insets insets = comboBox.getInsets();
                int buttonWidth = icon.getIconWidth() + buttonInsets.left +
                                  buttonInsets.right;
		arrowButton.setBounds( (comboBox.getWidth() - insets.right - buttonWidth)
				,
                            insets.top, buttonWidth,
                            comboBox.getHeight() - insets.top - insets.bottom);
            }
            else {
                Insets insets = comboBox.getInsets();
                int width = comboBox.getWidth();
                int height = comboBox.getHeight();
                arrowButton.setBounds( insets.left, insets.top,
                                       width - (insets.left + insets.right),
                                       height - (insets.top + insets.bottom) );
            }
        

        if (editor != null /*&& MetalLookAndFeel.usingOcean()*/) {
            Rectangle cvb = rectangleForCurrentValue();
            editor.setBounds(cvb);
        }
    }

	protected JButton createArrowButton() {
		//JButton button = new XPComboBoxButton(comboBox, // 
		//		new MetalComboBoxIcon(), comboBox.isEditable(), currentValuePane, listBox);
		
		
		JButton button=new JButton(new ImageIcon(this.getClass().getResource("comboright.png")));
		button.setBackground(new Color(239, 235, 231));
		
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setBorderPainted(false);
		button.setBorder(null);
	
		return button;
	}

	public PropertyChangeListener createPropertyChangeListener() {
		return new XPPropertyChangeListener();
	}

	/**
	 * This inner class is marked &quot;public&quot; due to a compiler bug. This
	 * class should be treated as a &quot;protected&quot; inner class.
	 * Instantiate it only within subclasses of <FooUI>.
	 */
	public class XPPropertyChangeListener extends BasicComboBoxUI.PropertyChangeHandler {
		public void propertyChange(PropertyChangeEvent e) {
			super.propertyChange(e);
			String propertyName = e.getPropertyName();

			if (propertyName.equals("editable")) {
				JButton button =  arrowButton;
				//FIXME button.setIconOnly(comboBox.isEditable());
				comboBox.repaint();
			} else if (propertyName.equals("background")) {
				Color color = (Color) e.getNewValue();
				listBox.setBackground(color);

			} else if (propertyName.equals("foreground")) {
				Color color = (Color) e.getNewValue();
				listBox.setForeground(color);
			}
		}
	}

	/**
	 * As of Java 2 platform v1.4 this method is no longer used. Do not call or
	 * override. All the functionality of this method is in the
	 * MetalPropertyChangeListener.
	 * 
	 * @deprecated As of Java 2 platform v1.4.
	 */
	protected void editablePropertyChanged(PropertyChangeEvent e) {
	}

	/*protected LayoutManager createLayoutManager() {
		return new MetouiaComboBoxLayoutManager();
	}*/

	/**
	 * This inner class is marked &quot;public&quot; due to a compiler bug. This
	 * class should be treated as a &quot;protected&quot; inner class.
	 * Instantiate it only within subclasses of <FooUI>.
	 */
	/*public class MetouiaComboBoxLayoutManager implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension preferredLayoutSize(Container parent) {
			JComboBox cb = (JComboBox) parent;
			return parent.getPreferredSize();
		}

		public Dimension minimumLayoutSize(Container parent) {
			JComboBox cb = (JComboBox) parent;
			return parent.getMinimumSize();
		}

		public void layoutContainer(Container parent) {
			JComboBox cb = (JComboBox) parent;
			int width = cb.getWidth();
			int height = cb.getHeight();

			Rectangle cvb;

			if (comboBox.isEditable()) {
				if (arrowButton != null) {
					arrowButton.setBounds(width - comboBoxButtonSize, 0, comboBoxButtonSize, height);
				}
				if (editor != null) {
					cvb = rectangleForCurrentValue2();
					editor.setBounds(cvb);
				}
			} else {
				arrowButton.setBounds(0, 0, width, height);
			}
		}
	}

	protected Rectangle rectangleForCurrentValue2() {
		int width = comboBox.getWidth();
		int height = comboBox.getHeight();
		Insets insets = getInsets();
		int buttonSize = height - (insets.top + insets.bottom);
		if (arrowButton != null) {
			buttonSize = comboBoxButtonSize;
		}
		if (comboBox.getComponentOrientation().isLeftToRight()) {
			return new Rectangle(insets.left, insets.top, width - (insets.left + insets.right + buttonSize), height - (insets.top + insets.bottom));
		} else {
			return new Rectangle(insets.left + buttonSize, insets.top, width - (insets.left + insets.right + buttonSize), height - (insets.top + insets.bottom));
		}
	}

	
	protected void removeListeners() {
		if (propertyChangeListener != null) {
			comboBox.removePropertyChangeListener(propertyChangeListener);
		}
	}

	// These two methods were overloaded and made public. This was probably a
	// mistake in the implementation. The functionality that they used to
	// provide is no longer necessary and should be removed. However,
	// removing them will create an uncompatible API change.

	public void configureEditor() {
		super.configureEditor();
	}

	public void unconfigureEditor() {
		super.unconfigureEditor();
	}

	public Dimension getMinimumSize(JComponent c) {
		if (!isMinimumSizeDirty) {
			return new Dimension(cachedMinimumSize);
		}

		Dimension size = null;

		if (!comboBox.isEditable() && arrowButton != null ) {

			JButton button =  arrowButton;
			Insets buttonInsets = new Insets(0, 0, 0, 0);
			Insets insets = comboBox.getInsets();

			size = getDisplaySize();
			size.width += comboBoxButtonSize + insets.left + insets.right; // Hack
			size.width += buttonInsets.left + buttonInsets.right;
			size.width += buttonInsets.right + 9;// FIXME button.getComboIcon().getIconWidth();
			size.height += insets.top + insets.bottom;
			size.height += buttonInsets.top + buttonInsets.bottom;
			size.height = Math.max(21, size.height);
		} else if (comboBox.isEditable() && arrowButton != null && editor != null) {
			size = super.getMinimumSize(c);
			Insets margin = arrowButton.getMargin();
			Insets insets = comboBox.getInsets();
			if (editor instanceof JComponent) {
				Insets editorInsets = ((JComponent) editor).getInsets();
			}
			size.height += margin.top + margin.bottom;
			size.height += insets.top + insets.bottom;

			// size.height = Math.max(20,size.height);
		} else {
			size = super.getMinimumSize(c);
		}

		cachedMinimumSize.setSize(size.width, size.height);
		isMinimumSizeDirty = false;

		return new Dimension(cachedMinimumSize);
	}
*/
}
