package edu.jmu.decaf;

import java.util.*;

/**
 * Performs register allocation for an ILOC program.
 */
public abstract class LocalRegisterAllocator implements ILOCProcessor
{
    /**
     * Number of physical registers available for allocation
     */
    protected int numPhysicalRegs;

    /**
     * Function that is currently being allocated
     */
    protected ILOCFunction currentFunction;

    /**
     * Basic block that is currently being allocated
     */
    protected ILOCBasicBlock currentBlock;

    /**
     * Collection of basic blocks that have already been allocated
     */
    protected Set<ILOCBasicBlock> handled;

    /**
     * Initialize register allocator
     *
     * @param numRegisters  Number of physical registers
     */
    public LocalRegisterAllocator(int numRegisters)
    {
        this.numPhysicalRegs = numRegisters;
        this.currentFunction = null;
        this.currentBlock = null;
        this.handled = new HashSet<ILOCBasicBlock>();
    }

    /**
     * Perform register allocation on each function.
     */
    public void process(ILOCProgram program)
    {
        for (ILOCFunction f : program.functions) {

            // build CFG
            ILOCBasicBlock entry = f.getEntryBlock();

            // register allocation
            currentFunction = f;
            process(entry);
            currentFunction.flattenCFG();
        }
    }

    /**
     * Perform register allocation for a basic block and all basic blocks
     * reachable from it.
     *
     * @param block     Entry block
     */
    protected void process(ILOCBasicBlock block)
    {
        currentBlock = block;
        allocateRegisters(block);

        // handle all target blocks
        handled.add(block);
        for (ILOCBasicBlock bb : block.targets) {
            if (!handled.contains(bb)) {
                process(bb);
            }
        }
    }

    /**
     * Perform local register allocation for a single basic block.
     * This should be overridden in actual implementations.
     *
     * @param block     Block to allocate
     */
    protected void allocateRegisters(ILOCBasicBlock block)
    {
    }

    /**
     * Emit a new instruction at the given index. The instruction will load the
     * value from BP-offset into the desired register.
     *
     * @param idx       Block index at which to insert the instruction
     * @param offset    Spill value offset
     * @param reg       Destination register
     * @param comment   Comment for new instruction (can be blank)
     */
    protected void emitSpillLoad(int idx, int offset, int reg, String comment)
    {
        ILOCOperand ops[] = new ILOCOperand[3];
        ops[0] = ILOCOperand.REG_BP;
        ops[1] = ILOCOperand.newIntConstant(-offset);
        ops[2] = ILOCOperand.newVirtualReg();
        ops[2].id = reg;
        currentBlock.instructions.add(idx, new ILOCInstruction(
                ILOCInstruction.Form.LOAD_AI, ops, comment));
    }

    /**
     * Emit a new instruction at the given index. The instruction will store the
     * value from the given register to a new spot on the stack (BP-offset).
     * Essentially, it adds a new "local" variable to the current stack frame.
     *
     * @param idx       Block index at which to insert the instruction
     * @param reg       Source register
     * @param comment   Comment for new instruction (can be blank)
     * @return          Destination offset from base pointer
     */
    protected int emitSpillStore(int idx, int reg, String comment)
    {
        currentFunction.localSize += Symbol.WORD_SIZE;
        ILOCOperand ops[] = new ILOCOperand[3];
        ops[0] = ILOCOperand.newVirtualReg();
        ops[0].id = reg;
        ops[1] = ILOCOperand.REG_BP;
        ops[2] = ILOCOperand.newIntConstant(-currentFunction.localSize);
        currentBlock.instructions.add(idx, new ILOCInstruction(
                ILOCInstruction.Form.STORE_AI, ops, comment));
        return currentFunction.localSize;
    }
}
