package org.ham;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.osaf.caldav4j.CalDAVCollection;
import org.osaf.caldav4j.CalDAVConstants;
import org.osaf.caldav4j.exceptions.CalDAV4JException;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;
import org.osaf.caldav4j.methods.HttpClient;
import org.osaf.caldav4j.model.request.CalendarQuery;
import org.osaf.caldav4j.util.GenerateQuery;

import java.util.List;
import java.util.logging.Logger;

/**
 * Queries a CalDav server (e.g. OwnCloud) for events
 */
public class CalDavHolidayCheck {

    private static final Logger LOG = Logger.getLogger(CalDavHolidayCheck.class.getName());

    private static final String HOST = "192.168.2.3";
    private static final String PROTOCOL = "https";
    private static final int PORT = 443;
    private static final String USERNAME = "android";
    private static final String PASSWORD = "XXXX";
    private static String CALENDAR_URL = PROTOCOL + "://" + HOST + "/remote.php/caldav/calendars/android/arbeit";
    private static String HOLIDAY_DESC = "Urlaub";

    public boolean isHolidayNow() throws CalDAV4JException {
        HttpClient httpClient = new HttpClient();
        httpClient.getHostConfiguration().setHost(HOST, PORT, PROTOCOL);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);

        CalDAVCollection collection = new CalDAVCollection(CALENDAR_URL, (HostConfiguration) httpClient.getHostConfiguration().clone(),
                new CalDAV4JMethodFactory(), CalDAVConstants.PROC_ID_DEFAULT);

        int status = collection.testConnection(httpClient);
        if( status != HttpStatus.SC_OK) {
            LOG.warning("Could not connect to calendar server. Status: " + status);
        }
        GenerateQuery query = new GenerateQuery();

        CalendarQuery calendarQuery = query.generate();

        List<net.fortuna.ical4j.model.Calendar> calendars = collection.queryCalendars(httpClient, calendarQuery);

        for(Calendar calendar : calendars) {
            ComponentList componentList = calendar.getComponents().getComponents(Component.VEVENT);
            for(Object nextObject : componentList) {
                VEvent event = (VEvent) nextObject;
                Date startDate = event.getStartDate().getDate();
                Date endDate = event.getEndDate().getDate();
                String summary = event.getSummary().getValue();
                Date now = new Date();
                if(startDate.before(now) && endDate.after(now) && summary.equalsIgnoreCase(HOLIDAY_DESC))
                {
                    return true;
                }
            }
        }

        return false;
    }

}
