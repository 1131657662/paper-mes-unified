package com.paper.mes.exporttask.service;

/** Pure state-transition decision used by the persistent storage monitor. */
record ExportTaskStorageAlertTransition(
        String previousState,
        String currentState,
        long transitionNo,
        boolean changed,
        boolean notificationRequired) {

    static ExportTaskStorageAlertTransition evaluate(String previousState, String currentState,
                                                      long transitionNo) {
        if (previousState == null || previousState.isBlank()) {
            return new ExportTaskStorageAlertTransition("UNKNOWN", currentState, transitionNo,
                    true, false);
        }
        if (previousState.equals(currentState)) {
            return new ExportTaskStorageAlertTransition(previousState, currentState, transitionNo,
                    false, false);
        }
        long nextTransitionNo = Math.addExact(transitionNo, 1L);
        return new ExportTaskStorageAlertTransition(previousState, currentState, nextTransitionNo,
                true, !"UNKNOWN".equals(previousState) || !ExportTaskStorageHealth.READY.equals(currentState));
    }
}
