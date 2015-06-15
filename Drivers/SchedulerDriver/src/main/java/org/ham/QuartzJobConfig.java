package org.ham;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds all configured jobs
 */
public class QuartzJobConfig {


    public Collection<JobConfig> getJobs() {
        Set<JobConfig> configs = new HashSet<>();

        String outTopic = "lights/rgb";
        //day: white light
        configs.add(new JobConfig("0 00 08 ? * *", outTopic, "0,0,0,255,1", null));
        //red light at night
        configs.add(new JobConfig("0 30 23 ? * *", outTopic, "255,0,0,0,1", null));

        //light on in the morning
        configs.add(new JobConfig("0 40 06 ? * MON-FRI", outTopic, "0,0,0,255,2", "Urlaub"));
        //light off
        configs.add(new JobConfig("0 30 08 ? * *", outTopic, "0,0,0,255,1", null));


        return configs;
    }

    class JobConfig {

        String cron;
        String topic;
        String message;
        String calendarSummary;

        public JobConfig(String cron, String topic, String message, String calendarSummary) {
            this.cron = cron;
            this.topic = topic;
            this.message = message;
            this.calendarSummary = calendarSummary;
        }
    }
}
