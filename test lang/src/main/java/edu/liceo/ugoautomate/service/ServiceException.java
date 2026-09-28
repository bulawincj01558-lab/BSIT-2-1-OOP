package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.util.AppException;

/**
 * A business-rule violation raised by the service layer (e.g. duplicate
 * registration, invalid login). The message is shown to the user as-is.
 */
public class ServiceException extends AppException {

    public ServiceException(String message) {
        super(message);
    }
}
