package org.jetlinks.project.busi.abutment.res;

import lombok.Data;

import java.util.List;

@Data
public class MenueRes {

    private String id;

    private String label;

    private List<MenueRes> children;

    private List<MenueRes> button;
}
