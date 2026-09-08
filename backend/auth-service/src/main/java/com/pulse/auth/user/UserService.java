package com.pulse.auth.user;

import com.pulse.auth.common.InvalidImageException;
import com.pulse.auth.common.NotFoundException;
import com.pulse.auth.user.dto.UpdateProfileRequest;
import com.pulse.auth.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

        String alias = request.alias().trim();
        // Friendly answer for the common case; the unique index still guards the
        // race between two people claiming the same alias at the same moment.
        if (userRepository.existsByAliasIgnoreCaseAndIdNot(alias, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That alias is already taken");
        }
        user.updateAlias(alias);
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

    /**
     * The uploaded picture when there is one, otherwise a generated disc with the
     * user's initial. Callers always get an image, so no client needs a fallback.
     *
     * @throws NotFoundException when the user itself does not exist
     */
    @Transactional(readOnly = true)
    public Avatar getAvatar(UUID userId) {
        return avatarRepository.findById(userId)
                .map(stored -> new Avatar(stored.getContentType(), stored.getImage(), true))
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("User not found"));
                    return new Avatar("image/svg+xml", GeneratedAvatar.forAlias(user.getAlias()), false);
                });
    }

    /** @param uploaded false when the bytes are the generated placeholder */
    public record Avatar(String contentType, byte[] image, boolean uploaded) {
    }
}
