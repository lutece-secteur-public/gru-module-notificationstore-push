package fr.paris.lutece.modules.notificationstorepush.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import fr.paris.lutece.modules.notificationstorepush.business.IPushMessagingService;
import fr.paris.lutece.modules.notificationstorepush.business.PushMessagingException;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.string.StringUtil;

public class FireBaseMessagingService implements IPushMessagingService
{

    public static final String SERVICE_ACCOUNT_PROPERTY = "notificationstorepush.messaging.serviceAccount.json" ;
   
    /**
     * init
     */
    private FireBaseMessagingService( )
    {
        try
        {
            final String serviceAccount = AppPropertiesService.getProperty( SERVICE_ACCOUNT_PROPERTY );
            if ( !StringUtils.isEmpty( serviceAccount ) )
            {
                final FirebaseOptions options = FirebaseOptions.builder( )
                        .setCredentials( GoogleCredentials.fromStream(  new ByteArrayInputStream( serviceAccount.getBytes( ) ) ) )
                        .build( );
                FirebaseApp.initializeApp( options );
                AppLogService.info("Successfully configured Firebase Application");
            }
            else
            {
                AppLogService.error( "ServiceAccount not found in properties" );
            }
        }
        catch( final IOException e )
        {
            AppLogService.error( "Problem while trying to read ServiceAccount credentials.", e );
        }
    }


    public void send( final List<String> registrationTokens, final String title, final String body, final Map<String, String> metadata )
            throws PushMessagingException
    {
	try
	{
	       final Notification notification = Notification.builder().setTitle(title).setBody(body).build();
	        final MulticastMessage message = MulticastMessage.builder( )
	                .setNotification(notification)
	                .putAllData( metadata )
	                .addAllTokens( registrationTokens ).build( );
	        final BatchResponse response = FirebaseMessaging.getInstance( ).sendEachForMulticast( message );
	        if ( response.getFailureCount( ) > 0 )
	        {
	            AppLogService.error( "{0} notifications in failure", response.getFailureCount( ) );
	        }
	}
	catch ( FirebaseMessagingException e)
	{
	    throw new PushMessagingException( "Push Messaging Service Error", e );
	}
 
    }
}
