package edu.jmu.decaf;

import java.io.*;
import java.time.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test cases for register allocation
 */
public class TestRegAlloc extends TestCase
{
    /**
     * Number of physical registers (currently uniform across all tests)
     */
    public static final int NUM_PHYS_REGS = 4;

    /**
     * Initialization
     *
     * @param testName name of the test case
     */
    public TestRegAlloc(String testName)
    {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(TestRegAlloc.class);
    }

    /**
     * Helper class: runs a single test in a thread so that it can be
     * interrupted if it times out.
     */
    private static class TimeoutTest implements Runnable
    {
        String text;
        int retVal;

        TimeoutTest(String text)
        {
            this.text = text;
        }

        public int getReturnValue()
        {
            return retVal;
        }

        public void run()
        {
            StaticAnalysis.resetErrors();
            ASTProgram program = null;
            try {
                program = (new MyDecafParser()).parse(
                        (new MyDecafLexer()).lex(text));
                program.traverse(new BuildParentLinks());
                program.traverse(new BuildSymbolTables());
                program.traverse(new MyDecafAnalysis());
                String errors = StaticAnalysis.getErrorString();
                if (errors.length() > 0) {
                    throw new InvalidProgramException(errors);
                }
            } catch (IOException ex) {
                assertTrue(false);
            } catch (InvalidTokenException ex) {
                assertTrue(false);
            } catch (InvalidSyntaxException ex) {
                assertTrue(false);
            } catch (InvalidProgramException ex) {
                assertTrue(false);
            }
            program.traverse(new AllocateSymbols());
            ILOCGenerator codegen = new MyILOCGenerator();
            program.traverse(codegen);
            (new RenumberRegistersAndLabels()).process(codegen.getProgram());
            (new MyLocalRegisterAllocator(NUM_PHYS_REGS)).process(codegen.getProgram());
            for (ILOCFunction func : codegen.getProgram().functions) {
                for (ILOCInstruction insn : func.instructions) {
                    for (ILOCOperand op : insn.operands) {
                        if (op.type == ILOCOperand.Type.VIRTUAL_REG && op.id >= NUM_PHYS_REGS) {
                            // too many registers!
                            assertTrue(false);
                        }
                    }
                }
            }
            ILOCInterpreter interp = new ILOCInterpreter();
            interp.process(codegen.getProgram());
            retVal = interp.getReturnValue();
        }
    }

    /**
     * Parse, analyze, and generate ILOC for the given Decaf source code. Also
     * runs the resulting ILOC in the interpreter and returns the result.
     * @param text Decaf source code
     * @return Integer program return value
     */
    protected static int runProgram(String text)
    {
        TimeoutTest test = new TimeoutTest(text);
        Thread t = new Thread(test);
        Instant start = Instant.now();
        t.start();
        while (t.isAlive()) {
            Instant curr = Instant.now();
            Duration len = Duration.between(start, curr);
            if (len.getSeconds() > 1) {
                t.interrupt();
                assertTrue(false);
            }
        }
        return test.getReturnValue();
    }

    /**
     * Wrapper for {@code runProgram()} for testing simple expressions
     * @param expr Decaf expression
     * @return Integer program return value
     */
    protected static int runExpr(String expr)
    {
        return runProgram("def int main() { return (" + expr + "); }");
    }

    public void testAssign() { assertEquals(runProgram(
                "def int main() { " +
                "  int a; a = 2 + 3 * 4; " +
                "  return a; }"),
            14); }
    /*
    public void testIf() { assertEquals(runProgram(
                "def int main() { " +
                "  if (true) { return 2+1; } " +
                "  else { return 3+1; } }"),
            3); }
   
    public void testWhile() { assertEquals(runProgram(
                "def int main() { " +
                "  int a; a = 0; " +
                "  while (a < 10) { a = a + 1; } " +
                "  return a; }"),
            10); }
    
    public void testFuncCall() { assertEquals(runProgram(
                "def int add(int a, int b) { return a + b; } " +
                "def int main() { return add(2,3); }"),
            5); }
     */
}
