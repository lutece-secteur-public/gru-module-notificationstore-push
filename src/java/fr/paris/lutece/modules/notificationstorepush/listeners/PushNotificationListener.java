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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import fr.paris.lutece.modules.notificationstorepush.business.PushMessagingException;
import fr.paris.lutece.modules.notificationstorepush.service.MessagingService;
import fr.paris.lutece.plugins.deviceregistration.exception.DeviceRegistrationException;
import fr.paris.lutece.plugins.deviceregistration.service.DeviceRegistrationService;
import fr.paris.lutece.plugins.grubusiness.business.customer.Customer;
import fr.paris.lutece.plugins.grubusiness.business.demand.Demand;
import fr.paris.lutece.plugins.grubusiness.business.demand.DemandType;
import fr.paris.lutece.plugins.grubusiness.business.notification.INotificationListener;
import fr.paris.lutece.plugins.grubusiness.business.notification.MyDashboardNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.notificationstore.service.DemandTypeService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

public class PushNotificationListener implements INotificationListener
{
    private static final String DEFAULT_ISSUER = AppPropertiesService.getProperty( "notificationstorepush.messaging.defaultIssuer" );
    private static final String PUSH_DEFAULT_SUBJECT = AppPropertiesService.getProperty( "notificationstorepush.messaging.push.default.subject" );
    private static final boolean IS_PUSH_ENABLED = AppPropertiesService.getPropertyBoolean( "notificationstorepush.messaging.push.enabled", false );
    public static final String NOTIFICATION_METADATA_DEMAND_ID = "demand_id";
    public static final String NOTIFICATION_METADATA_TYPE_ID = "type_id";
    public static final String NOTIFICATION_METADATA_CUID = "CUID";
    public static final String NOTIFICATION_METADATA_GUID = "GUID";
    public static final String NOTIFICATION_METADATA_DATE = "notificationDate";

    @Override
    public void onCreateNotification( final Notification notification )
    {
	if ( !IS_PUSH_ENABLED )
	{
	    return;
	}

	if ( Objects.isNull( notification ) || Objects.isNull( notification.getDemand( ) ) )
	{
	    AppLogService.error( "Error while trying to push a Notification : null" );
	    return ;
	}

	if ( Objects.isNull( notification.getMyDashboardNotification( ) ) )
	{
	    // only MyDashBoardNotifications are pushed
	    return;
	}

	final Optional<DemandType> optDemandType = DemandTypeService.instance( ).getDemandType( notification.getDemand( ).getTypeId( ) );
	
	if ( optDemandType.isEmpty( ) )
	{
	    AppLogService.error( "DemandType {} not found", notification.getDemand( ).getTypeId( ) );
	    return ;
	}
		

	if ( optDemandType.get( ).isPushDisabled( ) )
	{
	    // push disabled for this demand_type_id
	    return;
	}

	if ( Objects.isNull( notification.getDemand( ).getCustomer( ) ) )
	{
	    AppLogService.debug( "No Customer found to push Notification : {}", notification.toString( ) );
	    return;
	}

	try
	{
	    DemandType demandType = optDemandType.get( );
	    final Customer customer = notification.getDemand( ).getCustomer( );
	    final List<String> registrationTokens = DeviceRegistrationService.getInstance( ).getRegistrationTokensByCriteria( customer.getCustomerId( ),
		    customer.getConnectionId( ), DEFAULT_ISSUER );

	    if ( registrationTokens.isEmpty( ) )
	    {
		// no device registred for the user
		return;
	    }
	    
	    final MyDashboardNotification myDashboardNotification = notification.getMyDashboardNotification( );
	    String body = demandType.getDefaultSubject( ) != null ? demandType.getDefaultSubject() : PUSH_DEFAULT_SUBJECT;

	    if ( !myDashboardNotification.getSubject( ).isEmpty( ) )
	    {
		body = myDashboardNotification.getSubject( );
	    }

	    final Map<String, String> metadata = this.prepareMetaDataFromNotification( notification );
	    
	    // Push !
	    MessagingService.instance().send( registrationTokens, demandType.getLabel( ), body, metadata );
	    
	}
	catch( final DeviceRegistrationException e )
	{
	    AppLogService.error( "Error while retrieving user token with message : {}", e.getMessage( ) );
	}
	catch( final PushMessagingException e )
	{
	    AppLogService.error( "Error while sending message : {}", e.getMessage( ) );
	}

    }

    /**
     * get push notification metadata
     *  
     * @param notification
     * @return the map of metadata
     */
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
	    metadata.put( NOTIFICATION_METADATA_DATE, String.valueOf( notification.getDate( ) ) );
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
