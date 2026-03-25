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
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.ERROR;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.UPDATE_USER_JOB_RESPONSE;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.UPDATE_USER_NAME_RESPONSE;
import static io.cyborgcode.api.test.framework.base.Rings.RING_OF_API;
import static io.cyborgcode.api.test.framework.data.constants.PathVariables.ID_PARAM;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_JUNIOR_JOB;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_JUNIOR_NAME;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_SENIOR_JOB;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_SENIOR_NAME;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Users.ID_TWO;
import static io.cyborgcode.roa.api.validator.RestAssertionTarget.BODY;
import static io.cyborgcode.roa.api.validator.RestAssertionTarget.STATUS;
import static io.cyborgcode.roa.validator.core.AssertionTypes.IS;
import static io.cyborgcode.roa.validator.core.AssertionTypes.NOT_NULL;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * API tests for the PUT /api/users/{id} endpoint.
 * <p>
 * Covers:
 * - Happy path: update user with valid name and job
 * - Error scenario: update with missing required fields (empty body)
 *
 * @author Cyborg Code Syndicate 💍👨💻
 */
@API
class UpdateUserTest extends BaseQuest {

   @Test
   @Regression
   @Description("Happy path: successfully update user with name and job")
   void updateUserWithValidNameAndJobSuccess(Quest quest) {
      CreateUserDto updateUserRequest = CreateUserDto.builder()
            .name(USER_JUNIOR_NAME)
            .job(USER_JUNIOR_JOB)
            .build();

      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER.withPathParam(ID_PARAM, ID_TWO),
                  updateUserRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_NAME_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_JUNIOR_NAME).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_JOB_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_JUNIOR_JOB).build(),
                  Assertion.builder().target(BODY).key("updatedAt")
                        .type(NOT_NULL).expected(true).build()
            )
            .complete();
   }

   @Test
   @Regression
   @Description("Error scenario: update user with empty request body returns 400")
   void updateUserWithEmptyBodyFails(Quest quest) {
      CreateUserDto emptyRequest = CreateUserDto.builder().build();

      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER.withPathParam(ID_PARAM, ID_TWO),
                  emptyRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_BAD_REQUEST).build(),
                  Assertion.builder().target(BODY).key(ERROR.getJsonPath())
                        .type(NOT_NULL).expected(true).build()
            )
            .complete();
   }

   @Test
   @Regression
   @Description("Happy path variant: update user with different name and job")
   void updateUserWithDifferentNameAndJobSuccess(Quest quest) {
      CreateUserDto updateUserRequest = CreateUserDto.builder()
            .name(USER_SENIOR_NAME)
            .job(USER_SENIOR_JOB)
            .build();

      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER.withPathParam(ID_PARAM, ID_TWO),
                  updateUserRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_NAME_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_SENIOR_NAME).build(),
                  Assertion.builder().target(BODY).key(UPDATE_USER_JOB_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_SENIOR_JOB).build()
            )
            .complete();
   }
}
