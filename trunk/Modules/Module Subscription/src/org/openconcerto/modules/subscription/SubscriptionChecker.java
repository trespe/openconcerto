/*
 * Créé le 30 mai 2012
 */
package org.openconcerto.modules.subscription;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelectJoin;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

public class SubscriptionChecker {

    private final String type;
    private final SQLTable table;

    public SubscriptionChecker(SQLTable table) {
        this.table = table;

        if (this.table.getName().equalsIgnoreCase("SAISIE_VENTE_FACTURE")) {
            this.type = "FACTURE";
        } else if (this.table.getName().equalsIgnoreCase("DEVIS")) {
            this.type = "DEVIS";
        } else {
            this.type = "COMMANDE";
        }
    }

    public Map<SQLRow, Calendar> check() {

        // Fectcher pour récupérer la derniere facture ou devis de chaque abonnement
        final SQLRowValues vals = new SQLRowValues(this.table);
        vals.put(this.table.getKey().getName(), null);
        vals.put("NUMERO", null);
        vals.put("DATE", null);
        vals.put("ID_ABONNEMENT", null);

        final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(vals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {

                TableRef tableFAlias = sel.getAlias(SubscriptionChecker.this.table);
                SQLSelectJoin join = sel.addJoin("RIGHT", SubscriptionChecker.this.table, "f2", new Where(tableFAlias.getField("ARCHIVE"), "=", 0));
                Where w = new Where(join.getJoinedTable().getField("DATE"), "<=", tableFAlias.getField("DATE"));
                w = w.and(new Where(join.getJoinedTable().getField("ID_ABONNEMENT"), "=", tableFAlias.getField("ID_ABONNEMENT")));
                join.setWhere(w);
                sel.setWhere(new Where(tableFAlias.getField("ID_ABONNEMENT"), "IS NOT", (Object) null));

                System.err.println(sel.asString());
                return sel;
            }
        });

        List<SQLRowValues> list = fetcher.fetch();

        Map<Number, SQLRowValues> map = new HashMap<Number, SQLRowValues>();
        for (SQLRowValues sqlRow : list) {
            map.put(sqlRow.getInt("ID_ABONNEMENT"), sqlRow);
        }

        // Verification du renouvellement des abonnements
        SQLSelect selAb = new SQLSelect(this.table.getDBSystemRoot(), true);
        SQLTable tableAb = Configuration.getInstance().getRoot().findTable("ABONNEMENT");
        selAb.addSelectStar(tableAb);
        List<SQLRow> rows = SQLRowListRSH.execute(selAb);

        // Liste des abonnements associés à la prochaine date de renouvellement
        Map<SQLRow, Calendar> aboFactureRenouvel = new HashMap<SQLRow, Calendar>();

        Date toDay = new Date();

        for (SQLRow sqlRow : rows) {
            if (sqlRow.getBoolean("CREATE_" + this.type)) {
                int nbMois = sqlRow.getInt("NB_MOIS_" + this.type);

                // On recupere la derniere date de l'abonnement soit la date de debut soit la date
                // de la derniere facture
                Calendar calStartFact = sqlRow.getDate("DATE_DEBUT_" + this.type);
                SQLRowValues rowFact = map.get(sqlRow.getID());
                if (rowFact != null) {
                    if (rowFact.getObject("CREATION_AUTO_VALIDER") != null && rowFact.getBoolean("CREATION_AUTO_VALIDER")) {
                        calStartFact = rowFact.getDate("DATE");
                    } else {
                        // Si le dernier element de l'abonnement n'a pas été validé on ne crée pas
                        // le prochain
                        continue;
                    }
                }

                calStartFact.add(Calendar.MONTH, nbMois);
                if (toDay.after(calStartFact.getTime())) {

                    aboFactureRenouvel.put(sqlRow, calStartFact);
                }
            }
        }
        return aboFactureRenouvel;
    }

}
