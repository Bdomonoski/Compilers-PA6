package edu.jmu.decaf;

import java.util.*;

/**
 * Simulates a simple architecture that can directly execute ILOC programs.
 *
 * Features:
 * <ul>
 * <li>32-bit word</li>
 * <li>Unlimited 32-bit integer virtual registers</li>
 * <li>Four special-purpose 32-bit integer registers:<ul>
 *   <li>IP - instruction pointer</li>
 *   <li>SP - stack pointer</li>
 *   <li>BP - base pointer</li>
 *   <li>RET - function return value</li>
 *   </ul></li>
 * <li>64KB stack, heap, and data region</li>
 * <li>Fixed-size, read-only code region indexed by instruction</li>
 * </ul>
 *
 * Memory layout diagram:
 *
 * <pre>
 * ---------------  2^16-1
 * |    Stack    |
 * |      |      |
 * |      v      |
 * |             |
 * |             |
 * \/\/\/\/\/\/\/\
 * |             |
 * |      ^      |
 * |      |      |
 * |     Heap    |
 * ---------------
 * |     Data    |
 * ---------------  0
 *
 * ---------------
 * |     Code    |
 * ---------------
 * </pre>
 *
 * The IP register does not contain a valid memory address, but rather it
 * points to the index of the next {@link ILOCInstruction} in the
 * currently-executing {@link ILOCFunction}.
 *
 * The heap region is currently unused.
 */
public class ILOCInterpreter implements ILOCProcessor
{
    private Map<Integer, Integer> virtualRegs;
    private int ip, sp, bp, ret;
    private byte memory[];
    private static final int MEM_SIZE = 65536;

    private ILOCProgram currentProgram;
    private List<ILOCInstruction> allCode;
    private Map<Integer, Integer> jumpTargets;
    private Map<String, Integer> callTargets;
    private int returnValue;
    private boolean trace;

    public ILOCInterpreter()
    {
        this(false);
    }

    public ILOCInterpreter(boolean trace)
    {
        this.virtualRegs = new TreeMap<Integer, Integer>();
        this.memory = new byte[MEM_SIZE];
        this.currentProgram = null;
        this.allCode = new ArrayList<ILOCInstruction>();
        this.jumpTargets = new HashMap<Integer, Integer>();
        this.callTargets = new HashMap<String, Integer>();
        this.returnValue = -1;
        this.trace = trace;
    }

    public void process(ILOCProgram program)
    {
        // initialize system state
        //   - set registers ip, sp, bp, ret
        //   - collapse code into a single instruction list
        //   - build call and jump target maps
        currentProgram = program;
        ip = -1; sp = MEM_SIZE; bp = sp; ret = -1;
        for (ILOCFunction func : program.functions) {
            String name = func.functionSymbol.name;
            ILOCOperand[] ops = new ILOCOperand[4];
            ops[0] = ILOCOperand.newCallLabel(name);
            callTargets.put(name, Integer.valueOf(allCode.size()));
            allCode.add(new ILOCInstruction(ILOCInstruction.Form.LABEL, ops));
            for (ILOCInstruction insn: func.getInstructions()) {
                if (insn.form == ILOCInstruction.Form.LABEL) {
                    Integer id = Integer.valueOf(insn.operands[0].id);
                    jumpTargets.put(id, Integer.valueOf(allCode.size()));
                }
                allCode.add(insn);
            }
        }
        if (trace) {
            System.out.println();
            if (trace) System.out.println("All code:");
            for (int i=0; i<allCode.size(); i++) {
                System.out.print(String.format("%4d", i) + ": ");
                System.out.println(allCode.get(i).toString());
            }
            System.out.println();
        }

        if (trace) dumpSystemState();

        // goto program entry point
        if (trace) System.out.println("Calling main()\n");
        callFunction("main");

        // debug info
        if (trace) dumpSystemState();

        boolean done = false;
        while (!done) {

            // fetch & execute
            ILOCInstruction insn = allCode.get(Integer.valueOf(ip));
            if (trace) System.out.println("Executing: " + insn.toString());

            // increment instruction pointer
            ip++;

            // execute
            done = handle(insn);

            // debug info
            if (trace) dumpSystemState();
        }

        // set return value
        returnValue = ret;
    }

    public boolean handle(ILOCInstruction insn)
    {
        switch (insn.form) {

        case LOAD_I:
        case LOAD_S:
        case I2I:
            setInt(insn.operands[1], getInt(insn.operands[0]));
            break;

        case LOAD:
            setInt(insn.operands[1], loadInt(getInt(insn.operands[0])));
            break;
        case LOAD_AI:
        case LOAD_AO:
            setInt(insn.operands[2],
                   loadInt(getInt(insn.operands[0]) + getInt(insn.operands[1])));
            break;

        case STORE:
            storeInt(getInt(insn.operands[1]), getInt(insn.operands[0]));
            break;

        case STORE_AI:
        case STORE_AO:
            storeInt(getInt(insn.operands[1]) + getInt(insn.operands[2]),
                     getInt(insn.operands[0]));
            break;

        case ADD:
        case ADD_I:
            setInt(insn.operands[2],
                    getInt(insn.operands[0]) + getInt(insn.operands[1]));
            break;
        case SUB:
            setInt(insn.operands[2],
                    getInt(insn.operands[0]) - getInt(insn.operands[1]));
            break;
        case MULT:
        case MULT_I:
            setInt(insn.operands[2],
                    getInt(insn.operands[0]) * getInt(insn.operands[1]));
            break;
        case DIV:
            setInt(insn.operands[2],
                    getInt(insn.operands[0]) / getInt(insn.operands[1]));
            break;

        case AND:
            setBool(insn.operands[2],
                    getBool(insn.operands[0]) && getBool(insn.operands[1]));
            break;
        case OR:
            setBool(insn.operands[2],
                    getBool(insn.operands[0]) || getBool(insn.operands[1]));
            break;

        case NOT:
            setBool(insn.operands[1], !getBool(insn.operands[0]));
            break;
        case NEG:
            setInt(insn.operands[1], -getInt(insn.operands[0]));
            break;

        case JUMP:
            ip = findJumpTarget(insn.operands[0]) + 1;
            break;
        case CBR:
            if (getBool(insn.operands[0])) {
                ip = findJumpTarget(insn.operands[1]) + 1;
            } else {
                ip = findJumpTarget(insn.operands[2]) + 1;
            }
            break;

        case CMP_LT:
            setBool(insn.operands[2],
                    getInt(insn.operands[0]) < getInt(insn.operands[1]));
            break;
        case CMP_LE:
            setBool(insn.operands[2],
                    getInt(insn.operands[0]) <= getInt(insn.operands[1]));
            break;
        case CMP_EQ:
            setBool(insn.operands[2],
                    getInt(insn.operands[0]) == getInt(insn.operands[1]));
            break;
        case CMP_GE:
            setBool(insn.operands[2],
                    getInt(insn.operands[0]) >= getInt(insn.operands[1]));
            break;
        case CMP_GT:
            setBool(insn.operands[2],
                    getInt(insn.operands[0]) > getInt(insn.operands[1]));
            break;
        case CMP_NE:
            setBool(insn.operands[2],
                    getInt(insn.operands[0]) != getInt(insn.operands[1]));
            break;

        case PARAM:
            push(getInt(insn.operands[0]));
            break;

        case CALL:
            callFunction(insn.operands[0].strConstant);
            break;

        case RETURN:
            leaveFunction();
            ip = pop();
            if (ip == -1) {
                return true;
            }
            // TODO: pop parameters
            break;

        case PRINT:
            switch (insn.operands[0].type) {
            case VIRTUAL_REG:
                System.out.print(getInt(insn.operands[0]));
                break;
            case STR_CONST:
                System.out.print(insn.operands[0].strConstant);
                break;
            default:
                System.out.println("WARNING: Invalid PRINT operand: " + insn.toString());
            }

        case LABEL:
        case NOP:
            break;

        default:
            System.out.println("WARNING: Unhandled instruction: " + insn.toString());
            break;
        }
        return false;
    }

    public int getInt(ILOCOperand src)
    {
        int rval = -1;
        switch (src.type) {
        case BASE_REG:      rval = bp; break;
        case RETURN_REG:    rval = ret; break;
        case VIRTUAL_REG:   Integer id = Integer.valueOf(src.id);
                            assert(virtualRegs.containsKey(id));
                            rval = virtualRegs.get(id).intValue();
                            break;
        case INT_CONST:     rval = src.intConstant; break;
        default:            assert(false);      // invalid operand
        }
        return rval;
    }

    public void setInt(ILOCOperand dst, int value)
    {
        switch(dst.type) {
        case BASE_REG:      bp = value; break;
        case RETURN_REG:    ret = value; break;
        case VIRTUAL_REG:   Integer id = Integer.valueOf(dst.id);
                            virtualRegs.put(id, Integer.valueOf(value));
                            break;
        default:            assert(false);      // invalid operand
        }
    }

    public boolean getBool(ILOCOperand src)
    {
        return getInt(src) != 0;
    }

    public void setBool(ILOCOperand dst, int value)
    {
        setBool(dst, value != 0);
    }

    public void setBool(ILOCOperand dst, boolean value)
    {
        setInt(dst, value ? 1 : 0);
    }

    public int findJumpTarget(ILOCOperand targ)
    {
        assert(targ.type == ILOCOperand.Type.JUMP_LABEL);
        Integer id = Integer.valueOf(targ.id);
        assert(jumpTargets.containsKey(id));
        return jumpTargets.get(id).intValue();
    }

    public void storeInt(int address, int value)
    {
        int offset = (Symbol.WORD_SIZE-1) * 8;
        for (int i = 0; i < Symbol.WORD_SIZE; i++) {
            memory[address+i] = (byte)(value >> offset);
            offset -= 8;
        }
    }

    public int loadInt(int address)
    {
        int offset = (Symbol.WORD_SIZE-1) * 8;
        int value = 0;
        for (int i = 0; i < Symbol.WORD_SIZE; i++) {
            value |= (memory[address+i] & 0xFF) << offset;
            offset -= 8;
        }
        return value;
    }

    public void push(int value)
    {
        sp -= Symbol.WORD_SIZE;
        checkStack();
        storeInt(sp, value);
    }

    public int pop()
    {
        int value = loadInt(sp);
        sp += Symbol.WORD_SIZE;
        return value;
    }

    public void callFunction(String name)
    {
        assert callTargets.containsKey(name);
        ILOCFunction func = currentProgram.getFunction(name);

        // save return address
        push(ip);

        // enter (prologue)
        push(bp);                           // push ebp
        bp = sp;                            // mov esp, ebp
        sp -= func.localSize;               // sub <size>, esp
        checkStack();

        // transfer control flow
        ip = callTargets.get(name).intValue() + 1;
    }

    public void leaveFunction()
    {
        // leave (epilogue)
        sp = bp;                            // mov ebp, esp
        bp = pop();                         // pop ebp
    }

    public int getReturnValue()
    {
        return returnValue;
    }

    public void checkStack()
    {
        assert (sp >= 0);
        //assert (sp > currentProgram.totalStaticSize);
    }

    public void dumpSystemState()
    {
        // global static variables
        for (Symbol sym : currentProgram.staticSymbols.getSymbols()) {
            if (sym.location == Symbol.MemLoc.STATIC_FUNC) {
                continue;
            }
            System.out.print("  global " + sym.name + " = ");
            if (sym.length > 1) {
                boolean comma = false;
                System.out.print("[");
                for (int i=0; i<sym.length; i++) {
                    if (comma) {
                        System.out.print(",");
                    }
                    System.out.print(loadInt(sym.offset + i*sym.elementSize));
                    comma = true;
                }
                System.out.print("]");
            } else {
                System.out.print(loadInt(sym.offset));
            }
            System.out.println();
        }

        // registers
        System.out.println("  ip=" + ip + " sp=" + sp + " bp=" + bp +
                " ret=" + ret);
        System.out.print("  virtualRegs: { ");
        boolean comma = false;
        for (Map.Entry<Integer, Integer> pair : virtualRegs.entrySet()) {
            if (comma) {
                System.out.print(", ");
            } else {
                comma = true;
            }
            System.out.print("r" + pair.getKey() + "=" + pair.getValue());
        }
        System.out.println(" }");

        // stack
        System.out.println("  stack:");
        for (int i = MEM_SIZE - Symbol.WORD_SIZE; i >= sp; i -= Symbol.WORD_SIZE) {
            if (i < MEM_SIZE) {
                System.out.println("    " + i + ": " + loadInt(i));
            }
        }
        System.out.println();
    }
}
