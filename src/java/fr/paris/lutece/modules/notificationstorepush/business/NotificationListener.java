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
import fr.paris.lutece.modules.notificationstorepush.service.MessagingService;
import fr.paris.lutece.plugins.deviceregistration.exception.DeviceRegistrationException;
import fr.paris.lutece.plugins.deviceregistration.service.DeviceRegistrationService;
import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.notification.INotificationListener;
import fr.paris.lutece.plugins.grubusiness.business.notification.MyDashboardNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.notificationstore.business.DemandTypeHome;
import fr.paris.lutece.plugins.notificationstore.dto.DemandTypeDto;
import fr.paris.lutece.plugins.notificationstore.utils.NotificationStoreUtils;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationListener implements INotificationListener
{

    private static final Logger logger = Logger.getLogger( NotificationListener.class.getName( ) );
    private static final String DEFAULT_ISSUER = AppPropertiesService.getProperty( "module.notificationstore.defaultIssuer" );

    @Inject
    @Named("notificationstorepush.messagingservice")
    private MessagingService messagingService;

    @Override
    public void onCreateNotification( final Notification notification )
    {
        if ( Objects.nonNull( notification ) && Objects.nonNull( notification.getDemand( ) ) && Objects.nonNull( notification.getDemand( ).getCustomer( ) ) )
        {
            final String typeId = notification.getDemand( ).getTypeId( );
            final DemandTypeDto demandType = DemandTypeHome.getDemandType( typeId ).map( NotificationStoreUtils::toDto )
                    .orElseThrow( ( ) -> new EntityNotFoundException( "Demandtype not found with id " + typeId ) );

            if ( demandType.isPushEnable( ) )
            {
                try
                {
                    final Customer customer = notification.getDemand( ).getCustomer( );
                    final List<String> registrationTokens = DeviceRegistrationService.getInstance( ).getRegistrationTokensByCriteria( customer.getCustomerId( ),
                            customer.getConnectionId( ), DEFAULT_ISSUER );

                    String body = demandType.getNotificationDescription( );
                    final MyDashboardNotification myDashboardNotification = notification.getMyDashboardNotification( );

                    if ( Objects.nonNull( myDashboardNotification ) && !myDashboardNotification.getSubject( ).isEmpty( ) )
                    {
                        body = myDashboardNotification.getSubject( );
                    }

                    messagingService.send( registrationTokens, demandType.getLabel( ), body );
                }
                catch( DeviceRegistrationException e )
                {
                    logger.log( Level.WARNING, "Error while retrieving user token with message : {0}", e.getMessage( ) );
                }
                catch( FirebaseMessagingException e )
                {
                    logger.log( Level.WARNING, "Error while sending message : {0}", e.getMessage( ) );
                }

            }
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
