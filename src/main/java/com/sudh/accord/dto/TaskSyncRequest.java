package com.sudh.accord.dto;

import java.util.Map;
import java.util.UUID;

// Mirrors Android's TaskSyncRequest exactly. `recordId` is carried for parity
// with the client payload but the task to sync is taken from the {id} path
// variable. `changes` values are read from JSON as whatever type they arrive
// as (Boolean, String, null, ...); TaskService interprets them per field.
public record TaskSyncRequest(
        UUID recordId,
        int baseVersion,
        Map<String, Object> changes,
        String clientTimestamp
) {}