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
 
 package org.openconcerto.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ThreadHandler extends Handler {

    private static ThreadHandler instance = new ThreadHandler();

    private Socket socket;

    private PrintWriter out;

    private ThreadHandler() {

        try {
            socket = new Socket("localhost", 3200);
            out = new PrintWriter(socket.getOutputStream(), false);

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public synchronized static ThreadHandler getInstance() {
        return instance;
    }

    public void close() throws SecurityException {
        out.flush();
        out.close();

    }

    public void flush() {
        out.flush();
    }

    public void publish(LogRecord record) {
        String str = "[" + record.getThreadID() + "]" + record.getMessage();
        try {
            send(str);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void send(String str) {
        out.println(str);
    }

    /**
     * @param args
     */
    static Logger l = Logger.getLogger("org.openconcerto.test");

    public static void main(String[] args) {
        l.addHandler(ThreadHandler.getInstance());
        l.setLevel(Level.ALL);
        new Thread(new Runnable() {
            public void run() {
                log("Test1");
                log("Test11");
                log("Test11");
                log("Test11");
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                log("Test2");
                log("Test22");
                log("Test22");
                log("Test22");

                log("Test22");

            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                log("Test3");
                log("Test3");
                log("Test3");
                log("Test3");
                log("Test3");
                log("Test3");

            }
        }).start();
    }

    protected static void log(String string) {
        l.fine(string);

    }
}
