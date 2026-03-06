package com.verifico.server.feed_algorithm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.verifico.server.feed_algorithm.model.FeedNode;
import com.verifico.server.feed_algorithm.model.UserTagPreference;

@Component
public class BayesianScorer {

    private static final double ALPHA = 1.0;
    private static final double BETA = 1.0;

    public double score(FeedNode node, Map<String, UserTagPreference> preferenceByTag) {
        List<String> tags = node.getTags();
        if (tags.isEmpty()) {
            return 0.5;
        }

        double sum = 0.0;
        for (String tag : tags) {
            UserTagPreference pref = preferenceByTag.get(tag.toLowerCase());
            if (pref == null) {
                sum += 0.5;
                continue;
            }
            sum += posteriorLikeProbability(pref);
        }
        return sum / tags.size();
    }

    public Map<String, UserTagPreference> toMap(List<UserTagPreference> preferences) {
        Map<String, UserTagPreference> map = new HashMap<>();
        for (UserTagPreference preference : preferences) {
            map.put(preference.getTag().toLowerCase(), preference);
        }
        return map;
    }

    private double posteriorLikeProbability(UserTagPreference preference) {
        double positive = preference.getPositiveCount();
        double negative = preference.getNegativeCount();
        return (ALPHA + positive) / (ALPHA + BETA + positive + negative);
    }
}
