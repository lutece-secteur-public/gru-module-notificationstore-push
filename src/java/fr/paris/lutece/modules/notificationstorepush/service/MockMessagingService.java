package fr.paris.lutece.modules.notificationstorepush.service;

import java.util.List;
import java.util.Map;

import fr.paris.lutece.modules.notificationstorepush.business.IPushMessagingService;
import fr.paris.lutece.modules.notificationstorepush.business.PushMessagingException;
import fr.paris.lutece.portal.service.util.AppLogService;

public class MockMessagingService implements IPushMessagingService
{

    @Override
    public void send( List<String> registrationTokens, String title, String body, Map<String, String> metadata )
	    throws PushMessagingException
    {
	StringBuilder sb = new StringBuilder( "MockMessagingService : message received = \n" );
	sb.append( "tokens : " ).append(  registrationTokens ).append( "\n" );
	sb.append( "title : " ).append( title ).append( "\n" );
	sb.append( "body : " ).append( body ).append( "\n" );
	sb.append( "metadata : " ).append( metadata );
	
	 AppLogService.info( sb.toString( ) );

    }

}
