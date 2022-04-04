package net.boomerangplatform.miscs.internal;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import net.boomerangplatform.client.ExernalUserService;
import net.boomerangplatform.client.model.UserProfile;
import net.boomerangplatform.misc.FlowTests;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class BoomerangUserServiceTests extends FlowTests {

  @Autowired
  private ExernalUserService userService;

  @Test
  public void testGetUserProfile() {
    UserProfile userProfile = userService.getInternalUserProfile();
    assertEquals( "trbula@us.ibm.com", userProfile.getEmail());
  }

  @Override
  @BeforeEach
  public void setUp() throws IOException {

    super.setUp();

    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    mockServer
        .expect(manyTimes(), requestTo(containsString("http://localhost:8084/internal/users/user")))
        .andExpect(method(HttpMethod.GET)).andRespond(
            withSuccess(getMockFile("mock/users/users.json"), MediaType.APPLICATION_JSON));

  }
}
