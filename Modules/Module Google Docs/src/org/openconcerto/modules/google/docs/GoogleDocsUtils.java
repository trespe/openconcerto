package org.openconcerto.modules.google.docs;

/*
 * Copyright (c) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.openconcerto.ui.DefaultGridBagConstraints;

import com.google.gdata.client.GoogleAuthTokenFactory.UserToken;
import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Query;
import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.media.ResumableGDataFileUploader;
import com.google.gdata.client.media.ResumableGDataFileUploader.RequestType;
import com.google.gdata.client.uploader.ProgressListener;
import com.google.gdata.client.uploader.ResumableHttpFileUploader;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.acl.AclEntry;
import com.google.gdata.data.acl.AclFeed;
import com.google.gdata.data.acl.AclRole;
import com.google.gdata.data.acl.AclScope;
import com.google.gdata.data.docs.DocumentEntry;
import com.google.gdata.data.docs.DocumentListEntry;
import com.google.gdata.data.docs.DocumentListFeed;
import com.google.gdata.data.docs.FolderEntry;
import com.google.gdata.data.docs.PresentationEntry;
import com.google.gdata.data.docs.RevisionFeed;
import com.google.gdata.data.docs.SpreadsheetEntry;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.media.MediaSource;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

/**
 * An application that serves as a sample to show how the Documents List Service can be used to
 * search your documents, upload and download files, change sharing permission, file documents in
 * folders, and view revisions history.
 * 
 * 
 * 
 */
public class GoogleDocsUtils {
    public DocsService service;
    public GoogleService spreadsheetsService;

    public static final String DEFAULT_HOST = "docs.google.com";

    public static final String SPREADSHEETS_SERVICE_NAME = "wise";
    public static final String SPREADSHEETS_HOST = "spreadsheets.google.com";

    private final String URL_FEED = "/feeds";
    private final String URL_DOWNLOAD = "/download";
    private final String URL_DOCLIST_FEED = "/private/full";

    private final String URL_DEFAULT = "/default";
    private final String URL_FOLDERS = "/contents";
    private final String URL_ACL = "/acl";
    private final String URL_REVISIONS = "/revisions";

    private final String URL_CATEGORY_DOCUMENT = "/-/document";
    private final String URL_CATEGORY_SPREADSHEET = "/-/spreadsheet";
    private final String URL_CATEGORY_PDF = "/-/pdf";
    private final String URL_CATEGORY_PRESENTATION = "/-/presentation";
    private final String URL_CATEGORY_STARRED = "/-/starred";
    private final String URL_CATEGORY_TRASHED = "/-/trashed";
    private final String URL_CATEGORY_FOLDER = "/-/folder";
    private final String URL_CATEGORY_EXPORT = "/Export";

    private final String PARAMETER_SHOW_FOLDERS = "showfolders=true";

    private String host;

    private final Map<String, String> DOWNLOAD_DOCUMENT_FORMATS;
    {
        DOWNLOAD_DOCUMENT_FORMATS = new HashMap<String, String>();
        DOWNLOAD_DOCUMENT_FORMATS.put("doc", "doc");
        DOWNLOAD_DOCUMENT_FORMATS.put("txt", "txt");
        DOWNLOAD_DOCUMENT_FORMATS.put("odt", "odt");
        DOWNLOAD_DOCUMENT_FORMATS.put("pdf", "pdf");
        DOWNLOAD_DOCUMENT_FORMATS.put("png", "png");
        DOWNLOAD_DOCUMENT_FORMATS.put("rtf", "rtf");
        DOWNLOAD_DOCUMENT_FORMATS.put("html", "html");
        DOWNLOAD_DOCUMENT_FORMATS.put("zip", "zip");
    }

    private final Map<String, String> DOWNLOAD_PRESENTATION_FORMATS;
    {
        DOWNLOAD_PRESENTATION_FORMATS = new HashMap<String, String>();
        DOWNLOAD_PRESENTATION_FORMATS.put("pdf", "pdf");
        DOWNLOAD_PRESENTATION_FORMATS.put("png", "png");
        DOWNLOAD_PRESENTATION_FORMATS.put("ppt", "ppt");
        DOWNLOAD_PRESENTATION_FORMATS.put("swf", "swf");
        DOWNLOAD_PRESENTATION_FORMATS.put("txt", "txt");
    }

    private final Map<String, String> DOWNLOAD_SPREADSHEET_FORMATS;
    {
        DOWNLOAD_SPREADSHEET_FORMATS = new HashMap<String, String>();
        DOWNLOAD_SPREADSHEET_FORMATS.put("xls", "xls");
        DOWNLOAD_SPREADSHEET_FORMATS.put("ods", "ods");
        DOWNLOAD_SPREADSHEET_FORMATS.put("pdf", "pdf");
        DOWNLOAD_SPREADSHEET_FORMATS.put("csv", "csv");
        DOWNLOAD_SPREADSHEET_FORMATS.put("tsv", "tsv");
        DOWNLOAD_SPREADSHEET_FORMATS.put("html", "html");
    }

    /**
     * Constructor.
     * 
     * @param applicationName name of the application.
     * 
     * @throws IllegalArgumentException
     */
    public GoogleDocsUtils(String applicationName) {
        this(applicationName, DEFAULT_HOST);
    }

    /**
     * Constructor
     * 
     * @param applicationName name of the application
     * @param host the host that contains the feeds
     * 
     * @throws IllegalArgumentException
     */
    public GoogleDocsUtils(String applicationName, String host) {
        if (host == null) {
            throw new IllegalArgumentException("null passed in required parameters");
        }

        service = new DocsService(applicationName);

        // Creating a spreadsheets service is necessary for downloading spreadsheets
        spreadsheetsService = new GoogleService(SPREADSHEETS_SERVICE_NAME, applicationName);

        this.host = host;
    }

    /**
     * Set user credentials based on a username and password.
     * 
     * @param user username to log in with.
     * @param pass password for the user logging in.
     * 
     */
    public void login(String user, String pass) throws AuthenticationException {
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("Identifiant manquant");
        }
        if (pass == null || pass.isEmpty()) {
            throw new IllegalArgumentException("Mot de passe manquant");
        }

        service.setUserCredentials(user, pass);
        spreadsheetsService.setUserCredentials(user, pass);

    }

    /**
     * Allow a user to login using an AuthSub token.
     * 
     * @param token the token to be used when logging in.
     * 
     * @throws AuthenticationException
     */
    public void loginWithAuthSubToken(String token) throws AuthenticationException {
        if (token == null) {
            throw new IllegalArgumentException("null login credentials");
        }

        service.setAuthSubToken(token);
        spreadsheetsService.setAuthSubToken(token);
    }

    /**
     * Create a new item in the DocList.
     * 
     * @param title the title of the document to be created.
     * @param type the type of the document to be created. One of "spreadsheet", "presentation", or
     *        "document".
     * 
     * @throws ServiceException
     * @throws IOException
     */
    public DocumentListEntry createNew(String title, String type) throws MalformedURLException, IOException, ServiceException {
        if (title == null || type == null) {
            throw new IllegalArgumentException("null title or type");
        }

        final DocumentListEntry newEntry;
        if (type.equals("document")) {
            newEntry = new DocumentEntry();
        } else if (type.equals("presentation")) {
            newEntry = new PresentationEntry();
        } else if (type.equals("spreadsheet")) {
            newEntry = new SpreadsheetEntry();
        } else if (type.equals("folder")) {
            newEntry = new FolderEntry();
        } else {
            throw new IllegalArgumentException("unsupported type");
        }

        newEntry.setTitle(new PlainTextConstruct(title));
        return service.insert(buildUrl(URL_DEFAULT + URL_DOCLIST_FEED), newEntry);
    }

    /**
     * Gets a feed containing the documents.
     * 
     * @param category what types of documents to list: "all": lists all the doc objects (documents,
     *        spreadsheets, presentations) "folders": lists all doc objects including folders.
     *        "documents": lists only documents. "spreadsheets": lists only spreadsheets. "pdfs":
     *        lists only pdfs. "presentations": lists only presentations. "starred": lists only
     *        starred objects. "trashed": lists trashed objects.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public DocumentListFeed getDocsListFeed(String category) throws IOException, MalformedURLException, ServiceException {
        if (category == null) {
            throw new IllegalArgumentException("null category");
        }

        URL url;

        if (category.equals("all")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED);
        } else if (category.equals("folders")) {
            String[] parameters = { PARAMETER_SHOW_FOLDERS };
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_FOLDER, parameters);
        } else if (category.equals("documents")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_DOCUMENT);
        } else if (category.equals("spreadsheets")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_SPREADSHEET);
        } else if (category.equals("pdfs")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_PDF);
        } else if (category.equals("presentations")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_PRESENTATION);
        } else if (category.equals("starred")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_STARRED);
        } else if (category.equals("trashed")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_TRASHED);
        } else {
            return null;
        }

        return service.getFeed(url, DocumentListFeed.class);
    }

    /**
     * Gets the entry for the provided object id.
     * 
     * @param resourceId the resource id of the object to fetch an entry for.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public DocumentListEntry getDocsListEntry(String resourceId) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null) {
            throw new IllegalArgumentException("null resourceId");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId);

        return service.getEntry(url, DocumentListEntry.class);
    }

    /**
     * Gets the feed for all the objects contained in a folder.
     * 
     * @param folderResourceId the resource id of the folder to return the feed for the contents.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public DocumentListFeed getFolderDocsListFeed(String folderResourceId) throws IOException, MalformedURLException, ServiceException {
        if (folderResourceId == null) {
            throw new IllegalArgumentException("null folderResourceId");
        }
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderResourceId + URL_FOLDERS);
        return service.getFeed(url, DocumentListFeed.class);
    }

    /**
     * Gets a feed containing the documents.
     * 
     * @param resourceId the resource id of the object to fetch revisions for.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public RevisionFeed getRevisionsFeed(String resourceId) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null) {
            throw new IllegalArgumentException("null resourceId");
        }

        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_REVISIONS);

        return service.getFeed(url, RevisionFeed.class);
    }

    /**
     * Search the documents, and return a feed of docs that match.
     * 
     * @param searchParameters parameters to be used in searching criteria.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public DocumentListFeed search(Map<String, String> searchParameters) throws IOException, MalformedURLException, ServiceException {
        return search(searchParameters, null);
    }

    /**
     * Search the documents, and return a feed of docs that match.
     * 
     * @param searchParameters parameters to be used in searching criteria. accepted parameters are:
     *        "q": Typical search query "alt": "author": "updated-min": Lower bound on the last time
     *        a document' content was changed. "updated-max": Upper bound on the last time a
     *        document' content was changed. "edited-min": Lower bound on the last time a document
     *        was edited by the current user. This value corresponds to the app:edited value in the
     *        Atom entry, which represents changes to the document's content or metadata.
     *        "edited-max": Upper bound on the last time a document was edited by the current user.
     *        This value corresponds to the app:edited value in the Atom entry, which represents
     *        changes to the document's content or metadata. "title": Specifies the search terms for
     *        the title of a document. This parameter used without title-exact will only submit
     *        partial queries, not exact queries. "title-exact": Specifies whether the title query
     *        should be taken as an exact string. Meaningless without title. Possible values are
     *        true and false. "opened-min": Bounds on the last time a document was opened by the
     *        current user. Use the RFC 3339 timestamp format. For example:
     *        2005-08-09T10:57:00-08:00 "opened-max": Bounds on the last time a document was opened
     *        by the current user. Use the RFC 3339 timestamp format. For example:
     *        2005-08-09T10:57:00-08:00 "owner": Searches for documents with a specific owner. Use
     *        the email address of the owner. "writer": Searches for documents which can be written
     *        to by specific users. Use a single email address or a comma separated list of email
     *        addresses. "reader": Searches for documents which can be read by specific users. Use a
     *        single email address or a comma separated list of email addresses. "showfolders":
     *        Specifies whether the query should return folders as well as documents. Possible
     *        values are true and false.
     * @param category define the category to search. (documents, spreadsheets, presentations,
     *        starred, trashed, folders)
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public DocumentListFeed search(Map<String, String> searchParameters, String category) throws IOException, MalformedURLException, ServiceException {
        if (searchParameters == null) {
            throw new IllegalArgumentException("searchParameters null");
        }

        URL url;

        if (category == null || category.equals("")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED);
        } else if (category.equals("documents")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_DOCUMENT);
        } else if (category.equals("spreadsheets")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_SPREADSHEET);
        } else if (category.equals("presentations")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_PRESENTATION);
        } else if (category.equals("starred")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_STARRED);
        } else if (category.equals("trashed")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_TRASHED);
        } else if (category.equals("folders")) {
            url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + URL_CATEGORY_FOLDER);
        } else {
            throw new IllegalArgumentException("invaild category");
        }

        Query qry = new Query(url);

        for (String key : searchParameters.keySet()) {
            qry.setStringCustomParameter(key, searchParameters.get(key));
        }

        return service.query(qry, DocumentListFeed.class);
    }

    /**
     * Upload a file.
     * 
     * @param file file to upload
     * @param title title to use for uploaded file
     * @param synchronous wait upload completion
     */
    public DocumentListEntry uploadFile(File file, String remotePath, String title, boolean synchronous) throws Exception {
        if (file == null || title == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        // Progress UI
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        p.add(new JLabel(remotePath + "/" + title), c);
        c.gridx++;
        final JProgressBar bar = new JProgressBar(0, 100);
        p.add(bar, c);
        final JFrame f = new JFrame("Envoi vers Google Docs");
        f.setIconImage(new ImageIcon(this.getClass().getResource("icon.png")).getImage());
        f.setContentPane(p);
        f.setResizable(false);
        f.pack();
        Rectangle virtualBounds = new Rectangle();
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }
        }
        final int x = (int) (virtualBounds.getMaxX());
        final int y = (int) (virtualBounds.getMaxY() - 32);
        f.setLocation(x - f.getWidth(), y - f.getHeight());
        f.setVisible(true);

        final ProgressListener listener = new ProgressListener() {

            @Override
            public void progressChanged(final ResumableHttpFileUploader uploader) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        bar.setValue((int) (uploader.getProgress() * 100));
                        if (uploader.getProgress() >= 1D || uploader.isDone()) {
                            f.dispose();
                        }
                    }
                });

            }

        };
        // Create remote folders if needed
        DocumentListEntry rFolder = getRemoteFolderByPath(remotePath);

        final DocumentEntry newDocument = new DocumentEntry();
        newDocument.setTitle(new PlainTextConstruct(title));
        // Move to trash existing document
        final DocumentListEntry existingDocumentEntry = findDocumentIn(rFolder, newDocument.getTitle());
        if (existingDocumentEntry != null) {
            trashObject(existingDocumentEntry.getResourceId(), false);
        }
        // Upload document

        final String mimeType = DocumentListEntry.MediaType.fromFileName(file.getName()).getMimeType();
        final MediaFileSource ms = new MediaFileSource(file, mimeType);
        final int PROGRESS_UPDATE_INTERVAL = 500;
        final String spec = "https://docs.google.com/feeds/upload/create-session/default/private/full" + "/" + rFolder.getResourceId() + "/contents";
        // FIXME le pool n'est pas libéré et empeche la fermeture de la VM
        final ResumableGDataFileUploader.Builder builder = new ResumableGDataFileUploader.Builder(service, new URL(spec), ms, newDocument);
        builder.title(title);
        builder.requestType(RequestType.INSERT);
        builder.trackProgress(listener, PROGRESS_UPDATE_INTERVAL).build();
        final ResumableGDataFileUploader uploader = builder.build();
        uploader.start();
        // Wait if needed
        if (synchronous) {
            while (!uploader.isDone()) {
                Thread.sleep(PROGRESS_UPDATE_INTERVAL);
            }
        }

        return newDocument;

    }

    /**
     * Find a entry in a folder
     * */
    public DocumentListEntry findDocumentIn(DocumentListEntry rFolder, TextConstruct title) throws MalformedURLException, IOException, ServiceException {
        final DocumentListFeed feed = getFolderDocsListFeed(rFolder.getResourceId());
        final List<DocumentListEntry> entries = feed.getEntries();
        for (DocumentListEntry documentListEntry : entries) {
            if (documentListEntry.getTitle().getPlainText().equals(title.getPlainText())) {
                return documentListEntry;
            }
        }
        return null;
    }

    /**
     * Trash an object.
     * 
     * @param resourceId the resource id of object to be trashed.
     * @param delete true to delete the permanently, false to move it to the trash.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void trashObject(String resourceId, boolean delete) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null) {
            throw new IllegalArgumentException("null resourceId");
        }

        String feedUrl = URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId;
        if (delete) {
            feedUrl += "?delete=true";
        }

        service.delete(buildUrl(feedUrl), getDocsListEntry(resourceId).getEtag());
    }

    /**
     * Remove an object from a folder.
     * 
     * @param resourceId the resource id of an object to be removed from the folder.
     * @param folderResourceId the resource id of the folder to remove the object from.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void removeFromFolder(String resourceId, String folderResourceId) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null || folderResourceId == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderResourceId + URL_FOLDERS + "/" + resourceId);
        service.delete(url, getDocsListEntry(resourceId).getEtag());
    }

    /**
     * Downloads a file.
     * 
     * @param exportUrl the full url of the export link to download the file from.
     * @param filepath path and name of the object to be saved as.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void downloadFile(URL exportUrl, String filepath) throws IOException, MalformedURLException, ServiceException {
        if (exportUrl == null || filepath == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        MediaContent mc = new MediaContent();
        mc.setUri(exportUrl.toString());
        MediaSource ms = service.getMedia(mc);

        InputStream inStream = null;
        FileOutputStream outStream = null;

        try {
            inStream = ms.getInputStream();
            outStream = new FileOutputStream(filepath);

            int c;
            while ((c = inStream.read()) != -1) {
                outStream.write(c);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.flush();
                outStream.close();
            }
        }
    }

    /**
     * Downloads a spreadsheet file.
     * 
     * @param filepath path and name of the object to be saved as.
     * @param resourceId the resource id of the object to be downloaded.
     * @param format format to download the file to. The following file types are supported:
     *        spreadsheets: "ods", "pdf", "xls", "csv", "html", "tsv"
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void downloadSpreadsheet(String resourceId, String filepath, String format) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null || filepath == null || format == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        UserToken docsToken = (UserToken) service.getAuthTokenFactory().getAuthToken();
        UserToken spreadsheetsToken = (UserToken) spreadsheetsService.getAuthTokenFactory().getAuthToken();
        service.setUserToken(spreadsheetsToken.getValue());

        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("key", resourceId.substring(resourceId.lastIndexOf(':') + 1));
        parameters.put("exportFormat", format);

        // If exporting to .csv or .tsv, add the gid parameter to specify which
        // sheet to export
        if (format.equals(DOWNLOAD_SPREADSHEET_FORMATS.get("csv")) || format.equals(DOWNLOAD_SPREADSHEET_FORMATS.get("tsv"))) {
            parameters.put("gid", "0"); // download only the first sheet
        }

        URL url = buildUrl(SPREADSHEETS_HOST, URL_DOWNLOAD + "/spreadsheets" + URL_CATEGORY_EXPORT, parameters);

        downloadFile(url, filepath);

        // Restore docs token for our DocList client
        service.setUserToken(docsToken.getValue());
    }

    /**
     * Downloads a document.
     * 
     * @param filepath path and name of the object to be saved as.
     * @param resourceId the resource id of the object to be downloaded.
     * @param format format to download the file to. The following file types are supported:
     *        documents: "doc", "txt", "odt", "png", "pdf", "rtf", "html"
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void downloadDocument(String resourceId, String filepath, String format) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null || filepath == null || format == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }
        String[] parameters = { "docID=" + resourceId, "exportFormat=" + format };
        URL url = buildUrl(URL_DOWNLOAD + "/documents" + URL_CATEGORY_EXPORT, parameters);

        downloadFile(url, filepath);
    }

    /**
     * Downloads a presentation.
     * 
     * @param filepath path and name of the object to be saved as.
     * @param resourceId the resource id of the object to be downloaded.
     * @param format format to download the file to. The following file types are supported:
     *        presentations: "pdf", "ppt", "png", "swf", "txt"
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void downloadPresentation(String resourceId, String filepath, String format) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null || filepath == null || format == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        String[] parameters = { "docID=" + resourceId, "exportFormat=" + format };
        URL url = buildUrl(URL_DOWNLOAD + "/presentations" + URL_CATEGORY_EXPORT, parameters);

        downloadFile(url, filepath);
    }

    /**
     * Moves a object to a folder.
     * 
     * @param resourceId the resource id of the object to be moved to the folder.
     * @param folderId the id of the folder to move the object to.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public DocumentListEntry moveObjectToFolder(String resourceId, String folderId) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null || folderId == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        DocumentListEntry doc = new DocumentListEntry();
        doc.setId(buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId).toString());

        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderId + URL_FOLDERS);
        return service.insert(url, doc);
    }

    /**
     * Gets the access control list for a object.
     * 
     * @param resourceId the resource id of the object to retrieve the ACL for.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public AclFeed getAclFeed(String resourceId) throws IOException, MalformedURLException, ServiceException {
        if (resourceId == null) {
            throw new IllegalArgumentException("null resourceId");
        }
        final URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL);
        return service.getFeed(url, AclFeed.class);
    }

    /**
     * Add an ACL role to an object.
     * 
     * @param role the role of the ACL to be added to the object.
     * @param scope the scope for the ACL.
     * @param resourceId the resource id of the object to set the ACL for.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public AclEntry addAclRole(AclRole role, AclScope scope, String resourceId) throws IOException, MalformedURLException, ServiceException {
        if (role == null || scope == null || resourceId == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        AclEntry entry = new AclEntry();
        entry.setRole(role);
        entry.setScope(scope);
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL);

        return service.insert(url, entry);
    }

    /**
     * Change the ACL role of a file.
     * 
     * @param role the new role of the ACL to be updated.
     * @param scope the new scope for the ACL.
     * @param resourceId the resource id of the object to be updated.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public AclEntry changeAclRole(AclRole role, AclScope scope, String resourceId) throws IOException, ServiceException {
        if (role == null || scope == null || resourceId == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }
        final URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL);
        return service.update(url, scope, role);
    }

    /**
     * Remove an ACL role from a object.
     * 
     * @param scope scope of the ACL to be removed.
     * @param email email address to remove the role of.
     * @param resourceId the resource id of the object to remove the role from.
     * 
     * @throws IOException
     * @throws MalformedURLException
     * @throws ServiceException
     */
    public void removeAclRole(String scope, String email, String resourceId) throws IOException, MalformedURLException, ServiceException {
        if (scope == null || email == null || resourceId == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }
        final URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + resourceId + URL_ACL + "/" + scope + "%3A" + email);
        service.delete(url);
    }

    /**
     * Returns the format code based on a file extension, and object id.
     * 
     * @param resourceId the resource id of the object you want the format for.
     * @param ext extension of the file you want the format for.
     * 
     */
    public String getDownloadFormat(String resourceId, String ext) {
        if (resourceId == null || ext == null) {
            throw new IllegalArgumentException("null passed in for required parameters");
        }

        if (resourceId.indexOf("document") == 0) {
            if (DOWNLOAD_DOCUMENT_FORMATS.containsKey(ext)) {
                return DOWNLOAD_DOCUMENT_FORMATS.get(ext);
            }
        } else if (resourceId.indexOf("presentation") == 0) {
            if (DOWNLOAD_PRESENTATION_FORMATS.containsKey(ext)) {
                return DOWNLOAD_PRESENTATION_FORMATS.get(ext);
            }
        } else if (resourceId.indexOf("spreadsheet") == 0) {
            if (DOWNLOAD_SPREADSHEET_FORMATS.containsKey(ext)) {
                return DOWNLOAD_SPREADSHEET_FORMATS.get(ext);
            }
        }
        throw new IllegalArgumentException("invalid document type");
    }

    /**
     * Gets the suffix of the resourceId. If the resourceId is "document:dh3bw3j_0f7xmjhd8",
     * "dh3bw3j_0f7xmjhd8" will be returned.
     * 
     * @param resourceId the resource id to extract the suffix from.
     * 
     */
    public String getResourceIdSuffix(String resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("null resourceId");
        }

        if (resourceId.indexOf("%3A") != -1) {
            return resourceId.substring(resourceId.lastIndexOf("%3A") + 3);
        } else if (resourceId.indexOf(":") != -1) {
            return resourceId.substring(resourceId.lastIndexOf(":") + 1);
        }
        throw new IllegalArgumentException("Bad resourceId");
    }

    /**
     * Gets the prefix of the resourceId. If the resourceId is "document:dh3bw3j_0f7xmjhd8",
     * "document" will be returned.
     * 
     * @param resourceId the resource id to extract the suffix from.
     * 
     */
    public String getResourceIdPrefix(String resourceId) {
        if (resourceId == null) {
            throw new IllegalArgumentException("null resourceId");
        }

        if (resourceId.indexOf("%3A") != -1) {
            return resourceId.substring(0, resourceId.indexOf("%3A"));
        } else if (resourceId.indexOf(":") != -1) {
            return resourceId.substring(0, resourceId.indexOf(":"));
        } else {
            throw new IllegalArgumentException("Bad resourceId");
        }
    }

    /**
     * Builds a URL from a patch.
     * 
     * @param path the path to add to the protocol/host
     * 
     * @throws MalformedURLException
     */
    private URL buildUrl(String path) throws MalformedURLException {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }

        return buildUrl(path, null);
    }

    /**
     * Builds a URL with parameters.
     * 
     * @param path the path to add to the protocol/host
     * @param parameters parameters to be added to the URL.
     * 
     * @throws MalformedURLException
     * @throws IllegalArgumentException
     */
    private URL buildUrl(String path, String[] parameters) throws MalformedURLException {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }

        return buildUrl(host, path, parameters);
    }

    /**
     * Builds a URL with parameters.
     * 
     * @param domain the domain of the server
     * @param path the path to add to the protocol/host
     * @param parameters parameters to be added to the URL.
     * 
     * @throws MalformedURLException
     */
    private URL buildUrl(String domain, String path, String[] parameters) throws MalformedURLException {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }

        StringBuffer url = new StringBuffer();
        url.append("https://" + domain + URL_FEED + path);

        if (parameters != null && parameters.length > 0) {
            url.append("?");
            for (int i = 0; i < parameters.length; i++) {
                url.append(parameters[i]);
                if (i != (parameters.length - 1)) {
                    url.append("&");
                }
            }
        }

        return new URL(url.toString());
    }

    /**
     * Builds a URL with parameters.
     * 
     * @param domain the domain of the server
     * @param path the path to add to the protocol/host
     * @param parameters parameters to be added to the URL as key value pairs.
     * 
     * @throws MalformedURLException
     */
    private URL buildUrl(String domain, String path, Map<String, String> parameters) throws MalformedURLException {
        if (path == null) {
            throw new IllegalArgumentException("null path");
        }

        StringBuffer url = new StringBuffer();
        url.append("https://" + domain + URL_FEED + path);

        if (parameters != null && parameters.size() > 0) {
            Set<Map.Entry<String, String>> params = parameters.entrySet();
            Iterator<Map.Entry<String, String>> itr = params.iterator();

            url.append("?");
            while (itr.hasNext()) {
                Map.Entry<String, String> entry = itr.next();
                url.append(entry.getKey() + "=" + entry.getValue());
                if (itr.hasNext()) {
                    url.append("&");
                }
            }
        }

        return new URL(url.toString());
    }

    /**
     * Gets the root folders.
     * 
     * @return the root folders
     */
    public DocumentListFeed getRootFolders() {
        DocumentListFeed results = new DocumentListFeed();
        try {
            DocumentListFeed docs = getDocsListFeed("folders");
            List<DocumentListEntry> list = new ArrayList<DocumentListEntry>();
            for (DocumentListEntry doc : docs.getEntries()) {
                if (doc.getParentLinks().isEmpty()) {
                    list.add(doc);
                }
            }
            results.setEntries(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Document list find by title.
     * 
     * @param title the title
     * @param documentListFeed the document list feed
     * 
     * @return the document list entry
     */
    protected DocumentListEntry documentListFindByTitle(String title, DocumentListFeed documentListFeed) {
        for (DocumentListEntry doc : documentListFeed.getEntries()) {
            if (doc.getTitle().getPlainText().equals(title)) {
                return doc;
            }
        }
        return null;
    }

    /**
     * Gets the remote folder by path.
     * 
     * @param path the path
     * 
     * @return the remote folder by path
     */
    public DocumentListEntry getRemoteFolderByPath(String path) {
        if (path == null || path.length() < 1) {
            return null;
        }
        final String[] pathArray = path.split("/");
        DocumentListFeed remoteSubFolders = getRootFolders();
        DocumentListEntry parentRemoteFolder = null;
        DocumentListEntry currentRemoteFolder = null;
        for (int i = 0; i < pathArray.length; i++) {
            String folder = pathArray[i];
            if (folder.isEmpty()) {
                continue;
            }
            currentRemoteFolder = documentListFindByTitle(folder, remoteSubFolders);
            if (currentRemoteFolder == null || !currentRemoteFolder.getType().equals("folder")) {
                try {
                    if (parentRemoteFolder == null) {
                        currentRemoteFolder = createNew(folder, "folder");
                    } else {
                        currentRemoteFolder = createNewSubFolder(folder, parentRemoteFolder.getResourceId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            remoteSubFolders = getSubFolders(currentRemoteFolder);
            parentRemoteFolder = currentRemoteFolder;
        }
        return currentRemoteFolder;
    }

    /**
     * Gets the feed for all the folders contained in a folder.
     * 
     * @param folderResourceId the resource id of the folder to return the feed for the contents.
     * @return the sub folders
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MalformedURLException the malformed url exception
     * @throws ServiceException the service exception
     * @throws IllegalArgumentExceptionthe document list exception
     */
    public DocumentListFeed getSubFolders(String folderResourceId) throws IOException, MalformedURLException, ServiceException {
        if (folderResourceId == null) {
            throw new IllegalArgumentException("null folderResourceId");
        }
        String[] parameters = { PARAMETER_SHOW_FOLDERS };
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderResourceId + URL_FOLDERS + URL_CATEGORY_FOLDER, parameters);
        return service.getFeed(url, DocumentListFeed.class);
    }

    /**
     * Gets the sub folders.
     * 
     * @param folder the folder
     * 
     * @return the sub folders
     */
    public DocumentListFeed getSubFolders(DocumentListEntry folder) {
        DocumentListFeed results = null;
        if (folder == null) {
            return getRootFolders();
        }
        try {
            results = getSubFolders(folder.getResourceId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Creates the new sub folder.
     * 
     * @param title the title
     * @param folderResourceId the folder resource id
     * @return the document list entry
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ServiceException the service exception
     */
    public DocumentListEntry createNewSubFolder(String title, String folderResourceId) throws IOException, ServiceException {
        DocumentListEntry newEntry = new FolderEntry();
        newEntry.setTitle(new PlainTextConstruct(title));
        URL url = buildUrl(URL_DEFAULT + URL_DOCLIST_FEED + "/" + folderResourceId + URL_FOLDERS);
        return service.insert(url, newEntry);
    }

}
