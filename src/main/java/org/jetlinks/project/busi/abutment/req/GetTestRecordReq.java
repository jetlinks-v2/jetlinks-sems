package org.jetlinks.project.busi.abutment.req;

import lombok.Data;

@Data
public class GetTestRecordReq {

    private String projectStatus;

    private String createEndTime;

    private String createStartTime;

    private String pageNum;

    private String pageSize;
}
