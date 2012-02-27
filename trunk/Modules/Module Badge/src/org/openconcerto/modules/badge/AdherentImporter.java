package org.openconcerto.modules.badge;

import java.io.File;
import java.io.IOException;
import java.sql.Date;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.apache.poi.ss.usermodel.DateUtil;
import org.openconcerto.erp.importer.ArrayTableModel;
import org.openconcerto.erp.importer.DataImporter;
import org.openconcerto.erp.importer.RowValuesNavigatorPanel;
import org.openconcerto.erp.importer.ValueConverter;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLTable;

public class AdherentImporter {

    private ArrayTableModel model;
    private SQLTable tableClient;

    public AdherentImporter(File file) throws IOException {
        tableClient = Configuration.getInstance().getRoot().findTable("ADHERENT");
        final DataImporter importer = new DataImporter(tableClient);
        importer.setSkipFirstLine(true);
        model = importer.createModelFrom(file);
    }

    public void importAdherent() throws IOException {
        final DataImporter importer = new DataImporter(tableClient);
        importer.setSkipFirstLine(true);

        importer.map(0, tableClient.getField("NOM"));
        importer.map(1, tableClient.getField("PRENOM"));
        importer.map(4, tableClient.getField("DATE_VALIDITE_INSCRIPTION"), new ValueConverter(tableClient.getField("DATE_VALIDITE_INSCRIPTION")) {
            @Override
            public Object convertFrom(Object obj) {
                if (obj instanceof Double) {
                    Double d = (Double) obj;
                    return DateUtil.getJavaDate(d);
                }
                return new Date(System.currentTimeMillis());
            }
        });

        final ArrayTableModel m = importer.createConvertedModel(this.model);

        importer.importFromModel(m);

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                final RowValuesNavigatorPanel p = new RowValuesNavigatorPanel();
                p.setRowValuesToInsert(importer.getValuesToInsert());
                p.setRowValuesToUpdate(importer.getValuesToUpdate());
                final JFrame f = new JFrame("Import");
                f.setContentPane(p);
                f.pack();
                f.setVisible(true);
                f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            }
        });

    }
}
