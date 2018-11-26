package edu.jmu.decaf;

/**
 * Error encountered during lexing.
 */
public class InvalidTokenException extends Exception
{
    public static final long serialVersionUID = 1L;

    public InvalidTokenException(String msg)
    {
        super(msg);
    }
}

