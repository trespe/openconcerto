package org.openconcerto.modules.customerrelationship.call.ovh;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.VirtualMenu;
import org.openconcerto.utils.ExceptionHandler;

public class CallActionFactory implements IListeAction {

    @Override
    public ButtonsBuilder getHeaderButtons() {
        return ButtonsBuilder.emptyInstance();
    }

    @Override
    public Action getDefaultAction(IListeEvent evt) {
        return null;
    }

    @Override
    public PopupBuilder getPopupContent(PopupEvent evt) {
        final PopupBuilder actions = new PopupBuilder(this.getClass().getPackage().getName());
        final SQLRowAccessor rowClient = evt.getSelectedRow();

        // Tel fixe
        final String tel = getCleanTel(rowClient.getString("TEL"));
        if (tel != null) {
            final JMenuItem createCallAction = createCallAction("Appeler le standard (" + tel + ")", tel, rowClient.getString("NOM"));
            actions.addItem(createCallAction);
        }
        // Tel portable
        final String telP = getCleanTel(rowClient.getString("TEL_P"));
        if (telP != null) {
            final JMenuItem createCallAction = createCallAction("Appeler sur mobile (" + telP + ")", tel, rowClient.getString("NOM"));
            actions.addItem(createCallAction);
        }

        final SQLTable tableContact = rowClient.getTable().getTable("CONTACT");
        final Collection<? extends SQLRowAccessor> l = rowClient.asRow().getReferentRows(tableContact);
        final VirtualMenu contactActions = actions.getMenu().getSubmenu("Appeler");
        for (SQLRowAccessor sqlRowAccessor : l) {
            String t = getCleanTel(sqlRowAccessor.getString("TEL_DIRECT"));
            String nom = concatNotNull(sqlRowAccessor, new String[] { "PRENOM", "NOM" });
            final String fonction = sqlRowAccessor.getString("FONCTION");
            if (fonction != null && !fonction.isEmpty()) {
                if (!nom.isEmpty()) {
                    nom += ", " + fonction;
                } else {
                    nom = fonction;
                }
            }
            if (t != null && !nom.isEmpty()) {
                contactActions.addItem(createCallAction("Appeler " + nom + " (" + t + ")", t, rowClient.getString("NOM") + ", " + nom));
            }
            t = getCleanTel(sqlRowAccessor.getString("TEL_MOBILE"));
            if (t != null && !nom.isEmpty()) {
                contactActions.addItem(createCallAction("Appeler le mobile de " + nom + " (" + t + ")", t, rowClient.getString("NOM") + ", " + nom));
            }
        }
        return actions;
    }

    private String concatNotNull(SQLRowAccessor sqlRowAccessor, String[] fields) {
        String v = "";
        for (int i = 0; i < fields.length; i++) {
            final String value = sqlRowAccessor.getString(fields[i]);
            if (value != null) {
                v += value + " ";
            }
        }
        return v.trim();
    }

    private JMenuItem createCallAction(final String label, final String tel, final String nom) {
        return new JMenuItem(new AbstractAction(label) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final Thread t = new Thread(new Runnable() {
                        @Override
                        final public void run() {
                            try {
                                OVHApi.call(tel);
                            } catch (IOException e) {
                                JOptionPane.showMessageDialog(null, e.getMessage());
                            }

                        }
                    });
                    t.setName("OVH Call to " + tel);
                    t.start();
                    final SQLElement element = Configuration.getInstance().getDirectory().getElement(Module.TABLE_NAME);
                    final SQLRowValues r = new SQLRowValues(element.getTable());
                    r.put("DATE", new Date());
                    r.put("TYPE", "Appel émis");
                    r.put("NUMBER_FROM", OVHApi.getProperties().getProperty("from", "?"));
                    r.put("NUMBER_TO", tel);
                    r.put("FROM", UserManager.getInstance().getUser(UserManager.getUserID()).getFullName());
                    r.put("TO", nom);
                    r.put("DESCRIPTION", "");
                    r.put("DURATION", 0);
                    final SQLRow row = r.insert();
                    final EditFrame editFrame = new EditFrame(element, EditPanel.MODIFICATION);
                    editFrame.selectionId(row.getID());
                    editFrame.setVisible(true);
                } catch (Exception ex) {
                    ExceptionHandler.handle("Echec de l'appel", ex);
                }
            }
        });
    }

    public static String getCleanTel(String tel) {
        if (tel == null || tel.isEmpty()) {
            return null;
        }
        final StringBuilder b = new StringBuilder(10);
        for (int i = 0; i < tel.length(); i++) {
            final char ch = tel.charAt(i);
            if (Character.isDigit(ch)) {
                b.append(ch);
            }
        }
        return b.toString();
    }
}
