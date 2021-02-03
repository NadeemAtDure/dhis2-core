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
package org.hisp.dhis.dataitem.query.shared;

import static org.hisp.dhis.dataitem.query.DataItemQuery.DISPLAY_NAME_ORDER;
import static org.hisp.dhis.dataitem.query.DataItemQuery.NAME_ORDER;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isInstanceOf;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common ordering SQL statements for data items.
 *
 * @author maikel arabori
 */
public class OrderingStatement
{
    private OrderingStatement()
    {
    }

    public static String nameOrdering( final String tableName, final MapSqlParameterSource paramsMap )
    {
        final StringBuilder ordering = new StringBuilder();

        if ( paramsMap != null && paramsMap.hasValue( NAME_ORDER ) )
        {
            isInstanceOf( String.class, paramsMap.getValue( NAME_ORDER ),
                NAME_ORDER + " cannot be null and must be a String." );
            hasText( (String) paramsMap.getValue( NAME_ORDER ), NAME_ORDER + " cannot be null/blank." );

            if ( "ASC".equalsIgnoreCase( (String) paramsMap.getValue( NAME_ORDER ) ) )
            {
                ordering.append( " ORDER BY " + tableName + ".\"name\" ASC" );
            }
            else if ( "DESC".equalsIgnoreCase( (String) paramsMap.getValue( NAME_ORDER ) ) )
            {
                ordering.append( " ORDER BY " + tableName + ".\"name\" DESC" );
            }
        }

        return ordering.toString();
    }

    public static String displayColumnOrdering( final int columnNumber, final MapSqlParameterSource paramsMap )
    {
        final StringBuilder ordering = new StringBuilder();

        if ( paramsMap != null && paramsMap.hasValue( DISPLAY_NAME_ORDER ) )
        {
            isInstanceOf( String.class, paramsMap.getValue( DISPLAY_NAME_ORDER ),
                DISPLAY_NAME_ORDER + " cannot be null and must be a String." );
            hasText( (String) paramsMap.getValue( DISPLAY_NAME_ORDER ), DISPLAY_NAME_ORDER + " cannot be null/blank." );

            if ( "ASC".equalsIgnoreCase( (String) paramsMap.getValue( DISPLAY_NAME_ORDER ) ) )
            {
                ordering.append( " ORDER BY " + columnNumber + " ASC" );
            }
            else if ( "DESC".equalsIgnoreCase( (String) paramsMap.getValue( DISPLAY_NAME_ORDER ) ) )
            {
                ordering.append( " ORDER BY " + columnNumber + " DESC" );
            }
        }

        return ordering.toString();
    }
}