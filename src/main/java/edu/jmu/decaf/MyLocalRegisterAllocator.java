package edu.jmu.decaf;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class MyLocalRegisterAllocator extends LocalRegisterAllocator
{

    public MyLocalRegisterAllocator(int numRegisters)
    {
        super(numRegisters);
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
    }
}
