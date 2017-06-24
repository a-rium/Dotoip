package misc;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.io.IOException;
import java.io.File;

import java.net.URL;
import java.net.URISyntaxException;

/** Classe di metodi statici che possono tornare utili */
public class Utils
{
	private static Border stdBorder;

	/** Crea un TitledBorder con il titolo specificato */
	public static TitledBorder createTitledBorder(String title)
	{
		if(stdBorder == null)
			stdBorder = BorderFactory.createEtchedBorder();

		return new TitledBorder(stdBorder, title);
	}

	/** Legge un immagine da file e la trasforma in un ImageIcon inseribile in una JLabel
	 *
	 *  @throws IOException se il file indicato non e' valido/non si hanno le permissioni/non esiste
	 */
	public static ImageIcon readIcon(String filename, int width, int height)
		throws IOException
	{
		BufferedImage img;
		System.out.println("Trying to load resource '" + "/"+filename + "'");
		URL jarFilepath = Utils.class.getResource("/"+filename);
		if(jarFilepath != null)
		{
			img = ImageIO.read(jarFilepath);
			System.out.println("Loaded from jar");
		}
		else
		{
			img = ImageIO.read(new File(filename));
		}
		Image scaled      = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

		return new ImageIcon(scaled);
	}
}
