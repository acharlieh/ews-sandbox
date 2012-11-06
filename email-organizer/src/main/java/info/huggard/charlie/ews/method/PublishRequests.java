package info.huggard.charlie.ews.method;

import info.huggard.charlie.ews.CleanupMethod;
import info.huggard.charlie.ews.Configuration.Section;
import info.huggard.charlie.ews.config.AdditionalDefaults;
import info.huggard.charlie.ews.util.EWSUtil;

import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.CalendarFolder;
import microsoft.exchange.webservices.data.CalendarView;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.FolderId;
import microsoft.exchange.webservices.data.Mailbox;
import microsoft.exchange.webservices.data.WellKnownFolderName;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableMap;

/**
 * A Cleanup Method to update a uCern document with data from a shared calendar
 * @author Charlie Huggard
 */
public class PublishRequests implements CleanupMethod {
    private static Map<String, String> CONFIG_DEFAULTS = new ImmutableMap.Builder<String, String>(). // .
            put("timeZone", "America/Chicago"). //$NON-NLS-1$ //$NON-NLS-2$
            put("documentState", "published"). //$NON-NLS-1$ //$NON-NLS-2$
            put("subjectFormat", "'Team Document, As of:' EEE, d MMM yyyy HH:mm:ss Z"). //$NON-NLS-1$ //$NON-NLS-2$
            put("jiveEndpoint", "http://localhost/api/rest/documentService/documents/"). //$NON-NLS-1$ //$NON-NLS-2$
            build();

    private Section config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConfig(final Section config) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final EWSUtil util) throws Exception {
        config.setDefaults(new AdditionalDefaults(config.getDefaults(), CONFIG_DEFAULTS, null));

        final Mailbox mailbox = util.getConfiguredMailbox(config);
        final FolderId calendarFolder = new FolderId(WellKnownFolderName.Calendar, mailbox);
        final CalendarFolder folder = CalendarFolder.bind(util.getService(), calendarFolder);

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 12);
        final CalendarView view = new CalendarView(new Date(), calendar.getTime());

        // Find all appointments ending after today
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$
        final TimeZone tz = TimeZone.getTimeZone(config.getValue("timeZone")); //$NON-NLS-1$
        format.setTimeZone(tz);
        final Calendar cal = Calendar.getInstance();
        final List<Details> list = new ArrayList<Details>();
        final FindItemsResults<Appointment> results = folder.findAppointments(view);
        for (final Appointment item : results.getItems()) {
            final Details toAdd = new Details();
            toAdd.start = format.format(item.getStart());
            cal.setTime(item.getEnd());
            cal.add(Calendar.MINUTE, -5); // Subtract 5 minutes so that way we round end dates.
            toAdd.end = format.format(cal.getTime());
            toAdd.subject = item.getSubject();
            toAdd.author = item.getOrganizer().getName();
            list.add(toAdd);
        }

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final Document docBody = factory.newDocumentBuilder().newDocument();
        final Element root = docBody.createElement("body");
        final Element eventList = docBody.createElement("ul");
        for (final Details d : list) {
            final Element item = docBody.createElement("li");
            item.setTextContent(d.toString());
            eventList.appendChild(item);
        }
        root.appendChild(eventList);
        docBody.appendChild(root);

        factory.setNamespaceAware(true);
        final Document docRequest = factory.newDocumentBuilder().newDocument();
        final Element updateDocument = docRequest.createElementNS("http://jivesoftware.com/clearspace/webservices",
                "ns:updateDocument");

        final Element document = docRequest.createElement("document");
        final Element documentId = docRequest.createElement("documentID");
        documentId.setTextContent(config.getValue("documentID"));
        final Element body = docRequest.createElement("body");
        body.appendChild(docRequest.createCDATASection(docToString(docBody)));
        final Element documentState = docRequest.createElement("documentState");
        documentState.setTextContent(config.getValue("documentState"));
        final Element subject = docRequest.createElement("subject");
        final SimpleDateFormat outFormat = new SimpleDateFormat(config.getValue("subjectFormat"));
        outFormat.setTimeZone(tz);
        subject.setTextContent(outFormat.format(new Date()));
        document.appendChild(documentId);
        document.appendChild(body);
        document.appendChild(documentState);
        document.appendChild(subject);

        updateDocument.appendChild(document);
        docRequest.appendChild(updateDocument);

        final HttpClient client = new HttpClient();
        client.getParams().setAuthenticationPreemptive(true);

        final Credentials defaultcreds = new UsernamePasswordCredentials(config.getValue("user"), config
                .getValue("password"));
        client.getState().setCredentials(new AuthScope(null, -1), defaultcreds);

        final PutMethod put = new PutMethod(config.getValue("jiveEndpoint"));
        put.setRequestEntity(new StringRequestEntity(docToString(docRequest), "text/xml", "UTF-8"));
        client.executeMethod(put);
        final InputStream is = put.getResponseBodyAsStream();
        while (is.read() != -1) {
            // no-op, consume response
        }
    }

    private static String docToString(final Document doc) {
        try {
            final DOMSource domSource = new DOMSource(doc);
            final StringWriter writer = new StringWriter();
            final StreamResult result = new StreamResult(writer);
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static class Details {

        public Details() {
        }

        public String subject;
        public String start;
        public String end;
        public String author;

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(start);
            if (!start.equals(end)) {
                builder.append(' ');
                builder.append('→');
                builder.append(' ');
                builder.append(end);
            }
            builder.append(' ');
            builder.append(':');
            builder.append(' ');
            builder.append(subject);
            builder.append(' ');
            builder.append('(');
            builder.append(author);
            builder.append(')');
            return builder.toString();
        }
    }

}
