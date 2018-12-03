package edu.jmu.decaf;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class MyLocalRegisterAllocator extends LocalRegisterAllocator
{
    private static int INVALID_NAME = -1;
    private static int DEFAULT_NEXT = Integer.MAX_VALUE;
  
    private int Size;        /* Number of Physical Registers */
    private int Name[];      /* Vr names corresponding to Pr names. if Name[0] = Vr1 then Vr1 is in Pr0 */
    private int Next[];      /* Distance to next use */
    private boolean Free[];  /* Is physical register currently in use? */
    private int Stack[];     /* Free Register Stack */
    private int StackTop;

    public MyLocalRegisterAllocator(int numRegisters)
    {
        super(numRegisters);
        Size = numRegisters;
        
        Name = new int[Size];
        for (int i = 0; i < Size; i++)
          Name[i] = INVALID_NAME;     // all names initialized to -1.
        
        Next = new int[Size];
        for (int i = 0; i < Size; i++)
          Next[i] = DEFAULT_NEXT;     // all next use initialized to Integer.MAX_VALUE.
        
        Free = new boolean[Size];
        for (int i = 0; i < Size; i++)
          Free[i] = true;             // all physical registers begin free.
        
        Stack = new int[Size];
        for (int i = 0; i < Size; i++)
          Stack[i] = i;               // add all indexes to stack in order (all are free).
                                      // {0, 1, 2, 3} = {pr0, pr1, pr2, pr3} free.
        StackTop = 0;                 // pr0 at top of stack.
    }

    /**
     * Perform local register allocation for a single basic block.
     * This should be overridden in actual implementations.
     *
     * @param block     Block to allocate
     */
    @Override
    protected void allocateRegisters(ILOCBasicBlock block)
    {
      System.out.println("Block: " + block.id);
      
      for (ILOCInstruction i : block.instructions)
      {
        System.out.println(i.toString());
        for (ILOCOperand op : i.operands)
        {
          //System.out.println("Operand ID: " + op.id);
          //System.out.println("Operand TYPE: " + op.type);
          
        }
        
      }
      System.out.println();
    }
    
    protected int Ensure(ILOCOperand virtualRegister)
    {
      boolean alreadyInRegister = false;
      int physicalRegister = INVALID_NAME;
      
      for (int i = 0; i < Size; i++)
      {
        if (Name[i] == virtualRegister.id)
        {
          alreadyInRegister = true;
          physicalRegister = i;
        }
      }
      
      if (!alreadyInRegister)
      {
        // allocate
      }
      
      return -1;
    }
    
    
}
