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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Queries a CalDav server (e.g. OwnCloud) for events
 */
public class CalDavCheck {

    private static final Logger LOG = LoggerFactory.getLogger(CalDavCheck.class);

    private final CalDavSettings settings;

    public CalDavCheck(CalDavSettings settings) {
        this.settings = settings;
    }

    public boolean matchesNow(String calendarSummary) throws CalDAV4JException {
        HttpClient httpClient = new HttpClient();
//        httpClient.getHostConfiguration().setHost(HOST, PORT, PROTOCOL);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(settings.USERNAME, settings.PASSWORD);
        httpClient.getState().setCredentials(AuthScope.ANY, credentials);

        CalDAVCollection collection = new CalDAVCollection(settings.CALENDAR_URL, (HostConfiguration) httpClient.getHostConfiguration().clone(),
                new CalDAV4JMethodFactory(), CalDAVConstants.PROC_ID_DEFAULT);

        int status = collection.testConnection(httpClient);
        if( status != HttpStatus.SC_OK) {
            LOG.error("Could not connect to calendar server. Status: " + status);
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
                if(startDate.before(now) && endDate.after(now) && summary.equalsIgnoreCase(calendarSummary))
                {
                    return true;
                }
            }
        }

        return false;
    }

}
