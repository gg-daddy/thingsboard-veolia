package org.thingsboard.server.Response;

import lombok.Data;

import java.io.Serializable;

@Data
public class SelectBean implements Serializable {

    private static final long serialVersionUID = -827963185852172377L;

    private String cid;

    private String pid;

    private String name;
}
