package edu.jmu.decaf;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * "Stub" Decaf lexer. Contains some utility functions for manipulating {@link
 * StringBuffer} objects using {@link Pattern} regular expressions.
 */
public abstract class DecafLexer
{
    private String savedMatch;
    private List<Pattern> ignorePatterns;
    private List<Pattern> tokenPatterns;
    private Map<Pattern, Token.Type> tokenTypes;

    /**
     * Generic constructor. Initializes data structures.
     */
    public DecafLexer()
    {
        savedMatch = null;
        ignorePatterns = new ArrayList<Pattern>();
        tokenPatterns = new ArrayList<Pattern>();
        tokenTypes = new HashMap<Pattern, Token.Type>();
    }

    /**
     * Perform lexical analysis, converting Decaf source code into a stream of
     * {@link Token} objects.
     *
     * @param text Input text string
     * @return Queue of lexed tokens
     * @throws IOException Thrown if there is an error reading from the input
     * @throws InvalidTokenException Thrown if an invalid token is encountered
     */
    public Queue<Token> lex(String text)
            throws IOException, InvalidTokenException
    {
        return lex(new BufferedReader(new StringReader(text)), "<none>");
    }

    /**
     * Perform lexical analysis, converting Decaf source code into a stream of
     * {@link Token} objects.
     *
     * @param file Input text file
     * @return Queue of lexed tokens
     * @throws IOException Thrown if there is an error reading from the input
     * @throws InvalidTokenException Thrown if an invalid token is encountered
     */
    public Queue<Token> lex(File file)
            throws IOException, InvalidTokenException
    {
        return lex(new BufferedReader(new FileReader(file)), file.getName());
    }

    /**
     * Perform lexical analysis, converting Decaf source code into a stream of
     * {@link Token} objects.
     *
     * @param input Input text stream
     * @return Queue of lexed tokens
     * @throws IOException Thrown if there is an error reading from the input
     * @throws InvalidTokenException Thrown if an invalid token is encountered
     */
    public Queue<Token> lex(InputStream input)
            throws IOException, InvalidTokenException
    {
        return lex(new BufferedReader(new InputStreamReader(input)), "<none>");
    }

    /**
     * Perform lexical analysis, converting Decaf source code into a stream of
     * {@link Token} objects. This is the form of the method that should be
     * overridden in actual implementations; the other forms of this method
     * just call this one.
     *
     * @param input Input text reader
     * @param filename Source file name (for source info)
     * @return Queue of lexed tokens
     * @throws IOException Thrown if there is an error reading from the input
     * @throws InvalidTokenException Thrown if an invalid token is encountered
     */
    public Queue<Token> lex(BufferedReader input, String filename)
            throws IOException, InvalidTokenException
    {
        return new ArrayDeque<Token>();
    }

    /**
     * Test the given regular expression pattern against the beginning of the
     * string buffer. If the regex matches, the matching portion is deleted from
     * the buffer and saved for later retrieval via the {@link
     * DecafLexer#getLastMatch} method. Note that the matching is performed
     * greedily.
     *
     * @param pattern Regular expression
     * @param text Target text
     * @return A boolean indicating whether the pattern was matched
     */
    public boolean extract(Pattern pattern, StringBuffer text)
    {
        Matcher matcher = pattern.matcher(text);
        if (matcher.lookingAt()) {
            savedMatch = matcher.group();
            text.delete(matcher.start(), matcher.end());
            return true;
        }
        return false;
    }

    /**
     * Return the most recent text match extracted using {@link
     * DecafLexer#extract}.
     *
     * @return Last match
     */
    public String getLastMatch()
    {
        return savedMatch;
    }

    /**
     * Add a regular expression that describes an ignored token. Any text
     * matching this regex at the beginning will be discarded when {@link
     * DecafLexer#discardIgnored} is called.
     *
     * @param regex Regular expression string
     */
    public void addIgnoredPattern(String regex)
    {
        ignorePatterns.add(Pattern.compile(regex));
    }

    /**
     * Discard any ignored text. Check all regexes registered with {@link
     * DecafLexer#addIgnoredPattern} one at a time, discarding any text matched.
     *
     * @param text Target text
     */
    public void discardIgnored(StringBuffer text)
    {
        for (Pattern p : ignorePatterns) {
            Matcher matcher = p.matcher(text);
            if (matcher.lookingAt()) {
                text.delete(matcher.start(), matcher.end());
            }
        }
    }

    /**
     * Add a regular expression that describes a particular type of token.
     * These patterns are used by {@link DecafLexer#nextToken} to extract tokens
     * from the beginning of a StringBuffer. The order in which this function
     * is called is significant; patterns that are registered earlier will be
     * matched first.
     *
     * @param type Token type
     * @param regex Regular expression string
     */
    public void addTokenPattern(Token.Type type, String regex)
    {
        // match at beginning of line
        Pattern p = Pattern.compile(regex);
        tokenPatterns.add(p);
        tokenTypes.put(p, type);
    }

    /**
     * Extract a token from the given buffer. Checks all regexes registered with
     * {@link DecafLexer#addTokenPattern} one at a time, returning the next
     * token matched. The patterns are matched in the order that they were
     * added.
     *
     * @param text Target text
     * @return The new token, or null if there is no valid token
     */
    public Token nextToken(StringBuffer text)
    {
        for (Pattern p : tokenPatterns) {
            Matcher matcher = p.matcher(text);
            if (matcher.lookingAt()) {
                String tstr = matcher.group();
                text.delete(matcher.start(), matcher.end());
                return new Token(tokenTypes.get(p), tstr);
            }
        }
        return null;
    }
}

