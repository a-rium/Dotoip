import java.io.IOException;
import java.io.File;

import src.DomainTree;
import src.CacheServer;
import src.Resolver;
import src.RequestType;

import gui.MainWindow;

/** Classe contenente il punto di inizio dell'applicazione */
public class Main
{
  public static final String[] LocalDirectories = { "log/server", "log/resolver" };

  /** Punto di inizio dell'applicazione */
  public static void main(String[] args)
    throws IOException
  {
    buildFilesystemStructure(LocalDirectories);
    new MainWindow();
  }

  /** Metodo static che si assicura che le cartelle indicate da parametro esistano
  *  nella cartella corrente, e nel caso le crea.
  */
  private static void buildFilesystemStructure(String[] dirs)
  {
    for(String dir : dirs)
    {
      File explorer = new File(dir);
      if(!explorer.exists())
        explorer.mkdirs();
    }
  }
}
