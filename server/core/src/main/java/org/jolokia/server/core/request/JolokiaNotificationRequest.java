package org.jolokia.server.core.request;

import java.util.Deque;
import java.util.Map;

import javax.management.MalformedObjectNameException;

import org.jolokia.server.core.request.notification.NotificationCommand;
import org.jolokia.server.core.request.notification.NotificationCommandFactory;
import org.jolokia.server.core.util.RequestType;
import org.json.JSONObject;

/**
 * A request dealing with notification requests.
 *
 * @author roland
 * @since 19.03.13
 */
public class JolokiaNotificationRequest extends JolokiaRequest {

    // Command to execute
    private final NotificationCommand command;

    /**
     * Constructor for GET requests.
     *
     * @param pCommand command to use
     * @param pParams processing parameters
     */
    JolokiaNotificationRequest(NotificationCommand pCommand, ProcessingParameters pParams) {
        super(RequestType.NOTIFICATION, null, pParams, true);
        command = pCommand;
    }

    /**
     * Constructor for POST requests
     *
     * @param pCommand notification specific command
     * @param pRequestMap object representation of the request
     * @param pParams processing parameters
     */
    JolokiaNotificationRequest(NotificationCommand pCommand, JSONObject pRequestMap, ProcessingParameters pParams) {
        super(pRequestMap, pParams, true);
        command = pCommand;
    }

    /**
     * Command encapsulating the concrete notification request
     *
     * @return notification command
     */
    @SuppressWarnings("unchecked")
    public <T extends NotificationCommand> T getCommand() {
        return (T) command;
    }

    /** {@inheritDoc} */
    public JSONObject toJSON() {
        JSONObject ret = super.toJSON();
        for (Map.Entry<String, Object> e : command.toJSON().toMap().entrySet()) {
            ret.put(e.getKey(), e.getValue());
        }
        return ret;
    }

    /**
     * Creator for {@link JolokiaReadRequest}s
     *
     * @return the creator implementation
     */
    static RequestCreator<JolokiaNotificationRequest> newCreator() {
        return new RequestCreator<>() {
            /** {@inheritDoc} */
            public JolokiaNotificationRequest create(Deque<String> pStack, ProcessingParameters pParams) throws MalformedObjectNameException {
                NotificationCommand notifCommand = NotificationCommandFactory.createCommand(pStack);
                return new JolokiaNotificationRequest(notifCommand, pParams);
            }

            /** {@inheritDoc} */
            public JolokiaNotificationRequest create(JSONObject requestMap, ProcessingParameters pParams)
                    throws MalformedObjectNameException {
                NotificationCommand notifCommand = NotificationCommandFactory.createCommand(requestMap);
                return new JolokiaNotificationRequest(notifCommand, requestMap, pParams);
            }
        };
    }

}
