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
 
 package org.openconcerto.openoffice.generation;

import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.openconcerto.openoffice.generation.desc.ReportPart;
import org.openconcerto.openoffice.generation.desc.ReportType;
import org.openconcerto.openoffice.generation.desc.part.ConditionalPart;
import org.openconcerto.openoffice.generation.desc.part.ForkReportPart;
import org.openconcerto.openoffice.generation.desc.part.GeneratorReportPart;
import org.openconcerto.openoffice.generation.desc.part.InsertReportPart;
import org.openconcerto.openoffice.generation.desc.part.SubReportPart;
import org.openconcerto.utils.Tuple2;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import ognl.Ognl;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.Filter;

/**
 * Représente la génération d'un rapport.
 * 
 * @author Sylvain Cuaz
 * @param <C> type of GenerationCommon
 */
public class ReportGeneration<C extends GenerationCommon> {

    static {
        OgnlRuntime.setPropertyAccessor(Element.class, new PropertyAccessor() {
            public Object getProperty(Map context, Object target, Object name) {
                Element elem = (Element) target;
                String n = (String) name;
                // retourne le premier, TODO collections, attributes
                return elem.getChild(n);
            }

            public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
                // impossible
                throw new OgnlException("", new UnsupportedOperationException("setProperty not supported on XML elements"));
            }
        });
    }

    // instance members

    private final ReportType type;
    private C common;
    // Inheritable to allow generators to spawn threads
    private final InheritableThreadLocal<ReportPart> currentParts;
    private final InheritableThreadLocal<DocumentGenerator> currentGenerator;
    private Throwable interruptCause;
    // tous les générateurs s'exécuter dans ce groupe
    private final ThreadGroup thg;
    private final List<PropertyChangeListener> taskListeners;
    private final PropertyChangeListener taskListener;
    private Map<String, Object> commonData;

    /**
     * Crée une nouvelle instance pour générer un rapport.
     * 
     * @param type le type de rapport à générer.
     */
    public ReportGeneration(ReportType type) {
        this.type = new ReportType(type, this);
        // ne pas créer common tout de suite car il peut faire appel à des propriétés initialisées
        // seulement après dans le constructeur d'1 sous classe
        this.common = null;
        this.commonData = null;

        this.currentParts = new InheritableThreadLocal<ReportPart>();
        this.currentGenerator = new InheritableThreadLocal<DocumentGenerator>();
        this.interruptCause = null;
        this.thg = new ThreadGroup("Generateurs") {
            public void uncaughtException(Thread t, Throwable e) {
                ReportGeneration.this.interrupt(e);
            }
        };

        this.taskListeners = new ArrayList<PropertyChangeListener>();
        this.taskListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                for (final PropertyChangeListener l : ReportGeneration.this.taskListeners) {
                    l.propertyChange(evt);
                }
            }
        };
    }

    protected final ODSingleXMLDocument createTaskAndGenerate(GeneratorReportPart part) throws IOException, InterruptedException {
        final GenerationTask task = new GenerationTask(part.getName(), this.getCommon().createGenerator(part));
        task.addPropertyChangeListener(this.taskListener);
        try {
            synchronized (this) {
                this.currentGenerator.set(task.getGenerator());
            }
            final ODSingleXMLDocument res = task.generate();
            synchronized (this) {
                this.currentGenerator.set(null);
            }
            return res;
        } catch (IOException exn) {
            throw new IOException("Impossible de générer '" + part + "'", exn);
        }
    }

    public void addTaskListener(PropertyChangeListener l) {
        this.taskListeners.add(l);
    }

    /**
     * A new document generation has just begun.
     */
    protected void beginGeneration() {
    }

    /**
     * Whether to insert a page break between report parts. This implementation always return
     * <code>true</code>.
     * 
     * @return <code>true</code> if a page break should be inserted.
     */
    protected boolean pageBreak() {
        return true;
    }

    /**
     * The GenerationCommon needed for this generation. This implementation just returns a
     * {@link GenerationCommon}.
     * 
     * @param name name of the requested common.
     * @return the corresponding common.
     */
    @SuppressWarnings("unchecked")
    protected C createCommon(String name) {
        return (C) new GenerationCommon<ReportGeneration<?>>(this);
    }

    /**
     * Génére le rapport.
     * 
     * @return le fichier généré, ou <code>null</code> si interruption.
     * @throws Throwable si erreur lors de la génération.
     */
    public final ODSingleXMLDocument generate() throws Throwable {
        final Map<String, ODSingleXMLDocument> res = this.generateMulti();
        if (res.size() != 1)
            throw new IllegalStateException("more than one document: " + res);
        else
            return res.get(null);
    }

    /**
     * Generate a report with multiple documents. The main document has the <code>null</code> ID.
     * 
     * @return the generated documents, indexed by ID, or <code>null</code> if interrupted.
     * @throws Throwable if an error occurs.
     * @see #generate()
     */
    public final Map<String, ODSingleXMLDocument> generateMulti() throws Throwable {
        synchronized (this) {
            this.interruptCause = null;
        }

        Map<String, ODSingleXMLDocument> f = null;
        final FutureTask<Map<String, ODSingleXMLDocument>> future = new FutureTask<Map<String, ODSingleXMLDocument>>(new Callable<Map<String, ODSingleXMLDocument>>() {
            public Map<String, ODSingleXMLDocument> call() throws Exception {
                return createDocument();
            }
        });
        final Thread thr = new Thread(this.thg, future);
        thr.start();
        try {
            thr.join();
            f = future.get();
        } catch (InterruptedException e) {
            f = null;
        } catch (Exception e) {
            this.interrupt(e);
        }

        final Map<String, ODSingleXMLDocument> res;
        synchronized (this) {
            if (this.interruptCause != null) {
                throw this.interruptCause;
            } else if (Thread.currentThread().isInterrupted() || f == null) {
                res = null;
            } else {
                res = f;
            }
        }
        return res;
    }

    protected final void interrupt(Throwable cause) {
        synchronized (this) {
            if (this.interruptCause == null) {
                this.interruptCause = cause;
                this.thg.interrupt();
            }
        }
    }

    private Map<String, ODSingleXMLDocument> createDocument() throws IOException, OgnlException, InterruptedException {
        // recompute common data for each run
        this.commonData = null;
        this.beginGeneration();

        final Map<String, ODSingleXMLDocument> res = new HashMap<String, ODSingleXMLDocument>();
        res.put(null, this.createEmptyDocument());

        // les threads
        final Map<String, GenThread> forked = new HashMap<String, GenThread>();
        // a stack to handle SubReportPart (and their optional document)
        final Stack<Tuple2<Iterator<ReportPart>, ODSingleXMLDocument>> s = new Stack<Tuple2<Iterator<ReportPart>, ODSingleXMLDocument>>();
        s.push(Tuple2.create(this.type.getParts().iterator(), res.get(null)));
        while (hasNext(s) && !Thread.currentThread().isInterrupted()) {
            final Iterator<ReportPart> i = s.peek().get0();
            final ODSingleXMLDocument currentDoc = s.peek().get1();
            final ReportPart part = i.next();

            // always set current part, so that the condition can use it.
            synchronized (this) {
                this.currentParts.set(part);
            }
            if (this.mustGenerate(part)) {
                if (part instanceof ForkReportPart) {
                    GenThread thread = new GenThread(part.getName(), ((ForkReportPart) part).getChildren());
                    forked.put(part.getName(), thread);
                    thread.start();
                } else if (part instanceof SubReportPart) {
                    final SubReportPart subReportPart = (SubReportPart) part;
                    // the document for <sub>
                    final ODSingleXMLDocument newDoc;
                    final String docID = subReportPart.getDocumentID();
                    if (docID == null)
                        newDoc = currentDoc;
                    else if (res.containsKey(docID)) {
                        newDoc = res.get(docID);
                    } else {
                        newDoc = this.createEmptyDocument();
                        res.put(docID, newDoc);
                    }
                    // ajoute ses enfants
                    s.push(Tuple2.create(subReportPart.getChildren().iterator(), newDoc));
                } else if (part instanceof InsertReportPart) {
                    final GenThread thread = forked.get(part.getName());
                    if (thread == null)
                        throw new IllegalStateException(part.getName() + " has not been forked previously.");
                    final List<ODSingleXMLDocument> forkedList = thread.getRes();
                    if (forkedList == null) {
                        Thread.currentThread().interrupt();
                    } else {
                        for (final ODSingleXMLDocument doc : forkedList) {
                            add(currentDoc, doc);
                        }
                    }
                } else {
                    add(currentDoc, this.createTaskAndGenerate(((GeneratorReportPart) part)));
                }
            }
            synchronized (this) {
                this.currentParts.set(null);
            }
        }
        return res;
    }

    private boolean hasNext(final Stack<Tuple2<Iterator<ReportPart>, ODSingleXMLDocument>> s) {
        if (s.peek().get0().hasNext())
            return true;
        else {
            // the current iterator is done, so remove it
            s.pop();
            if (s.isEmpty())
                return false;
            else
                return this.hasNext(s);
        }
    }

    private ODSingleXMLDocument createEmptyDocument() throws IOException, InterruptedException {
        final ODSingleXMLDocument f;
        final DocumentGenerator templateGenerator = this.getCommon().getStyleTemplateGenerator(this.type.getTemplate());
        if (templateGenerator == null)
            try {
                f = ODSingleXMLDocument.createFromPackage(this.type.getTemplate());
            } catch (JDOMException e) {
                throw new IOException("invalid template " + this.type.getTemplate(), e);
            }
        else
            f = templateGenerator.generate();

        // seulement intéressé par les styles et les user fields
        // TODO y passer dans fwk OO
        f.getBody().removeContent(new Filter() {
            public boolean matches(Object obj) {
                if (obj instanceof Element) {
                    final Element elem = (Element) obj;
                    final boolean isUserField = elem.getNamespace().equals(f.getVersion().getTEXT()) && elem.getName().equals("user-field-decls");
                    return !isUserField;
                } else
                    return true;
            }
        });

        this.getCommon().preProcessDocument(f);

        return f;
    }

    private final void add(final ODSingleXMLDocument f, final ODSingleXMLDocument toAdd) {
        // whether the added document is the first following the style template
        f.add(toAdd, f.getNumero() > 0 && pageBreak());
    }

    private final boolean mustGenerate(ReportPart part) throws OgnlException {
        if (part instanceof ConditionalPart) {
            final ConditionalPart p = (ConditionalPart) part;
            return p.getCondition() == null || ((Boolean) Ognl.getValue(p.getCondition(), getCommonData())).booleanValue();
        } else
            return true;
    }

    /**
     * Une thread qui pour une liste de générateurs.
     */
    private class GenThread extends Thread {

        private final List m;
        private final List<ODSingleXMLDocument> res;

        public GenThread(String name, List generators) {
            super(name);
            this.m = generators;
            this.res = new ArrayList<ODSingleXMLDocument>(generators.size());
        }

        public void run() {
            final Iterator i = this.m.iterator();
            try {
                while (i.hasNext() && !Thread.currentThread().isInterrupted()) {
                    // ATTN les fork ne peuvent être imbriqués
                    final GeneratorReportPart part = (GeneratorReportPart) i.next();
                    this.res.add(ReportGeneration.this.createTaskAndGenerate(part));
                }
            } catch (Exception e) {
                ReportGeneration.this.interrupt(e);
            }
        }

        /**
         * Retourne la liste des documents générés.
         * 
         * @return la liste, ou bien <code>null</code> s'il y a interruption.
         */
        public final List<ODSingleXMLDocument> getRes() {
            // on s'assure d'avoir fini
            try {
                if (!this.isInterrupted()) {
                    this.join();
                    return this.res;
                }
            } catch (InterruptedException exn) {
                // justement on fait rien
            }
            return null;
        }
    }

    // *** getter

    /**
     * Ognl data used in the evaluation of conditions among other things.
     * 
     * @return a map of objects.
     */
    public final Map<String, Object> getCommonData() {
        if (this.commonData == null) {
            // set it before initializing it, that way even if initCommonData() needs a previous
            // value stored it won't loop infinitely (eg initCommonData() has
            // { put("a", "A") ; put("bPlus", getReportType().getParam("b")+"Plus"); }
            // and "b" is defined as a + "B" )
            this.commonData = new HashMap<String, Object>();
            this.initCommonData(this.commonData);
        }
        return Collections.unmodifiableMap(this.commonData);
    }

    protected void initCommonData(final Map<String, Object> res) {
        res.put("rg", this);
        res.put("variante", this.getReportType().getParam("variante"));
        res.put("dateFmt", DateFormat.getDateInstance(DateFormat.LONG));
        try {
            res.put("join", Ognl.getValue(":[@org.openconcerto.utils.CollectionUtils@join( #this, #sep == null ? ', ' : #sep )]", null));
            res.put("silentFirst", Ognl.getValue(":[#this.size == 0 ? null : #this[0]]", null));
        } catch (OgnlException exn) {
            // n'arrive jamais, la syntaxe est correcte
            exn.printStackTrace();
        }
    }

    public String toString() {
        return "generation of '" + this.getReportType() + "'";
    }

    public final C getCommon() {
        if (this.common == null)
            this.common = this.createCommon(this.type.getCommon());
        return this.common;
    }

    public final ReportType getReportType() {
        return this.type;
    }

    public synchronized final ReportPart getCurrentPart() {
        return this.currentParts.get();
    }

    public synchronized final DocumentGenerator getCurrentGenerator() {
        return this.currentGenerator.get();
    }
}
