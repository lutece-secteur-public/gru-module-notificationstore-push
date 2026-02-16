package fr.paris.lutece.modules.notificationstorepush.business;

import java.util.List;
import java.util.Map;

public interface IPushMessagingService
{

    public void send( final List<String> registrationTokens, final String title, final String body, final Map<String, String> metadata )
            throws PushMessagingException;
    
}
