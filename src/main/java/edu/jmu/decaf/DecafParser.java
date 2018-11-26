package edu.jmu.decaf;

import java.util.*;

/**
 * "Stub" Decaf parser. Contains utility methods useful for manipulating
 * token lists.
 */
public abstract class DecafParser
{
    public DecafParser() { }

    /**
     * Perform syntax analysis, converting a stream of {@link Token} objects
     * into an {@link ASTProgram}.
     *
     * @param tokens Input token queue
     * @return Parsed abstract syntax tree (AST)
     * @throws InvalidSyntaxException Thrown if a syntax error is encountered
     */
    public ASTProgram parse(Queue<Token> tokens) throws InvalidSyntaxException
    {
        return new ASTProgram();
    }

    /**
     * Match and discard the given symbol from the head of the token queue. If
     * the token is not present, it throws an exception. This is intended for
     * use in LL(1) parsing routines.
     *
     * @param tokens Input token queue
     * @param symbol Expected symbol text
     * @throws InvalidSyntaxException Thrown if the queue is empty or the token
     * does not match
     */
    public void matchSymbol(Queue<Token> tokens, String symbol)
            throws InvalidSyntaxException
    {
        if (tokens.size() == 0) {
            throw new InvalidSyntaxException("Symbol \"" + symbol +
                    "\" expected but not found at end of input");
        }
        Token peek = tokens.peek();
        if (peek.type == Token.Type.SYM && peek.text.equals(symbol)) {
            consumeNextToken(tokens);
        } else {
            throw new InvalidSyntaxException("Symbol \"" + symbol +
                    "\" expected but not found at " +
                    getCurrentSourceInfo(tokens).toString());
        }
    }

    /**
     * Match and discard the given keyword from the head of the token queue. If
     * the token is not present, it throws an exception. This is intended for
     * use in LL(1) parsing routines.
     *
     * @param tokens Input token queue
     * @param keyword Expected keyword text
     * @throws InvalidSyntaxException Thrown if the queue is empty or the token
     * does not match
     */
    public void matchKeyword(Queue<Token> tokens, String keyword)
            throws InvalidSyntaxException
    {
        if (tokens.size() == 0) {
            throw new InvalidSyntaxException("Keyword \"" + keyword +
                    "\" expected but not found at end of input");
        }
        Token peek = tokens.peek();
        if (peek.type == Token.Type.KEY && peek.text.equals(keyword)) {
            consumeNextToken(tokens);
        } else {
            throw new InvalidSyntaxException("Keyword \"" + keyword +
                    "\" expected but not found at " +
                    getCurrentSourceInfo(tokens).toString());
        }
    }

    /**
     * Returns true if and only if the head of the queue is a token of the
     * expected type.
     *
     * @param tokens Input token queue
     * @param type Expected token type
     * @return Boolean indicating whether the next token matches
     */
    public boolean isNextToken(Queue<Token> tokens, Token.Type type)
    {
        Token peek = tokens.peek();
        if (peek != null && peek.type == type) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if and only if the head of the queue is a symbol token with
     * the expected text.
     *
     * @param tokens Input token queue
     * @param symbol Expected symbol text
     * @return Boolean indicating whether the next token matches
     */
    public boolean isNextTokenSymbol(Queue<Token> tokens, String symbol)
    {
        Token peek = tokens.peek();
        if (peek != null && peek.type == Token.Type.SYM && peek.text.equals(symbol)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if and only if the head of the queue is a keyword token with
     * the expected text.
     *
     * @param tokens Input token queue
     * @param keyword Expected keyword text
     * @return Boolean indicating whether the next token matches
     */
    public boolean isNextTokenKeyword(Queue<Token> tokens, String keyword)
    {
        Token peek = tokens.peek();
        if (peek != null && peek.type == Token.Type.KEY && peek.text.equals(keyword)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Discard the next token. Throws an exception if the queue is empty.
     *
     * @param tokens Input token queue
     * @throws InvalidSyntaxException Thrown if the queue is empty
     */
    public void consumeNextToken(Queue<Token> tokens)
            throws InvalidSyntaxException
    {
        if (tokens.size() == 0) {
            throw new InvalidSyntaxException("Unexpected end of input");
        }
        tokens.remove();
    }

    /**
     * Retrieve the source info associated with the token at the head of the
     * queue. If the queue is empty, it returns the INVALID constant.
     *
     * @param tokens Input token queue
     * @return Source info for the head of the queue
     */
    public SourceInfo getCurrentSourceInfo(Queue<Token> tokens)
    {
        if (tokens.size() > 0) {
            return tokens.peek().source;
        } else {
            return SourceInfo.INVALID;
        }
    }
}

