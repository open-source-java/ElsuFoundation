package elsu.support;

import elsu.common.*;
import java.util.*;

/**
 *
 * @author ssd.administrator
 */
public interface IEventSubscriber {

    Object EventHandler(Object sender, StatusType status, String message, Object o);
}
