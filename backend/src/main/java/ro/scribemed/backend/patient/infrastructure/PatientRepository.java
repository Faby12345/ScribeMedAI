package ro.scribemed.backend.patient.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.patient.domain.PatientStatus;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByIdAndTenant_Id(UUID id, UUID tenantId);

    List<Patient> findByTenant_IdAndStatusOrderByLastNameAscFirstNameAsc(UUID tenantId, PatientStatus status);
}
