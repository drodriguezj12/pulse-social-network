package com.pulse.auth.user;

import com.pulse.auth.common.InvalidImageException;
import com.pulse.auth.common.NotFoundException;
import com.pulse.auth.user.dto.UpdateProfileRequest;
import com.pulse.auth.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Whitelist of image types the profile picture accepts. */
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final UserAvatarRepository avatarRepository;

    public UserService(UserRepository userRepository, UserAvatarRepository avatarRepository) {
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> UserResponse.from(user, avatarRepository.existsById(userId)))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.updateNames(request.firstName().trim(), request.lastName().trim());
        log.info("AUDIT profile_updated userId={} username={}", userId, user.getUsername());
        return UserResponse.from(user, avatarRepository.existsById(userId));
    }

    @Transactional
    public void saveAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidImageException("Only JPEG, PNG or WebP images are allowed");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("Could not read the uploaded image");
        }

        avatarRepository.findById(userId).ifPresentOrElse(
                existing -> {
                    existing.replaceWith(contentType, bytes);
                    avatarRepository.save(existing);
                },
                () -> avatarRepository.save(new UserAvatarEntity(userId, contentType, bytes)));
        log.info("AUDIT avatar_updated userId={} contentType={} sizeBytes={}",
                userId, contentType, bytes.length);
    }

    @Transactional(readOnly = true)
    public UserAvatarEntity getAvatar(UUID userId) {
        return avatarRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Avatar not found"));
    }
}
