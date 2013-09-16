/*
 * Créé le 7 juin 2012
 */
package org.openconcerto.modules.timetracking.mail;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.mail.PasswordAuthentication;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.modules.ModuleFactory;
import org.openconcerto.modules.timetracking.Module;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;

public class MailingTimeTracking {

    public MailingTimeTracking(CollectionMap<Number, SQLRowValues> map, ModuleFactory factory) {
        final Preferences prefs = factory.getPreferences(false, ComptaPropsConfiguration.getInstanceCompta().getRootSociete());
        final String address = prefs.get(Module.ADR_MAIL_PREFS, "").trim();
        final String smtpServer = prefs.get(Module.SMTP_PREFS, "").trim();
        if (address.length() == 0 || smtpServer.length() == 0) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "Impossible d'envoyer les temps par mail." + "\n Un serveur SMTP et une adresse mail doivent être définis dans les préférences.");
                }
            });
        }

        final Account account = new Account(prefs.get(Module.EXPEDITEUR_MAIL_PREFS, ""), address, smtpServer, prefs.getInt(Module.PORT_MAIL_PREFS, 25), prefs.getBoolean(Module.SSL_MAIL_PREFS,
                Boolean.FALSE));

        final String idMail = prefs.get(Module.ID_MAIL_PREFS, "").trim();
        if (idMail.length() != 0) {
            account.setAuth(new PasswordAuthentication(idMail, prefs.get(Module.PWD_MAIL_PREFS, "")));
        }

        final DateFormat format = new SimpleDateFormat("dd/MM/yy");
        final Set<Number> s = map.keySet();

        for (Number number : s) {
            final List<SQLRowValues> rowVals = (List<SQLRowValues>) map.getNonNull(number);
            String corps = prefs.get(Module.ENTETE_MAIL_PREFS, "") + "\n";
            String recipient = null;
            String client = "";

            final List<SQLRowValues> rowValsUp = new ArrayList<SQLRowValues>();
            for (SQLRowValues sqlRowValues : rowVals) {
                corps += "\n" + format.format(sqlRowValues.getDate("DATE").getTime());
                corps += ", " + sqlRowValues.getFloat("TEMPS") + "h";
                SQLRowAccessor foreign = sqlRowValues.getForeign("ID_USER_COMMON");
                corps += ", " + foreign.getString("NOM") + " " + foreign.getString("PRENOM");
                corps += ", " + sqlRowValues.getString("DESCRIPTIF");
                if (recipient == null) {
                    SQLRowAccessor foreign2 = sqlRowValues.getForeign("ID_AFFAIRE").getForeign("ID_CLIENT");
                    recipient = foreign2.getString("MAIL");
                    client = foreign2.getString("NOM");
                }
                final SQLRowValues rowValsToUp = sqlRowValues.createEmptyUpdateRow();
                rowValsToUp.put("ENVOYE_PAR_MAIL", Boolean.TRUE);
                rowValsUp.add(rowValsToUp);
            }

            corps += "\n\n" + prefs.get(Module.PIED_MAIL_PREFS, "");

            if (recipient != null && recipient.trim().length() > 0) {
                try {
                    final Mail mail = new Mail(prefs.get(Module.SUBJECT_MAIL_PREFS, ""), recipient);
                    mail.setText(corps);
                    mail.send(account);
                    for (SQLRowValues sqlRowValues : rowValsUp) {
                        try {
                            sqlRowValues.update();
                        } catch (SQLException exn) {
                            exn.printStackTrace();
                        }
                    }
                } catch (Exception exn) {
                    ExceptionHandler.handle("Erreur lors de l'envoi du message!", exn);
                }
            } else {
                final String nomClient = client;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, "Impossible d'envoyer les temps par mail pour le client : " + nomClient + "\n L'adresse mail n'est pas définie.");
                    }
                });
            }
        }

    }
}
