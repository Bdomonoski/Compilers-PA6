package edu.jmu.decaf;

import java.util.*;

/**
 * AST post-visitor; converts a Decaf program in AST IR to ILOC IR by walking
 * the tree and recursively emitting sequential IR.
 * 
 * Dependencies: {@link BuildSymbolTables} and {@link AllocateSymbols}
 */
public abstract class ILOCGenerator extends DefaultASTVisitor
{
    private ILOCProgram program;

    public ILOCGenerator()
    {
        program = new ILOCProgram();
    }

    /**
     * Returns the generated ILOC program.
     * @return Generated ILOC program
     */
    public ILOCProgram getProgram()
    {
        return program;
    }

    /**
     * Finalize ILOC program. Copy info from AST regarding static data region,
     * and create {@link ILOCFunction} objects with info from the AST, including
     * the sizes of the function stack frames and the generated instructions.
     */
    @Override
    public void postVisit(ASTProgram node)
    {
        SymbolTable table = (SymbolTable)node.attributes.get("symbolTable");
        for (Symbol s : table.getSymbols()) {
            try {
                program.staticSymbols.insert(s.name, s);
            } catch (InvalidProgramException ex) {
                assert(false);  // if this happens, symbols are somehow
                                // getting added after the original symbol
                                // generation pass and before the code
                                // generation pass
            }
        }
        program.staticSize = ((Integer)node.attributes.get("staticSize")).intValue();

        for (ASTFunction func : node.functions)
        {
            ILOCFunction ilocFunc = new ILOCFunction(DecafAnalysis.lookupSymbol(node, func.name));
            ilocFunc.localSize = ((Integer)func.attributes.get("localSize")).intValue();
            program.functions.add(ilocFunc);
            for (ILOCInstruction insn : getCode(func)) {
                ilocFunc.getInstructions().add(insn);
            }
        }
    }

    /**
     * Returns the ILOC code associated with an AST node (via the "code"
     * attribute).
     * @param node AST node
     * @return List of ILOC instructions (by reference)
     */
    protected List<ILOCInstruction> getCode(ASTNode node)
    {
        if (node.attributes.containsKey("code")) {
            return (ArrayList)(node.attributes.get("code"));
        } else {
            List<ILOCInstruction> code = new ArrayList<ILOCInstruction>();
            node.attributes.put("code", code);
            return code;
        }
    }

    /**
     * Generate a new ILOC instruction and add it to the code for an AST node.
     * General form.
     * @param node Destination AST node
     * @param insn ILOC instruction to add
     */
    protected void emit(ASTNode node, ILOCInstruction insn)
    {
        if (!node.attributes.containsKey("code")) {
            node.attributes.put("code", new ArrayList<ILOCInstruction>());
        }
        getCode(node).add(insn);
    }

    /**
     * Generate a new ILOC instruction and add it to the code for an AST node.
     * Zero-operand shortcut form.
     * @param node Destination AST node
     * @param form ILOC instruction form (icode) to add
     */
    protected void emit(ASTNode node, ILOCInstruction.Form form)
    {
        ILOCOperand[] operands = new ILOCOperand[0];
        emit(node, new ILOCInstruction(form, operands));
    }

    /**
     * Generate a new ILOC instruction and add it to the code for an AST node.
     * One-operand shortcut form.
     * @param node Destination AST node
     * @param form ILOC instruction form (icode) to add
     * @param op ILOC instruction operand
     */
    protected void emit(ASTNode node, ILOCInstruction.Form form, ILOCOperand op)
    {
        ILOCOperand[] operands = new ILOCOperand[1];
        operands[0] = op;
        emit(node, new ILOCInstruction(form, operands));
    }

    /**
     * Generate a new ILOC instruction and add it to the code for an AST node.
     * Two-operand shortcut form.
     * @param node Destination AST node
     * @param form ILOC instruction form (icode) to add
     * @param op1 First ILOC instruction operand
     * @param op2 Second ILOC instruction operand
     */
    protected void emit(ASTNode node, ILOCInstruction.Form form,
            ILOCOperand op1, ILOCOperand op2)
    {
        ILOCOperand[] operands = new ILOCOperand[2];
        operands[0] = op1;
        operands[1] = op2;
        emit(node, new ILOCInstruction(form, operands));
    }

    /**
     * Generate a new ILOC instruction and add it to the code for an AST node.
     * Three-operand shortcut form.
     * @param node Destination AST node
     * @param form ILOC instruction form (icode) to add
     * @param op1 First ILOC instruction operand
     * @param op2 Second ILOC instruction operand
     * @param op3 Third ILOC instruction operand
     */
    protected void emit(ASTNode node, ILOCInstruction.Form form,
            ILOCOperand op1, ILOCOperand op2, ILOCOperand op3)
    {
        ILOCOperand[] operands = new ILOCOperand[3];
        operands[0] = op1;
        operands[1] = op2;
        operands[2] = op3;
        emit(node, new ILOCInstruction(form, operands));
    }

    /**
     * Copy the ILOC code from one AST node into another. Appends the code from
     * the source to the end of the code in the destination.
     * @param dest Destination AST node
     * @param src Source AST node
     */
    protected void copyCode(ASTNode dest, ASTNode src)
    {
        for (ILOCInstruction insn : getCode(src)) {
            getCode(dest).add(insn);
        }
    }

    /**
     * Sets the temporary ILOC virtual register associated with the result of
     * evaluating a node in the AST.
     * @param node AST node
     * @param reg ILOC virtual temporary register
     */
    protected void setTempReg(ASTNode node, ILOCOperand reg)
    {
        node.attributes.put("reg", reg);
    }

    /**
     * Returns the temporary ILOC virtual register associated with the result of
     * evaluating a node in the AST.
     * @param node AST node
     * @return ILOC virtual temporary register
     */
    protected ILOCOperand getTempReg(ASTNode node)
    {
        assert(node.attributes.containsKey("reg"));
        return (ILOCOperand)node.attributes.get("reg");
    }

    /**
     * Fills a register with the base address of a variable.
     * @param dest Destination AST Node
     * @param variable Desired variable
     * @return Virtual register that contains the base address
     */
    public ILOCOperand base(ASTNode dest, Symbol variable)
    {
        ILOCOperand baseReg = ILOCOperand.INVALID;
        switch (variable.location) {
        case STATIC_VAR:
            // constant-based access
            baseReg = ILOCOperand.newVirtualReg();
            emit(dest, ILOCInstruction.Form.LOAD_I,
                    ILOCOperand.newIntConstant(variable.offset), baseReg);
            break;

        case STACK_PARAM:
        case STACK_LOCAL:
            // BP-based access
            baseReg = ILOCOperand.REG_BP;
            break;
        default:
            assert(false);  // no need to handle STATIC_FUNC or UNKNOWN symbols
        }
        return baseReg;
    }

    /**
     * Calculates the offset of a scalar variable reference and returns the
     * adjusted offset in an {@link ILOCOperand}.
     * @param variable Desired variable
     * @return Integer constant operand
     */
    public ILOCOperand offset(Symbol variable)
    {
        ILOCOperand offsetReg = ILOCOperand.INVALID;
        switch (variable.location) {
        case STATIC_VAR:
            offsetReg = ILOCOperand.ZERO;
            break;
        case STACK_PARAM:
            // BP-based access (positive, skip base pointer and return address)
            offsetReg = ILOCOperand.newIntConstant(variable.offset +
                    (Symbol.WORD_SIZE * 2));
            break;
        case STACK_LOCAL:
            // BP-based access (negative)
            offsetReg = ILOCOperand.newIntConstant(-variable.offset -
                    variable.totalSize);
            break;
        default:
            assert(false);  // no need to handle STATIC_FUNC or UNKNOWN symbols
        }
        return offsetReg;
    }

    /**
     * Calculates the offset of an indexed variable reference and returns the
     * adjusted offset in an {@link ILOCOperand}.
     * @param dest Destination AST Node
     * @param variable Desired variable
     * @param indexReg Register with the evaluated index
     * @return Virtual register reference containing indexed location offset
     */
    public ILOCOperand indexedOffset(ASTNode dest, Symbol variable, ILOCOperand indexReg)
    {
        ILOCOperand sizeConst = ILOCOperand.newIntConstant(variable.elementSize);
        ILOCOperand scaledIndexReg = ILOCOperand.newVirtualReg();
        emit(dest, ILOCInstruction.Form.MULT_I, indexReg, sizeConst, scaledIndexReg);
        return scaledIndexReg;
    }

    /**
     * Emits a right-hand-side load for a given location.
     * @param dest Destination AST node
     * @return Virtual register reference containing the loaded value
     */
    public ILOCOperand emitLoad(ASTLocation dest)
    {
        ILOCOperand destReg = ILOCOperand.newVirtualReg();
        ILOCInstruction.Form icode = ILOCInstruction.Form.LOAD_AI;
        Symbol variable = DecafAnalysis.lookupSymbol(dest, dest.name);
        ILOCOperand ops[] = { base(dest, variable), offset(variable), destReg };
        if (dest.hasIndex()) {
            copyCode(dest, dest.index);
            icode = ILOCInstruction.Form.LOAD_AO;
            ops[1] = indexedOffset(dest, variable, getTempReg(dest.index));
        }
        ILOCInstruction newIns = new ILOCInstruction(icode, ops);
        newIns.variableName = variable.name;
        emit(dest, newIns);
        return destReg;
    }

    /**
     * Emits a store for a given assignment.
     * @param dest Destination AST node
     * @param srcReg Source register
     */
    public void emitStore(ASTAssignment dest, ILOCOperand srcReg)
    {
        Symbol variable = DecafAnalysis.lookupSymbol(dest, dest.location.name);
        ILOCInstruction.Form icode = ILOCInstruction.Form.STORE_AI;
        ILOCOperand ops[] = { srcReg, base(dest, variable), offset(variable) };
        if (dest.location.hasIndex()) {
            copyCode(dest, dest.location.index);
            icode = ILOCInstruction.Form.STORE_AO;
            ops[2] = indexedOffset(dest, variable, getTempReg(dest.location.index));
        }
        ILOCInstruction newIns = new ILOCInstruction(icode, ops);
        newIns.variableName = variable.name;
        emit(dest, newIns);
    }

    /**
     * Add a comment to the most recently-emitted ILOC instruction.
     * @param node AST node to which the the comment should be added
     * @param text Comment text
     */
    protected void addComment(ASTNode node, String text)
    {
        assert(node.attributes.containsKey("code"));
        List<ILOCInstruction> code = getCode(node);
        code.get(code.size()-1).comment = text;
    }
}
