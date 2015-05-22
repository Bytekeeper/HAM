package org.ham;

import java.util.regex.Pattern;

/**
 * Created by dante on 22.05.15.
 */
public class Topics {
    public static boolean matches(String topic, String subscription) {
        String quotedSubscription = Pattern.quote(subscription);
        String subscriptionPattern = quotedSubscription.replaceAll("#\\\\E$", "\\\\E(.*)").replace("+", "\\E([^/]+)\\Q");
        return topic.matches(subscriptionPattern);
    }
}
