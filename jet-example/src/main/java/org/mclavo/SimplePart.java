package org.mclavo;

public class SimplePart {
    private final EmptyBean bean;

    public SimplePart(EmptyBean emptyBean) {
        this.bean = emptyBean;
    }

    public String getMessage() {
        return bean.getMessage();
    }
}
