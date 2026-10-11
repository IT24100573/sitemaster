package com.sitemaster.profile;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileResponses;
import com.sitemaster.profile.dto.ChangePasswordRequest;
import com.sitemaster.profile.dto.ProfileResponse;
import com.sitemaster.profile.dto.ProfileUpdateRequest;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Profile page (every signed-in role) and profile photos. */
@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final MessageResolver messages;

    public ProfileController(ProfileService profileService, MessageResolver messages) {
        this.profileService = profileService;
        this.messages = messages;
    }

    @GetMapping("/api/profile")
    public ProfileResponse get() {
        return profileService.getMine();
    }

    @PutMapping("/api/profile")
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateMine(request);
    }

    @PostMapping(value = "/api/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadPhoto(@RequestPart("file") @ValidFile(category = FileCategory.IMAGE) MultipartFile file) {
        return profileService.changeMyPhoto(file);
    }

    @DeleteMapping("/api/profile/photo")
    public ProfileResponse removePhoto() {
        return profileService.removeMyPhoto();
    }

    @PutMapping("/api/profile/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changeMyPassword(request);
        return new MessageResponse(messages.get("profile.password.changed"));
    }

    @GetMapping("/api/users/{userId}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long userId) {
        return FileResponses.inline(profileService.openPhoto(userId));
    }
}
