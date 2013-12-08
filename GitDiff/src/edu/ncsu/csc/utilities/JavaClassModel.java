package edu.ncsu.csc.utilities;

import java.io.StringReader;
import java.util.ArrayList;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * Model representation of a Java Class.
 * 
 * @author Chris Stroud (clstroud@ncsu.edu)
 * @version 1.0.0
 */
public class JavaClassModel
{

    /** The QDox representation of the class object */
    private JavaClass                  classRep      = null;

    /** The methods declared in the class' source code */
    private ArrayList<JavaMethodModel> methodList    = new ArrayList<JavaMethodModel>();

    /** The class' fully qualified package string */
    private String                     packageString = null;

    /**
     * Constructs a new JavaClassModel with the given
     * source code blob
     * 
     * @param blob
     *            The code snippet from which the class is parsed
     */
    public JavaClassModel(String blob)
    {
        // Pass the code snippet (blob) to QDox so it can
        // hopefully parse the class into a usable class
        // "JavaClass" structure. Then use the parsed JavaClass
        // to get the parts we want converted for storage in
        // this model.

        JavaProjectBuilder builder = new JavaProjectBuilder();

        try {
            builder.addSource(new StringReader(blob));
        } catch (Exception e) {
            System.out.println("Failed to parse class:\n" + e);
            return;
        }

        this.classRep = (JavaClass) builder.getClasses().toArray()[0];

        this.packageString = this.classRep.getGenericFullyQualifiedName();

        for (JavaMethod aMethod : this.classRep.getMethods()) {
            addMethod(new JavaMethodModel(aMethod));
        }

    }

    /**
     * Gets the list of methods contained in this class
     * 
     * @return the list of methods
     */
    public ArrayList<JavaMethodModel> getMethodList()
    {
        return this.methodList;
    }

    /**
     * Add a method to this class' model
     * 
     * @param aMethod
     *            The method model
     */
    private void addMethod(JavaMethodModel aMethod)
    {
        this.methodList.add(aMethod);
    }

    /**
     * Returns the fully qualified package string for this class
     * 
     * @return The package string
     */
    public String getPackageName()
    {
        return this.packageString;
    }

    /**
     * Takes a line number and returns the method context
     * in which that line resides. If the line is not
     * within a method context, null will be returned.
     * 
     * @param line
     *            The line number to check against
     * 
     * @return The method signature String
     */
    public String methodSignatureForLine(int line)
    {
        for (JavaMethodModel method : this.methodList) {

            int index = method.getLineRange().index;
            int length = method.getLineRange().length;

            if (index <= line && line <= (index + length)) {
                return method.getMethodSignature();
            }
        }

        return null;
    }
}
