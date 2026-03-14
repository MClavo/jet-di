package org.mclavo;

import org.mclavo.annotation.Jet;

@Jet
public class Service {
    private String s;

    public Service(String s) {
        this.s = s;
    }

    void send() {
        System.out.println(s);
    }
}
