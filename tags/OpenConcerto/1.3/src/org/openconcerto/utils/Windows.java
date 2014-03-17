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
 
 /*
 * Créé le 15 oct. 2004
 *
 */
package org.openconcerto.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author ilm
 * 
 * TODO Pour changer le modèle de ce commentaire de type généré, allez à :
 * Fenêtre - Préférences - Java - Style de code - Modèles de code
 */
public class Windows {
    public static List getRunningProcess() {
        Vector result = new Vector();
        try {
            int i = 0;
            String e = "ps.exe";
            /*
             * e = PATH + " " +
             * "file:///C:/eclipse/workspace/ImmoNegoce/Photos/3/index.html";
             */
            //System.out.println(e);
            // e = e.replace('\\', '/');
            Process p = Runtime.getRuntime().exec(e);
            String s = null;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            
            while ((s = stdInput.readLine()) != null) {
                i++;
                if (i > 8) {
                    //System.out.println(s);
                    StringTokenizer tok=new StringTokenizer(s);
                    if(tok.hasMoreTokens())
                        result.add(tok.nextToken().trim());
                }
            }
            stdInput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    public static void kill(String processName){
        try {
            int i = 0;
            String e = "kill.exe "+processName;
           
            Process p = Runtime.getRuntime().exec(e);
            String s = null;
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            
            while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                    
                
            }
            stdInput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
	public static void openFile(String file){
			try {
				int i = 0;
				String e = "explorer.exe "+file;
           
				Process p = Runtime.getRuntime().exec(e);
				String s = null;
				BufferedReader stdInput = new BufferedReader(new InputStreamReader(
						p.getInputStream()));

            
				while ((s = stdInput.readLine()) != null) {
						System.out.println(s);
                    
                
				}
				stdInput.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    
}
