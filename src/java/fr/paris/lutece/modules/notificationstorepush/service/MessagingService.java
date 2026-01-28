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
import com.google.firebase.messaging.Notification;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class MessagingService
{

    public static final String SERVICE_ACCOUNT_PATH = AppPropertiesService.getProperty( "module.messaging.serviceAccount.path" );
    private static MessagingService _instance;

    private MessagingService( )
    {
        try
        {
            final Path serviceAccountPath = Paths.get( SERVICE_ACCOUNT_PATH );
            if ( Files.exists( serviceAccountPath ) )
            {
                final FirebaseOptions options = FirebaseOptions.builder( )
                        .setCredentials( GoogleCredentials.fromStream( Files.newInputStream( serviceAccountPath ) ) )
                        .build( );
                FirebaseApp.initializeApp( options );
                AppLogService.info("Successfully configured Firebase Application");
            }
            else
            {
                AppLogService.error( "ServiceAccount file not found in the resources directory" );
            }
        }
        catch( final IOException e )
        {
            AppLogService.error( "Problem while trying to read ServiceAccount file.", e );
        }
    }

    public static MessagingService instance()
    {
        if(_instance == null)
        {
            _instance = new MessagingService();
        }
        return _instance;
    }

    public void send( final List<String> registrationTokens, final String title, final String body, final Map<String, String> metadata )
            throws FirebaseMessagingException
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
}
