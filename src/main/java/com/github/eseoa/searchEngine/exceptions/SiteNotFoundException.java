package com.github.eseoa.searchEngine.exceptions;

public class SiteNotFoundException extends RuntimeException {

    public SiteNotFoundException() {
        super("Site not found");
    }

    public SiteNotFoundException(String s) {
        super(s);
    }
}
