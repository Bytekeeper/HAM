package org.ham;


import org.eclipse.paho.client.mqttv3.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.text.ParseException;

/**
 * Driver for scheduling events using Quartz and optional querying of CalDav calendars
 */
public class SchedulerDriver implements MqttCallback, org.ham.Driver {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerDriver.class);

    private static final String OUT_TOPIC = "test/topic";

    private MqttClient mqttClient;

    private Scheduler scheduler = null;

    public static void main(String[] args) throws MqttException, IOException {
        args = new String[]{"192.168.2.3"};
        Connector connector = new Connector();
        connector.startFromCommandline(args, "SchedulerDriver", new SchedulerDriver());
    }

    @Override
    public void setup(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    @Override
    public void start() throws MqttException {

        SchedulerFactory sf = new StdSchedulerFactory();
        try {
            scheduler = sf.getScheduler();
            JobFactory myJobFactory = (bundle, scheduler1) -> new MqttQuartzJob(mqttClient, new CalDavCheck(new CalDavSettings()));
            scheduler.setJobFactory(myJobFactory);
            scheduler.start();
        } catch (SchedulerException e) {
            LOG.error("Error creating scheduler", e);
        }

        scheduleNewJob("0/10 * * * * ?", OUT_TOPIC, "the message", "Urlaub");

        LOG.info("setup of scheduler complete");
    }

    private void scheduleNewJob(String cronExpression, String outTopic, String outMessage, String calendarSummary) {
        try {
            CronExpression expression = new CronExpression(cronExpression);

            JobDetail job = JobBuilder.newJob(MqttQuartzJob.class).
                    usingJobData(MqttQuartzJob.JOB_OUT_TOPIC_KEY, outTopic).
                    usingJobData(MqttQuartzJob.JOB_MESSAGE_KEY, outMessage).
                    usingJobData(MqttQuartzJob.CALENDAR_SUMMARY_KEY, calendarSummary).
                    build();

            Trigger trigger = TriggerBuilder.newTrigger().
                    withSchedule(CronScheduleBuilder.cronSchedule(expression)).
                    build();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException | ParseException e) {
            LOG.error("Error setting up scheduler", e);
        }
    }

    @Override
    public void stop() {
        LOG.info("shutting down scheduler");
        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            LOG.error("Error shutting down scheduler", e);
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
