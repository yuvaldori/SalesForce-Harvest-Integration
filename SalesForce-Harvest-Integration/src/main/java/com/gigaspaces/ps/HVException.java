package com.gigaspaces.ps;

/**
 * Created by yuval on 5/7/17.
 */
public class HVException extends Exception{
    public HVException() { super(); }
    public HVException(String message) { super(message); }
    public HVException(String message, Exception e) { super(message, e); }
}
