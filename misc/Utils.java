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


public class Utils
{
	private static Border stdBorder;

	public static TitledBorder createTitledBorder(String title)
	{
		if(stdBorder == null)
			stdBorder = BorderFactory.createEtchedBorder();

		return new TitledBorder(stdBorder, title);
	}

	public static ImageIcon readIcon(String filename, int width, int height)
		throws IOException
	{
		BufferedImage img = ImageIO.read(new File(filename));
		Image scaled      = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

		return new ImageIcon(scaled);
	}
}
