package org.hisp.dhis.webapi.controller.dataitem.validator;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.feedback.ErrorCode.E2015;
import static org.hisp.dhis.feedback.ErrorCode.E2036;
import static org.hisp.dhis.feedback.ErrorCode.E2037;
import static org.hisp.dhis.webapi.controller.dataitem.Order.Attribute.getNames;
import static org.hisp.dhis.webapi.controller.dataitem.Order.Nature.getValues;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.FILTER_ATTRIBUTE_NAME;

import java.util.Set;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.webapi.controller.dataitem.Filter;
import org.hisp.dhis.webapi.controller.dataitem.Order;

/**
 * Validator class responsible for validating order parameters.
 *
 * @author maikel arabori
 */
public class OrderValidator
{
    public static final byte ORDERING_ATTRIBUTE_NAME = 0;

    public static final byte ORDERING_VALUE = 1;

    private OrderValidator()
    {
    }

    /**
     * Checks if the given set o filters are valid, and contains only filter
     * names and operators supported.
     *
     * @param orderParams a set containing elements in the format
     *        "attributeName:asc"
     * @throws IllegalQueryException if the set contains a non-supported name or
     *         operator, or contains invalid syntax.
     */
    public static void checkOrderParams( final Set<String> orderParams )
    {
        if ( isNotEmpty( orderParams ) )
        {
            for ( final String orderParam : orderParams )
            {
                final String[] orderAttributeValuePair = orderParam.split( ":" );
                final String orderAttributeName = trimToEmpty( orderAttributeValuePair[ORDERING_ATTRIBUTE_NAME] );
                final String orderValue = trimToEmpty( orderAttributeValuePair[ORDERING_VALUE] );

                final boolean filterHasCorrectForm = orderAttributeValuePair.length == 2;

                if ( filterHasCorrectForm )
                {
                    // Check for valid order attribute name. Only a few DataItem
                    // attributes are allowed.
                    if ( !getNames().contains( orderAttributeName ) )
                    {
                        throw new IllegalQueryException( new ErrorMessage( E2037, orderAttributeName ) );
                    }

                    // Check for valid ordering. Only "asc" and "desc" are
                    // allowed.
                    if ( !getValues().contains( orderValue ) )
                    {
                        throw new IllegalQueryException( new ErrorMessage( E2037, orderValue ) );
                    }
                }
                else
                {
                    throw new IllegalQueryException( new ErrorMessage( E2015, orderParam ) );
                }
            }
        }
    }

    /**
     * Checks if the given of order params can be used in combination with the
     * given filters. Currently the only valid combinations allowed when useing
     * "name" or "displayName" are:
     * 
     * 1) "order=displayName:asc => filter=displayName:eq:something" 2)
     * "order=name:asc => filter=name:eq:something"
     *
     * In the other hand these are NOT ALLOWED:
     * 
     * 1) "order=displayName:asc => filter=name:eq:something" 2) "order=name:asc
     * => filter=displayName:eq:something"
     * 
     * In other words, if one filters by "name", it cannot be ordered by
     * "displayName" and vice-versa.
     * 
     * @param orderParams a set containing elements in the format
     *        "attributeName:asc"
     * @param filters the set of filters, where each element has the format
     *        something like "attributeName:eq:value"
     * @throws IllegalQueryException if some of the filters are not allowed with
     *         any order param.
     */
    public static void checkOrderParamsAndFiltersAllowance( final Set<String> orderParams, final Set<String> filters )
    {
        if ( isNotEmpty( orderParams ) && isNotEmpty( filters ) )
        {
            for ( final String orderParam : orderParams )
            {
                final String[] orderAttributeValuePair = orderParam.split( ":" );
                final String orderAttributeName = trimToEmpty( orderAttributeValuePair[ORDERING_ATTRIBUTE_NAME] );

                for ( final String filter : filters )
                {
                    final String[] array = filter.split( ":" );

                    final String filterAttributeName = trimToEmpty( array[FILTER_ATTRIBUTE_NAME] );

                    if ( trimToEmpty( orderAttributeName ).equalsIgnoreCase( Order.Attribute.DISPLAY_NAME.getName() )
                        && trimToEmpty( filterAttributeName ).equalsIgnoreCase( Filter.Attribute.NAME.getName() ) )
                    {
                        throw new IllegalQueryException( new ErrorMessage( E2036, orderParam + " + " + filter ) );
                    }

                    if ( trimToEmpty( orderAttributeName ).equalsIgnoreCase( Order.Attribute.NAME.getName() )
                        && trimToEmpty( filterAttributeName )
                            .equalsIgnoreCase( Filter.Attribute.DISPLAY_NAME.getName() ) )
                    {
                        throw new IllegalQueryException( new ErrorMessage( E2036, orderParam + " + " + filter ) );
                    }
                }
            }
        }
    }
}