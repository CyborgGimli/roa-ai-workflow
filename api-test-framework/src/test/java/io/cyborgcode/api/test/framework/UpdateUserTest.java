package io.cyborgcode.api.test.framework;

import io.cyborgcode.api.test.framework.api.dto.request.CreateUserDto;
import io.cyborgcode.roa.api.annotations.API;
import io.cyborgcode.roa.framework.annotation.Regression;
import io.cyborgcode.roa.framework.base.BaseQuest;
import io.cyborgcode.roa.framework.quest.Quest;
import io.cyborgcode.roa.validator.core.Assertion;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

import static io.cyborgcode.api.test.framework.api.AppEndpoints.PUT_UPDATE_USER;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.UPDATE_USER_JOB_RESPONSE;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.UPDATE_USER_NAME_RESPONSE;
import static io.cyborgcode.api.test.framework.base.Rings.RING_OF_API;
import static io.cyborgcode.api.test.framework.data.constants.PathVariables.ID_PARAM;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_UPDATED_JOB;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_UPDATED_NAME;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Users.ID_THREE;
import static io.cyborgcode.roa.api.validator.RestAssertionTarget.BODY;
import static io.cyborgcode.roa.api.validator.RestAssertionTarget.STATUS;
import static io.cyborgcode.roa.validator.core.AssertionTypes.IS;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Tests for the PUT /api/users/{id} endpoint covering multiple scenarios.
 * <p>
 * Demonstrates:
 * - Updating an existing user with valid data (primary scenario)
 * - Updating a different user with the same endpoint (variant scenario)
 * - Using path parameters with PUT request
 * - Asserting response status and body fields on both test cases
 *
 * @author Cyborg Code Syndicate 💍👨💻
 */
@API
class UpdateUserTest extends BaseQuest {

   @Test
   @Regression
   @Description("Update an existing user with valid name and job, expecting 200 OK and verified response body.")
   void updateUserValidDataSuccess(Quest quest) {
      CreateUserDto updateUserRequest = CreateUserDto.builder()
            .name(USER_UPDATED_NAME)
            .job(USER_UPDATED_JOB)
            .build();

      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER.withPathParam(ID_PARAM, ID_THREE),
                  updateUserRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_NAME_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_UPDATED_NAME).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_JOB_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_UPDATED_JOB).build()
            )
            .complete();
   }

   @Test
   @Regression
   @Description("Update a different existing user to verify endpoint works for multiple users.")
   void updateDifferentUserSuccess(Quest quest) {
      final int differentUserId = 2;
      CreateUserDto updateUserRequest = CreateUserDto.builder()
            .name(USER_UPDATED_NAME)
            .job(USER_UPDATED_JOB)
            .build();

      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER.withPathParam(ID_PARAM, differentUserId),
                  updateUserRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_NAME_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_UPDATED_NAME).build()
            )
            .complete();
   }

}
