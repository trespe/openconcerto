package org.openconcerto.modules.finance.payment.ebics;

public class OrderType {
    /**
     * Send amendment of the subscriber key for identification and authentication and encryption
     */
    public static final String HCA = "HCA";

    /**
     * Transmission of the subscriber key for ES, identification and authentication and encryption
     * */
    public static final String HCS = "HCS";

    /**
     * Transmission of the subscriber key for identification and authentication and encryption
     * within the framework of subscriber initialisation
     */
    public static final String HIA = "HIA";

    /** Transfer the public bank key (download) */
    public static final String HPB = "HPB";

    /** Download bank parameters */
    public static final String HPD = "HPD";

    /** Send password initialisation Customer’s public key for the ES */
    public static final String INI = "INI";

    /** Download supported EBICS versions */
    public static final String HEV = "HEV";

    /**
     * Send public key for signature verification Customer’s public key for the ES (see Appendix
     * Chapter 15) SPR Suspension of access authorisation Transmission of an ES file with a
     * signature for a dummy file that only contains a space
     */
    public static final String PUB = "PUB";

    /**
     * File Upload. Upload de fichiers dont le type est en paramètre
     */
    public static final String FUL = "FUL";

    /**
     * File Download. Download de fichiers dont le type est en paramètre
     */
    public static final String FDL = "FDL";
    //
    public static final String PTK = "PTK";
    /**
     * Download retrievable order types
     * */
    public static final String HAA = "HAA";
}
