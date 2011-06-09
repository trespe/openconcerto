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

import org.openconcerto.utils.ExceptionHandler;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/** Ecoute une proprièté du bean (M) et le PropertyView (V).
 * @author ILM Informatique 1 juil. 2004
 */
class PropertyController implements PropertyChangeListener {
	// la feuille de notre vue
	private final PropertySheet sheet;
	private PropertyView view;
	private PropertyDescriptor desc;

	// est-ce que le modele est mis a jour immédiatement
	private boolean autoUpdate;
	// si non le cache de la vue
	private Object viewValueCache;

	public PropertyController(PropertySheet sheet, PropertyDescriptor desc, PropertyView view) {
		this.autoUpdate = false;
		this.viewValueCache = null;

		this.sheet = sheet;
		this.desc = desc;
		this.view = view;
		// on ecoute les changements de la vue
		this.view.addListener(this);

		// on initialise la vue (notre proprièté est encore à null)
		this.initView();
	}

	/** La valeur de notre proprièté. Si notre bean est null, renvoie <code>null</code>.
	 * @return la valeur de notre proprièté.
	 */
	private Object getPropertyValue() {
		Object result = null;
		if (this.sheet.getTarget() != null) {
			try {
				result = this.desc.getReadMethod().invoke(this.getTarget(), new Object[0]);
			} catch (IllegalArgumentException e) {
				// impossible (pas d'arguments)
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// impossible (un getter est public)
				e.printStackTrace();
			} catch (InvocationTargetException exn) {
				ExceptionHandler.die("Impossible d'obtenir la valeur de " + this.getName(), exn);
			}
		}
		return result;
	}

	private void setPropertyValue(Object val) {
		// TODO exn
		try {
			desc.getWriteMethod().invoke(this.getTarget(), new Object[] { val });
		} catch (IllegalArgumentException exn) {
			ExceptionHandler.die("Impossible de changer la valeur de " + this.getName(), exn);
		} catch (IllegalAccessException exn) {
			ExceptionHandler.die("Impossible de changer la valeur de " + this.getName(), exn);
		} catch (InvocationTargetException exn) {
			ExceptionHandler.die("Impossible de changer la valeur de " + this.getName(), exn);
		}
	}

	public String getName() {
		return this.desc.getName();
	}

	/**
	 * Initialise la vue avec la valeur de la proprièté.
	 */
	private void initView() {
		this.updateView(this.getPropertyValue());
		this.view.setEditable(this.isEditable());
	}

	private boolean isSettable() {
		return this.sheet.getTarget() != null && this.isEditable();
	}

	private boolean isEditable() {
		return this.desc.getWriteMethod() != null;
	}

	private void updateView(Object modelValue) {
		this.view.update(modelValue);
	}

	/**
	 * Met à jour le modèle avec la dernière valeur de la vue. Noop si
	 * pas de bean ou bien pas de setter.
	 * <p>
	 * Note:
	 * Est appelée automatiquement en autoUpdate.
	 * </p>
	 */
	public void updateModel() {
		if (this.isSettable())
			this.setPropertyValue(this.viewValueCache);
	}

	/** S'enleve des listeners de l'ancien bean, se rajoute au nouveau, et change la vue. 
	 * @param old l'ancien bean (peut être <code>null</code>).
	 */
	protected void targetChanged(Bean old) {
		this.targetChanged(old, true);
	}

	/** S'enleve des listeners de l'ancien bean, se rajoute au nouveau, et suivant updateView
	 * change la vue pour refléter le nouveau bean, ou bien change le nouveau bean pour 
	 * refléter la vue. 
	 * @param old l'ancien bean (peut être <code>null</code>).
	 */
	protected void targetChanged(Bean old, boolean updateView) {
		if (old != null)
			old.removePropertyChangeListener(this.desc.getName(), this);
		if (this.sheet.getTarget() != null)
			this.sheet.getTarget().addPropertyChangeListener(this.desc.getName(), this);
		if (updateView)
			this.initView();
		else
			this.updateModel();
	}

	/** La vue ou la proprièté a changé. Ne pas toujours propager le changement pour eviter les
	 * récursions infinies. 
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO code cleanup
		if (evt.getSource() == this.sheet.getTarget()) {
			// ne changer que si les valeurs sont diff
			if (different(evt.getOldValue(), evt.getNewValue()))
				this.updateView(evt.getNewValue());
		} else if (evt.getSource() == this.view) {
			this.viewValueCache = evt.getNewValue();
			// si l'on peut changer la valeur et qu'elle est différente
			if (this.autoUpdate && this.isSettable() && different(evt.getOldValue(), evt.getNewValue()))
				this.updateModel();
		}
	}

	private boolean different(Object o1, Object o2) {
		return (o1 != null || o2 != null) && (o1 == null || o2 == null || !o1.equals(o2));
	}

	public String toString() {
		return "PropController[" + this.getName() + "]=" + this.viewValueCache;
	}

	private Object getTarget() throws InvocationTargetException {
		return DottedPropertyDescriptor.getTarget(this.desc, this.sheet.getTarget());
	}
}
