package com.sitemaster.profile;

import com.sitemaster.file.FileDownload;
import com.sitemaster.profile.dto.ChangePasswordRequest;
import com.sitemaster.profile.dto.ProfileResponse;
import com.sitemaster.profile.dto.ProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * The signed-in user's own profile, photo and password, plus serving profile
 * photos to other signed-in users.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ProfileService {

    ProfileResponse getMine();

    ProfileResponse updateMine(ProfileUpdateRequest request);

    ProfileResponse changeMyPhoto(MultipartFile file);

    ProfileResponse removeMyPhoto();

    void changeMyPassword(ChangePasswordRequest request);

    /** A user's photo, if the viewer may see it. Suppliers may only see their own. */
    FileDownload openPhoto(Long userId);
}
