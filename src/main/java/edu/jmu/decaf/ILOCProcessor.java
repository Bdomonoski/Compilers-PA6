package edu.jmu.decaf;

/**
 * All ILOC processing passes (static analysis, optimization, machine code
 * generation, etc.) should implement this (admittedly simple) standardized
 * interface.
 */
public interface ILOCProcessor
{
    /**
     * Perform analysis and/or modification on the given ILOC program
     * @param program {@link ILOCProgram} to process
     */
    public void process(ILOCProgram program);
}
