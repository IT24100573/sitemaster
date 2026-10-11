package com.sitemaster.project;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.file.FileDownload;
import com.sitemaster.project.dto.ProjectDetail;
import com.sitemaster.project.dto.ProjectMapMarker;
import com.sitemaster.project.dto.ProjectRequest;
import com.sitemaster.project.dto.ProjectSummary;
import com.sitemaster.project.dto.PublicProjectCard;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Projects: listing (respecting visibility), creating, editing, status changes,
 * cover photos and the public gallery and map.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ProjectService {

    PageResponse<ProjectSummary> search(ProjectStatus status, PageQuery pageQuery);

    ProjectDetail get(Long id);

    ProjectDetail create(ProjectRequest request);

    ProjectDetail update(Long id, ProjectRequest request);

    ProjectDetail changeStatus(Long id, String status);

    ProjectDetail changeCover(Long id, MultipartFile file);

    ProjectDetail removeCover(Long id);

    FileDownload openCover(Long id);

    List<PublicProjectCard> completedGallery();

    List<ProjectMapMarker> mapMarkers();

    FileDownload openPublicCover(Long id);
}
