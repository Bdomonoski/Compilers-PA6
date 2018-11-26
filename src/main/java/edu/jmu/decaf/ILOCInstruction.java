package edu.jmu.decaf;

import java.util.*;

/**
 * Represents a single ILOC instruction in a pseudo-triple format, including a
 * "form" indicator (enum) and up to three operands, which can
 * represent virtual register IDs, special registers, constants, or jump/call
 * targets. Boolean constants are represented using zero for false and any
 * other value for true.
 */
public class ILOCInstruction implements Cloneable
{
    /**
     * Possible forms of an ILOC instruction
     */
    public enum Form
    {
        ADD,            //  r1 + r2     =>      r3
        SUB,            //  r1 - r2     =>      r3
        MULT,           //  r1 * r2     =>      r3
        DIV,            //  r1 / r2     =>      r3

        ADD_I,          //  r1 + c2     =>      r3
        MULT_I,         //  r1 * c2     =>      r3

        // missing: SUB_I, RSUB_I, DIV_I, RDIV_I

        // missing: LSHIFT, LSHIFT_I, RSHIFT, RSHIFT_I

        AND,            //  r1 && r2    =>      r3
        OR,             //  r1 || r2    =>      r3

        // missing: AND_I, OR_I, XOR, XOR_I

        LOAD_I,         //  c1          =>      r2
        LOAD,           //  [r1]        =>      r2
        LOAD_AI,        //  [r1+c2]     =>      r3
        LOAD_AO,        //  [r1+r2]     =>      r3

        // missing: CLOAD, CLOAD_AI, CLOAD_AO

        STORE,          //  r1          =>      [r2]
        STORE_AI,       //  r1          =>      [r2+c3]
        STORE_AO,       //  r1          =>      [r2+r3]

        // missing: CSTORE, CSTORE_AI, CSTORE_AO

        I2I,            //  r1          =>      r2

        // missing: C2C, C2I, I2C

        JUMP,           //              =>      PC=l1
        CBR,            //  r1          =>      PC=l2 || PC=l3

        // missing: JUMP_I, TBL

        CMP_LT,         //  r1 <  r2    =>      r3
        CMP_LE,         //  r1 <= r2    =>      r3
        CMP_EQ,         //  r1 == r2    =>      r3
        CMP_GE,         //  r1 >= r2    =>      r3
        CMP_GT,         //  r1 >  r2    =>      r3
        CMP_NE,         //  r1 != r2    =>      r3

        // missing: COMP, CBR_LT, CBR_LE, CBR_EQ, CBR_GE, CBR_GT, CBR_NE

        // new (not in EAC):

        LOAD_S,         //  & s1        =>      r2
        NOT,            //  ! r1        =>      r2
        NEG,            //  - r1        =>      r2
        LABEL,          //  l1
        PARAM,          //  r1
        CALL,
        RETURN,
        PRINT,          //  c/r1
        NOP,

        // SSA
        PHI             //  phi(r1, r2) =>      r3

    }

    /**
     * Debug information for data flow analysis
     */
    public String variableName;

    /**
     * Instruction form
     */
    public Form form;

    /**
     * Instruction operands
     */
    public ILOCOperand[] operands;

    /**
     * Optional human-readable comment for this instruction
     */
    public String comment;

    /**
     * Create a new instruction
     * @param form Instruction form
     * @param operands Instruction operands
     */
    public ILOCInstruction(Form form, ILOCOperand[] operands)
    {
        this(form, operands, "");
    }

    /**
     * Create a new instruction
     * @param form Instruction form
     * @param operands Instruction operands
     * @param comment Human-readable comment
     */
    public ILOCInstruction(Form form, ILOCOperand[] operands, String comment)
    {
        this.variableName = null;
        this.form = form;
        this.operands = operands;
        this.comment = comment;
    }

    /**
     * Create a *deep copy* of this instruction. This is important for
     * register/label renumbering; otherwise, the shared {@link ILOCOperand}
     * ID info interferes with the renumbering.
     */
    public Object clone() throws CloneNotSupportedException
    {
        ILOCOperand[] newOps = new ILOCOperand[operands.length];
        for (int i=0; i<operands.length; i++) {
            newOps[i] = (ILOCOperand)operands[i].clone();
        }
        ILOCInstruction insn = new ILOCInstruction(this.form, newOps, this.comment);
        insn.variableName = this.variableName;
        return insn;
    }

    /**
     * Returns a list of ILOCOperands read by this instruction. Reports only
     * register or memory operands.
     * @return List of read operands
     */
    public List<ILOCOperand> getReadOperands()
    {
        return getReadOperands(false);
    }

    /**
     * Returns a list of ILOCOperands read by this instruction. Reports only
     * register or memory operands, unless the given flag is true in which case
     * it also returns literal operands.
     * @param includeLiterals Return literal operands as well
     * @return List of read operands
     */
    public List<ILOCOperand> getReadOperands(boolean includeLiterals)
    {
        List<ILOCOperand> ops = new ArrayList<ILOCOperand>();
        switch (form) {
        case STORE_AO:
            ops.add(operands[0]);
            ops.add(operands[1]);
            ops.add(operands[2]);
            break;
        case ADD:
        case SUB:
        case MULT:
        case DIV:
        case AND:
        case OR:
        case CMP_LT:
        case CMP_LE:
        case CMP_EQ:
        case CMP_NE:
        case CMP_GE:
        case CMP_GT:
        case LOAD_AO:
        case STORE:
        case STORE_AI:
        case PHI:
            ops.add(operands[0]);
            ops.add(operands[1]);
            break;
        case ADD_I:
        case MULT_I:
        case LOAD:
        case LOAD_AI:
        case I2I:
        case CBR:
        case NOT:
        case NEG:
        case PARAM:
        case PRINT:
            if (operands[0].type == ILOCOperand.Type.VIRTUAL_REG ||
                operands[0].type == ILOCOperand.Type.BASE_REG ||
                operands[0].type == ILOCOperand.Type.RETURN_REG)
            {
                ops.add(operands[0]);
            }
            break;
        case LOAD_I:
            if (includeLiterals) {
                ops.add(operands[0]);
            }
            break;
        case LOAD_S:
        case LABEL:
        case JUMP:
        case CALL:
        case RETURN:
        case NOP:
            break;
        }
        return ops;
    }

    /**
     * Returns a list of ILOCOperands written by this instruction. Reports only
     * register or memory operands.
     * @return List of written operands
     */
    public List<ILOCOperand> getWriteOperands()
    {
        List<ILOCOperand> ops = new ArrayList<ILOCOperand>();
        switch (form) {
        case ADD:
        case SUB:
        case MULT:
        case DIV:
        case AND:
        case OR:
        case CMP_LT:
        case CMP_LE:
        case CMP_EQ:
        case CMP_NE:
        case CMP_GE:
        case CMP_GT:
        case ADD_I:
        case MULT_I:
        case LOAD_AI:
        case LOAD_AO:
        case PHI:
            ops.add(operands[2]);
            break;
        case LOAD:
        case LOAD_I:
        case LOAD_S:
        case I2I:
        case NOT:
        case NEG:
            ops.add(operands[1]);
            break;
        case STORE:
        case STORE_AI:
        case STORE_AO:
        case CBR:
        case LABEL:
        case JUMP:
        case PARAM:
        case CALL:
        case RETURN:
        case PRINT:
        case NOP:
            break;
        }
        return ops;
    }

    /**
     * Returns a single ILOCOperands written by this instruction. This
     * should be called only when it is known that the instruction only
     * writes a single operand.
     * @return Single written operand
     */
    public ILOCOperand getWriteOperand()
    {
        List<ILOCOperand> writes = getWriteOperands();
        assert(writes.size() == 1);
        return writes.get(0);
    }

    /**
     * Builds a standardized string representation of an ILOC instruction
     */
    public String toString()
    {
        return toString(true);
    }

    /**
     * Builds a standardized string representation of an ILOC instruction
     * @param showComments Include comments
     * @return String representation of instruction
     */
    public String toString(boolean showComments)
    {
        StringBuilder str = new StringBuilder();
        if (form != Form.LABEL) {
            str.append("  ");
        }
        switch(form) {
        case ADD:
            str.append("add "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case SUB:
            str.append("sub "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case MULT:
            str.append("mult "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case DIV:
            str.append("div "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case ADD_I:
            str.append("addI "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case MULT_I:
            str.append("multI "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case AND:
            str.append("and "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case OR:
            str.append("or "  + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case LOAD_I:
            str.append("loadI "  + operands[0].toString());
            str.append(" => " + operands[1].toString());
            break;
        case LOAD:
            str.append("load [" + operands[0].toString());
            str.append("] => " + operands[1].toString());
            break;
        case LOAD_AI:
            str.append("loadAI [" + operands[0].toString());
            str.append(operands[1].intConstant >= 0 ? "+" : "");
            str.append(operands[1].toString() + "]");
            str.append(" => " + operands[2].toString());
            break;
        case LOAD_AO:
            str.append("loadAO [" + operands[0].toString());
            str.append("+" + operands[1].toString() + "]");
            str.append(" => " + operands[2].toString());
            break;
        case STORE:
            str.append("store " + operands[0].toString());
            str.append(" => [" + operands[1].toString() + "]");
            break;
        case STORE_AI:
            str.append("storeAI " + operands[0].toString());
            str.append(" => [" + operands[1].toString());
            str.append(operands[2].intConstant >= 0 ? "+" : "");
            str.append(operands[2] + "]");
            break;
        case STORE_AO:
            str.append("storeAO " + operands[0].toString());
            str.append(" => [" + operands[1].toString());
            str.append("+" + operands[2].toString() + "]");
            break;
        case I2I:
            str.append("i2i " + operands[0].toString());
            str.append(" => " + operands[1].toString());
            break;
        case JUMP:
            str.append("jump " + operands[0].toString());
            break;
        case CBR:
            str.append("cbr " + operands[0].toString());
            str.append(" => " + operands[1].toString());
            str.append(", " + operands[2]);
            break;
        case CMP_LT:
            str.append("cmp_LT " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case CMP_LE:
            str.append("cmp_LE " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case CMP_EQ:
            str.append("cmp_EQ " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case CMP_GE:
            str.append("cmp_GE " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case CMP_GT:
            str.append("cmp_GT " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case CMP_NE:
            str.append("cmp_NE " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        case LOAD_S:
            str.append("loadS " + operands[0].toString());
            str.append(" => " + operands[1].toString());
            break;
        case NOT:
            str.append("not " + operands[0].toString());
            str.append(" => " + operands[1].toString());
            break;
        case NEG:
            str.append("neg " + operands[0].toString());
            str.append(" => " + operands[1].toString());
            break;
        case LABEL:
            str.append(operands[0].toString() + ":");
            break;
        case PARAM:
            str.append("param " + operands[0].toString());
            break;
        case CALL:
            str.append("call " + operands[0].toString());
            break;
        case RETURN:
            str.append("return");
            break;
        case PRINT:
            str.append("print " + operands[0].toString());
            break;
        case NOP:
            str.append("nop");
            break;
        case PHI:
            str.append("phi " + operands[0].toString());
            str.append(", " + operands[1].toString());
            str.append(" => " + operands[2].toString());
            break;
        }
        if (variableName != null) {
            str.append(" {" + variableName + "}");
        }
        if (showComments && !comment.equals("")) {
            while (str.length() < 40) {
                str.append(' ');
            }
            str.append("// " + comment);
        }
        return str.toString();
    }
}
