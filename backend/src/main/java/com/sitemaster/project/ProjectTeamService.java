package com.sitemaster.project;

import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.project.dto.MemberResponse;

import java.util.List;

/**
 * Project team: the PM assigns site engineers and workers; a site engineer on
 * the project may assign workers.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ProjectTeamService {

    List<MemberResponse> members(Long projectId);

    List<LookupOption> candidates(Long projectId);

    MemberResponse add(Long projectId, Long userId);

    void remove(Long projectId, Long userId);
}
