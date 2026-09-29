package ro.scribemed.backend.shared.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scribemed.dev.seed")
public class DevSeedProperties {

    private boolean enabled;
    private String tenantName;
    private String doctorEmail;
    private String doctorPassword;
    private String doctorDisplayName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getDoctorEmail() {
        return doctorEmail;
    }

    public void setDoctorEmail(String doctorEmail) {
        this.doctorEmail = doctorEmail;
    }

    public String getDoctorPassword() {
        return doctorPassword;
    }

    public void setDoctorPassword(String doctorPassword) {
        this.doctorPassword = doctorPassword;
    }

    public String getDoctorDisplayName() {
        return doctorDisplayName;
    }

    public void setDoctorDisplayName(String doctorDisplayName) {
        this.doctorDisplayName = doctorDisplayName;
    }
}
