package edu.jmu.decaf;

import java.util.*;

/**
 * Simple ILOC analysis pass that renumbers all the virtual register and
 * anonymous jump label IDs. This allows for easier comparison between
 * implementations.
 */
public class RenumberRegistersAndLabels implements ILOCProcessor
{
    private Map<Integer, Integer> registerMapping;
    private Map<Integer, Integer> labelMapping;

    public RenumberRegistersAndLabels()
    {
        registerMapping = new HashMap<Integer, Integer>();
        labelMapping = new HashMap<Integer, Integer>();
    }

    /**
     * Renumber registers and jump labels
     */
    public void process(ILOCProgram program)
    {
        Integer nextRegID = Integer.valueOf(1);
        Integer nextLblID = Integer.valueOf(1);

        // pass 1: build old => new ID mapping
        for (ILOCFunction func : program.functions) {
            for (ILOCInstruction insn : func.getInstructions()) {
                for (int i=0; i<insn.operands.length; i++) {
                    if (insn.operands[i].type == ILOCOperand.Type.VIRTUAL_REG) {
                        Integer id = Integer.valueOf(insn.operands[i].id);
                        if (!registerMapping.containsKey(id)) {
                            registerMapping.put(id, nextRegID);
                            nextRegID = Integer.valueOf(nextRegID.intValue()+1);
                        }
                    }
                }
                if (insn.form == ILOCInstruction.Form.LABEL) {
                    Integer id = Integer.valueOf(insn.operands[0].id);
                    if (!labelMapping.containsKey(id)) {
                        labelMapping.put(id, nextLblID);
                        nextLblID = Integer.valueOf(nextLblID.intValue()+1);
                    }
                }
            }
        }

        // pass 2: copy instructions and change IDs
        for (ILOCFunction func : program.functions) {
            List<ILOCInstruction> newCode = new ArrayList<ILOCInstruction>();
            for (ILOCInstruction insn : func.getInstructions()) {
                try {
                    ILOCInstruction newInsn = (ILOCInstruction)insn.clone();
                    for (int i=0; i<newInsn.operands.length; i++) {
                        if (newInsn.operands[i].type == ILOCOperand.Type.VIRTUAL_REG) {
                            Integer id = Integer.valueOf(newInsn.operands[i].id);
                            newInsn.operands[i].id = registerMapping.get(id).intValue();
                        } else if (newInsn.operands[i].type == ILOCOperand.Type.JUMP_LABEL) {
                            Integer id = Integer.valueOf(newInsn.operands[i].id);
                            newInsn.operands[i].id = labelMapping.get(id).intValue();
                        }
                    }
                    newCode.add(newInsn);
                } catch (CloneNotSupportedException ex) { }
            }
            func.setInstructions(newCode);    // replace instructions
        }
    }
}
