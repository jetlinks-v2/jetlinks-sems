package org.jetlinks.project.busi.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.project.busi.entity.TestConfigDeviceEntity;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class TestConfigDeviceService extends GenericReactiveCrudService<TestConfigDeviceEntity,String> {

    public static HashMap<String, DateTime> setOverlap(DateTime startDate1,
                                                       DateTime endDate1, DateTime startDate2, DateTime endDate2) {
        HashMap<String, DateTime> intersection = new HashMap<>();

        if (startDate1.compareTo(startDate2) >= 0 && startDate1.compareTo(endDate2) <= 0
            && endDate1.compareTo(endDate2) > 0) {
            intersection.put("startDate", startDate1);
            intersection.put("endDate", endDate2);
        } else if (endDate1.compareTo(startDate2) >= 0
            && startDate1.compareTo(endDate2) <= 0
            && startDate1.compareTo(startDate2) < 0) {
            intersection.put("startDate", startDate2);
            intersection.put("endDate", endDate2);

        } else if (startDate1.compareTo(startDate2) >= 0
            && endDate1.compareTo(endDate2) <= 0) {
            intersection.put("startDate", startDate1);
            intersection.put("endDate", endDate1);
        } else if ((startDate1.compareTo(startDate2) <= 0
            && endDate1.compareTo(endDate2) >= 0)) {
            intersection.put("startDate", startDate2);
            intersection.put("endDate", endDate2);
        }

        return intersection;
    }
}
