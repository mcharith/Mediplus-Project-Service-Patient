package lk.ijse.eca.patientservice.repository;

import lk.ijse.eca.patientservice.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, String> {
}
