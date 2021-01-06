package org.hisp.dhis.webapi.controller.dataitem.query;

/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hisp.dhis.common.DimensionItemType.INDICATOR;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.webapi.controller.dataitem.DataItemViewObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component
public class IndicatorQuery implements DataItemQuery
{
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public IndicatorQuery( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    private String getIndicatorQuery( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder(
            "SELECT i.\"name\" AS name, i.uid AS uid"
                + " FROM indicator i WHERE"
                + "("
                + " (i.publicaccess LIKE '__r%' OR i.publicaccess LIKE 'r%' OR i.publicaccess IS NULL)"
                + " OR i.indicatorid IN (SELECT iua.indicatorid FROM indicatoruseraccesses iua"
                + " WHERE iua.useraccessid IN (SELECT useraccessid FROM useraccess WHERE access LIKE '__r%' AND useraccess.userid = :userId))"
                + " OR i.indicatorid IN (SELECT iuga.indicatorid FROM indicatorusergroupaccesses iuga"
                + " WHERE iuga.usergroupaccessid IN (SELECT usergroupaccessid FROM usergroupaccess WHERE access LIKE '__r%' AND usergroupid"
                + " IN (SELECT usergroupid FROM usergroupmembers WHERE userid = :userId)))"
                + ")" );

        if ( isNotEmpty( (String) paramsMap.getValue( "ilikeName" ) ) )
        {
            sql.append( " AND (i.\"name\" ILIKE :ilikeName)" );
        }

        if ( paramsMap.hasValue( "nameOrder" ) && isNotEmpty( (String) paramsMap.getValue( "nameOrder" ) ) )
        {
            if ( "ASC".equalsIgnoreCase( (String) paramsMap.getValue( "nameOrder" ) ) )
            {
                sql.append( " ORDER BY i.\"name\" ASC" );
            }
            else if ( "DESC".equalsIgnoreCase( (String) paramsMap.getValue( "nameOrder" ) ) )
            {
                sql.append( " ORDER BY i.\"name\" DESC" );
            }
        }

        if ( paramsMap.hasValue( "maxRows" ) && (int) paramsMap.getValue( "maxRows" ) > 0 )
        {
            sql.append( " LIMIT :maxRows" );
        }

        return sql.toString();
    }

    @Override
    public List<DataItemViewObject> find( final MapSqlParameterSource paramsMap )
    {
        final List<DataItemViewObject> dataItemViewObjects = new ArrayList<>();

        final SqlRowSet rowSet = namedParameterJdbcTemplate
            .queryForRowSet( getIndicatorQuery( paramsMap ), paramsMap );

        while ( rowSet.next() )
        {
            final DataItemViewObject viewItem = new DataItemViewObject();

            viewItem.setName( rowSet.getString( "name" ) );
            viewItem.setUid( rowSet.getString( "uid" ) );
            viewItem.setDimensionItemType( INDICATOR );

            dataItemViewObjects.add( viewItem );
        }

        return dataItemViewObjects;
    }

    @Override
    public int count( final MapSqlParameterSource paramsMap )
    {
        return 0;
    }
}
