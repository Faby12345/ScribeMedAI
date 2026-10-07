package ro.scribemed.backend.medication.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.scribemed.backend.medication.domain.Medication;
import ro.scribemed.backend.medication.dto.MedicationResponse;

import java.util.List;


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

    @Query(
            """
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
            where m.authorizationSuspended = false
              and (
                  lower(m.cimCode) like lower(concat('%', :query, '%'))
                  or lower(m.commercialName) like lower(concat('%', :query, '%'))
                  or lower(m.activeSubstance) like lower(concat('%', :query, '%'))
                  or lower(m.atcCode) like lower(concat('%', :query, '%'))
              )
            order by
                case
                    when lower(m.cimCode) = lower(:query) then 0
                    when lower(m.commercialName) = lower(:query) then 1
                    when lower(m.cimCode) like lower(concat(:query, '%')) then 2
                    when lower(m.commercialName) like lower(concat(:query, '%')) then 3
                    when lower(m.activeSubstance) like lower(concat(:query, '%')) then 4
                    when lower(m.atcCode) like lower(concat(:query, '%')) then 5
                    else 6
                end,
                m.commercialName,
                m.cimCode
            """
    )
    List<MedicationResponse> findByQuery(
            @Param("query") String query,
            Pageable pageable
    );
}
