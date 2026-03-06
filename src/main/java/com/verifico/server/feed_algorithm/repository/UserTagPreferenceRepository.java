package com.verifico.server.feed_algorithm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verifico.server.feed_algorithm.model.UserTagPreference;

public interface UserTagPreferenceRepository extends JpaRepository<UserTagPreference, Long> {

    List<UserTagPreference> findByUserId(Long userId);

    Optional<UserTagPreference> findByUserIdAndTag(Long userId, String tag);
}
