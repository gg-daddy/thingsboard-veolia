package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class IndexOpenResponse implements Serializable {


    private static final long serialVersionUID = 8829501922579431281L;


    private String type;


    private List<List<Long>> dataList;
}
