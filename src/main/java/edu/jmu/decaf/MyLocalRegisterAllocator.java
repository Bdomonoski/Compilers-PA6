package edu.jmu.decaf;

import java.util.HashMap;
import java.util.Map;

public class MyLocalRegisterAllocator extends LocalRegisterAllocator
{
    private static int INVALID_REG, IN_USE = -1;
    private static int DEFAULT_NEXT = Integer.MAX_VALUE;
  
    private int Size;        /* Number of Physical Registers */
    private int Name[];      /* Vr names corresponding to Pr names. if Name[0] = Vr1 then Vr1 is in Pr0 */
    private int Next[];      /* Distance to next use */
    private boolean Free[];  /* Is physical register currently in use? */
    private int StackTop;
    private HashMap<Integer, Integer> virtualNext;

    public MyLocalRegisterAllocator(int numRegisters)
    {
        super(numRegisters);
        Size = numRegisters;
        
        Name = new int[Size];
        for (int i = 0; i < Size; i++)
          Name[i] = INVALID_REG;         // all "name" initialized to -1.
        
        Next = new int[Size];
        for (int i = 0; i < Size; i++)
          Next[i] = DEFAULT_NEXT;        // all "next use" initialized to Integer.MAX_VALUE.
        
        Free = new boolean[Size];
        for (int i = 0; i < Size; i++)
          Free[i] = true;                // all physical registers begin free.
        
                                         // {0, 1, 2, 3} = {pr0, pr1, pr2, pr3} free.
        StackTop = 0;                    // pr0 at top of stack.
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
      virtualNext = null;
      virtualNext = new HashMap<Integer, Integer>();
      
      getNextUse(block);      // Calculate Next[] values for all virtual register names.
      
      System.out.println(virtualNext.toString());
      
      
      System.out.println("Block " + block.id);
      int index = 0;
      for (ILOCInstruction i : block.instructions)  // for each instruction i in block:
      {
        System.out.println("\t" +i.toString());
        for (ILOCOperand op : i.operands)
        {
          
          /** 1. Make sure Operands are in registers **/
          if (op.type == ILOCOperand.Type.VIRTUAL_REG)  // for each vr in i:
          {
            int physicalReg = Ensure(op.id, index);      // pr = Ensure(vr)
            op.id = physicalReg;                         // replace vr with pr in i
            Next[physicalReg] = virtualNext.get(op.id);  // set Next.
            
          }
        }
        index++;
      }
    }
    
    /**
     * Insert's virtual register value into physical register, and
     * returns the physical register.
     * 
     * @param int register to be assigned to physical register.
     * @param index current Block Index (current line in ILOCBasicBlock).
     * @return corresponding Physical Register
     */
    protected int Ensure(int virtualRegister, int index)
    {
      boolean alreadyInRegister = false;
      int physicalRegister = INVALID_REG;
      
      for (int i = 0; i < Size; i++)
      {
        if (Name[i] == virtualRegister)        // Check if virtualRegister already in a physical register.
        {
          alreadyInRegister = true;
          physicalRegister = i;
        }
      }
      
      if (!alreadyInRegister)                     
      {
        physicalRegister = Allocate(virtualRegister, index);
        // emit LOAD code here.
      }
      
      return -1;
    }
    
    
    protected int Allocate(int virtualReg, int index)
    {
      int physicalReg;
      
      if (StackTop != INVALID_REG)    // Case 1: A physical register is free.
      {
        physicalReg = popStack();
      }
      else                            // Case 2: NO physical register's free.
      {
        physicalReg = getRegLongestNext();
        emitSpillStore(physicalReg, index, "");
      }
      
      return -1;
    }
    
    
    /**
     * Returns the physical register with the "Next Use" farthest in the future.
     *   This should NEVER be called if all physical registers aren't occupied,
     *   so Next[i] SHOULD NEVER be Integer.MAX_VALUE.
     * 
     * @return int register ID of register with greatest Next[] value.
     */
    protected int getRegLongestNext()
    {
      int physicalReg = INVALID_REG;
      int currLong = -1;
      
      for (int i = 0; i < Size; i++)
      {
        if (Next[i] > currLong)
        {
          currLong = Next[i];
          physicalReg = i;
        }
      }
      
      return physicalReg;
    }
    
    
    /**
     * Returns the next free register, or INVALID_REG if no physical registers are free.
     * @return int register ID
     */
    protected int popStack()
    {
      int physicalReg = INVALID_REG;
      
      if (StackTop != INVALID_REG)
      {
        physicalReg = StackTop;
        Free[StackTop] = false;
      }
      updateStack();
      return physicalReg;
    }
    
    /**
     * Pushes this register onto the free stack 
     *    and reinitialize's the register's attributes.
     *    
     * @param phsyicalReg Physical Register to be added to the Free "Stack"
     */
    protected void pushStack(int physicalReg)
    {
      assert(physicalReg > INVALID_REG && physicalReg < Size);
      
      /* Re-initialize the physical register */
      Free[physicalReg] = true;                // Mark Register as free.
      Name[physicalReg] = INVALID_REG;         // No VR name now corresponds to this.
      Next[physicalReg] = DEFAULT_NEXT;        // No VR value so no next use.
      
      if (physicalReg < StackTop)              // Stack is NOT first-in first-out. if StackTop is currently 3,
        updateStack();                         // and register 0 is pushed to the stack, StackTop should become 0.
    }
    
    
    /**
     * Updates the StackTop variable to be the next free register
     *  in the Stack, or EMPTY_STACK if no physicalRegisters are free.
     *  
     *                 *** SUCCESSFULLY TESTED ***
     */
    protected void updateStack()
    {
      int newTop = INVALID_REG;
      
      for (int i = 0; i < Size; i++)
      {
        if (Free[i]) // Register is free.
        {
          newTop = i;
          break;
        }
      }
      StackTop = newTop;
    }
    
    /**
     * Scans the block for virtual registers. if it's the first time seeing
     *  the virtual register name, it adds it to the virtualNext hashmap, with its
     *  index. When it's found again, it saves the creation index, removes the hashmap
     *  reference, and adds the same virtual register to the map with the
     *    current index - creation index = distance to next use at creation of VR.
     *    
     * @param block The ILOCBasicBlock to scan for virtual registers.
     */
    protected void getNextUse(ILOCBasicBlock block)
    {
      int index = 0;
      for (ILOCInstruction i : block.instructions)
      {
        for (ILOCOperand op : i.operands)
        {
          
          if (op.type == ILOCOperand.Type.VIRTUAL_REG)
          {
            if (!virtualNext.containsKey(op.id))
              virtualNext.put(op.id, index);
            else
            {
              int created = virtualNext.get(op.id);
              virtualNext.remove(op.id);
              virtualNext.put(op.id, index - created); // current index - creation index = index til next use at creation.
            }
          }
        }
        index++;
      }
    }
}
