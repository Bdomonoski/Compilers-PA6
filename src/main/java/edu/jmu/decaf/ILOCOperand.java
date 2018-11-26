package edu.jmu.decaf;

/**
 * Single ILOC instruction operand: a register (named or virtual), a jump/call
 * label, an integer constant, or a string constant.
 *
 * The constructor for this function is private, meaning that you should never
 * create {@link ILOCOperand} objects yourself; rather, you could call one of
 * the factory methods ("{@code newX()}").
 */
public class ILOCOperand implements Cloneable
{
    public enum Type {
        INVALID,
        BASE_REG,
        RETURN_REG,
        VIRTUAL_REG,
        JUMP_LABEL,
        CALL_LABEL,
        INT_CONST,
        STR_CONST
    }

    private static int numTempRegisters = 0;
    private static int numAnonLabels = 0;

    /**
     * Operand type
     */
    public Type type;

    /**
     * Unique ID (for virtual register and jump label operands)
     */
    public int id;

    /**
     * String constant (for call label and string constant operands)
     */
    public String strConstant;

    /**
     * Integer constant (for int constant operands)
     */
    public int intConstant;

    /**
     * Base pointer register (ebp/rbp on x86)
     */
    public static final ILOCOperand REG_BP =
            new ILOCOperand(Type.BASE_REG, 0, "", 0);

    /**
     * Function return value register (eax/rax on x86)
     */
    public static final ILOCOperand REG_RET =
            new ILOCOperand(Type.RETURN_REG, 0, "", 0);

    /**
     * Special constant (zero)
     */
    public static final ILOCOperand ZERO =
            new ILOCOperand(Type.INT_CONST, 0, "", 0);

    /**
     * Invalid operand tag
     */
    public static final ILOCOperand INVALID =
            new ILOCOperand(Type.INVALID, 0, "", 0);

    /**
     * Allocate and return a new virtual register reference
     * @return Register operand
     */
    public static ILOCOperand newVirtualReg()
    {
        return new ILOCOperand(Type.VIRTUAL_REG, ++numTempRegisters, "", 0);
    }

    /**
     * Allocate and return a new anonymous jump label
     * @return Jump label operand
     */
    public static ILOCOperand newAnonymousLabel()
    {
        return new ILOCOperand(Type.JUMP_LABEL, ++numAnonLabels, "", 0);
    }

    /**
     * Return an operand for the given call target
     * @param name Target function name
     * @return Call label operand
     */
    public static ILOCOperand newCallLabel(String name)
    {
        return new ILOCOperand(Type.CALL_LABEL, 0, name, 0);
    }

    /**
     * Return an operand for the given integer literal
     * @param value Integer literal
     * @return Integer literal operand
     */
    public static ILOCOperand newIntConstant(int value)
    {
        return new ILOCOperand(Type.INT_CONST, 0, "", value);
    }

    /**
     * Return an operand for the given string literal
     * @param value String literal
     * @return String literal operand
     */
    public static ILOCOperand newStrConstant(String value)
    {
        return new ILOCOperand(Type.STR_CONST, 0, value, 0);
    }

    private ILOCOperand(Type type, int id, String strConstant, int intConstant)
    {
        this.type = type;
        this.id = id;
        this.strConstant = strConstant;
        this.intConstant = intConstant;
    }

    /**
     * Creates a copy of this {@link ILOCOperand} object.
     */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    /**
     * Ensure that two operands are considered "equal" if their string-based
     * representations are identical.
     *
     * WARNING: This is wildly inefficient!
     */
    public boolean equals(Object o)
    {
        return toString().equals(o.toString());
    }

    /**
     * Ensure that operands can be stored in hash sets and maps properly.
     *
     * WARNING: This is wildly inefficient!
     */
    public int hashCode()
    {
        return toString().hashCode();
    }

    /**
     * Builds a string representing the operand
     * @return String representation
     */
    public String toString()
    {
        switch (type) {
        case BASE_REG:      return "bp";
        case RETURN_REG:    return "ret";
        case VIRTUAL_REG:   return "r" + id;
        case JUMP_LABEL:    return "l" + id;
        case CALL_LABEL:    return strConstant;
        case INT_CONST:     return "" + intConstant;
        case STR_CONST:     return "\"" + ASTLiteral.addEscapeCodes(strConstant) + "\"";
        default:            return "INVALID";
        }
    }
}
