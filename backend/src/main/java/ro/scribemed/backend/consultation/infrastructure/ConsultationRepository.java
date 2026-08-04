package ro.scribemed.backend.consultation.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.scribemed.backend.consultation.application.ConsultationResponse;
import ro.scribemed.backend.consultation.domain.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Optional<Consultation> findByIdAndTenant_Id(UUID id, UUID tenantId);

    @Query(
            value = """
                    select new ro.scribemed.backend.consultation.application.ConsultationResponse(
                        c.id,
                        c.tenant.id,
                        c.patient.id,
                        c.patient.firstName,
                        c.patient.lastName,
                        c.doctorUser.id,
                        c.status,
                        c.patientInformedAt,
                        c.createdAt,
                        c.updatedAt
                    )
                    from Consultation c
                    where c.tenant.id = :tenantId
                    """,
            countQuery = """
                    select count(c)
                    from Consultation c
                    where c.tenant.id = :tenantId
                    """
    )
    Page<ConsultationResponse> findResponsesByTenantId(UUID tenantId, Pageable pageable);
}
