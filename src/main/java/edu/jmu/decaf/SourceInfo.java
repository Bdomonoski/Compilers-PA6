package edu.jmu.decaf;

/**
 * Stores information about a location in a program's source code. At the most
 * basic level, this consists of a filename and a line number.
 */
public class SourceInfo {

    /** Flag value used to indicate missing or erroneous source information
     */
    public static final SourceInfo INVALID =
            new SourceInfo("<invalid>", -1);

    public String filename;
    public int lineNumber;

    public SourceInfo(String filename)
    {
        this(filename, -1);
    }

    public SourceInfo(String filename, int lineNumber)
    {
        this.filename = filename;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString()
    {
        return filename + ":" + lineNumber;
    }
}
