package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.client.ClientError;
import org.bahmni.module.hip.web.model.BundledOPConsultResponse;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DiagnosticReportBundle;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.service.OPConsultService;
import org.bahmni.module.hip.web.service.ValidationService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

import static org.bahmni.module.hip.web.utils.DateUtils.parseDate;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip/opConsult")
@RestController
public class OPConsultController extends BaseRestController {
    private final OPConsultService opConsultService;
    private final ValidationService validationService;

    @Autowired
    public OPConsultController(OPConsultService opConsultService, ValidationService validationService) {
        this.opConsultService = opConsultService;
        this.validationService = validationService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/visit", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> get(@RequestParam String patientId,
                          @RequestParam String visitType,
                          @RequestParam String fromDate,
                          @RequestParam String toDate) throws ParseException {
        if (patientId == null || patientId.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noPatientIdProvided());
        if (visitType == null || visitType.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noVisitTypeProvided());
        if (!validationService.isValidVisit(visitType))
            return ResponseEntity.badRequest().body(ClientError.invalidVisitType());
        if (!validationService.isValidPatient(patientId))
            return ResponseEntity.badRequest().body(ClientError.invalidPatientId());
        List<OPConsultBundle> opConsultBundle =
                opConsultService.getOpConsultsForVisit(patientId, new DateRange(parseDate(fromDate), parseDate(toDate)), visitType);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(new BundledOPConsultResponse(opConsultBundle));
    }
}