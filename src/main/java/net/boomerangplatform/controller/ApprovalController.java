package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.Approval;
import net.boomerangplatform.service.FlowApprovalService;

@RestController
@RequestMapping("/workflow/")
public class ApprovalController {

  @Autowired
  private FlowApprovalService flowApprovalService;

  @GetMapping(value = "/approvals/mine")
  public List<Approval> getApprovalsForUser() {
    return flowApprovalService.getApprovalsForUser();
  }

  @GetMapping(value = "/approvals/{teamId")
  public List<Approval> getApprovalsForTeam(@PathVariable String teamId) {
    return flowApprovalService.getApprovalsForTeam(teamId);
  }

  @PutMapping(value = "/approvals/action")
  public void actionApproval(@RequestParam String id, @RequestParam Boolean approved) {
    flowApprovalService.actionApproval(id, approved);
  }  
}
