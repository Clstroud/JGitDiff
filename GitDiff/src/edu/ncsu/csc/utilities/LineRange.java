package edu.ncsu.csc.utilities;

/**
 * Convenience class for storing a range structure.
 * 
 * In this context a range consists of a starting index
 * and a length for which the range encompasses.
 * 
 * @author Chris Stroud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class LineRange
{

    /** The starting index for the range */
    int index;

    /** The length of the range */
    int length;

    /**
     * Constructs a new range instance with the given
     * base index and length parameters.
     * 
     * @param idx
     *            The starting index of the range
     * @param len
     *            The length of the range
     */
    public LineRange(int idx, int len)
    {
        super();
        this.index = idx;
        this.length = len;
    }

    /**
     * Returns the index at which the range begins
     * 
     * @return the index
     */
    public int getIndex()
    {
        return this.index;
    }

    /**
     * Returns the length of the range
     * 
     * @return the length
     */
    public int getLength()
    {
        return this.length;
    }

}
