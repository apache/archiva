package org.apache.maven.archiva.reporting;

/**
 * Limits on how much data should be returned by the report sources.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DataLimits
{
    private int currentPage = 0;

    private int perPageCount = 25;

    private int countOfPages = 1;

    private int totalCount = 0;

    public int getCountOfPages()
    {
        return countOfPages;
    }

    public void setCountOfPages( int countOfPages )
    {
        this.countOfPages = countOfPages;
    }

    public int getCurrentPage()
    {
        return currentPage;
    }

    public void setCurrentPage( int currentPage )
    {
        this.currentPage = currentPage;
    }

    public int getPerPageCount()
    {
        return perPageCount;
    }

    public void setPerPageCount( int perPageCount )
    {
        this.perPageCount = perPageCount;
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount( int totalCount )
    {
        this.totalCount = totalCount;
    }
}
