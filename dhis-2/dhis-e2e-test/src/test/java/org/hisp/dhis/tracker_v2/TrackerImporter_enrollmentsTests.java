/*
 * Copyright (c) 2004-2020, University of Oslo
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

package org.hisp.dhis.tracker_v2;

import com.google.gson.JsonObject;
import org.hamcrest.Matchers;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.tracker_v2.TrackerActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackerImporter_enrollmentsTests
    extends
    ApiTest
{
    private TrackerActions trackerActions;

    private RestApiActions enrollmentActions;

    @BeforeAll
    public void beforeAll()
    {
        trackerActions = new TrackerActions();
        enrollmentActions = new RestApiActions( "/enrollments" );

        new LoginActions().loginAsSuperUser();
    }

    @Test
    public void shouldImportTeisWithEnrollments()
    {

        ApiResponse response = trackerActions
            .postAndGetJobReport( new File( "src/test/resources/tracker/v2/teis/teiWithEnrollments.json" ) );

        response.validate().statusCode( 200 )
            .body( "status", Matchers.equalTo( "OK" ) )
            .body( "stats.created", equalTo( 2 ) )
            .body( "bundleReport.typeReportMap.TRACKED_ENTITY", Matchers.notNullValue() )
            .rootPath( "bundleReport.typeReportMap.TRACKED_ENTITY" )
            .body( "stats.created", Matchers.equalTo( 1 ) )
            .noRootPath()
            .body( "bundleReport.typeReportMap.ENROLLMENT", Matchers.notNullValue() )
            .rootPath( "bundleReport.typeReportMap.ENROLLMENT" )
            .body( "stats.created", Matchers.equalTo( 1 ) );

        // assert that the tei was imported
        String teiId = response.extractString( "bundleReport.typeReportMap.TRACKED_ENTITY.objectReports.uid[0]" );
        String enrollmentId = response.extractString( "bundleReport.typeReportMap.ENROLLMENT.objectReports.uid[0]" );

        enrollmentActions.get( enrollmentId )
            .validate()
            .statusCode( 200 )
            .body( "trackedEntityInstance", equalTo( teiId ) );

    }

    @Test
    public void shouldImportTeiAndConnectedEnrollment()
        throws Exception
    {
        String id = new IdGenerator().generateUniqueId();
        JsonObject jsonObject = new FileReaderUtils().read( new File( "src/test/resources/tracker/v2/teis/teiAndEnrollment.json" ) )
            .replacePropertyValuesWith( "trackedEntity", id )
            .get( JsonObject.class );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( jsonObject );

        response.validateSuccessfulImport()
            .validate().body( "bundleReport.typeReportMap.TRACKED_ENTITY", Matchers.notNullValue() )
            .rootPath( "bundleReport.typeReportMap.TRACKED_ENTITY" )
            .body( "stats.created", Matchers.equalTo( 1 ) )
            .noRootPath()
            .body( "bundleReport.typeReportMap.ENROLLMENT", Matchers.notNullValue() )
            .rootPath( "bundleReport.typeReportMap.ENROLLMENT" )
            .body( "stats.created", Matchers.equalTo( 1 ) );

        // assert that the tei was imported
        String teiId = response.extractImportedTeis().get( 0 );
        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        enrollmentActions.get( enrollmentId )
            .validate()
            .statusCode( 200 )
            .body( "trackedEntityInstance", equalTo( teiId ) );
    }

    @Test
    public void shouldImportEnrollmentToExistingTei()
        throws Exception
    {
        JsonObject jsonObject = new FileReaderUtils().read( new File( "src/test/resources/tracker/v2/teis/tei.json" ) )
            .get( JsonObject.class );

        String teiId = trackerActions.postAndGetJobReport( jsonObject ).extractImportedTeis().get( 0 );
        assertNotNull( "tei wasn't imported", teiId );

        JsonObject enrollment = new FileReaderUtils()
            .read( new File( "src/test/resources/tracker/v2/enrollments/enrollment.json" ) )
            .replacePropertyValuesWith( "trackedEntity", teiId )
            .get( JsonObject.class );

        TrackerApiResponse response = trackerActions.postAndGetJobReport( enrollment );

        response.validateSuccessfulImport()
            .validate().rootPath( "bundleReport.typeReportMap.ENROLLMENT" )
            .body( "", notNullValue() )
            .body( "stats.created", equalTo( 1 ) )
            .body( "objectReports", notNullValue() )
            .body( "objectReports.uid", notNullValue() );

        String enrollmentId = response.extractImportedEnrollments().get( 0 );

        enrollmentActions.get( enrollmentId )
            .validate()
            .statusCode( 200 )
            .body( "trackedEntityInstance", equalTo( teiId ) );

    }
}
