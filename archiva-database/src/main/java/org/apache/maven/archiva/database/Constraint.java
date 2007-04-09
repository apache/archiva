package org.apache.maven.archiva.database;

/**
 * Constraint 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Constraint
{
    public static final String ASCENDING = "ascending";

    public static final String DESCENDING = "descending";

    /**
     * Get the fetch limits on the object.
     * 
     * @return the fetch limits on the object. (can be null) (O/RM specific)
     */
    public String getFetchLimits();

    /**
     * Get the SELECT WHERE (condition) value for the constraint.
     * 
     * @return the equivalent of the SELECT WHERE (condition) value for this constraint. (can be null)
     */
    public String getWhereCondition();

    /**
     * Get the sort column name.
     * 
     * @return the sort column name. (can be null)
     */
    public String getSortColumn();

    /**
     * Get the sort direction name.
     * 
     * @return the sort direction name. ("ASC" or "DESC") (only valid if {@link #getSortColumn()} is specified.)
     */
    public String getSortDirection();
    
    /**
     * Get the declared imports used for this query. (optional)
     * 
     * NOTE: This is DAO implementation specific.
     * 
     * @return the imports. (can be null)
     */
    public String[] getDeclaredImports();
    
    /**
     * Get the declared parameters used for this query. (optional)
     * 
     * NOTE: This is DAO implementation specific.
     * 
     * @return the parameters. (can be null)
     */
    public String[] getDeclaredParameters();
    
    /**
     * Get the parameters used for this query. (required if using {@link #getDeclaredParameters()} )
     * 
     * NOTE: This is DAO implementation specific.
     * 
     * @return the parameters. (can be null)
     */
    public Object[] getParameters();
}
