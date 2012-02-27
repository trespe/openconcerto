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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class AbstractListeColumnSheetXml extends SheetXml {

    // Valeur de la liste
    protected Map<Integer, List<Map<String, Object>>> listAllSheetValues = new HashMap<Integer, List<Map<String, Object>>>();

    // Style des lignes
    protected Map<Integer, Map<Integer, String>> styleAllSheetValues = new HashMap<Integer, Map<Integer, String>>();

    // Valeur à l'extérieur de la liste
    protected Map<Integer, Map<String, Object>> mapAllSheetValues = new HashMap<Integer, Map<String, Object>>();

    // Nom des feuilles
    protected List<String> sheetNames = new ArrayList<String>();

    private File generatedOpenDocumentFile;

    public AbstractListeColumnSheetXml() {
        generatedOpenDocumentFile = new File(getDocumentOutputDirectory(), getValidFileName(getName()) + ".ods");
    }

    public final Future<SheetXml> createDocumentAsynchronous() {
        Callable<SheetXml> c = new Callable<SheetXml>() {
            @Override
            public SheetXml call() throws Exception {
                try {
                    createListeValues();
                    generatedOpenDocumentFile = OOgenerationListeColumnXML.genere(getTemplateId(), getDocumentOutputDirectory(), getValidFileName(getName()), listAllSheetValues, mapAllSheetValues,
                            styleAllSheetValues, sheetNames, null);
                    return AbstractListeColumnSheetXml.this;
                } catch (Exception e) {
                    DEFAULT_HANDLER.uncaughtException(null, e);
                    // rethrow exception so that the unsuspecting caller can use this as the
                    // original task
                    throw e;
                } catch (Throwable e) {
                    DEFAULT_HANDLER.uncaughtException(null, e);
                    return null;
                }

            }
        };
        return runnableQueue.submit(c);
    }

    /**
     * To fill listAllSheetValues, styleAllSheetValues, mapAllSheetValues, sheetNames
     */
    protected abstract void createListeValues();

    @Override
    public String getStoragePathP() {
        return StringUtils.firstUp(elt.getPluralName());
    }

    @Override
    public File getGeneratedFile() {
        return generatedOpenDocumentFile;
    }

    @Override
    public File getDocumentOutputDirectoryP() {
        return DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(this.getTemplateId());
    }

    @Override
    public File getPDFOutputDirectoryP() {
        return DocumentLocalStorageManager.getInstance().getPDFOutputDirectory(this.getTemplateId());
    }
}
