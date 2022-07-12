package com.github.eseoa.searchEngine.exceptions;

public class IndexingIsNotRunningException extends RuntimeException {

    public IndexingIsNotRunningException() {
        super("Indexing is not running");
    }

    public IndexingIsNotRunningException(String s) {
        super(s);
    }
}
