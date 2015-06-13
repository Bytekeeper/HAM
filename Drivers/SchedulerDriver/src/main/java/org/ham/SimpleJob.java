package org.ham;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osaf.caldav4j.exceptions.CalDAV4JException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Job executed by Quartz from SchedulerDriver
 */
public class SimpleJob implements Job {

    private static final Logger LOG = Logger.getLogger(SimpleJob.class.getName());
    private final MqttClient mqttClient;

    public static final String JOB_OUT_TOPIC_KEY = "jobOutTopic";
    public static final String JOB_MESSAGE_KEY = "jobMessage";
    public static final String HOLIDAY_CHECK_KEY = "jobMessage";

    public SimpleJob(MqttClient mqttClient)
    {
        this.mqttClient = mqttClient;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        String jobMessage = (String) context.getJobDetail().getJobDataMap().get(JOB_MESSAGE_KEY);
        String outTopic = (String) context.getJobDetail().getJobDataMap().get(JOB_OUT_TOPIC_KEY);
        Boolean holidayCheck = (Boolean) context.getJobDetail().getJobDataMap().get(HOLIDAY_CHECK_KEY);

        LOG.info("executing job. Topic=" + outTopic + ", message=" + jobMessage);
        //check if we should run
        try {
            if (Boolean.FALSE.equals(holidayCheck) || !new CalDavHolidayCheck().isHolidayNow()) {
                mqttClient.publish(outTopic, new MqttMessage(jobMessage.getBytes("UTF-8")));
            }
        } catch (CalDAV4JException | UnsupportedEncodingException | MqttException e) {
            throw new JobExecutionException(e);
        }
    }
}
