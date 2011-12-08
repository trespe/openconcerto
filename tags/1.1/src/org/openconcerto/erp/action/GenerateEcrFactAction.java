package org.openconcerto.erp.action;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieVenteFacture;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

public class GenerateEcrFactAction extends AbstractAction {

    public GenerateEcrFactAction() {
        this.putValue(Action.NAME, "Regénérer les écritures des factures");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (JOptionPane.showConfirmDialog(null, "Etes vous sûr de vouloir effectuer cette tâche?") == JOptionPane.YES_OPTION) {
            generate();
        }

    }

    private void generate() {

        SQLSelect sel = new SQLSelect(Configuration.getInstance().getBase());
        SQLTable tableFact = Configuration.getInstance().getBase().getTable("SAISIE_VENTE_FACTURE");

        sel.addSelectStar(tableFact);

        sel.setWhere(new Where(tableFact.getField("PREVISIONNELLE"), "=", Boolean.TRUE));
        List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString());

        for (SQLRow sqlRow : l) {
            SQLRowValues rowVals = sqlRow.asRowValues();
            rowVals.put("PREVISIONNELLE", Boolean.TRUE);
            try {
                rowVals.update();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            new GenerationMvtSaisieVenteFacture(sqlRow.getID());
        }
        JOptionPane.showMessageDialog(null, "Fin de l'import");
    }
}
