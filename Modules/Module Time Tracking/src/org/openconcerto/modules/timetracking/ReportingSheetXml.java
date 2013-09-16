package org.openconcerto.modules.timetracking;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openconcerto.erp.generationDoc.AbstractListeSheetXml;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.cc.ITransformer;

public class ReportingSheetXml extends AbstractListeSheetXml {

    private final static SQLTable tableAffTemps = base.getTable("AFFAIRE_TEMPS");

    private final static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    protected int id;

    private final SQLRow rowMonth;
    private final SQLRow rowUser;
    private final SQLRow rowProject;
    private Date date;
    private final Date debut;
    private final Date fin;
    public static String TEMPLATE_ID = "Reporting";
    public static String TEMPLATE_PROPERTY_NAME = "LocationReporting";

    public ReportingSheetXml(int year, SQLRow month, SQLRow user, SQLRow project) {
        super();

        this.printer = PrinterNXProps.getInstance().getStringProperty("JournauxPrinter");

        this.rowMonth = month;
        this.rowUser = user;
        this.rowProject = project;
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.YEAR, year);
        int moisD = rowMonth == null ? -1 : rowMonth.getID() - 2;
        if (moisD >= 0) {
            c.set(Calendar.MONTH, moisD);
            c.set(Calendar.DAY_OF_MONTH, 1);
            this.debut = c.getTime();
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            this.fin = c.getTime();
        } else {
            c.set(Calendar.MONTH, Calendar.JANUARY);
            c.set(Calendar.DAY_OF_MONTH, 1);
            this.debut = c.getTime();
            c.set(Calendar.MONTH, Calendar.DECEMBER);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            this.fin = c.getTime();
        }

    }

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected String getStoragePathP() {
        return "Reporting";
    }

    @Override
    public String getName() {
        if (this.date == null) {
            this.date = new Date();
        }
        return "Reporting" + this.date.getTime();
    }

    protected void createListeValues() {

        final SQLRowValues vals = new SQLRowValues(tableAffTemps);

        vals.putRowValues("ID_USER_COMMON").put("NOM", null);
        vals.put("TEMPS", null);
        SQLRowValues rowValsAff = vals.putRowValues("ID_AFFAIRE");
        rowValsAff.putRowValues("ID_CLIENT").put("NOM", null);
        rowValsAff.put("NUMERO", null);
        vals.put("DATE", null);
        vals.put("DESCRIPTIF", null);
        SQLRowValues rowValsCmd = vals.putRowValues("ID_COMMANDE_CLIENT_ELEMENT");
        rowValsCmd.put("NOM", null);

        SQLRowValues rowValsFamille = rowValsCmd.putRowValues("ID_ARTICLE").putRowValues("ID_FAMILLE_ARTICLE");
        rowValsFamille.put("NOM", null);

        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(vals);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect sel) {

                final SQLTable tableProjectTime = sel.getTable("AFFAIRE_TEMPS");
                Where w = new Where(tableProjectTime.getField("DATE"), debut, fin);
                if (rowUser != null && !rowUser.isUndefined()) {
                    w = w.and(new Where(tableProjectTime.getField("ID_USER_COMMON"), "=", rowUser.getID()));
                }
                if (rowProject != null && !rowProject.isUndefined()) {
                    w = w.and(new Where(tableProjectTime.getField("ID_AFFAIRE"), "=", rowProject.getID()));
                }
                sel.setWhere(w);
                sel.addFieldOrder(tableProjectTime.getField("DATE"));
                return sel;
            }
        });

        final List<SQLRowValues> list = fetcher.fetch();

        final List<Map<String, Object>> tableauVals = new ArrayList<Map<String, Object>>();
        this.listAllSheetValues.put(0, tableauVals);

        final Map<Integer, String> style = new HashMap<Integer, String>();
        this.styleAllSheetValues.put(0, style);
        double tempsTotal = 0;
        for (SQLRowValues rowEcr : list) {

            final Map<String, Object> values = new HashMap<String, Object>();
            values.put("DATE", dateFormat.format(rowEcr.getDate("DATE").getTime()));

            float temps = rowEcr.getFloat("TEMPS");
            tempsTotal += temps;
            values.put("TEMPS", temps);
            values.put("AFFAIRE_NUMERO", rowEcr.getForeign("ID_AFFAIRE").getString("NUMERO"));

            values.put("DESCRIPTIF", rowEcr.getString("DESCRIPTIF"));

            final SQLRowAccessor cmdElt = rowEcr.getForeign("ID_COMMANDE_CLIENT_ELEMENT");
            if (cmdElt != null) {
                SQLRowAccessor article = cmdElt.getForeign("ID_ARTICLE");
                if (article != null) {
                    SQLRowAccessor famille = article.getForeign("ID_FAMILLE_ARTICLE");
                    if (famille != null) {
                        values.put("FAMILLE", famille.getString("NOM"));
                    }
                }
                values.put("NOM", cmdElt.getString("NOM"));
            }
            values.put("CLIENT", rowEcr.getForeign("ID_AFFAIRE").getForeign("ID_CLIENT").getString("NOM"));

            tableauVals.add(values);

        }

        final Map<String, Object> sheetVals = new HashMap<String, Object>();
        this.mapAllSheetValues.put(0, sheetVals);

        if (rowUser != null && !rowUser.isUndefined()) {
            sheetVals.put("NOM", rowUser.getString("PRENOM") + " " + rowUser.getString("NOM"));
        } else {
            sheetVals.put("NOM", "Tous");
        }
        sheetVals.put("DATE", this.date);
        sheetVals.put("PERIODE", "du " + dateFormat.format(debut) + " au " + dateFormat.format(fin));
        sheetVals.put("TEMPS_TOTAL", tempsTotal);
    }
}
