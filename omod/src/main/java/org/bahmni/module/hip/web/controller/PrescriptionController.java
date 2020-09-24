package org.bahmni.module.hip.web.controller;

import org.apache.log4j.Logger;
import org.bahmni.module.hip.web.model.Prescription;
import org.bahmni.module.hip.web.service.PrescriptionService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip")
@Controller
public class PrescriptionController {
    private HipContext hipContext;
    private PrescriptionService prescriptionService;
    private static final Logger log = Logger.getLogger(PrescriptionController.class);

    @Autowired
    public PrescriptionController(HipContext hipContext, PrescriptionService prescriptionService) {
        this.hipContext = hipContext;
        this.prescriptionService = prescriptionService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/prescriptions", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<Object> get(@RequestParam String patientId) {
        try {
            List<Prescription> prescriptions =  prescriptionService.getPrescriptions(patientId, getFromDate(), new Date());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(prescriptions);
        } catch (Exception e) {
            log.error("Error occurred while trying to call prescriptionService.getPrescriptions", e);
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(e.getMessage());
        }
    }

    private Date getFromDate() {
        LocalDateTime dateTime = LocalDateTime.now();
        dateTime = dateTime.minusDays(60);
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}