package elsu.support;

import elsu.common.*;
import java.util.*;

/**
 *
 * @author ssd.administrator
 */
public abstract class AbstractEventPublisher implements IEventPublisher {

    List<IEventSubscriber> _listeners = new ArrayList<>();

    protected synchronized List<IEventSubscriber> getEventListeners() {
        return this._listeners;
    }
    
    @Override
    public synchronized void addEventListener(IEventSubscriber listener) {
        getEventListeners().add(listener);
    }

    @Override
    public synchronized void removeEventListener(IEventSubscriber listener) {
        getEventListeners().remove(listener);
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            getEventListeners().clear();
        } catch (Exception exi) {
        } finally {
            super.finalize();
        }
    }

    @Override
    public synchronized Object notifyListeners(Object sender, IStatusType status,
            String message, Object o) {
        Object result = null;
        
        // if listeners are not setup, then just output to console
        if (getEventListeners().size() == 0) {
            System.out.println(status.getName() + ":" + message);
        } else {
            Iterator i = getEventListeners().iterator();

            while (i.hasNext()) {
                try {
                    result = ((IEventSubscriber) i.next()).EventHandler(sender, status, message, o);
                } catch (Exception ex) {
                    System.out.println(getClass().toString() + ", notifyListeners(), "
                            + ex.getMessage());
                }
            }
        }
        
        return result;
    }
}
