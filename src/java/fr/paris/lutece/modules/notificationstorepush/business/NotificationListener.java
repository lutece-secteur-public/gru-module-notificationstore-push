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
package fr.paris.lutece.modules.notificationstorepush.business;

import com.google.firebase.messaging.FirebaseMessagingException;
import fr.paris.lutece.modules.notificationstorepush.service.DemandTypeService;
import fr.paris.lutece.modules.notificationstorepush.service.MessagingService;
import fr.paris.lutece.modules.notificationstorepush.service.RegistrationTokenService;
import fr.paris.lutece.plugins.deviceregistration.exception.DeviceRegistrationException;
import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.demand.Demand;
import fr.paris.lutece.plugins.grubusiness.business.notification.INotificationListener;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.grubusiness.service.notification.NotificationException;
import fr.paris.lutece.plugins.notificationstore.dto.DemandTypeDto;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationListener implements INotificationListener
{

    private static final Logger logger = Logger.getLogger( NotificationListener.class.getName( ) );

    @Inject
    @Named("notificationstorepush.notificationlistener")
    private MessagingService messagingService;

    @Override
    public void onCreateNotification( final Notification notification )
    {
        try
        {
            final Demand demand = notification.getDemand( );
            final DemandTypeDto demandType = DemandTypeService.getInstance( ).getDemandType( notification );
            if ( demandType.isPushEnable( ) )
            {
                final Customer customer = demand.getCustomer( );
                final List<String> registrationTokens = RegistrationTokenService.getRegistrationTokens( customer );
                messagingService.buildAndSendMessage( registrationTokens, demandType );
            }
        }
        catch( DeviceRegistrationException e )
        {
            logger.log( Level.WARNING, "Error while retrieving user token with message : {0}", e.getMessage( ) );
        }
        catch( FirebaseMessagingException e )
        {
            logger.log( Level.WARNING, "Error while sending message." );
        }
        catch( NotificationException e )
        {
            logger.log( Level.WARNING, e.getMessage( ) );
        }
    }

    @Override
    public void onUpdateNotification( Notification notification )
    {
        logger.info( "Operation not permitted" );
    }

    @Override
    public void onDeleteDemand( String s, String s1 )
    {
        logger.info( "Operation not permitted" );
    }

}
