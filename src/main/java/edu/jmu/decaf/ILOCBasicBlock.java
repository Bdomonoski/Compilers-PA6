package edu.jmu.decaf;

import java.util.*;

public class ILOCBasicBlock
{
    public static int nextID = 1;
    public int id;

    public List<ILOCBasicBlock> sources;
    public List<ILOCBasicBlock> targets;
    
    public List<ILOCInstruction> instructions;

    public static ILOCBasicBlock newBasicBlock()
    {
        return new ILOCBasicBlock(nextID++);
    }
    
    private ILOCBasicBlock(int id)
    {
        this.id = id;
        this.sources = new ArrayList<ILOCBasicBlock>();
        this.targets = new ArrayList<ILOCBasicBlock>();
        this.instructions = new ArrayList<ILOCInstruction>();
    }
    
    public void debugPrint()
    {
        System.out.println("BLOCK ID=" + id);
        for (ILOCBasicBlock src : sources) {
            System.out.println("  SRC: " + src.id);
        }
        for (ILOCInstruction i : instructions) {
            System.out.println(i.toString());
        }
        for (ILOCBasicBlock trg : targets) {
            System.out.println("  TRG: " + trg.id);
        }
    }

}
