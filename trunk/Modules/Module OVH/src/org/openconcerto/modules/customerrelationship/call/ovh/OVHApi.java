package org.openconcerto.modules.customerrelationship.call.ovh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.openconcerto.utils.ExceptionHandler;

import com.ovh.soapi.manager.ManagerBindingStub;
import com.ovh.soapi.manager.ManagerServiceLocator;
import com.ovh.soapi.manager.TelephonyCallListReturn;
import com.ovh.soapi.manager.TelephonyCallStruct;

public class OVHApi {
    public static void testOVHAccount() throws Exception {
        ManagerBindingStub stub = getStub();
        Properties props = getProperties();
        String session = stub.login(props.getProperty("account"), props.getProperty("accountpassword"), "fr", false);
        TelephonyCallListReturn r = stub.telephonyCallList(session, props.getProperty("from"), "fr", 0, 0, "", "", true, "", "", "");
        TelephonyCallStruct[] list = r.getList();
        for (int i = 0; i < list.length; i++) {
            TelephonyCallStruct s = list[i];
            System.out.println(s.getNature() + ":" + s.getNumber() + "-" + s.getCallingNumber() + " " + s.getDesignation() + " " + s.getDestination() + " " + s.getDate() + " " + s.getDuration());

        }
        stub.logout(session);
    }

    public static List<TelephonyCallStruct> getCallHistory() throws Exception {

        ManagerBindingStub stub = getStub();
        Properties props = getProperties();
        if (props == null)
            return Collections.emptyList();

        String session = stub.login(props.getProperty("account"), props.getProperty("accountpassword"), "fr", false);
        TelephonyCallListReturn r = stub.telephonyCallList(session, props.getProperty("from"), "fr", 0, 0, "all", null, true, "", "", "all");
        TelephonyCallStruct[] list = r.getList();
        List<TelephonyCallStruct> result = new ArrayList<TelephonyCallStruct>(list.length);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD' 'HH:mm:ss");
        for (int i = 0; i < list.length; i++) {
            TelephonyCallStruct s = list[i];
            // incoming:0366721467-0102030405 OVH-1 OVH VoIP 2011-09-02 15:54:14 00:01:17
            // id:1165280795 0
            System.out.println(s.getNature() + ":" + s.getNumber() + "-" + s.getCallingNumber() + " " + s.getDesignation() + " " + s.getDestination() + " " + s.getDate() + " " + s.getDuration()
                    + " id:" + s.getIdkey() + " " + s.getPresentation());
            System.out.println(dateFormat.parse(s.getDate()));
            result.add(s);
        }
        stub.logout(session);
        return result;
    }

    static ManagerBindingStub getStub() throws IOException {
        final ManagerServiceLocator loc = new ManagerServiceLocator();
        return new ManagerBindingStub(new URL("https://www.ovh.com:1664"), loc);

    }

    public static Properties getProperties() throws IOException {
        final File prefFile = OvhPreferencePanel.getPrefFile(OvhPreferencePanel.OVH_PROPERTIES);
        if (prefFile.isFile()) {
            Properties props = new Properties();
            final FileInputStream inStream = new FileInputStream(prefFile);
            props.load(inStream);
            inStream.close();
            return props;
        } else {
            return null;
        }
    }

    public static void call(String number) throws IOException {
        Properties props = getProperties();
        if (props == null) {
            ExceptionHandler.handle("Unable to process call to " + number + "\n" + OvhPreferencePanel.getPrefFile(OvhPreferencePanel.OVH_PROPERTIES) + " missing or not properly configured.");
        } else {
            ManagerBindingStub stub = getStub();
            stub.telephonyClick2CallDo(props.getProperty("login"), props.getProperty("password"), props.getProperty("from"), number, props.getProperty("from"));
        }
    }
}
