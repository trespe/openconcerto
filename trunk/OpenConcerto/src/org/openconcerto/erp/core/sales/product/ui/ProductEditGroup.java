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
 
 package org.openconcerto.erp.core.sales.product.ui;

import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.LayoutHints;

public class ProductEditGroup extends Group {

    public ProductEditGroup() {
        super("sales.product");
        final Group g = new Group("sales.quote.identifier");
        g.addItem("sales.product.code");
        g.addItem("sales.product.family");
        g.addItem("sales.product.label", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        g.addItem("sales.product.barcode");

        this.add(g);

        final Group gSales = new Group("sales.product.sales");
        gSales.addItem("sales.product.sales.price");
        gSales.addItem("sales.product.tax");
        gSales.addItem("sales.product.sales.price.total");
        gSales.addItem("sales.product.sales.unit");

        gSales.addItem("sales.product.margin.min", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        gSales.addItem("sales.product.sales.shipment", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        gSales.addItem("sales.product.pricelist.items", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        add(gSales);

        final Group gPurchase = new Group("sales.product.purchase");
        // gPurchase.addItem("sales.product.purchase.unit");
        gPurchase.addItem("sales.product.purchase.price");
        gPurchase.addItem("sales.product.purchase.frombom");
        gPurchase.addItem("sales.product.supplier");
        gPurchase.addItem("sales.product.purchase.quantity");
        gPurchase.addItem("sales.product.purchase.shipment");

        add(gPurchase);

        final Group gInventory = new Group("sales.product.inventory");
        gInventory.addItem("sales.product.stockable", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        gInventory.addItem("sales.product.quantity.min");
        gInventory.addItem("sales.product.quantity.max");
        add(gInventory);

        final Group gDesc = new Group("sales.product.description");
        gDesc.addItem("sales.product.description.text", new LayoutHints(true, true, true, true, true, true, true, true));
        add(gDesc);

        final Group gInternationalization = new Group("sales.product.i18n");
        gInternationalization.addItem("sales.product.i18n.items", new LayoutHints(true, true, true, true, true, true, true, true));
        add(gInternationalization);
        final Group gAccounting = new Group("sales.product.accounting");
        gAccounting.addItem("sales.product.sales.account", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        gAccounting.addItem("sales.product.purchase.account", LayoutHints.DEFAULT_VERY_LARGE_FIELD_HINTS);
        gAccounting.addItem("sales.product.service");
        add(gAccounting);

        final Group gBom = new Group("sales.product.bom");
        gBom.addItem("sales.product.bom.merge");
        gBom.addItem("sales.product.bom.items", LayoutHints.DEFAULT_SEPARATED_GROUP_HINTS);
        add(gBom);

        final Group gInfo = new Group("sales.product.information");
        gInfo.addItem("sales.product.deprecated");
        gInfo.addItem("sales.product.needserial");

        final Group gDimension = new Group("sales.product.dimension", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        gDimension.addItem("sales.product.length");
        gDimension.addItem("sales.product.width");
        gDimension.addItem("sales.product.weight");
        final Group gExport = new Group("sales.product.info.export", LayoutHints.DEFAULT_NOLABEL_SEPARATED_GROUP_HINTS);
        gExport.addItem("sales.product.customs.code");
        gInfo.add(gDimension);
        gInfo.add(gExport);
        gInfo.addItem("sales.product.comment", LayoutHints.DEFAULT_VERY_LARGE_TEXT_HINTS);
        add(gInfo);

    }
}
