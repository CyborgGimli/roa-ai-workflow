package io.cyborgcode.api.test.framework;

import io.cyborgcode.api.test.framework.api.dto.request.CreateUserDto;
import io.cyborgcode.api.test.framework.data.creator.DataCreator;
import io.cyborgcode.roa.api.annotations.API;
import io.cyborgcode.roa.framework.annotation.Craft;
import io.cyborgcode.roa.framework.annotation.Regression;
import io.cyborgcode.roa.framework.base.BaseQuest;
import io.cyborgcode.roa.framework.quest.Quest;
import io.cyborgcode.roa.validator.core.Assertion;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

import static io.cyborgcode.api.test.framework.api.AppEndpoints.PUT_UPDATE_USER;
import static io.cyborgcode.api.test.framework.api.AppEndpoints.PUT_UPDATE_USER_UNAUTHENTICATED;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.CREATE_USER_JOB_RESPONSE;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.CREATE_USER_NAME_RESPONSE;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.ERROR;
import static io.cyborgcode.api.test.framework.api.extractors.ApiResponsesJsonPaths.UPDATED_AT;
import static io.cyborgcode.api.test.framework.base.Rings.RING_OF_API;
import static io.cyborgcode.api.test.framework.data.constants.PathVariables.ID_PARAM;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_UPDATE_JOB;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Roles.USER_UPDATE_NAME;
import static io.cyborgcode.api.test.framework.data.constants.TestConstants.Users.ID_TWO;
import static io.cyborgcode.roa.api.validator.RestAssertionTarget.BODY;
import static io.cyborgcode.roa.api.validator.RestAssertionTarget.STATUS;
import static io.cyborgcode.roa.validator.core.AssertionTypes.IS;
import static io.cyborgcode.roa.validator.core.AssertionTypes.NOT_NULL;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * API tests for the update user endpoint: PUT /api/users/{id}.
 * <p>
 * Covers:
 * <ul>
 *   <li>Happy path: valid request body returns 200 with updated name, job, and non-null updatedAt.</li>
 *   <li>Error path: request without API key returns 400 bad request.</li>
 * </ul>
 *
 * @author Cyborg Code Syndicate 💍👨💻
 */
@API
class UpdateUserTest extends BaseQuest {

   @Test
   @Regression
   @Description("Happy path: PUT /api/users/{id} with valid name and job returns 200 with updated fields and a non-null updatedAt timestamp.")
   void updateUserValidNameAndJob_returnsUpdatedUserWith200(
         Quest quest,
         @Craft(model = DataCreator.Data.USER_UPDATE) CreateUserDto updateUserRequest) {
      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER.withPathParam(ID_PARAM, ID_TWO),
                  updateUserRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_OK).build(),
                  Assertion.builder().target(BODY).key(CREATE_USER_NAME_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_UPDATE_NAME).build(),
                  Assertion.builder().target(BODY).key(CREATE_USER_JOB_RESPONSE.getJsonPath())
                        .type(IS).expected(USER_UPDATE_JOB).build(),
                  Assertion.builder().target(BODY).key(UPDATED_AT.getJsonPath())
                        .type(NOT_NULL).expected(true).build()
            )
            .complete();
   }

   @Test
   @Regression
   @Description("Error path: PUT /api/users/{id} without API key header returns 400 bad request with an error field.")
   void updateUserMissingApiKey_returnsBadRequest(
         Quest quest,
         @Craft(model = DataCreator.Data.USER_UPDATE) CreateUserDto updateUserRequest) {
      quest
            .use(RING_OF_API)
            .requestAndValidate(
                  PUT_UPDATE_USER_UNAUTHENTICATED.withPathParam(ID_PARAM, ID_TWO),
                  updateUserRequest,
                  Assertion.builder().target(STATUS).type(IS).expected(SC_BAD_REQUEST).build(),
                  Assertion.builder().target(BODY).key(ERROR.getJsonPath())
                        .type(NOT_NULL).expected(true).build()
            )
            .complete();
   }

}
