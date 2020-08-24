package net.boomerangplatform.controller.teams;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.FlowTeam;
import net.boomerangplatform.model.FlowUser;
import net.boomerangplatform.model.OneTimeCode;
import net.boomerangplatform.model.TeamQueryResult;
import net.boomerangplatform.model.UserQueryResult;
import net.boomerangplatform.model.profile.SortSummary;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.crud.TeamService;


@RestController
@ConditionalOnProperty(value = "boomerang.standalone", havingValue = "true", matchIfMissing = false)
@RequestMapping("/flow/manage")
public class ManagementController {

  @Autowired
  private TeamService teamService;

  @Autowired
  private UserIdentityService userIdentityService;

  
  @PatchMapping(value = "/users/{userId}")
  public void updateFlowUser(@PathVariable String userId,
      @RequestBody FlowUser flowUser) {
    userIdentityService.updateFlowUser(userId, flowUser);
  }
  
  
  @GetMapping(value = "/users")
  public UserQueryResult getUsers(@RequestParam(required = false) String query,
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {

    Sort pagingSort = Sort.by(Direction.ASC, "firstLoginDate");

    SortSummary sortSummary = new SortSummary();
    sortSummary.setProperty("firstLoginDate");
    sortSummary.setDirection(Direction.ASC.toString());

    if (StringUtils.isNotBlank(sort)) {
      Direction direction = order == null ? Direction.ASC : order;
      sortSummary.setDirection(direction.toString());
      sortSummary.setProperty(sort);
      pagingSort = Sort.by(direction, sort);
    }

    final Pageable pageable = PageRequest.of(page, size, pagingSort);
    if (StringUtils.isNotBlank(query)) {
      UserQueryResult result = userIdentityService.getUserViaSearchTerm(query, pageable);
      result.setupSortSummary(sortSummary);
      return result;
    } else {
      UserQueryResult result = userIdentityService.getUsers(pageable);
      result.setupSortSummary(sortSummary);
      return result;
    }
  }

  @GetMapping(value = "/teams")
  public TeamQueryResult getTeams(@RequestParam(required = false) String query,
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {

    SortSummary sortSummary = new SortSummary();
    sortSummary.setProperty("name");
    sortSummary.setDirection(Direction.ASC.toString());

    Sort pagingSort = Sort.by(Direction.ASC, "name");

    if (StringUtils.isNotBlank(sort)) {
      Direction direction = order == null ? Direction.ASC : order;
      sortSummary.setDirection(direction.toString());
      sortSummary.setProperty(sort);

      pagingSort = Sort.by(direction, sort);
    }

    final Pageable pageable = PageRequest.of(page, size, pagingSort);

    TeamQueryResult response = teamService.getAllAdminTeams(pageable);
    response.setupSortSummary(sortSummary);
    return response;
  }
  
  

  @PostMapping(value = "/teams")
  public FlowTeam addTeam(FlowTeam flowTeam) {
    String teamName = flowTeam.getName();
    return teamService.createStandaloneTeam(teamName);
  }

  @GetMapping(value = "/teams/{teamId}")
  public FlowTeam getTeam(@PathVariable String teamId) {
    return teamService.getTeamById(teamId);
  }

  @PatchMapping(value = "/teams/{teamId}/members")
  public void updateTeamMembers(@PathVariable String teamId,
      @RequestBody List<String> teamMembers) {
    teamService.updateTeamMembers(teamId, teamMembers);
  }
  
  @PutMapping(value = "/teams/{teamId}")
  public void updateTeamMembers(@PathVariable String teamId, @RequestBody FlowTeam flow) {
    teamService.updateTeam(teamId, flow);
  }
  
  @PostMapping(value = "/activate")
  public ResponseEntity<Boolean> validateSetup(@RequestBody OneTimeCode otc) {
    if (userIdentityService.activateSetup(otc)) {
      return new ResponseEntity<>(true, HttpStatus.OK);
    } else {
      return new ResponseEntity<>(false, HttpStatus.FORBIDDEN);
    }
  }
}
