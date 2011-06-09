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
 
 package org.openconcerto.utils.beans;

import org.openconcerto.utils.AutoLayouter;
import org.openconcerto.utils.ExceptionHandler;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Un panel pour afficher un bean. Ce panel n'affiche qu'un type de beans.
 * @author ILM Informatique 30 juin 2004
 */
public class PropertySheet extends JPanel {

	/** Le type de bean que ce panel affiche */
	private Class targetClass;
	/** Les controllers qui assurent la liaison entre les vues et modèles des prop */
	private List controllers;
	/** Le bean qui est edité */
	private Bean target;
	// si la feuille reste éditable sans bean
	private boolean allowNullEditing;

	private PropertySheet(Class targetClass, Bean bean) {
		this.allowNullEditing = true;
		this.controllers = new ArrayList();
		this.setTargetClass(targetClass);
		this.setTarget(bean);
	}

	/** Crée une feuille pour éditer ce type de bean.
	 * @param targetClass le type de bean a éditer.
	 */
	public PropertySheet(Class targetClass) {
		this(targetClass, null);
	}

	/** Crée une feuille pour éditer ce bean. Cette feuille ne pourra
	 * donc éditer que des beans du même type.
	 * @param bean le bean à éditer.
	 */
	public PropertySheet(Bean bean) {
		this(bean.getClass(), bean);
	}

	/** Returns the bean currently being edited.
	 * @return the bean currently being edited.
	 */
	public Bean getTarget() {
		return this.target;
	}

	/** Change the bean being edited and update the sheet.
	 * @param bean the new bean to edit, <code>null</code> to just stop editing current bean.
	 */
	public void setTarget(Bean bean) {
		this.setTarget(bean, true);
	}

	/** Change the bean being edited and update it with the content of the sheet.
	 * @param bean the new bean to edit
	 * @throws NullPointerException if <code>null</code> is passed.
	 */
	public void setTarget_UpdateFromView(Bean bean) {
		if (bean == null)
			throw new NullPointerException("cannot update null from view !");
		this.setTarget(bean, false);
	}

	private void setTarget(Bean bean, boolean updateView) {
		if (bean != null && this.targetClass != bean.getClass())
			throw new IllegalArgumentException("this bean cannot be edited with this property sheet");

		// prevenir les controller
		Bean old = this.getTarget();
		this.target = bean;
		Iterator i = this.controllers.iterator();
		while (i.hasNext()) {
			PropertyController controller = (PropertyController) i.next();
			controller.targetChanged(old, updateView);
		}

		// on (des)active si null
		if (!allowNullEditing) {
			// ne pas faire old != bean, dans le constructeur les 2 sont null
			if (old == null || bean == null) {
				Component[] children = this.getComponents();
				for (int j = 0; j < children.length; j++) {
					Component child = children[j];
					child.setEnabled(bean != null);
				}
			}
		}
	}

	/**
	 * Mets a jour le bean a partir de l'interface. L'inverse est automatique.
	 */
	public void commit() {
		Iterator i = this.controllers.iterator();
		while (i.hasNext()) {
			PropertyController controller = (PropertyController) i.next();
			controller.updateModel();
		}
	}

	/**
	 * Réinitialise l'interface a partir du bean.
	 */
	public void reset() {
		this.setTarget(this.getTarget());
	}

	private void setTargetClass(Class clazz) {
		if (this.targetClass != null)
			throw new IllegalStateException("targetClass already configured");
		if (!Bean.class.isAssignableFrom(clazz))
			throw new IllegalArgumentException("targetClass is not a CBean");
		this.targetClass = clazz;

		this.setUp(this.targetClass);
	}

	private void setUp(Class clazz) {
		PropertyDescriptor[] properties = null;
		try {
			BeanInfo bi = Introspector.getBeanInfo(clazz);
			properties = bi.getPropertyDescriptors();
		} catch (IntrospectionException exn) {
			ExceptionHandler.die("PropertySheet: Couldn't introspect", exn);
		}

		AutoLayouter autoLayouter = new AutoLayouter(this);
		autoLayouter.getConstraints().fill = GridBagConstraints.BOTH;
		for (int i = 0; i < properties.length; i++) {

			// Don't display hidden or expert properties.
			if (properties[i].isHidden() || properties[i].isExpert()) {
				continue;
			}

			String name = properties[i].getDisplayName();
			Class type = properties[i].getPropertyType();
			Method getter = properties[i].getReadMethod();

			// Only display properties that can be read.
			if (getter == null) {
				continue;
			}

			PropertyView view = null;
			PropertyEditor editor = PropertyEditorManager.findEditor(type);

			// If we can't edit this component, skip it.
			if (editor == null) {
				continue;
			}

			// Now figure out how to display it...
			if (editor.isPaintable() && editor.supportsCustomEditor()) {
				view = new PropertyCanvas(null, editor);
			} else if (editor.getTags() != null) {
				view = new PropertySelector(editor);
			} else if (editor.getAsText() != null) {
				view = new PropertyText(editor);
			} else {
				ExceptionHandler.die("Warning: Property \"" + name + "\" has non-displayabale editor.  Skipping.");
				continue;
			}
			this.controllers.add(new PropertyController(this, properties[i], view));
			// ajout dans l'ui
			autoLayouter.add(name, (JComponent) view);
		}
	}

}
