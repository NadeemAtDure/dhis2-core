package org.hisp.dhis.webapi.controller.dataitem.validator;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.feedback.ErrorCode.E2014;
import static org.hisp.dhis.feedback.ErrorCode.E2034;
import static org.hisp.dhis.feedback.ErrorCode.E2035;
import static org.hisp.dhis.feedback.ErrorCode.E2038;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Attribute.getNames;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Combination.getCombinations;
import static org.hisp.dhis.webapi.controller.dataitem.Filter.Operation.getAbbreviations;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.webapi.controller.dataitem.Filter;

/**
 * Validator class responsible for validating filter parameters.
 * 
 * @author maikel arabori
 */
public class FilterValidator
{
    public static final byte FILTER_ATTRIBUTE_NAME = 0;

    public static final byte FILTER_OPERATOR = 1;

    public static final byte FILTER_ATTRIBUTE_VALUE = 2;

    public static final byte MIN_TEXT_SEARCH_LENGTH = 2;

    private FilterValidator()
    {
    }

    /**
     * Checks if the given set o filters are valid, and contains only filter
     * names and operators supported.
     *
     * @param filters in the format filterName:eq:aWord
     * @throws IllegalQueryException if the set contains a non-supported name or
     *         operator, or and invalid syntax.
     */
    public static void checkNamesAndOperators( final Set<String> filters )
    {
        if ( isNotEmpty( filters ) )
        {
            for ( final String filter : filters )
            {
                {
                    final String[] filterAttributeValuePair = filter.split( ":" );
                    final boolean filterHasCorrectForm = filterAttributeValuePair.length == 3;

                    if ( filterHasCorrectForm )
                    {
                        final String attributeName = trimToEmpty(
                            filterAttributeValuePair[FILTER_ATTRIBUTE_NAME] );

                        final String operator = trimToEmpty( filterAttributeValuePair[FILTER_OPERATOR] );

                        final String attributeValue = trimToEmpty( filterAttributeValuePair[FILTER_ATTRIBUTE_VALUE] );

                        if ( trimToEmpty( attributeValue ).length() < MIN_TEXT_SEARCH_LENGTH )
                        {
                            throw new IllegalQueryException(
                                new ErrorMessage( E2038, MIN_TEXT_SEARCH_LENGTH, filter ) );
                        }

                        if ( !getNames().contains( attributeName ) )
                        {
                            throw new IllegalQueryException( new ErrorMessage( E2034, attributeName ) );
                        }

                        if ( !getAbbreviations().contains( operator ) )
                        {
                            throw new IllegalQueryException( new ErrorMessage( E2035, operator ) );
                        }

                        if ( getCombinations().stream().noneMatch( combination -> filter.startsWith( combination ) ) )
                        {
                            throw new IllegalQueryException(
                                new ErrorMessage( E2035, StringUtils.substringBeforeLast( filter, ":" ) ) );
                        }
                    }
                    else
                    {
                        throw new IllegalQueryException( new ErrorMessage( E2014, filter ) );
                    }
                }
            }
        }
    }

    /**
     * Simply checks if the given set of filters contains the given filter
     * prefix.
     *
     * @param filters
     * @param withPrefix
     * @return true if a filter prefix is found, false otherwise.
     */
    public static boolean containsFilterWithPrefix( final Set<String> filters, final String withPrefix )
    {
        if ( isNotEmpty( filters ) )
        {
            for ( final String filter : filters )
            {
                if ( filterHasPrefix( filter, withPrefix ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Simply checks if the given set of filters contains any one of the given
     * filter prefixes.
     *
     * @param filters
     * @param withPrefixOne
     * @param withPrefixTwo
     * @return true anyone of the prefixes is found, false otherwise.
     */
    public static boolean containsFilterWithOneOfPrefixes( final Set<String> filters, final String withPrefixOne,
        final String withPrefixTwo )
    {
        if ( isNotEmpty( filters ) )
        {
            for ( final String filter : filters )
            {
                if ( filterHasOneOfPrefixes( filter, withPrefixOne, withPrefixTwo ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Simply checks if a given filter start the prefix provided.
     * 
     * @param filter the full filter param, in the format: name:eq:someName,
     *        where 'name' is the attribute and 'eq' is the operator
     * @param prefix the prefix to be matched. See {@link Filter.Combination}
     *        for valid ones
     * @return true if the current filter starts with given prefix, false
     *         otherwise
     */
    public static boolean filterHasPrefix( final String filter, final String prefix )
    {
        return trimToEmpty( filter ).startsWith( trimToEmpty( prefix ) );
    }

    /**
     * Simply checks if a given filter starts with any one of the prefix
     * provided.
     * 
     * @param filter the full filter param, in the format: name:eq:someName,
     *        where 'name' is the attribute and 'eq' is the operator
     * @param prefixOne the first prefix to be matched. See
     *        {@link Filter.Combination} for valid ones
     * @param prefixTwo the second prefix to be matched. See
     *        {@link Filter.Combination} for valid ones
     * @return true if the current filter starts with any one of the given
     *         prefixes, false otherwise
     */
    public static boolean filterHasOneOfPrefixes( final String filter, final String prefixOne,
        final String prefixTwo )
    {
        return trimToEmpty( filter ).startsWith( prefixOne ) || trimToEmpty( filter ).startsWith( prefixTwo );
    }
}