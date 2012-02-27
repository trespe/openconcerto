package org.openconcerto.modules.badge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Relai {
    private final String ip;
    private final int numero;

    public Relai(String ip, int numero) {
        this.ip = ip;
        this.numero = numero;
    }

    /**
     * Ferme le relai pendant un nombre de secondes puis l'ouvre
     * */
    public void pulse(int seconds) {
        on();
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        off();
    }

    /**
     * Ferme le relai
     * */
    public void on() {
        setEnabled(true);
    }

    /**
     * Ouvre le relai
     * */
    public void off() {
        setEnabled(false);

    }

    public void setEnabled(boolean b) {
        try {
            final URL url = new URL("http://" + ip + "/preset.htm?led" + numero + "=" + (b ? '0' : '1'));
            final URLConnection uc = url.openConnection();
            final InputStreamReader input = new InputStreamReader(uc.getInputStream());
            final BufferedReader in = new BufferedReader(input);
            in.read();
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

}
