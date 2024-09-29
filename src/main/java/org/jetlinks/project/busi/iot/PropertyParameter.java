package org.jetlinks.project.busi.iot;

import lombok.Getter;
import lombok.Setter;

/**
 * @author hsy
 * @date 2022/10/26 14:21
 * @ClassName PropertyParameter
 */
@Getter
@Setter
public class PropertyParameter {

    private String pageNum;

    private String pageIndex;

    private String pageSize;

    private String from;

    private String to;

    private Boolean paging = true;

    private String sort = "desc";
}
