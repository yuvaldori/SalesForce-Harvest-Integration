package com.gigaspaces.ps;

/**
 * Created by yuval on 12/14/16.
 */
public class SFException extends Exception{
    public SFException() { super(); }
    public SFException(String message) { super(message); }
    public SFException(String message, Exception e) { super(message, e); }
}
