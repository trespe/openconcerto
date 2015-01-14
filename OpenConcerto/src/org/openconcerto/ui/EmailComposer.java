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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.preferences.EmailProps;
import org.openconcerto.utils.EmailClient;
import org.openconcerto.utils.ExceptionHandler;

import java.io.File;
import java.io.IOException;

public class EmailComposer {
    private static EmailComposer instance = new EmailComposer();

    public synchronized static EmailComposer getInstance() {
        return instance;
    }

    public void compose(String to, String subject, String text, File... attachedFile) throws IOException, InterruptedException {
        if (to == null) {
            to = "";
        }
        if (subject == null) {
            subject = "";
        }
        if (text == null) {
            text = "";
        }
        to = to.trim();
        subject = subject.trim();
        text = text.trim();

        EmailClient emailClient = null;
        final int mode = EmailProps.getInstance().getMode();
        if (mode == EmailProps.THUNDERBIRD) {
            String app = EmailProps.getInstance().getThunderbirdPath();
            emailClient = EmailClient.Thunderbird.createFromExe(new File(app));
        } else if (mode == EmailProps.OUTLOOK) {
            emailClient = EmailClient.Outlook;
        }
        // e.g. thunderbird has been uninstalled, fall back to getPreferred()
        // ATTN getPreferred() might itself be wrong (e.g. stale system settings)
        if (emailClient == null) {
            emailClient = EmailClient.getPreferred();
        }
        try {
            emailClient.compose(to, subject, text, attachedFile);
        } catch (Exception e) {
            // mailto shouldn't fail
            if (emailClient == EmailClient.MailTo)
                throw new IOException(e);
            ExceptionHandler.handle(TM.tr("email.fallback"), e);
            EmailClient.MailTo.compose(to, subject, text, attachedFile);
        }
    }
}
