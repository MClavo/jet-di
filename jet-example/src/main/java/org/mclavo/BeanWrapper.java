package org.mclavo;

public class BeanWrapper {
    private final EmptyBean bean;

    public BeanWrapper(EmptyBean emptyBean) {
        this.bean = emptyBean;
    }

    public String getMessage() {
        return bean.getMessage();
    }
}
