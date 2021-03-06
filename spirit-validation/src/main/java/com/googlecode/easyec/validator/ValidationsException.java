package com.googlecode.easyec.validator;

import java.util.ArrayList;
import java.util.List;

public class ValidationsException extends Exception {

    private static final long serialVersionUID = -1162531965859257859L;
    private List<ValidationException> _exceptions = new ArrayList<>();

    public ValidationsException() { }

    public void add(ValidationException e) {
        if (e != null) this._exceptions.add(e);
    }

    public List<ValidationException> getExceptions() {
        return this._exceptions;
    }

    public boolean hasExceptions() {
        return getExceptions().size() > 0;
    }
}
