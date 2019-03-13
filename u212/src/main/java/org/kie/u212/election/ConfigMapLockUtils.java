package org.kie.u212.election;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * from org.apache.camel.component.kubernetes.cluster.lock
 * Utilities for managing ConfigMaps that contain lock information.
 */
public final class ConfigMapLockUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigMapLockUtils.class);

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

    private static final String LEADER_PREFIX = "leader.pod.";

    private static final String LOCAL_TIMESTAMP_PREFIX = "leader.local.timestamp.";

    private ConfigMapLockUtils() {
    }

    public static ConfigMap createNewConfigMap(String configMapName, LeaderInfo leaderInfo) {
        return new ConfigMapBuilder().
                withNewMetadata()
                .withName(configMapName)
                .addToLabels("provider", "camel")
                .addToLabels("kind", "locks").
                        endMetadata()
                .addToData(LEADER_PREFIX + leaderInfo.getGroupName(), leaderInfo.getLeader())
                .addToData(LOCAL_TIMESTAMP_PREFIX + leaderInfo.getGroupName(), formatDate(leaderInfo.getLocalTimestamp()))
                .build();
    }

    public static ConfigMap getConfigMapWithNewLeader(ConfigMap configMap, LeaderInfo leaderInfo) {
        return new ConfigMapBuilder(configMap)
                .addToData(LEADER_PREFIX + leaderInfo.getGroupName(), leaderInfo.getLeader())
                .addToData(LOCAL_TIMESTAMP_PREFIX + leaderInfo.getGroupName(), formatDate(leaderInfo.getLocalTimestamp()))
                .build();
    }

    public static LeaderInfo getLeaderInfo(ConfigMap configMap, Set<String> members, String group) {
        return new LeaderInfo(group, getLeader(configMap, group), getLocalTimestamp(configMap, group), members);
    }

    private static String getLeader(ConfigMap configMap, String group) {
        return getConfigMapValue(configMap, LEADER_PREFIX + group);
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        try {
            return new SimpleDateFormat(DATE_TIME_FORMAT).format(date);
        } catch (Exception e) {
            LOG.warn("Unable to format date '" + date + "' using format " + DATE_TIME_FORMAT, e);
        }

        return null;
    }

    private static Date getLocalTimestamp(ConfigMap configMap, String group) {
        String timestamp = getConfigMapValue(configMap, LOCAL_TIMESTAMP_PREFIX + group);
        if (timestamp == null) {
            return null;
        }

        try {
            return new SimpleDateFormat(DATE_TIME_FORMAT).parse(timestamp);
        } catch (Exception e) {
            LOG.warn("Unable to parse time string '" + timestamp + "' using format " + DATE_TIME_FORMAT, e);
        }

        return null;
    }

    private static String getConfigMapValue(ConfigMap configMap, String key) {
        if (configMap == null || configMap.getData() == null) {
            return null;
        }
        return configMap.getData().get(key);
    }

}
