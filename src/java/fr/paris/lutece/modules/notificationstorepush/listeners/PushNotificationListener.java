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
package fr.paris.lutece.modules.notificationstorepush.listeners;

import com.google.firebase.messaging.FirebaseMessagingException;
import fr.paris.lutece.modules.notificationstorepush.service.MessagingService;
import fr.paris.lutece.plugins.deviceregistration.exception.DeviceRegistrationException;
import fr.paris.lutece.plugins.deviceregistration.service.DeviceRegistrationService;
import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.demand.Demand;
import fr.paris.lutece.plugins.grubusiness.business.demand.DemandType;
import fr.paris.lutece.plugins.grubusiness.business.notification.INotificationListener;
import fr.paris.lutece.plugins.grubusiness.business.notification.MyDashboardNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.notificationstore.business.DemandTypeHome;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

import javax.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PushNotificationListener implements INotificationListener
{
    private static final String DEFAULT_ISSUER = AppPropertiesService.getProperty( "module.notificationstore.defaultIssuer" );
    public static final String NOTIFICATION_METADATA_DEMAND_ID = "demand_id";
    public static final String NOTIFICATION_METADATA_TYPE_ID = "type_id";
    public static final String NOTIFICATION_METADATA_CUID = "CUID";
    public static final String NOTIFICATION_METADATA_GUID = "GUID";
    public static final String NOTIFICATION_METADATA_DATE = "date";

    @Override
    public void onCreateNotification( final Notification notification )
    {
        if ( Objects.nonNull( notification ) && Objects.nonNull( notification.getDemand( ) ) && Objects.nonNull( notification.getDemand( ).getCustomer( ) ) )
        {
            final String typeId = notification.getDemand( ).getTypeId( );
            final DemandType demandType = DemandTypeHome.getDemandType( typeId )
                    .orElseThrow( ( ) -> new EntityNotFoundException( "Demandtype not found with id " + typeId ) );

            if ( demandType.isPushEnable( ) )
            {
                try
                {
                    final Customer customer = notification.getDemand( ).getCustomer( );
                    final List<String> registrationTokens = DeviceRegistrationService.getInstance( ).getRegistrationTokensByCriteria( customer.getCustomerId( ),
                            customer.getConnectionId( ), DEFAULT_ISSUER );

                    final MyDashboardNotification myDashboardNotification = notification.getMyDashboardNotification( );
                    String body = demandType.getDefaultSubject( );

                    if ( Objects.nonNull( myDashboardNotification ) && !myDashboardNotification.getSubject( ).isEmpty( ) )
                    {
                        body = myDashboardNotification.getSubject( );
                    }

                    final Map<String, String> metadata = this.prepareMetaDataFromNotification( notification );
                    MessagingService.instance().send( registrationTokens, demandType.getLabel( ), body, metadata );
                }
                catch( final DeviceRegistrationException e )
                {
                    AppLogService.error( "Error while retrieving user token with message : {}", e.getMessage( ) );
                }
                catch( final FirebaseMessagingException e )
                {
                    AppLogService.error( "Error while sending message : {}", e.getMessage( ) );
                }

            }
        }

    }

    private Map<String, String> prepareMetaDataFromNotification( final Notification notification )
    {
        final Map<String, String> metadata = new HashMap<>( );
        final Demand demand = notification.getDemand( );
        metadata.put( NOTIFICATION_METADATA_DEMAND_ID, demand.getId( ) );
        metadata.put( NOTIFICATION_METADATA_TYPE_ID, demand.getTypeId( ) );
        final Customer customer = demand.getCustomer( );
        if ( Objects.nonNull( customer.getCustomerId( ) ) )
        {
            metadata.put( NOTIFICATION_METADATA_CUID, customer.getCustomerId( ) );
        }
        if ( Objects.nonNull( customer.getConnectionId( ) ) )
        {
            metadata.put( NOTIFICATION_METADATA_GUID, customer.getConnectionId( ) );
        }
        if ( Objects.nonNull( notification.getDate( ) ) )
        {
            final Instant instant = Instant.ofEpochMilli( notification.getDate( ) );
            final LocalDateTime localDateTime = LocalDateTime.ofInstant( instant, ZoneId.systemDefault( ) );
            final String formattedDate = localDateTime.format( DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") );
            metadata.put( NOTIFICATION_METADATA_DATE, formattedDate );
        }
        return metadata;
    }

    @Override
    public void onUpdateNotification( Notification notification )
    {
        // No operation
    }

    @Override
    public void onDeleteDemand( String s, String s1 )
    {
        // No operation
    }

}
