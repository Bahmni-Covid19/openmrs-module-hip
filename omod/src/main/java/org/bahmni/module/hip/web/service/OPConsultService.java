package org.bahmni.module.hip.web.service;

import org.apache.log4j.Logger;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.controller.HipControllerAdvice;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.model.OpenMrsCondition;
import org.bahmni.module.hip.web.model.OpenMrsOPConsult;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.logic.op.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OPConsultService {
    private final FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder;
    private final OPConsultDao opConsultDao;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final ObsService obsService;
    private final ConceptService conceptService;
    private static final Logger log = Logger.getLogger(OPConsultService.class);

    @Autowired
    public OPConsultService(FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder, OPConsultDao opConsultDao,
                            PatientService patientService, EncounterService encounterService, ObsService obsService,
                            ConceptService conceptService) {
        this.fhirBundledOPConsultBuilder = fhirBundledOPConsultBuilder;
        this.opConsultDao = opConsultDao;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.obsService = obsService;
        this.conceptService = conceptService;
    }

    public List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, DateRange dateRange, String visitType) throws ParseException {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);

        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = getEncounterChiefComplaintsMap(patientUuid, visitType, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = getEncounterMedicalHistoryMap(patientUuid, visitType, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = getEncounterPhysicalExaminationMap(patientUuid, visitType, fromDate, toDate);

        for (Map.Entry<Encounter, List<OpenMrsCondition>> entry : encounterChiefComplaintsMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
            log.warn("Entry ->" + entry.getKey().toString());
            for(OpenMrsCondition o: chiefComplaintList){
                log.warn("Chief Complaint ->" + o.toString());
            }
        }

        for (Map.Entry<Encounter, List<OpenMrsCondition>> entry : encounterMedicalHistoryMap.entrySet()) {
            List<OpenMrsCondition> medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
            log.warn("Entry ->" + entry.getKey().toString());
            for(OpenMrsCondition o: medicalHistoryList){
                log.warn("Medical History ->" + o.toString());
            }
        }

        for (Map.Entry<Encounter, List<Obs>> entry : encounterPhysicalExaminationMap.entrySet()) {
            List<Obs> physicalExamination = encounterPhysicalExaminationMap.get(entry.getKey());
            log.warn("Entry ->" + entry.getKey().toString());
            for(Obs o: physicalExamination){
                log.warn("Physical Examination ->" + o.toString());
            }
        }

        List<OpenMrsOPConsult> openMrsOPConsultList = OpenMrsOPConsult.getOpenMrsOPConsultList(encounterChiefComplaintsMap, encounterMedicalHistoryMap, encounterPhysicalExaminationMap, patient);

        for(OpenMrsOPConsult o: openMrsOPConsultList){
            log.warn("OpenMRSOPConsult -> " + o.toString());
        }

        List<OPConsultBundle> opConsultBundles = openMrsOPConsultList.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList());

        return opConsultBundles;
    }

    private Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMap(String patientUuid, String visitType, Date fromDate, Date toDate) {
        List<Integer> physicalExaminationObsIds = opConsultDao.getPhysicalExamination(patientUuid, visitType, fromDate, toDate);
        List<Obs> physicalExaminationObs = physicalExaminationObsIds.stream().map(obsService::getObs).collect(Collectors.toList());
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = new HashMap<>();
        for (Obs o : physicalExaminationObs) {
            Encounter encounter = o.getEncounter();
            if(!encounterPhysicalExaminationMap.containsKey(encounter)) {
                encounterPhysicalExaminationMap.put(encounter, new ArrayList<>());
            }
            encounterPhysicalExaminationMap.get(encounter).add(o);
        }
        return encounterPhysicalExaminationMap;
    }

    private Map<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMap(String patientUuid, String visitType, Date fromDate, Date toDate) {
        List<Integer> obsIdsOfChiefComplaints = opConsultDao.getChiefComplaints(patientUuid, visitType, fromDate, toDate);
        List<Obs> chiefComplaintsObs = obsIdsOfChiefComplaints.stream().map(obsService::getObs).collect(Collectors.toList());
        HashMap<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = new HashMap<>();
        for (Obs o : chiefComplaintsObs) {
            Encounter encounter = o.getEncounter();
            if(!encounterChiefComplaintsMap.containsKey(encounter)) {
                encounterChiefComplaintsMap.put(encounter, new ArrayList<>());
            }
            encounterChiefComplaintsMap.get(encounter).add(new OpenMrsCondition(o.getUuid(), o.getValueCoded().getDisplayString(), o.getDateCreated()));
        }
        return encounterChiefComplaintsMap;
    }

    private Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryMap(String patientUuid, String visitType, Date fromDate, Date toDate) throws ParseException {
        List<String[]> medicalHistoryIds =  opConsultDao.getMedicalHistory(patientUuid, visitType, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = new HashMap<>();
        for (Object[] id : medicalHistoryIds) {
            log.warn("id of medical history -> " + id[0] + " " +  id[1]  + " " + id[2] + " " + id[3]);
            Encounter encounter = encounterService.getEncounter(Integer.parseInt(String.valueOf(id[3])));
            if (!encounterMedicalHistoryMap.containsKey(encounter))
                encounterMedicalHistoryMap.put(encounter, new ArrayList<>());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = dateFormat.parse(String.valueOf(id[4]));
            encounterMedicalHistoryMap.get(encounter).add(new OpenMrsCondition(String.valueOf(id[2]), conceptService.getConcept(Integer.parseInt(String.valueOf(id[1]))).getDisplayString(), date));
        }
        return encounterMedicalHistoryMap;
    }

}