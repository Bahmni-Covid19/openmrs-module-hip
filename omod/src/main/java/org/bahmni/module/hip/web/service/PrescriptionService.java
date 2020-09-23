package org.bahmni.module.hip.web.service;


import org.apache.log4j.Logger;
import org.bahmni.module.hip.web.model.Prescription;
import org.hibernate.validator.constraints.NotEmpty;
import org.openmrs.DrugOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {
    private static final Logger log = Logger.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final PrescriptionGenerator prescriptionGenerator;

    @Autowired
    public PrescriptionService(OpenMRSDrugOrderClient openMRSDrugOrderClient, PrescriptionGenerator prescriptionGenerator) {
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.prescriptionGenerator = prescriptionGenerator;
    }


    public List<Prescription> getPrescriptions(String patientIdUuid, Date fromDate, Date toDate) {
        List<DrugOrder> drugOrders = openMRSDrugOrderClient.getDrugOrdersByDateFor(patientIdUuid, fromDate, toDate);
        return prescriptionsFor(drugOrders);
    }

    public String getEncounterUuidForOrder(DrugOrder order) {
        return order.getEncounter().getUuid();
    }

    private List<Prescription> prescriptionsFor(List<DrugOrder> drugOrders) {
        if (CollectionUtils.isEmpty(drugOrders)) {
            return new ArrayList<>();
        }

        Map<String, List<DrugOrder>> encounterDrugOrderMap = drugOrders
                .stream()
                .collect(Collectors.groupingBy(this::getEncounterUuidForOrder));

        return encounterDrugOrderMap.values()
                .stream()
                .map(this::createPrescription)
                .collect(Collectors.toList());
    }

    private Prescription createPrescription(@NotEmpty List<DrugOrder> drugOrders) {
        return prescriptionGenerator.generate(drugOrders);
    }

}
