package org.openconcerto.modules.label;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;

import org.openconcerto.erp.modules.AbstractModule;
import org.openconcerto.erp.modules.ComponentsContext;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.IListeAction.IListeEvent;
import org.openconcerto.sql.view.list.RowAction.PredicateRowAction;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.StringUtils;

public final class ModuleLabel extends AbstractModule {

    public ModuleLabel(ModuleFactory f) throws IOException {
        super(f);
    }

    @Override
    protected void setupComponents(ComponentsContext ctxt) {
        final String actionName = "Imprimer les étiquettes";
        final PredicateRowAction predicateRowAction = new PredicateRowAction(new AbstractAction(actionName) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {

                    final IListe list = IListe.get(arg0);
                    final LabelFrame f = new LabelFrame(list.copySelectedRows(), new LabelRenderer() {

                        @Override
                        public void paintLabel(Graphics g, SQLRowAccessor row, int x, int y, int gridWith, int gridHeight) {
                            // Default font at 10pt black
                            g.setColor(Color.BLACK);
                            final float fontSize = 10f;
                            g.setFont(g.getFont().deriveFont(fontSize));
                            // Labels borders
                            final int hBorder = 12;
                            final int vBorder = 8;
                            // Product name
                            final String text = row.getString("NOM");
                            final List<String> l = StringUtils.wrap(text, g.getFontMetrics(), gridWith - 2 * hBorder);
                            final int lineHeight = g.getFontMetrics().getHeight();
                            int lineY = y + (int) ((gridHeight - l.size() * lineHeight) / 2);
                            for (String line : l) {
                                g.drawString(line, x + hBorder, lineY);
                                lineY += lineHeight;
                            }
                            // Price
                            g.setFont(g.getFont().deriveFont(fontSize + 2));
                            final String price = GestionDevise.currencyToString(row.getBigDecimal("PV_TTC")) + " € TTC";
                            final Rectangle2D r2 = g.getFont().getStringBounds(price, g.getFontMetrics().getFontRenderContext());
                            g.drawString(price, x + (int) (gridWith - hBorder - r2.getWidth()), y + gridHeight - vBorder);

                        }
                    });
                    f.setTitle(actionName);
                    f.setLocationRelativeTo(null);
                    f.pack();
                    f.setVisible(true);
                } catch (Exception e) {
                    ExceptionHandler.handle("Erreur d'impression", e);
                }

            }
        }, true, false);
        predicateRowAction.setPredicate(IListeEvent.createSelectionCountPredicate(1, Integer.MAX_VALUE));
        ctxt.getElement("ARTICLE").getRowActions().add(predicateRowAction);
    }

    @Override
    protected void start() {
    }

    @Override
    protected void stop() {
    }
}
