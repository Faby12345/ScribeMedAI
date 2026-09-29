package ro.scribemed.backend.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import ro.scribemed.backend.patient.domain.PatientSex;

public record CreatePatientRequest(
        @NotBlank(message = "Prenumele pacientului este obligatoriu.")
        @Size(max = 100, message = "Prenumele pacientului nu poate depasi 100 de caractere.")
        String firstName,

        @NotBlank(message = "Numele pacientului este obligatoriu.")
        @Size(max = 100, message = "Numele pacientului nu poate depasi 100 de caractere.")
        String lastName,

        @PastOrPresent(message = "Data nasterii nu poate fi in viitor.")
        LocalDate birthDate,

        PatientSex sex,

        @Size(max = 50, message = "Numarul de telefon nu poate depasi 50 de caractere.")
        String phone,

        @Email(message = "Adresa de email nu este valida.")
        @Size(max = 320, message = "Adresa de email nu poate depasi 320 de caractere.")
        String email
) {
}
