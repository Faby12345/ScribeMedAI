package ro.scribemed.backend.medication.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ro.scribemed.backend.medication.domain.Medication;
import ro.scribemed.backend.medication.dto.MedicationResponse;



@Repository
public interface MedicationRepository extends JpaRepository<Medication, String> {

    @Query(
            value = """
                      select new ro.scribemed.backend.medication.dto.MedicationResponse(
                          m.cimCode,
                          m.commercialName,
                          m.activeSubstance,
                          m.pharmaceuticalForm,
                          m.concentration,
                          m.appManufacturer,
                          m.appHolder,
                          m.atcCode,
                          m.therapeuticAction,
                          m.prescriptionType,
                          m.appPackagingAuthorization,
                          m.packaging,
                          m.packagingVolume,
                          m.packagingValidity,
                          m.centralizedPendingRomanianDecision,
                          m.temporaryCirculation,
                          m.centralizedAuthorized,
                          m.authorizationSuspended,
                          m.hasAdditionalInformation,
                          m.sourceUpdatedAt
                      )
                      from Medication m
                      """,
            countQuery = """
                      select count(m)
                      from Medication m
                      """
    )
    Page<MedicationResponse> findResponse(Pageable page);
}
