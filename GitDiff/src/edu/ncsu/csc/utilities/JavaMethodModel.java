package edu.ncsu.csc.utilities;

import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaType;

/**
 * Model representation of a given Java method.
 * 
 * @author Chris Stroud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class JavaMethodModel
{

    /** The signature String of the method */
    private String    methodSignature = "";

    /** The range of lines the method encompasses */
    private LineRange linesRange      = null;

    /**
     * Constructs a new JavaMethodModel with the given
     * QDox Java method object as its data-source.
     * 
     * @param aMethod
     *            The QDox model object
     */
    public JavaMethodModel(final JavaMethod aMethod)
    {
        // Use a StringBuilder to construct the formatted signature,
        // starting by removing the existing parameter list (they are
        // fully qualified, which is too verbose) and appending our
        // own, more concise version.
        //
        // Then get the line count the old fashioned way
        String noParams = aMethod.getCallSignature().replaceAll("\\(.*\\)", "");
        StringBuilder sigBuilder = new StringBuilder(noParams);

        sigBuilder.append("(");

        for (JavaType param : aMethod.getParameterTypes()) {
            sigBuilder.append(param.getValue() + ", ");
        }

        int idx = sigBuilder.lastIndexOf(",");
        if (idx != -1) {
            this.methodSignature = sigBuilder.substring(0, idx).replaceAll("[,]?\\s*$", "").concat(")");
        } else {
            this.methodSignature = sigBuilder.toString().replaceAll("[,]?\\s*$", "").concat(")");
        }

        int lineCount = aMethod.getCodeBlock().split("(?:\r\n|[\r\n])").length + 1;
        this.linesRange = new LineRange(aMethod.getLineNumber(), lineCount);
    }

    /**
     * Returns the fully qualified method signature String for the method
     * 
     * @return The method signature String
     */
    public String getMethodSignature()
    {
        return this.methodSignature;
    }

    /**
     * Returns the appropriate LineRange for the method
     * 
     * @return The LineRange corresponding for the method
     */
    public LineRange getLineRange()
    {
        return this.linesRange;
    }

}
