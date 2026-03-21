package lk.ijse.eca.patientservice.service.impl;

import com.sun.jdi.request.DuplicateRequestException;
import lk.ijse.eca.patientservice.dto.PatientRequestDTO;
import lk.ijse.eca.patientservice.dto.PatientResponseDTO;
import lk.ijse.eca.patientservice.entity.Patient;
import lk.ijse.eca.patientservice.exception.FileOperationException;
import lk.ijse.eca.patientservice.exception.PatientNotFoundException;
import lk.ijse.eca.patientservice.mapper.PatientMapper;
import lk.ijse.eca.patientservice.repository.PatientRepository;
import lk.ijse.eca.patientservice.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Value("${app.storage.path}")
    private String storagePathStr;

    private Path storagePath;


    @Override
    @Transactional
    public PatientResponseDTO createPatient(PatientRequestDTO dto) {
        log.info("Creating patient with NIC {}", dto.getNic());

        if (patientRepository.existsById(dto.getNic())){
            log.warn("Patient with NIC {} already exists", dto.getNic());
            throw new DuplicateRequestException("Patient with NIC " + dto.getNic() + " already exists");
        }

        String pictureId = UUID.randomUUID().toString();

        Patient patient = patientMapper.toEntity(dto);
        patient.setPicture(pictureId);

        patientRepository.save(patient);
        log.debug("Patient created successfully: {}",dto.getNic());

        savePicture(pictureId, dto.getPicture());

        log.info("Patient created successfully: {}",dto.getNic());
        return patientMapper.toPatientResponseDTO(patient);
    }

    @Override
    @Transactional
    public PatientResponseDTO updatePatient(String nic, PatientRequestDTO dto) {
        log.debug("Updating patient with NIC {}", nic);

        Patient patient = patientRepository.findById(nic)
                .orElseThrow(() -> {
                    log.warn("Patient with NIC {} not found", nic);
                    return new PatientNotFoundException(nic);
                });

        String oldPictureId = patient.getPicture();
        boolean pictureChanged = dto.getPicture() != null && !dto.getPicture().isEmpty();
        String newPictureId = pictureChanged ? UUID.randomUUID().toString() : oldPictureId;

        patientMapper.updateEntity(dto, patient);
        patient.setPicture(newPictureId);

        patientRepository.save(patient);
        log.debug("Patient updated successfully: {}",nic);

        if (pictureChanged){
            savePicture(newPictureId, dto.getPicture());
            tryDeletePicture(oldPictureId);
        }

        log.info("Patient updated successfully: {}",nic);
        return patientMapper.toPatientResponseDTO(patient);
    }

    @Override
    @Transactional
    public void deletePatient(String nic) {
        log.debug("Deleting patient with NIC {}", nic);

        Patient patient = patientRepository.findById(nic)
                .orElseThrow(() -> {
                    log.warn("Patient with NIC {} not found", nic);
                    return new PatientNotFoundException(nic);
                });

        String pictureId = patient.getPicture();

        patientRepository.deleteById(nic);
        log.debug("Patient deleted successfully: {}",nic);

        deletePicture(pictureId);
        log.info("Patient deleted successfully: {}",nic);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatient(String nic) {
        log.debug("Getting patient with NIC {}", nic);
        return patientRepository.findById(nic)
                .map(patientMapper::toPatientResponseDTO)
                .orElseThrow(() -> {
                    log.warn("Patient with NIC {} not found", nic);
                    return new PatientNotFoundException(nic);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getAllPatients() {
        log.debug("Getting all patients");
        List<PatientResponseDTO> patient = patientRepository.findAll()
                .stream()
                .map(patientMapper::toPatientResponseDTO)
                .toList();
        log.debug("Fetched {} patients.", patient.size());
        return patient;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPatientPicture(String nic) {
        log.debug("Fetching patient picture with NIC {}", nic);
        Patient patient = patientRepository.findById(nic)
                .orElseThrow(() -> {
                    log.warn("Patient with NIC {} not found", nic);
                    return new PatientNotFoundException(nic);
                });
        Path filePath = storagePath().resolve(patient.getPicture());
        try {
            return Files.readAllBytes(filePath);
        }catch (IOException e){
            log.error("Failed to read picture file: {}", nic, e);
            throw new FileOperationException("Failed to read patient: " + nic,e);
        }
    }




    private Path storagePath() {
        if (storagePath == null) {
            storagePath = Paths.get(storagePathStr);
        }
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new FileOperationException(
                    "Failed to create storage directory: " + storagePath.toAbsolutePath(), e);
        }
        return storagePath;
    }

    private void savePicture(String pictureId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileOperationException("Picture file must not be empty");
        }
        Path filePath = storagePath().resolve(pictureId);
        try {
            Files.write(filePath, file.getBytes());
            log.debug("Picture saved: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save picture: {}", filePath, e);
            throw new FileOperationException("Failed to save picture file: " + pictureId, e);
        }
    }

    private void deletePicture(String pictureId) {
        Path filePath = storagePath().resolve(pictureId);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("Picture deleted: {}", filePath);
            } else {
                log.warn("Picture file not found on disk (already removed?): {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete picture: {}", filePath, e);
            throw new FileOperationException("Failed to delete picture file: " + pictureId, e);
        }
    }

    private void tryDeletePicture(String pictureId) {
        try {
            deletePicture(pictureId);
        } catch (FileOperationException e) {
            log.warn("Could not delete old picture file '{}'. Manual cleanup may be required.", pictureId);
        }
    }
}
