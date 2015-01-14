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
 
 package org.openconcerto.erp.core.sales.pos.io;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.util.HexDump;

public class ConcertProtocol {
    // Message start
    private static final char STX = (char) 0x02;
    // Message end
    private static final char ETX = (char) 0x03;
    // End of transmission
    private static final char EOT = (char) 0x04;
    // Start of transmission
    private static final char ENQ = (char) 0x05;
    // Positive ACK
    private static final char ACK = (char) 0x06;
    // Negative ACK
    private static final char NACK = (char) 0x15;

    // Mode
    private static final char MODE_CARD = '1';

    // Type
    private static final char TYPE_BUY = '0';

    // Currency
    private static final String CURRENCY_EUR = "978";
    private String port;

    public ConcertProtocol(String port) {
        this.port = port;
    }

    public boolean sendCardPayment(int amountInCents, String currency) throws Exception {
        if (currency == null) {
            currency = CURRENCY_EUR;
        }
        return sendPrototolE(1, amountInCents, true, MODE_CARD, TYPE_BUY, currency, "OpenConcerto");
    }

    public boolean sendPrototolE(int posIndex, int amountInCents, boolean requireResponse, char mode, char type, String currency, String string) throws Exception {
        boolean result = false;
        if (posIndex > 99 || posIndex < 0) {
            throw new IllegalArgumentException("Pos index must be between 0 and 99");
        }
        if (amountInCents < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Bad currency code : " + currency);
        }

        final SerialPort serialPort = getSerialPort();
        final OutputStream out = serialPort.getOutputStream();
        final InputStream in = serialPort.getInputStream();

        out.write(ENQ);

        byte[] buffer = new byte[512];
        int nbRead = in.read(buffer);

        if (nbRead != 1 || buffer[0] != ACK) {
            String r = HexDump.toHex(buffer, nbRead);
            serialPort.close();
            throw new IllegalStateException("Bad response received : " + r);
        }
        //
        final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        bOut.write(STX);
        // POS Index
        String n = rightAlign(posIndex, 2, '0');
        bOut.write(n.getBytes());
        // Amount in cents
        bOut.write(rightAlign(amountInCents, 8, '0').getBytes());
        if (requireResponse) {
            bOut.write('1');
        } else {
            bOut.write('0');
        }
        // Mode & type
        bOut.write(mode);
        bOut.write(type);
        // Currency
        bOut.write(currency.getBytes());
        // Text
        bOut.write(leftAlign(string, 10, ' ').getBytes());
        bOut.write(ETX);
        byte b = getLrc(bOut.toByteArray());
        bOut.write(b);
        out.write(bOut.toByteArray());

        // READ ACK
        nbRead = in.read(buffer);
        if (nbRead != 1 || buffer[0] != ACK) {
            String r = HexDump.toHex(buffer, nbRead);
            serialPort.close();
            throw new IllegalStateException("Bad response received : " + nbRead + ": " + r);
        }

        // END
        out.write(EOT);

        // Wait reply
        int count = 0;
        final int maxCount = 60 * 5;
        while (count < maxCount) {
            nbRead = in.read(buffer);
            if (nbRead > 0) {
                if (buffer[0] == ENQ) {
                    out.write(ACK);
                } else if (buffer[0] == STX) {
                    if (buffer[3] == '0') {
                        result = true;
                        out.write(ACK);
                        count = maxCount;
                    } else if (buffer[3] == '7') {
                        out.write(NACK);
                        count = maxCount;
                    }
                } else if (buffer[0] == EOT) {
                    count = maxCount;
                }
            }
            Thread.sleep(200);
            count++;

        }

        out.close();
        in.close();
        serialPort.close();
        return result;
    }

    private byte getLrc(byte[] bytes) {
        byte LRC = (byte) 0x0;
        for (int i = 1; i < bytes.length; i++) {
            LRC ^= bytes[i];
        }
        return LRC;
    }

    private SerialPort getSerialPort() throws Exception {
        if (port == null || port.length() == 0) {
            throw new IllegalStateException("Invalid serial port name: " + port);
        }

        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
        if (portIdentifier.isCurrentlyOwned()) {
            throw new IllegalAccessError("Port " + port + " is currently in use");
        }
        CommPort commPort = portIdentifier.open("CommUtil", 2000);

        if (!(commPort instanceof SerialPort)) {
            throw new IllegalStateException("Invalid serial port: " + port);
        }

        SerialPort serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        return serialPort;
    }

    private static String rightAlign(int value, int maxWidth, char fillChar) {
        String r = String.valueOf(value);
        if (r.length() > maxWidth) {
            return r.substring(0, maxWidth);
        }
        int n = maxWidth - r.length();
        for (int i = 0; i < n; i++) {
            r = fillChar + r;
        }
        return r;
    }

    private static String leftAlign(String r, int maxWidth, char fillChar) {
        if (r.length() > maxWidth) {
            return r.substring(0, maxWidth);
        }
        int n = maxWidth - r.length();
        for (int i = 0; i < n; i++) {
            r += fillChar;
        }
        return r;
    }
}
