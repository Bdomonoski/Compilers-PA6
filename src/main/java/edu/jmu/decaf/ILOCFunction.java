package edu.jmu.decaf;

import java.util.*;

/**
 * Decaf function in ILOC intermediate representation (IR). Eases the process of
 * intraprocedural analysis by aggregating all information relevant to a single
 * procedure.
 */
public class ILOCFunction
{
    /**
     * Helper wrapper class for debug variable information (useful for data flow analysis)
     */
    public class Variable
    {
        public ILOCOperand base;
        public ILOCOperand offset;

        public Variable(ILOCOperand base, ILOCOperand offset)
        {
            this.base = base;
            this.offset = offset;
        }

        @Override
        public int hashCode()
        {
            return ("" + base.hashCode() + offset.hashCode()).hashCode();

        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof Variable) {
                Variable oNode = (Variable) o;
                return (this.base.equals(oNode.base) && this.offset.equals(oNode.offset));
            }
            return false;
        }
    }

    /**
     * Function {@link Symbol} from the AST symbol tables
     */
    public Symbol functionSymbol;

    /**
     * Size (in bytes) to allocate on stack for local variables
     */
    public int localSize;

    /**
     * List of ILOC instructions
     */
    public List<ILOCInstruction> instructions;

    /**
     * Entry block (start of CFG)
     */
    public ILOCBasicBlock entryBlock;

    /**
     * It is useful to maintain a list of all blocks for liveness analysis
     */
    public List<ILOCBasicBlock> allBlocks;

    /**
     * Create a new (empty) ILOC function
     *
     * @param symbol {@link Symbol} object from AST
     */
    public ILOCFunction(Symbol symbol)
    {
        this.functionSymbol = symbol;
        this.localSize = 0;
        this.instructions = new ArrayList<ILOCInstruction>();
        this.entryBlock = null;
        this.allBlocks = new ArrayList<ILOCBasicBlock>();
    }

    /**
     * Get the current instruction sequence.
     *
     * @return List of {@link ILOCInstruction} objects
     */
    public List<ILOCInstruction> getInstructions()
    {
        return this.instructions;
    }

    /**
     * Replace all instructions with a new sequence.
     *
     * @param instructions Replacement instructions
     */
    public void setInstructions(List<ILOCInstruction> instructions)
    {
        this.instructions = instructions;
    }

    /**
     * Append a new instruction to the function.
     *
     * @param insn New instruction
     */
    public void addInstruction(ILOCInstruction insn)
    {
        this.instructions.add(insn);
    }

    /**
     * Get the entry point to the function's control flow graph. This will cause
     * the CFG to be built the first time it is executed.
     *
     * @return CFG entry point
     */
    public ILOCBasicBlock getEntryBlock()
    {
        if (this.entryBlock == null) {
            buildCFG();
        }
        return this.entryBlock;
    }

    /**
     * Builds the control flow graph for this function.
     *
     * Original code written by Steven Young, CS 480 honors student.
     */
    public void buildCFG()
    {
        Map<Integer, ILOCBasicBlock> blockByLabelID = new HashMap<Integer, ILOCBasicBlock>();
        ILOCBasicBlock curBlock;

        // create entry point
        curBlock = ILOCBasicBlock.newBasicBlock();
        this.entryBlock = curBlock;
        allBlocks.add(this.entryBlock);

        // PHASE 1 - split instructions into basic blocks

        // for each instruction
        for (int i = 0; i < instructions.size(); i++) {
            ILOCInstruction insn = instructions.get(i);

            // if it's a leader (i.e., a label or it directly follows a branch)
            if (insn.form == ILOCInstruction.Form.LABEL
                    || (i > 0 && instructions.get(i - 1).form == ILOCInstruction.Form.JUMP)
                    || (i > 0 && instructions.get(i - 1).form == ILOCInstruction.Form.CBR)) {

                if (i > 0) {
                    // don't create a new block if this is the first instruction
                    // (b/c it has already been created)
                    curBlock = ILOCBasicBlock.newBasicBlock();
                    allBlocks.add(curBlock);
                }

                // add to label lookup
                blockByLabelID.put(insn.operands[0].id, curBlock);
            }

            // add instruction to most recently-created basic block
            curBlock.instructions.add(insn);
        }

        // PHASE 2 - detect control flow edges between basic blocks

        // for each basic block
        for (int i = 0; i < allBlocks.size(); i++) {
            ILOCBasicBlock sourceBlock = allBlocks.get(i);

            // look for explicit branches out of sourceBlock
            for (ILOCInstruction inst : sourceBlock.instructions) {
                if (inst.form == ILOCInstruction.Form.JUMP) {
                    ILOCBasicBlock targetBlock = blockByLabelID.get(inst.operands[0].id);
                    sourceBlock.targets.add(targetBlock);
                    targetBlock.sources.add(sourceBlock);
                }
                if (inst.form == ILOCInstruction.Form.CBR) {
                    ILOCBasicBlock targetBlock = blockByLabelID.get(inst.operands[1].id);
                    sourceBlock.targets.add(targetBlock);
                    targetBlock.sources.add(sourceBlock);
                    targetBlock = blockByLabelID.get(inst.operands[2].id);
                    sourceBlock.targets.add(targetBlock);
                    targetBlock.sources.add(sourceBlock);
                }
            }

            // look for implicit fall-through edges
            if (sourceBlock.instructions.size() > 0 && i < allBlocks.size() - 1) {
                ILOCInstruction lastInsn = sourceBlock.instructions.get(sourceBlock.instructions.size() - 1);
                ILOCBasicBlock nextBlock = allBlocks.get(i + 1);
                if (lastInsn.form != ILOCInstruction.Form.JUMP && lastInsn.form != ILOCInstruction.Form.CBR) {
                    sourceBlock.targets.add(nextBlock);
                    nextBlock.sources.add(sourceBlock);
                }
            }
        }

    }


    /**
     * Flattens the control-flow graph into linear form (presumably after
     * modifications have been made to the CFG).
     */
    public void flattenCFG()
    {
        Set<ILOCBasicBlock> handled = new HashSet<ILOCBasicBlock>();
        Queue<ILOCBasicBlock> workQueue = new ArrayDeque<ILOCBasicBlock>();

        // create new instruction list
        this.instructions = new ArrayList<ILOCInstruction>();

        // start at entry block
        workQueue.add(getEntryBlock());

        // for each block in the work queue
        while (!workQueue.isEmpty()) {
            ILOCBasicBlock b = workQueue.poll();

            // don't handle any block twice
            if (handled.contains(b)) {
                continue;
            }

            // add all instructions, tracking whether the most recently-added
            // was a branch
            boolean lastWasBranch = false;
            for (ILOCInstruction insn : b.instructions) {
                this.instructions.add(insn);
                if (insn.form == ILOCInstruction.Form.JUMP ||
                    insn.form == ILOCInstruction.Form.CBR) {
                    lastWasBranch = true;
                } else {
                    lastWasBranch = false;
                }
            }

            // if the last instruction was not a branch but there is a target,
            // there must have been a fallthrough edge; the only safe thing to
            // do in this case is to insert an explicit jump to the target
            if (!lastWasBranch && b.targets.size() > 0) {
                assert(b.targets.size() == 1);
                assert(b.targets.get(0).instructions.size() > 0);
                ILOCInstruction target = b.targets.get(0).instructions.get(0);
                assert(target.form == ILOCInstruction.Form.LABEL);
                ILOCOperand ops[] = new ILOCOperand[1];
                ops[0] = ILOCOperand.newAnonymousLabel();
                ops[0].id = target.operands[0].id;
                this.instructions.add(new ILOCInstruction(
                        ILOCInstruction.Form.JUMP, ops, "new jump"));
            }

            // add any unhandled targets to the work queue
            handled.add(b);
            for (ILOCBasicBlock bb : b.targets) {
                if (!handled.contains(bb)) {
                    workQueue.add(bb);
                }
            }
        }
    }

    /**
     * Builds a standard string-based representation of an ILOC function
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append(functionSymbol.name + ":");
        for (ILOCInstruction i : instructions) {
            str.append("\n");
            str.append(i.toString());
        }
        return str.toString();
    }
}
