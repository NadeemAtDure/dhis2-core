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
package org.hisp.dhis.dataitem.query;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.programIdFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.skipValueType;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.displayNameOrdering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of
 * ProgramIndicators.
 *
 * @author maikel arabori
 */
@Component
public class ProgramIndicatorQuery implements DataItemQuery
{
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProgramIndicatorQuery( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    public List<DataItem> find( final MapSqlParameterSource paramsMap )
    {
        final List<DataItem> dataItems = new ArrayList<>();

        // Very specific case, for Indicator objects, needed to handle filter by
        // value type NUMBER.
        // When the value type filter does not have a NUMBER type, we should not
        // execute this query.
        // It returns an empty instead.
        if ( skipValueType( NUMBER, paramsMap ) )
        {
            return dataItems;
        }

        final SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet(
            getProgramIndicatorQuery( paramsMap ), paramsMap );

        while ( rowSet.next() )
        {
            final DataItem viewItem = new DataItem();
            final String name = trimToNull( rowSet.getString( "name" ) );
            final String displayName = defaultIfBlank( trimToNull( rowSet.getString( "pi_i18n_name" ) ), name );

            viewItem.setName( name );
            viewItem.setDisplayName( displayName );
            viewItem.setProgramId( rowSet.getString( "program_uid" ) );
            viewItem.setId( rowSet.getString( "uid" ) );
            viewItem.setCode( rowSet.getString( "code" ) );
            viewItem.setDimensionItemType( PROGRAM_INDICATOR.name() );

            // Specific case where we have to force a vale type. Program
            // Indicators don't have a value type but they always evaluate to
            // numbers.
            viewItem.setValueType( NUMBER.name() );
            viewItem.setSimplifiedValueType( NUMBER.name() );

            dataItems.add( viewItem );
        }

        return dataItems;
    }

    @Override
    public int count( final MapSqlParameterSource paramsMap )
    {
        // Very specific case, for Indicator objects, needed to handle filter by
        // value type NUMBER.
        // When the value type filter does not have a NUMBER type, we should not
        // execute this query.
        // It returns ZERO.
        if ( skipValueType( NUMBER, paramsMap ) )
        {
            return 0;
        }

        final StringBuilder sql = new StringBuilder();

        sql.append( "SELECT COUNT(*) FROM (" )
            .append( getProgramIndicatorQuery( paramsMap ).replace( maxLimit( paramsMap ), EMPTY ) )
            .append( ") t" );

        return namedParameterJdbcTemplate.queryForObject( sql.toString(), paramsMap, Integer.class );
    }

    @Override
    public Class<? extends BaseDimensionalItemObject> getAssociatedEntity()
    {
        return ProgramIndicator.class;
    }

    private String getProgramIndicatorQuery( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        sql.append(
            "SELECT programindicator.\"name\", programindicator.uid, programindicator.code, program.uid AS program_uid" );

        if ( hasStringPresence( paramsMap, LOCALE ) )
        {
            sql.append( ", pi_displayname.value AS pi_i18n_name" );
        }
        else
        {
            sql.append( ", programindicator.\"name\" AS pi_i18n_name" );
        }

        sql.append( " FROM programindicator" )
            .append( " JOIN program ON program.programid = programindicator.programid" );

        if ( hasStringPresence( paramsMap, LOCALE ) )
        {
            sql.append(
                " LEFT JOIN jsonb_to_recordset(program.translations) as p_displayname(value TEXT, locale TEXT, property TEXT) ON p_displayname.locale = :"
                    + LOCALE + " AND p_displayname.property = 'NAME'" );
            sql.append(
                " LEFT JOIN jsonb_to_recordset(programindicator.translations) as pi_displayname(value TEXT, locale TEXT, property TEXT) ON pi_displayname.locale = :"
                    + LOCALE + " AND pi_displayname.property = 'NAME'" );
        }

        sql.append( " WHERE (" )
            .append( sharingConditions( "programindicator", paramsMap ) )
            .append( ")" );

        sql.append( nameFiltering( "programindicator", paramsMap ) );

        sql.append( programIdFiltering( paramsMap ) );

        sql.append( uidFiltering( "programindicator", paramsMap ) );

        sql.append( specificDisplayNameFilter( paramsMap ) );

        sql.append( specificLocaleFilter( paramsMap ) );

        sql.append( noDisplayNameAndNoLocaleFilter( paramsMap ) );

        sql.append( maxLimit( paramsMap ) );

        return sql.toString();
    }

    private String specificDisplayNameFilter( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            if ( hasStringPresence( paramsMap, LOCALE ) )
            {
                sql.append( fetchDisplayName( paramsMap, true ) );

                // 5, 2 means pi_i18n_name and programindicator.uid
                // respectively
                sql.append( displayNameOrdering( "5, 2", paramsMap ) );
            }
            else
            {
                // User does not have any locale set.
                sql.append( " AND ( programindicator.\"name\" ILIKE :" + DISPLAY_NAME + ")" );

                sql.append(
                    " GROUP BY program.\"name\", program.uid, programindicator.\"name\", programindicator.uid,"
                        + " programindicator.code, programindicator.translations, pi_i18n_name" );

                // 1, 2 means programindicator."name" and
                // programindicator.uid respectively
                sql.append( displayNameOrdering( "1, 2", paramsMap ) );
            }
        }

        return sql.toString();
    }

    private String specificLocaleFilter( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        if ( !hasStringPresence( paramsMap, DISPLAY_NAME ) && hasStringPresence( paramsMap, LOCALE ) )
        {

            sql.append( fetchDisplayName( paramsMap, false ) );

            // 5, 2 means pi_i18n_name and programindicator.uid
            // respectively
            sql.append( displayNameOrdering( "5, 2", paramsMap ) );
        }

        return sql.toString();
    }

    private String noDisplayNameAndNoLocaleFilter( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        if ( !hasStringPresence( paramsMap, DISPLAY_NAME ) && !hasStringPresence( paramsMap, LOCALE ) )
        {
            // No filter by display name is set and any locale is defined.
            sql.append(
                " GROUP BY program.\"name\", program.uid, programindicator.\"name\", programindicator.uid,"
                    + " programindicator.code, pi_i18n_name" );
        }

        return sql.toString();
    }

    private String fetchDisplayName( final MapSqlParameterSource paramsMap, final boolean filterByDisplayName )
    {
        final StringBuilder sql = new StringBuilder();

        if ( filterByDisplayName )
        {
            sql.append( " AND (pi_displayname.value ILIKE :" + DISPLAY_NAME + ")" );
        }

        sql.append( " AND pi_displayname.value IS NOT NULL" );

        sql.append( " UNION " )
            .append(
                " SELECT programindicator.\"name\", programindicator.uid, programindicator.code, program.uid AS program_uid, programindicator.\"name\" AS pi_i18n_name" )
            .append( " FROM programindicator" )
            .append( " JOIN program ON program.programid = programindicator.programid" )
            .append(
                " LEFT JOIN jsonb_to_recordset(program.translations) AS p_displayname(value TEXT, locale TEXT, property TEXT) ON TRUE" )
            .append(
                " LEFT JOIN jsonb_to_recordset(programindicator.translations) AS pi_displayname(value TEXT, locale TEXT, property TEXT) ON TRUE" )
            .append( " WHERE " )
            .append( " programindicator.uid NOT IN (" )
            .append( " SELECT programindicator.uid" )
            .append( " FROM programindicator" )
            .append( " JOIN program ON program.programid = programindicator.programid" )
            .append(
                " LEFT JOIN jsonb_to_recordset(program.translations) AS p_displayname(value TEXT, locale TEXT, property TEXT) ON TRUE" )
            .append(
                " LEFT JOIN jsonb_to_recordset(programindicator.translations) AS pi_displayname(value TEXT, locale TEXT, property TEXT) ON TRUE" )
            .append( "  WHERE" )
            .append( " (pi_displayname.locale = :" + LOCALE + ")" )
            .append( " )" );

        if ( filterByDisplayName )
        {
            sql.append( " AND (programindicator.name ILIKE :" + DISPLAY_NAME + ")" );
        }

        sql.append( programIdFiltering( paramsMap ) )
            .append( uidFiltering( "programindicator", paramsMap ) )
            .append( " AND (" + sharingConditions( "programindicator", paramsMap ) + ")" )
            .append( " UNION " )
            .append(
                " SELECT programindicator.\"name\", programindicator.uid, programindicator.code, program.uid AS program_uid, programindicator.\"name\" as pi_i18n_name" )
            .append( " FROM programindicator" )
            .append( " JOIN program ON program.programid = programindicator.programid" )
            .append( " WHERE" )
            .append(
                " (programindicator.translations = '[]' OR programindicator.translations IS NULL) " );

        if ( filterByDisplayName )
        {
            sql.append( " AND programindicator.name ILIKE :" + DISPLAY_NAME );
        }

        sql.append( programIdFiltering( paramsMap ) )
            .append( uidFiltering( "programindicator", paramsMap ) )
            .append( " AND (" + sharingConditions( "programindicator", paramsMap ) + ")" )
            .append(
                " GROUP BY programindicator.\"name\", programindicator.uid, program.uid, programindicator.code" );

        return sql.toString();
    }
}
