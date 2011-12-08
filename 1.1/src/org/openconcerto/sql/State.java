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
 
 package org.openconcerto.sql;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * Une classe qui permet de connaître l'état du Framework en temps réel. Le serveur est lancé sur le
 * premier port disponible à partir de PORT.
 * 
 * @author Sylvain CUAZ
 */
public final class State extends Thread {

    public static final String DEAF = "org.openconcerto.sql.deafState";

    public static final int PORT = 1394;

    public static final boolean DEBUG = true;

    public static final org.openconcerto.sql.State INSTANCE = new org.openconcerto.sql.State();

    public static final DateFormat TIME_FMT;

    static {
        TIME_FMT = DateFormat.getTimeInstance();
        TIME_FMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final List<String> requests;

    private final List<String> failed;

    private int failedStmts;

    private int connectionTotalCount;

    private int connectionCount;

    private int requestsTotalCount;

    private final long upDate;

    private int frameCount;

    private int framesVisible;

    private PrintWriter out;

    private Thread thread;

    private int cacheHit;

    private final ScriptEngine engine;
    private final Bindings ognlCtxt;

    /**
     * Once created, this thread starts immediately.
     */
    private State() {
        super("State thread");
        this.setDaemon(true);
        this.requests = new ArrayList<String>();
        this.requestsTotalCount = 0;
        this.failed = new ArrayList<String>();
        this.failedStmts = 0;
        this.connectionTotalCount = 0;
        this.connectionCount = 0;
        this.frameCount = 0;
        this.framesVisible = 0;
        this.cacheHit = 0;
        this.upDate = System.currentTimeMillis();
        this.engine = new ScriptEngineManager().getEngineByName("javascript");
        this.ognlCtxt = new SimpleBindings();

        if (!Boolean.getBoolean(DEAF))
            this.start();
    }

    private Bindings getContext() {
        this.ognlCtxt.put("base", this.getBase());
        this.ognlCtxt.put("dir", Configuration.getInstance().getDirectory());
        return this.ognlCtxt;
    }

    /**
     * Crée la socket du serveur.
     * 
     * @return la socket ou <code>null</code> si pb.
     */
    private ServerSocket createSocket() {
        ServerSocket serverSocket = null;
        int port = PORT;
        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
                Log.get().info("listening on " + port);
            } catch (BindException e) {
                Log.get().config("port " + port + " already in use");
                // on essaye le suivant
                port++;
            } catch (IOException e) {
                Log.get().warning("cannot create a socket");
                e.printStackTrace();
                return null;
            }
        }
        return serverSocket;
    }

    public void run() {
        final ServerSocket serverSocket = this.createSocket();
        if (serverSocket == null)
            return;

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                Log.get().info("accepted");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.out = new PrintWriter(clientSocket.getOutputStream(), true);
                this.out.println("Console d'administration: " + clientSocket.getLocalSocketAddress() + " <-> " + clientSocket.getRemoteSocketAddress());
                this.out.println("help (ou h) pour l'aide");
                this.out.println("quit (ou q) pour quitter");
                this.out.flush();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("q") || inputLine.startsWith("quit"))
                        break;
                    if (inputLine.equals("h") || inputLine.startsWith("help"))
                        printHelp();

                    try {
                        this.out.println(getAnswer(inputLine));
                    } catch (Exception exn) {
                        exn.printStackTrace();
                        exn.printStackTrace(this.out);
                    }
                    this.out.print(">");
                    this.out.flush();
                }

                in.close();
                if (this.thread != null)
                    this.thread.interrupt();
                this.out.close();
                clientSocket.close();
                Log.get().info("closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // TODO detect when app quit to clean up
        // serverSocket.close();
    }

    private void printHelp() {
        this.out.println("Commandes possibles:");
        this.out.println("req     : affiche la liste des requêtes en cours");
        this.out.println("all     : affiche les stats du logiciels");
        this.out.println("gc      : lance le garbage collector");
        this.out.println("log LEVEL [loggerName] : définit un niveau de log");
        this.out.println("unarchive TABLE ID     : désarchive un élément d'une table (et propage le desarchivage)");
        this.out.println("\t ex: unarchive SITE 125 va désarchiver BATIMENTs etc...");
        this.out.println("set    : définit une variable OGNL ou affiche la liste");
        this.out.println("\t ex: set siteT base.getTable('SITE')");
        this.out.println("eval   : evalue une expression OGNL");
        this.out.println("\t ex: #siteT.getRow(123).getReferentRows()");
        this.out.println();
        this.out.flush();
    }

    private String getAnswer(String question) throws SQLException {
        question = question.trim();
        if (question.equals("req"))
            return this.requests.toString();
        else if (question.equals("all")) {
            return getFull();
        } else if (question.equals("gc")) {
            System.gc();
            return "System.gc() called.";
        } else if (question.startsWith("log")) {
            // log LEVEL [loggerName]
            final String[] args = question.split(" ");
            final String level = args[1];
            final Level l;
            try {
                final Field f = Level.class.getField(level);
                l = (Level) f.get(null);
            } catch (Exception e) {
                return "cannot get field '" + level + "' of Level";
            }
            final String name = args.length > 2 ? args[2] : "";
            Logger.getLogger(name).setLevel(l);
            return "logger '" + name + "' level set to " + l.getName();
        } else if (question.equals("allc")) {
            this.thread = new Thread() {
                public void run() {
                    int i = 0;
                    while (true) {
                        org.openconcerto.sql.State.this.out.println("--------------");
                        org.openconcerto.sql.State.this.out.println("Iteration: " + i++);
                        org.openconcerto.sql.State.this.out.println("--------------");
                        org.openconcerto.sql.State.this.out.println(getFull());
                        org.openconcerto.sql.State.this.out.println("\n");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // s'arrêter
                            break;
                        }
                    }
                }
            };
            this.thread.start();
            return "beginning";
        } else if (question.startsWith("unarchive")) {
            return archive(question, false);
        } else if (question.startsWith("archive")) {
            return archive(question, true);
        } else if (question.startsWith("eval")) {
            try {
                return ognlResult(eval(question.substring("eval".length())));
            } catch (ScriptException e) {
                return ExceptionUtils.getStackTrace(e);
            }
        } else if (question.startsWith("set")) {
            final String[] args = question.split(" ", 3);
            if (args.length == 1) {
                return CollectionUtils.join(this.ognlCtxt.entrySet(), "\n");
            } else {
                try {
                    final Object res = eval(args[2]);
                    this.ognlCtxt.put(args[1], res);
                    return ognlResult(res);
                } catch (ScriptException e) {
                    return ExceptionUtils.getStackTrace(e);
                }
            }
        } else
            return "commande non connue.";
    }

    private String archive(String question, boolean arch) throws SQLException {
        final String[] args = question.split(" ");
        final String tableName = args[1];
        final int id = Integer.parseInt(args[2]);
        final String s;
        if (arch) {
            this.getElement(tableName).archive(id);
            s = "archived";
        } else {
            this.getElement(tableName).unarchive(id);
            s = "unarchived";
        }
        return s + " " + this.getBase().getTable(tableName).getRow(id);
    }

    public final Object eval(String s) throws ScriptException {
        return this.engine.eval(s, this.getContext());
    }

    private String ognlResult(Object res) {
        return res == null ? "<no result>" : res.toString();
    }

    private final SQLBase getBase() {
        return Configuration.getInstance().getBase();
    }

    private final SQLElement getElement(String tableName) {
        return Configuration.getInstance().getDirectory().getElement(tableName);
    }

    private String getFull() {
        String res = "failed requests: " + this.failed.size();
        res += "\nfailed statements: " + this.failedStmts;
        res += "\nrequests: " + this.requests.size();
        res += "\ntotal requests: " + this.requestsTotalCount;
        res += "\nconnections: " + this.connectionCount;
        res += "\ntotal connections: " + this.connectionTotalCount;
        res += "\ncache hit: " + this.cacheHit;
        res += "\nuptime: " + this.getUptime();

        res += "\n\nvisible frames: " + this.framesVisible;
        res += "\ntotal: " + this.frameCount;

        res += "\n\nhost: " + this.getHostDesc();
        return res;
    }

    private String getUptime() {
        return TIME_FMT.format(new Long(System.currentTimeMillis() - this.upDate));
    }

    private String getHostDesc() {
        final Runtime rt = Runtime.getRuntime();
        String res = "processors: " + rt.availableProcessors();
        res += "\nfree memory: " + formatBytes(rt.freeMemory());
        res += "\ntotal memory: " + formatBytes(rt.totalMemory());
        return res;
    }

    private String formatBytes(long b) {
        return b / 1024 / 1024 + "MB";
    }

    /**
     * Put an object in the context, it can then be accessed by "#<code>s</code>".
     * 
     * @param s the name of the value, eg "count".
     * @param val the value, eg 3.
     */
    public void put(String s, Object val) {
        this.ognlCtxt.put(s, val);
    }

    public synchronized void beginRequest(String req) {
        this.requests.add(req);
        this.requestsTotalCount++;
    }

    public synchronized void endRequest(String req) {
        this.requests.remove(req);
    }

    public synchronized void addFailedRequest(String query) {
        this.failed.add(query);
    }

    public synchronized void addFailedStatement() {
        this.failedStmts++;
    }

    public synchronized void connectionCreated() {
        this.connectionTotalCount++;
        this.connectionCount++;
    }

    public synchronized void connectionRemoved() {
        this.connectionCount--;
    }

    public synchronized void frameCreated() {
        this.frameCount++;
    }

    public synchronized void frameShown() {
        this.framesVisible++;
    }

    public synchronized void frameHidden() {
        this.framesVisible--;
    }

    public synchronized void addCacheHit() {
        this.cacheHit++;
    }

}
