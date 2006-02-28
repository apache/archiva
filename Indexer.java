
package org.maven.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;


/**
 * Indexer utility used to create repository index 
 * for using in the repository search (e.g. from IDE plugins). 
 * 
 * @author Eugene Kuleshov
 */
public class Indexer {
  public static final String JAR_NAME = "j";
  public static final String JAR_SIZE = "s";
  public static final String JAR_DATE = "d";
  public static final String NAMES = "c";
  
  private static long totalClasses = 0;
  private static long totalFiles = 0;
  private static long totalSize = 0;
  
 
  public static void main( String[] args) throws IOException {
    if( args.length<2) {
      printUsage();
      return;
    }
    
    String command = args[ 0];
    if( "index".equals( command)) {
      String repositoryPath = args[ 1];  
      String indexPath = args.length==2 ? "index" : args[ 2];
      
      Indexer.reindex( new File( indexPath), repositoryPath);

//    } else if( "search".equals( command)) {
//      String query = args[ 1];
//      String indexPath = args.length==2 ? "index" : args[ 2];
//
//      indexer = new Indexer( new File[] { new File( indexPath)});
//      Map res = indexer.search( query, NAMES);
//      
//      for( Iterator it = res.entrySet().iterator(); it.hasNext();) {
//        Map.Entry e = ( Map.Entry) it.next();
//        System.err.println( e);
//      }
      System.err.println( "Not implemented yet");
    
    }
  }

  private static void printUsage() {
    System.err.println( "indexer <command> <args>");
    System.err.println( "  index <repository path> <index path>");
//    System.err.println( "  search <query> <index path>");
  }

  
  public static void reindex( File indexPath, String repositoryPath) throws IOException {
    Analyzer analyzer = new StandardAnalyzer();

    IndexWriter w = new IndexWriter( indexPath, analyzer, true);
    
    long l1 = System.currentTimeMillis();
    processDir(new File( repositoryPath), w, repositoryPath);
    long l2 = System.currentTimeMillis();
    System.err.println( "Done. "+((l2-l1)/1000f));
  
    long l3 = System.currentTimeMillis();
    System.err.println( "Optimizing...");
    w.optimize();
    w.close();
    long l4 = System.currentTimeMillis();
    System.err.println( "Done. "+((l4-l3)/1000f));
    
    System.err.println( "Total classes: " + totalClasses);
    System.err.println( "Total jars:    " + totalFiles);
    System.err.println( "Total size:    " + ( totalSize / 1024 / 1024)+" Mb");
    System.err.println( "Speed:         " + ( totalSize / ((l2-l1) / 1000f)) + " b/sec");
    
    createArchive( indexPath);
  }

  // TODO close all files and streams in try/finally
  private static void createArchive( File indexPath ) throws IOException {
    String name = indexPath.getName();
    ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( new File( indexPath.getParent(), name + ".zip" ) ) );
    zos.setLevel( 9 );

    File[] files = indexPath.listFiles();
    for( int i = 0; i < files.length; i++ ) {
      ZipEntry e = new ZipEntry( files[i].getName() );
      zos.putNextEntry( e );

      FileInputStream is = new FileInputStream( files[i] );
      byte[] buf = new byte[4096];
      int n;
      while( ( n = is.read( buf ) ) > 0 ) {
        zos.write( buf, 0, n );
      }
      is.close();
      zos.flush();

      zos.closeEntry();
    }

    zos.close();
  }

  private static void processDir( File dir, IndexWriter w, String repositoryPath) throws IOException {
    if(dir==null) return;

    File[] files = dir.listFiles();
    for( int i = 0; i < files.length; i++) {
      File f = files[ i];
      if(f.isDirectory()) processDir(f, w, repositoryPath);
      else processFile(f, w, repositoryPath);
    }
  }

  private static void processFile( File f, IndexWriter w, String repositoryPath) {
    if(f.isFile() && f.getName().endsWith( ".pom")) {  // TODO
      String name = f.getName();
      File jarFile = new File( f.getParent(), name.substring( 0, name.length() - 4) + ".jar");

      String absolutePath = f.getAbsolutePath();
      long size = 0;
      if( jarFile.exists()) {
        size = jarFile.length();
        absolutePath = jarFile.getAbsolutePath();
      }
      
      totalFiles++;
      totalSize += size;
      if(( totalFiles % 100)==0) {
        System.err.println( "Indexing "+totalFiles+" "+f.getParentFile().getAbsolutePath().substring( repositoryPath.length()));
      }

      Document doc = new Document();
      doc.add( Field.Text( JAR_NAME, absolutePath.substring( repositoryPath.length())));
      doc.add( Field.Text( JAR_DATE, DateField.timeToString( f.lastModified())));
      doc.add( Field.Text( JAR_SIZE, Long.toString(size)));
      // TODO calculate jar's sha1 or md5

//      ZipFile jar = null;
      try {
/*      
        jar = new ZipFile( f);
        
        StringBuffer sb = new StringBuffer();
        for( Enumeration en = jar.entries(); en.hasMoreElements();) {
          ZipEntry e = ( ZipEntry) en.nextElement();
          String name = e.getName();
          if( name.endsWith( ".class")) {
            totalClasses++;
            // TODO verify if class is public or protected
            // TODO skipp all inner classes for now
            int i = name.lastIndexOf( "$");
            if( i==-1) {
              sb.append( name.substring( 0, name.length() - 6)).append( "\n");
            }
          }
        }
        doc.add( Field.Text( NAMES, sb.toString()));
        

      } finally {
        try {
          jar.close();
        } catch( Exception e) {
        }
*/    
      w.addDocument(doc);
    } catch( Exception e) {
      System.err.println( "Error for file "+f);
      System.err.println( "  "+e.getMessage());
    }
    }
  }

}

