package com.tutortimetracker.api.service;

/**
 * Thrown when a timeslot id cannot be resolved in the selected project.
 */
public class TimeslotNotFoundException extends RuntimeException {

    /**
     * @param message error details
     */
    public TimeslotNotFoundException(String message) {
        super(message);
    }
}
