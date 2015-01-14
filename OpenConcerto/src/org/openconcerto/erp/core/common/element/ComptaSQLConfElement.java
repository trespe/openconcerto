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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;

import org.openconcerto.erp.config.Log;

import org.openconcerto.erp.config.Gestion;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.ui.light.GroupToLightUIConvertor;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.AutoHideListener;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.light.LightUIDescriptor;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.convertor.ValueConvertor;

import java.awt.Component;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * SQLElement de la base société
 * 
 * @author Administrateur
 * 
 */
public abstract class ComptaSQLConfElement extends SQLElement {

    private static DBRoot baseSociete;
    public static final TableCellRenderer CURRENCY_RENDERER = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Component res = super.getTableCellRendererComponent(table, GestionDevise.currencyToString((BigDecimal) value), isSelected, hasFocus, row, column);
            // this renderer can be decorated by e.g. ListeFactureRenderer which does a
            // setBackground(), thus always reset the colors
            // MAYBE always use ProxyComp as in AlternateTableCellRenderer to leave the decorated
            // renderer as found
            TableCellRendererUtils.setColors(res, table, isSelected);
            ((JLabel) res).setHorizontalAlignment(SwingConstants.RIGHT);
            return res;
        }
    };

    static public final JPanel createAdditionalPanel() {
        return AutoHideListener.listen(new JPanel());
    }

    private static DBRoot getBaseSociete() {
        if (baseSociete == null)
            baseSociete = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete();
        return baseSociete;
    }

    private Group groupForCreation;
    private Group groupForModification;

    {
        this.setL18nLocation(Gestion.class);
    }

    public ComptaSQLConfElement(String tableName, String singular, String plural) {
        super(singular, plural, getBaseSociete().findTable(tableName, true));
    }

    public ComptaSQLConfElement(String tableName) {
        this(tableName, null);
    }

    public ComptaSQLConfElement(String tableName, String code) {
        super(getBaseSociete().findTable(tableName, true), null, code);
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage();
    }

    /**
     * Return a code that doesn't change when subclassing to allow to easily change a SQLElement
     * while keeping the same code. To achieve that, the code isn't
     * {@link #createCodeFromPackage(Class) computed} with <code>this.getClass()</code>. We iterate
     * up through our superclass chain, and as soon as we find an abstract class, we stop and use
     * the previous class (i.e. non abstract). E.g. any direct subclass of
     * {@link ComptaSQLConfElement} will still use <code>this.getClass()</code>, but so is one of
     * its subclass.
     * 
     * @return a code computed from the superclass just under the first abstract superclass.
     * @see #createCodeFromPackage(Class)
     */
    protected final String createCodeFromPackage() {
        return createCodeFromPackage(getLastNonAbstractClass());
    }

    private final Class<? extends ComptaSQLConfElement> getLastNonAbstractClass() {
        Class<?> prev = null;
        Class<?> cl = this.getClass();
        // test loop
        assert !Modifier.isAbstract(cl.getModifiers()) && ComptaSQLConfElement.class.isAssignableFrom(cl) && Modifier.isAbstract(ComptaSQLConfElement.class.getModifiers());
        while (!Modifier.isAbstract(cl.getModifiers())) {
            prev = cl;
            cl = cl.getSuperclass();
        }
        assert ComptaSQLConfElement.class.isAssignableFrom(prev);
        @SuppressWarnings("unchecked")
        final Class<? extends ComptaSQLConfElement> res = (Class<? extends ComptaSQLConfElement>) prev;
        return res;
    }

    static protected String createCodeFromPackage(final Class<? extends ComptaSQLConfElement> cl) {
        String canonicalName = cl.getName();
        if (canonicalName.contains("erp.core") && canonicalName.contains(".element")) {
            int i = canonicalName.indexOf("erp.core") + 9;
            int j = canonicalName.indexOf(".element");
            canonicalName = canonicalName.substring(i, j);
        }
        return canonicalName;
    }

    @Override
    protected void _initTableSource(SQLTableModelSourceOnline res) {
        super._initTableSource(res);
        for (final SQLTableModelColumn col : res.getColumns()) {
            // TODO getDeviseFields()
            if (col.getValueClass() == Long.class || col.getValueClass() == BigInteger.class) {
                col.setConverter(new ValueConvertor<Number, BigDecimal>() {
                    @Override
                    public BigDecimal convert(Number o) {
                        if (o == null) {
                            System.err.println("ComptaSQLConfElement._initTableSource: Warning null Number conversion (" + this + ")");
                            return BigDecimal.ZERO;
                        }
                        return new BigDecimal(o.longValue()).movePointLeft(2);
                    }

                    @Override
                    public Number unconvert(BigDecimal o) {

                        if (o == null) {
                            System.err.println("ComptaSQLConfElement._initTableSource: Warning null BigDecimal conversion (" + this + ")");
                            return 0;
                        }
                        return o.movePointRight(2);
                    }
                }, BigDecimal.class);
                col.setRenderer(CURRENCY_RENDERER);
            }
        }
    }

    public LightUIDescriptor getUIDescriptorForCreation(PropsConfiguration configuration) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        final Group group = getGroupForCreation();
        if (group == null) {
            Log.get().severe("The group for creation is null for this element : " + this);
            return null;
        }
        final LightUIDescriptor desc = convertor.convert(group);
        return desc;
    }

    public LightUIDescriptor getUIDescriptorForModification(PropsConfiguration configuration, long id) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        final Group group = getGroupForModification();
        if (group == null) {
            Log.get().severe("The group for modification is null for this element : " + this);
            return null;
        }
        final LightUIDescriptor desc = convertor.convert(getGroupForModification());
        return desc;
    }

    public Group getGroupForCreation() {
        if (groupForCreation != null) {
            return groupForCreation;
        }
        return getDefaultGroup();
    }

    public Group getGroupForModification() {
        if (groupForModification != null) {
            return groupForModification;
        }
        return getDefaultGroup();
    }
}
