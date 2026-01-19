/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.modules.notificationstorepush.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import fr.paris.lutece.modules.notificationstorepush.business.NotificationListener;
import fr.paris.lutece.plugins.notificationstore.dto.DemandTypeDto;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingService
{

    private static final Logger logger = Logger.getLogger( MessagingService.class.getName( ) );

    public static final String SERVICE_ACCOUNT_PATH = "fr/paris/lutece/modules/notificationstorepush/resources/service-account.json";

    private MessagingService( )
    {
        try
        {
            final InputStream serviceAccountStream = NotificationListener.class.getClassLoader( ).getResourceAsStream( SERVICE_ACCOUNT_PATH );
            if ( Objects.isNull( serviceAccountStream ) )
            {
                throw new FileNotFoundException( "ServiceAccount file not found in the resources directory" );
            }
            final FirebaseOptions options = FirebaseOptions.builder( ).setCredentials( GoogleCredentials.fromStream( serviceAccountStream ) ).build( );
            FirebaseApp.initializeApp( options );
        }
        catch( FileNotFoundException e )
        {
            logger.log( Level.SEVERE, "ServiceAccount file not found in the resources directory", e );
        }
        catch( IOException e )
        {
            logger.log( Level.SEVERE, "Problem while trying to fetch properties.", e );
        }
    }

    public void buildAndSendMessage( final List<String> registrationTokens, final DemandTypeDto demandType ) throws FirebaseMessagingException
    {
        MulticastMessage message = MulticastMessage.builder( ).setNotification( com.google.firebase.messaging.Notification.builder( )
                .setTitle( demandType.getLabel( ) ).setBody( demandType.getNotificationDescription( ) ).build( ) ).addAllTokens( registrationTokens ).build( );
        BatchResponse response = FirebaseMessaging.getInstance( ).sendEachForMulticast( message );
        if ( response.getFailureCount( ) > 0 )
        {
            logger.log( Level.WARNING, "{0} notifications in failure", response.getFailureCount( ) );
        }
    }
}
