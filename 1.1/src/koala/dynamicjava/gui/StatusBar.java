/*
 * DynamicJava - Copyright (C) 1999 Dyade
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of Dyade shall not be used in advertising or
 * otherwise to promote the sale, use or other dealings in this Software without prior written
 * authorization from Dyade.
 */

package koala.dynamicjava.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import koala.dynamicjava.gui.resource.ResourceManager;

/**
 * The status bar
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/21
 */

public class StatusBar extends JPanel implements MessageHandler {
    /**
     * The line number label
     */
    protected JLabel line;

    /**
     * The message label
     */
    protected JLabel message;

    /**
     * The resource manager
     */
    protected ResourceManager resources;

    /**
     * The main message
     */
    protected String mainMessage;

    /**
     * The line number symbol
     */
    protected String lineSymbol;

    /**
     * Creates a new status bar
     * 
     * @param rm the resource manager that finds the message
     */
    public StatusBar(final ResourceManager rm) {
        super(new BorderLayout(0, 0));
        this.resources = rm;
        this.lineSymbol = rm.getString("Status.lineSymbol");

        this.line = new JLabel(this.lineSymbol + 1);
        this.line.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        this.line.setPreferredSize(new Dimension(70, 18));
        add("West", this.line);

        final JPanel p = new JPanel(new BorderLayout(0, 0));
        this.message = new JLabel();
        this.message.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        p.add(this.message);
        add(p);
    }

    /**
     * Sets the line number message
     * 
     * @param n the line number
     */
    public void setLine(final int n) {
        this.line.setText(this.lineSymbol + n);
    }

    /**
     * Sets a temporary message
     * 
     * @param s the message
     */
    public void setMessage(final String s) {
        this.message.setText(this.resources.getString(s));
        new DisplayThread().start();
    }

    /**
     * Sets a temporary message to display
     * 
     * @param s the message
     * @param s2 a string to concatenate with the message
     */
    public void setMessage(final String s, final String s2) {
        this.message.setText(this.resources.getString(s) + " " + s2);
        new DisplayThread().start();
    }

    /**
     * Sets the main message
     * 
     * @param s the message
     */
    public void setMainMessage(final String s) {
        this.mainMessage = this.resources.getString(s);
        this.message.setText(this.mainMessage);
    }

    /**
     * Sets the main message to display
     * 
     * @param s the message
     * @param s2 a string to concatenate with the message
     */
    public void setMainMessage(final String s, final String s2) {
        this.mainMessage = this.resources.getString(s) + " " + s2;
        this.message.setText(this.mainMessage);
    }

    /**
     * To display the main message
     */
    protected class DisplayThread extends Thread {
        public DisplayThread() {
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(3000);
            } catch (final InterruptedException e) {
            }
            StatusBar.this.message.setText(StatusBar.this.mainMessage);
        }
    }
}
