package edu.jmu.decaf;

/**
 * Error encountered during parsing.
 */
public class InvalidSyntaxException extends Exception
{
    public static final long serialVersionUID = 1L;

    public InvalidSyntaxException(String msg)
    {
        super(msg);
    }
}

