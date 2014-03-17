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
 
 package org.openconcerto.erp.panel;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.preferences.UserProps;
import org.openconcerto.ui.tips.Tip;
import org.openconcerto.ui.tips.TipsFrame;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class ComptaTipsFrame extends TipsFrame {

    private ComptaTipsFrame(boolean checked) {
        super(checked);

    }

    @Override
    protected void checkBoxModified(boolean selected) {
        System.err.println("CheckBox changed to " + selected);
        UserProps.getInstance().setProperty("HideTips", String.valueOf(!selected));
        UserProps.getInstance().store();
    }

    private static ComptaTipsFrame f;

    public static ComptaTipsFrame getFrame(boolean checked) {

        if (f != null) {
            return f;
        } else {
            f = new ComptaTipsFrame(checked);

            Tip t1 = new Tip();
            t1.addText("Les fonctions de gestion courante se trouvent dans le menu 'Saisie'.");
            t1.addText("  ");
            t1.addText("Vous y trouverez les interfaces de création :");
            t1.addText("- des devis et factures");
            t1.addText("- des achats, livraisons et mouvements stocks");
            t1.addText("- des commandes et bons de réception");
            t1.addText("  ");
            t1.addImage(ComptaTipsFrame.class.getResource("tips_gnx.png"));

            f.addTip(t1);

            Tip t2 = new Tip();
            t2.addText("Le logiciel intègre un module de cartographie.");
            t2.addText("  ");
            t2.addImage(ComptaTipsFrame.class.getResource("tips_map.png"));
            f.addTip(t2);

            Tip t3 = new Tip();
            t3.addText(Configuration.getInstance().getAppName() + " fonctionne sous Windows 7, Vista et XP");
            t3.addText("et aussi sous Linux et MacOS...");
            t3.addText("  ");
            t3.addImage(ComptaTipsFrame.class.getResource("tips_os.png"));
            f.addTip(t3);

            Tip t4 = new Tip();
            t4.addText("Les sélecteurs intègrent l'auto-complétion.");
            t4.addText(" ");
            t4.addText("Vous commencez à écrire, le logiciel suggère les possibilités!");
            t4.addText(" ");
            t4.addImage(ComptaTipsFrame.class.getResource("tips_auto.png"));
            f.addTip(t4);

            Tip t5 = new Tip();
            t5.addText("Un clic droit sur une ligne...");
            t5.addText("et les fonctionnalités contextuelles apparaissent.");
            t5.addImage(ComptaTipsFrame.class.getResource("tips_click.png"));
            f.addTip(t5);

            Tip t6 = new Tip();
            t6.addText("Le logiciel peut mémoriser des valeurs (textes).");
            t6.addText(" ");
            t6.addText("Un clic droit sur les sélecteurs vous permet d'ajouter et de supprimer des éléments.");
            t6.addText(" ");
            t6.addImage(ComptaTipsFrame.class.getResource("tips_combo.png"));
            f.addTip(t6);

            Tip t7 = new Tip();
            t7.addText("Les listes possèdent une recherche intégrée.");

            t7.addText("Les résutats sont visibles en temps réel.");
            t7.addText(" ");

            t7.addImage(ComptaTipsFrame.class.getResource("tips_search.png"));
            t7.addText(" ");
            t7.addText("Dans cet exemple, nous avons immédiatement les éléments relatifs");
            t7.addText("à M. Blanc en 2010 !");
            f.addTip(t7);

            Tip t8 = new Tip();
            t8.addText("Tous les documents créés par le logiciel sont standards.");
            t8.addText("");
            t8.addText("Nous utilisons pour cela : ");
            t8.addText("- format PDF  ");
            t8.addText("- format OpenDocument utilisable avec OpenOffice");
            t8.addText(" ");
            t8.addImage(ComptaTipsFrame.class.getResource("tips_oo.png"));
            f.addTip(t8);

            Tip t9 = new Tip();
            t9.addText("Vous pouvez envoyer directement les documents par email.");
            t9.addText(" ");
            t9.addText("Le logiciel est compatible avec :");
            t9.addText("- Outlook, Outlook Express et Mail");
            t9.addText("- Thunderbird ");
            t9.addText(" ");
            t9.addImage(ComptaTipsFrame.class.getResource("tips_firefox.png"));
            f.addTip(t9);

            Tip t10 = new Tip();
            t10.addText("Le logiciel est basé sur la technologie SQL.");
            t10.addText(" ");
            t10.addText("Le logiciel fonctionne avec les bases de données :");
            t10.addText("- postgreSQL");
            t10.addText("- MySQL ");
            t10.addText("- H2 ");
            t10.addText(" ");
            t10.addImage(ComptaTipsFrame.class.getResource("tips_db.png"));
            f.addTip(t10);

            Tip t11 = new Tip();
            t11.addText("Le logiciel bloque les saisies incorrectes.");
            t11.addText(" ");
            t11.addText("Si le bouton ajouter reste grisé, un clic affiche la cause.");
            t11.addText(" ");
            t11.addImage(ComptaTipsFrame.class.getResource("tips_add.png"));
            f.addTip(t11);
            f.setCurrentTip(0);
            // don't setAlwaysOnTop(true) since this will hide errors and emergency module frame
            // (plus this is system wide)
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return f;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        TipsFrame f = ComptaTipsFrame.getFrame(true);
        // Centrage
        f.setLocationRelativeTo(null);
        f.setCurrentTip(0);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

}
