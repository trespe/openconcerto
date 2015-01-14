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
 
 package org.openconcerto.erp.core.supplychain.stock.element;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;

import java.util.ArrayList;
import java.util.List;

public class StockItem {

    public enum Type {
        REEL, THEORIQUE
    };

    private double realQty, virtualQty, receiptQty, deliverQty;
    public SQLRowAccessor article;

    List<StockItemComponent> components = new ArrayList<StockItemComponent>();

    public StockItem(SQLRowAccessor article) {
        this.article = article;
        if (this.article.isForeignEmpty("ID_STOCK")) {
            this.realQty = 0;
            this.virtualQty = 0;
            this.receiptQty = 0;
            this.deliverQty = 0;
        } else {
            SQLRowAccessor row = this.article.getForeign("ID_STOCK");
            this.realQty = row.getFloat("QTE_REEL");
            this.virtualQty = row.getFloat("QTE_TH");
            this.receiptQty = row.getFloat("QTE_RECEPT_ATTENTE");
            this.deliverQty = row.getFloat("QTE_LIV_ATTENTE");
        }
    }

    public void updateQty(double qty, Type t) {
        updateQty(qty, t, false);
    }

    public SQLRowAccessor getArticle() {
        return article;
    };

    public void addItemComponent(StockItemComponent item) {
        this.components.add(item);
    };

    public void updateQtyFromChildren() throws IllegalArgumentException {
        if (components.size() == 0) {
            throw new IllegalArgumentException("Impossible de calculé les quantités depuis les composants. Cet article n'est pas composé!");
        }
        StockItemComponent comp = components.get(0);
        double real = comp.getItem().getRealQty() == 0 ? 0 : Math.ceil(comp.getItem().getRealQty() / (comp.getQty() * comp.getQtyUnit().doubleValue()));
        double virtual = comp.getItem().getVirtualQty() == 0 ? 0 : Math.ceil(comp.getItem().getVirtualQty() / (comp.getQty() * comp.getQtyUnit().doubleValue()));
        for (StockItemComponent stockItemComponent : components) {
            real = Math.min(
                    real,
                    stockItemComponent.getItem().getRealQty() == 0 ? 0 : Math.ceil(stockItemComponent.getItem().getRealQty()
                            / (stockItemComponent.getQty() * stockItemComponent.getQtyUnit().doubleValue())));
            virtual = Math.min(
                    virtual,
                    stockItemComponent.getItem().getVirtualQty() == 0 ? 0 : Math.ceil(stockItemComponent.getItem().getVirtualQty()
                            / (stockItemComponent.getQty() * stockItemComponent.getQtyUnit().doubleValue())));

        }
        this.realQty = real;
        this.virtualQty = virtual;
    }

    /**
     * Mise à jour des quantités de stocks. Stock Reel : inc/dec QTE_REEL, inc/dec
     * QTE_LIV_ATTENTE/inc/dec QTE_RECEPT_ATTENTE Stock Th : inc/dec QTE_TH, inc/dec
     * QTE_LIV_ATTENTE/inc/dec QTE_RECEPT_ATTENTE
     * 
     * @param qty quantité à ajouter ou à soustraire
     * @param t Type de stock à mettre à jour (réel ou virtuel)
     * @param archive annulation du stock
     */
    public void updateQty(double qty, Type t, boolean archive) {

        if (t == Type.REEL) {
            final double qteNvlle;
            final double qteOrigin = this.realQty;
            if (archive) {
                qteNvlle = qteOrigin - qty;
                // Réception
                if (qty > 0) {
                    this.receiptQty += qty;
                } else {
                    // Livraison
                    this.deliverQty -= qty;
                }
            } else {
                qteNvlle = qteOrigin + qty;
                // Réception
                if (qty > 0) {
                    this.receiptQty -= qty;
                } else {
                    // Livraison
                    this.deliverQty += qty;
                }
            }

            this.realQty = qteNvlle;

        } else {
            final double qteNvlle;
            final double qteOrigin = this.virtualQty;
            if (archive) {
                qteNvlle = qteOrigin - qty;
                // Réception
                if (qty > 0) {
                    this.receiptQty -= qty;
                } else {
                    // Livraison
                    this.deliverQty += qty;
                }
            } else {
                qteNvlle = qteOrigin + qty;
                // Réception
                if (qty > 0) {
                    this.receiptQty += qty;
                } else {
                    // Livraison
                    this.deliverQty -= qty;
                }
            }

            this.virtualQty = qteNvlle;
        }
    }

    public double getDeliverQty() {
        return deliverQty;
    }

    public double getRealQty() {
        return realQty;
    }

    public double getReceiptQty() {
        return receiptQty;
    }

    public double getVirtualQty() {
        return virtualQty;
    }

    public boolean isStockInit() {
        return !this.article.isForeignEmpty("ID_STOCK");
    }

    public String getUpdateRequest() {
        final SQLTable stockTable = this.article.getTable().getForeignTable("ID_STOCK");
        UpdateBuilder update = new UpdateBuilder(stockTable);
        update.setWhere(new Where(stockTable.getKey(), "=", getArticle().getForeign("ID_STOCK").getID()));
        update.setObject("QTE_REEL", getRealQty());
        update.setObject("QTE_TH", getVirtualQty());
        update.setObject("QTE_LIV_ATTENTE", getDeliverQty());
        update.setObject("QTE_RECEPT_ATTENTE", getReceiptQty());
        return update.asString();
    }

}
