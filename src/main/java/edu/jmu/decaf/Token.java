package edu.jmu.decaf;

/**
 * Data structure that represents a single token of a Decaf program.
 *
 */
public class Token
{
    /**
     * Token type.
     * 
     * <p> May be any of the following:
     *
     * <ul>
     * <li> ID - identifier </li>
     * <li> DEC - positive decimal integer literal </li>
     * <li> HEX - positive hexadecimal integer literal (prefixed by '0x') </li>
     * <li> STR - string literal (enclosed in quote marks) </li>
     * <li> KEY - keyword </li>
     * <li> SYM - symbol (including multi-character symbols) </li>
     * </ul>
     */
    public enum Type
    {
        ID, DEC, HEX, STR, KEY, SYM
    }

    public static String typeToString(Type type)
    {
        switch (type) {
            case ID:     return "ID ";
            case DEC:    return "DEC";
            case HEX:    return "HEX";
            case STR:    return "STR";
            case KEY:    return "KEY";
            case SYM:    return "SYM";
            default:     return "???";
        }
    }

    /**
     * Token type
     */
    public Type type;

    /**
     * Token text--raw data from the source file
     */
    public String text;

    /**
     * Token source info (filename and line number)
     */
    public SourceInfo source;

    public Token(Type type)
    {
        this(type, "", SourceInfo.INVALID);
    }

    public Token(Type type, String text)
    {
        this(type, text, SourceInfo.INVALID);
    }

    public Token(Type type, String text, SourceInfo source)
    {
        this.type = type;
        this.text = text;
        this.source = source;
    }

    /**
     * Returns a nicely-formatted representation suitable for debug printing
     */
    @Override
    public String toString()
    {
        return typeToString(type) + "\t\"" + text +
            "\"\t[" + source.toString() + "]";
    }
}

