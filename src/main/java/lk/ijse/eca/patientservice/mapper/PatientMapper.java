package lk.ijse.eca.patientservice.mapper;

import lk.ijse.eca.patientservice.dto.PatientRequestDTO;
import lk.ijse.eca.patientservice.dto.PatientResponseDTO;
import lk.ijse.eca.patientservice.entity.Patient;
import org.mapstruct.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class PatientMapper {

    @Mapping(target = "picture", expression = "java(buildPictureUrl(patient))")
    public abstract PatientResponseDTO toPatientResponseDTO(Patient patient);

    @Mapping(target = "picture", ignore = true)
    public abstract Patient toEntity(PatientRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "nic", ignore = true)
    @Mapping(target = "picture", ignore = true)
    public abstract void updateEntity(PatientRequestDTO dto,@MappingTarget Patient patient);

    protected String buildPictureUrl(Patient patient) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/patient/{nic}/picture")
                .buildAndExpand(patient.getNic())
                .toUriString();
    }
}
