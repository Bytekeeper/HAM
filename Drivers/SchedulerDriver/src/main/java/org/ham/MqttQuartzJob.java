package org.ham;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osaf.caldav4j.exceptions.CalDAV4JException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;


/**
 * Job executed by Quartz from SchedulerDriver
 */
public class MqttQuartzJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(MqttQuartzJob.class);

    public static final String JOB_OUT_TOPIC_KEY = "jobOutTopic";
    public static final String JOB_MESSAGE_KEY = "jobMessage";
    public static final String CALENDAR_SUMMARY_KEY = "calendarSummary";

    private final CalDavCheck calDavCheck;
    private final MqttClient mqttClient;

    private CalDavSettings settings;

    public MqttQuartzJob(MqttClient mqttClient, CalDavCheck calDavCheck)
    {
        this.mqttClient = mqttClient;
        this.calDavCheck = calDavCheck;

        settings = new CalDavSettings();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {


        String jobMessage = (String) context.getJobDetail().getJobDataMap().get(JOB_MESSAGE_KEY);
        String outTopic = (String) context.getJobDetail().getJobDataMap().get(JOB_OUT_TOPIC_KEY);
        String calendarSummary = (String) context.getJobDetail().getJobDataMap().get(CALENDAR_SUMMARY_KEY);
        settings.calendarSummary = calendarSummary;

        LOG.debug("executing job. Topic=" + outTopic + ", message=" + jobMessage);
        try {
            //check if we should run
            if (calendarSummary == null || !calDavCheck.matchesNow(settings)) {
                mqttClient.publish(outTopic, new MqttMessage(jobMessage.getBytes("UTF-8")));
            }
        } catch (CalDAV4JException | UnsupportedEncodingException | MqttException e) {
            throw new JobExecutionException(e);
        }
    }
}
