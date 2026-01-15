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

import fr.paris.lutece.plugins.grubusiness.business.demand.DemandType;
import fr.paris.lutece.plugins.grubusiness.business.notification.MyDashboardNotification;
import fr.paris.lutece.plugins.grubusiness.business.notification.Notification;
import fr.paris.lutece.plugins.grubusiness.service.notification.NotificationException;
import fr.paris.lutece.plugins.notificationstore.business.DemandTypeHome;
import fr.paris.lutece.plugins.notificationstore.dto.DemandTypeDto;
import fr.paris.lutece.plugins.notificationstore.utils.NotificationStoreUtils;

import javax.persistence.EntityNotFoundException;
import java.util.Objects;

public class DemandTypeService
{

    private static DemandTypeService instance;

    private DemandTypeService( )
    {
    }

    public static DemandTypeService getInstance( )
    {
        if ( Objects.isNull( instance ) )
        {
            instance = new DemandTypeService( );
        }
        return instance;
    }

    public DemandTypeDto getDemandType( final Notification notification ) throws NotificationException
    {
        if ( Objects.isNull( notification ) || Objects.isNull( notification.getDemand( ) ) )
        {
            throw new NotificationException( "Missing demand in notification" );
        }
        final String typeId = notification.getDemand( ).getTypeId( );
        final DemandType demandType = DemandTypeHome.getDemandType( typeId )
                .orElseThrow( ( ) -> new EntityNotFoundException( "Demandtype not found with id " + typeId ) );

        final DemandTypeDto response = NotificationStoreUtils.toDto( demandType );
        updateNotificationDescription( notification.getMyDashboardNotification( ), response );

        return response;
    }

    public void updateNotificationDescription( final MyDashboardNotification myDashboardNotification, final DemandTypeDto demandType )
    {
        if ( Objects.isNull( myDashboardNotification ) || myDashboardNotification.getSubject( ).isEmpty( ) )
        {
            return;
        }
        demandType.setNotificationDescription( myDashboardNotification.getSubject( ) );
    }
}
