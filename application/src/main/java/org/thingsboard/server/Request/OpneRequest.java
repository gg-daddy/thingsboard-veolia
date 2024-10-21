package org.thingsboard.server.Request;

import lombok.Data;
import scala.Serializable;

@Data
public class OpneRequest implements Serializable {
    private static final long serialVersionUID = 4081755721948070938L;

    private String code;


}
