package com.sudh.accord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sudh.accord.enums.SyncStatus;

import java.util.List;

// Shape varies by status: APPLIED/MERGED carry `task`, CONFLICT carries
// `serverTask` + `conflictingFields` instead. NON_NULL keeps each response
// body limited to the fields that actually apply.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskSyncResponse(
        SyncStatus status,
        TaskResponse task,
        TaskResponse serverTask,
        List<String> conflictingFields
) {
    public static TaskSyncResponse applied(TaskResponse task) {
        return new TaskSyncResponse(SyncStatus.APPLIED, task, null, null);
    }

    public static TaskSyncResponse merged(TaskResponse task) {
        return new TaskSyncResponse(SyncStatus.MERGED, task, null, null);
    }

    public static TaskSyncResponse conflict(TaskResponse serverTask, List<String> conflictingFields) {
        return new TaskSyncResponse(SyncStatus.CONFLICT, null, serverTask, conflictingFields);
    }
}