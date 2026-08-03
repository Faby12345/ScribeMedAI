package ro.scribemed.backend.patient.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.scribemed.backend.patient.application.PatientResponse;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.domain.PatientStatus;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndTenant_Id(UUID id, UUID tenantId);

    List<Patient> findByTenant_IdAndStatusOrderByLastNameAscFirstNameAsc(UUID tenantId, PatientStatus status);

    @Query("""
      select new ro.scribemed.backend.patient.application.PatientResponse(
          p.id,
          p.tenant.id,
          p.firstName,
          p.lastName,
          p.birthDate,
          p.sex,
          p.phone,
          p.email,
          p.status,
          p.createdAt,
          p.updatedAt
      )
      from Patient p
      where p.tenant.id = :tenantId
        and p.status = ro.scribemed.backend.patient.domain.PatientStatus.ACTIVE
      order by p.lastName asc, p.firstName asc
  """)
    List<PatientResponse> findActiveResponsesByTenantId(UUID tenantId);

    Patient findPatientById(UUID id);

    boolean existsByTenant_IdAndEmailIgnoreCase(UUID tenantId, String email);

    Optional<Patient> findPatientByIdAndTenantId(UUID id, UUID tenantId);
}
