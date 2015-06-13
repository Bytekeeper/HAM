package org.ham;

import org.eclipse.paho.client.mqttv3.*;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

/**
 * Driver for scheduling events using Quartz and optional querying of CalDav calendars
 */
public class SchedulerDriver implements MqttCallback, org.ham.Driver {

    private static final Logger LOG = Logger.getLogger(SchedulerDriver.class.getName());

    private static final String OUT_TOPIC = "test/topic";



    private MqttClient mqttClient;

    public static void main(String[] args) throws MqttException, IOException {
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
        Scheduler scheduler = null;
        JobFactory myJobFactory = (bundle, scheduler1) -> new SimpleJob(mqttClient);
        try {
            scheduler = sf.getScheduler();
            scheduler.setJobFactory(myJobFactory);
            scheduler.start();
        } catch (SchedulerException e) {
            LOG.throwing(this.getClass().getName(), "start", e);
        }

        try {
            CronExpression expression = new CronExpression("0/10 * * * * ?");
            JobDetail job = JobBuilder.newJob(SimpleJob.class).
                    withIdentity("testjob").
                    usingJobData(SimpleJob.JOB_OUT_TOPIC_KEY, OUT_TOPIC).
                    usingJobData(SimpleJob.JOB_MESSAGE_KEY, "the message").
                    usingJobData(SimpleJob.HOLIDAY_CHECK_KEY, Boolean.FALSE).
                    build();

            Trigger trigger = TriggerBuilder.newTrigger().
                    withSchedule(CronScheduleBuilder.cronSchedule(expression)).
                    build();
            scheduler.scheduleJob(job, trigger);

            LOG.info("setup of scheduler complete");
        } catch (SchedulerException | ParseException e) {
            LOG.throwing(this.getClass().getName(), "start", e);
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
