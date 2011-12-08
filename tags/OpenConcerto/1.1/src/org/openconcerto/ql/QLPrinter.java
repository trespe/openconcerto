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
 
 package org.openconcerto.ql;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

public class QLPrinter {
    private String host;
    private boolean highQuality;
    private boolean paperCutAuto;
    private int printWidth;
    private int port = 515;
    private int timeout = 10000;

    public QLPrinter(String hostname) {
        if (hostname == null || hostname.trim().length() == 0) {
            throw new IllegalArgumentException("Bad HostName : " + hostname);
        }
        this.host = hostname;
        this.paperCutAuto = true;
        this.printWidth = 62;
    }

    public void setHighQuality(boolean b) {
        this.highQuality = b;
    }

    /**
     * Set print width (in milimeters)
     * */
    public void setPrintWidth(int mm) {
        this.printWidth = mm;
    }

    public void setPaperCutAuto(boolean b) {
        this.paperCutAuto = b;
    }

    public void print(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        print(img);
    }

    public void print(BufferedImage img) throws IOException {
        final byte data[] = getContent(img);
        print(data);
    }

    private byte[] getContent(BufferedImage img) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        int nbLines = img.getHeight();
        int width = img.getWidth();

        // Header: 0x00 : 200 fois
        for (int i = 0; i < 200; i++) {
            out.write(0x00);
        }
        // Init 0x1B 0x40
        out.write(0x1B);
        out.write(0x40);
        // ?? : 0x1B 0x69 0x61 0x01
        out.write(0x1B);
        out.write(0x69);
        out.write(0x61);
        out.write(0x01);
        // Page Length
        out.write(0x1B);
        out.write(0x69);
        out.write(0x7A);

        out.write(-58);
        out.write(10);
        out.write(this.printWidth);
        out.write(0);

        out.write(nbLines);
        out.write(nbLines >> 8);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        // Paper Cut
        out.write(0x1B);
        out.write(0x69);
        out.write(0x4D);
        if (this.paperCutAuto) {
            out.write(0x40);
        } else {
            out.write(0x00);
        }

        // ?? : 0x1B 0x69 0x41 0x01
        out.write(0x1B);
        out.write(0x69);
        out.write(0x41);
        out.write(0x01);
        // Set Mode
        out.write(0x1B);
        out.write(0x69);
        out.write(0x4B);
        if (!this.highQuality) {
            out.write(0x08);
        } else {
            out.write(0x48);
        }
        // Set Margin
        out.write(0x1B);
        out.write(0x69);
        out.write(0x64);
        out.write(0x00);
        out.write(0x00);
        // Set Compression
        out.write(0x4D);
        out.write(0x02);

        // Lines
        for (int i = 0; i < nbLines; i++) {
            int[] pixels = new int[width];
            img.getRGB(0, i, width, 1, pixels, 0, 4);
            try {

                final byte[] encodedLine = encodeLine(pixels);
                if (encodedLine.length > 1) {
                    out.write(0x67);
                    out.write(0);
                    out.write(encodedLine.length);
                }
                out.write(encodedLine);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // END
        out.write(0x1A);

        return out.toByteArray();
    }

    private byte[] encodeLine(int[] pixels) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pixels.length);
        byte[] bytesToEncode = new byte[pixels.length / 8];
        int index = 0;
        for (int i = 0; i < bytesToEncode.length; i++) {
            int points = 0;
            for (int j = 0; j < 8; j++) {
                int c = pixels[pixels.length - index - 1];
                int r = (c & 0x00ff0000) >> 16;
                int g = (c & 0x0000ff00) >> 8;
                int b = c & 0x000000ff;

                int grayScale = (int) (21.2671 * r + 71.5160 * g + 7.2169 * b);
                boolean isBlack = grayScale < 12000;

                points = points * 2;
                if (isBlack) {
                    points++;
                }
                index++;
            }
            bytesToEncode[i] = (byte) points;

        }

        boolean emptyLine = true;
        for (int i = 0; i < bytesToEncode.length; i++) {

            if (bytesToEncode[i] != 0) {
                emptyLine = false;
                break;
            }
        }

        if (emptyLine) {

            out.write(0x5A);
        } else {

            ByteArrayOutputStream outTemp = new ByteArrayOutputStream();
            byte last = bytesToEncode[0];
            outTemp.write(last);
            boolean sameByteMode = false;
            for (int i = 1; i < bytesToEncode.length; i++) {
                byte b = bytesToEncode[i];
                if (b == last) {
                    if (sameByteMode) {
                        // On a le meme octet que ceux précédents
                        sameByteMode = true;
                        outTemp.write(b);
                    } else {
                        if (outTemp.size() > 1) {
                            // On encode en raw
                            send(out, outTemp.toByteArray(), sameByteMode);
                            // Nouvelle serie
                            outTemp = new ByteArrayOutputStream();
                            sameByteMode = false;
                        } else {
                            sameByteMode = true;
                        }
                        outTemp.write(b);
                    }
                } else {
                    if (sameByteMode) {
                        // On envoie la serie d'octets identiques
                        send(out, outTemp.toByteArray(), sameByteMode);
                        // Nouvelle serie
                        outTemp = new ByteArrayOutputStream();
                        sameByteMode = false;
                        outTemp.write(b);
                    } else {
                        sameByteMode = false;
                        outTemp.write(b);

                    }
                }
                if (outTemp.size() > 120) {
                    // On envoie la serie
                    send(out, outTemp.toByteArray(), sameByteMode);
                    // Nouvelle serie
                    outTemp = new ByteArrayOutputStream();
                    sameByteMode = false;

                }
                last = b;
            }
            if (outTemp.size() > 0) {
                // On envoie la serie
                send(out, outTemp.toByteArray(), sameByteMode);
            }

        }
        return out.toByteArray();
    }

    private void send(ByteArrayOutputStream out, byte[] byteArray, boolean sameByteMode) throws IOException {
        if (sameByteMode) {
            out.write(257 - byteArray.length);
            out.write(byteArray[0]);
        } else {
            out.write(byteArray.length - 1);
            out.write(byteArray);
        }
    }

    private String getNewJobId() {
        String id = String.valueOf((int) Math.floor(Math.random() * 999));
        while (id.length() < 3) {
            id = "0" + id;
        }
        return id;
    }

    public void print(byte data[]) throws UnknownHostException, IOException {
        final Socket socket = new Socket(InetAddress.getByName(this.host), this.port);
        socket.setSoTimeout(this.timeout);
        final String queue = "lpr";
        final String jobid = getNewJobId();
        if (socket != null) {
            String myHostName;
            try {
                myHostName = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                myHostName = "Host";
            }
            final String user = "Java";
            String cfa = getCFA(myHostName, user, "Label", jobid);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // Init
            out.write(0x02);
            out.writeBytes(queue + "\n");
            out.flush();
            if (in.read() != 0) {
                throw new IOException("Error printing on queue");
            }

            // Control file
            out.write(0x02);
            out.writeBytes(String.valueOf(cfa.length()) + " ");
            out.writeBytes("cfA" + jobid + user + "\n");
            out.flush();
            if (in.read() != 0) {
                throw new IOException("Error sending start of control file");
            }
            out.writeBytes(cfa);
            out.writeByte(0x00);
            out.flush();
            if (in.read() != 0) {
                throw new IOException("Error sending control file");
            }

            // Data
            out.write(0x03);
            out.writeBytes(String.valueOf(data.length) + " ");
            out.writeBytes("dfA" + jobid + user + "\n");
            out.flush();
            if (in.read() != 0) {
                throw new IOException("Error sending start of data");
            }
            out.write(data);
            out.writeByte(0x00);
            out.flush();
            if (in.read() != 0) {
                throw new IOException("Error sending end of data");
            }
            out.flush();

            out.close();
            in.close();
            socket.close();
        }
    }

    private String getCFA(String myHostName, String user, String documentName, String jobid) {
        String cfA = "";
        cfA += ("H" + myHostName + "\n");
        cfA += ("P" + user + "\n");
        cfA += ("J" + documentName + "\n");
        cfA += ("ldfA" + jobid + user + "\n");
        cfA += ("UdfA" + jobid + myHostName + "\n");
        cfA += ("N" + documentName + "\n");
        return cfA;
    }

}
