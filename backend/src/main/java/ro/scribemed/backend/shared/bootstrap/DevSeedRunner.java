package ro.scribemed.backend.shared.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.identity.domain.UserRole;
import ro.scribemed.backend.identity.domain.UserStatus;
import ro.scribemed.backend.identity.infrastructure.AppUserRepository;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;

@Component
@ConditionalOnProperty(name = "scribemed.dev.seed.enabled", havingValue = "true")
public class DevSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);

    private final DevSeedProperties properties;
    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DevSeedRunner(
            DevSeedProperties properties,
            TenantRepository tenantRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.tenantRepository = tenantRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateProperties();

        String email = properties.getDoctorEmail().strip();
        if (appUserRepository.findByEmail(email).isPresent()) {
            log.info("dev_seed_user_skipped reason=user_exists email={}", email);
            return;
        }

        Tenant tenant = tenantRepository.findByName(properties.getTenantName().strip())
                .orElseGet(() -> tenantRepository.save(new Tenant(
                        properties.getTenantName().strip(),
                        TenantStatus.ACTIVE
                )));

        AppUser user = new AppUser(
                tenant,
                email,
                passwordEncoder.encode(properties.getDoctorPassword()),
                properties.getDoctorDisplayName().strip(),
                UserRole.DOCTOR,
                UserStatus.ACTIVE
        );
        appUserRepository.save(user);

        log.info(
                "dev_seed_user_created tenantId={} userId={} email={}",
                tenant.getId(),
                user.getId(),
                email
        );
    }

    private void validateProperties() {
        if (isBlank(properties.getTenantName())) {
            throw new IllegalStateException("SCRIBEMED_DEV_TENANT_NAME is required when dev seed is enabled");
        }
        if (isBlank(properties.getDoctorEmail())) {
            throw new IllegalStateException("SCRIBEMED_DEV_DOCTOR_EMAIL is required when dev seed is enabled");
        }
        if (isBlank(properties.getDoctorPassword())) {
            throw new IllegalStateException("SCRIBEMED_DEV_DOCTOR_PASSWORD is required when dev seed is enabled");
        }
        if (isBlank(properties.getDoctorDisplayName())) {
            throw new IllegalStateException("SCRIBEMED_DEV_DOCTOR_DISPLAY_NAME is required when dev seed is enabled");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
