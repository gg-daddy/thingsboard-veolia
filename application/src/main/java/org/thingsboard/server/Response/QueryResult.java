package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class QueryResult {

    private Boolean flag;

    private   Map<String, List<List<Long>>> data;


}
